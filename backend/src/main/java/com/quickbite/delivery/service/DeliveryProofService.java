package com.quickbite.delivery.service;

import com.quickbite.common.feature.FeatureFlagService;
import com.quickbite.delivery.dto.DeliveryProofResponseDTO;
import com.quickbite.delivery.entity.DeliveryProof;
import com.quickbite.delivery.entity.ProofType;
import com.quickbite.delivery.repository.DeliveryProofRepository;
import com.quickbite.notifications.entity.NotificationType;
import com.quickbite.notifications.service.NotificationService;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.exception.BusinessException;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.orders.service.EventTimelineService;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for delivery proof-of-delivery workflows:
 * - Photo proof submission
 * - OTP generation & verification
 * - Feature-flag gating for required proof types
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryProofService {

    private final DeliveryProofRepository proofRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final FeatureFlagService featureFlagService;
    private final NotificationService notificationService;
    private final EventTimelineService eventTimelineService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;

    // ── Photo Proof ──────────────────────────────────────

    /**
     * Submit photo proof of delivery.
     *
     * @param orderId order UUID
     * @param driverId driver (current user) UUID
     * @param photo multipart photo file
     * @param notes optional delivery notes
     * @param lat GPS latitude at delivery
     * @param lng GPS longitude at delivery
     * @return proof response DTO
     */
    @Transactional
    public DeliveryProofResponseDTO submitPhotoProof(UUID orderId, UUID driverId,
                                                      MultipartFile photo,
                                                      String notes,
                                                      BigDecimal lat, BigDecimal lng) {
        if (!featureFlagService.isEnabled("proof-of-delivery")) {
            throw new BusinessException("Proof-of-delivery feature is disabled");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found: " + orderId));

        validateDriverOwnership(order, driverId);
        validateOrderForProof(order);

        // Prevent duplicate photo proof
        if (proofRepository.existsByOrderId(orderId)) {
            throw new BusinessException("Delivery proof already submitted for this order");
        }

        // Store photo
        String photoUrl = fileStorageService.store(photo, orderId.toString());

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException("Driver not found: " + driverId));

        DeliveryProof proof = DeliveryProof.builder()
                .order(order)
                .driver(driver)
                .proofType(ProofType.PHOTO)
                .photoUrl(photoUrl)
                .notes(notes)
                .lat(lat)
                .lng(lng)
                .submittedAt(OffsetDateTime.now())
                .build();

        proof = proofRepository.save(proof);
        log.info("Photo proof submitted for order {} by driver {}", orderId, driverId);

        // Record timeline event
        eventTimelineService.recordEvent(orderId, driverId, "PROOF_PHOTO_SUBMITTED",
                Map.of("proofId", proof.getId().toString(),
                        "photoUrl", photoUrl));

        // Notify customer
        notificationService.createNotification(
                order.getCustomer().getId(),
                NotificationType.ORDER_UPDATE,
                "Delivery Proof Submitted",
                "Your driver has submitted proof of delivery for order " + order.getOrderNumber(),
                orderId
        );

        return toDTO(proof);
    }

    // ── OTP Flow ─────────────────────────────────────────

    /**
     * Generate a 6-digit OTP for delivery confirmation.
     * The OTP is stored on the proof record and should be shown to the customer.
     */
    @Transactional
    public DeliveryProofResponseDTO generateOtp(UUID orderId, UUID driverId) {
        if (!featureFlagService.isEnabled("delivery-otp-required")) {
            throw new BusinessException("Delivery OTP feature is disabled");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found: " + orderId));

        validateDriverOwnership(order, driverId);
        validateOrderForProof(order);

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException("Driver not found: " + driverId));

        String otpCode = generateOtpCode();

        // Create or update OTP proof record
        DeliveryProof proof = proofRepository.findByOrderId(orderId)
                .map(existing -> {
                    existing.setOtpCode(otpCode);
                    existing.setOtpVerified(false);
                    return existing;
                })
                .orElse(DeliveryProof.builder()
                        .order(order)
                        .driver(driver)
                        .proofType(ProofType.OTP)
                        .otpCode(otpCode)
                        .otpVerified(false)
                        .submittedAt(OffsetDateTime.now())
                        .build());

        proof = proofRepository.save(proof);
        log.info("OTP generated for order {} by driver {}", orderId, driverId);

        // Notify customer with OTP (in a real app, send via SMS/push)
        notificationService.createNotification(
                order.getCustomer().getId(),
                NotificationType.ORDER_UPDATE,
                "Delivery OTP",
                "Your delivery OTP is: " + otpCode + ". Share it with your driver to confirm delivery.",
                orderId
        );

        // Record timeline
        eventTimelineService.recordEvent(orderId, driverId, "PROOF_OTP_GENERATED",
                Map.of("proofId", proof.getId().toString()));

        return toDTO(proof);
    }

    /**
     * Verify the delivery OTP entered by the driver.
     */
    @Transactional
    public DeliveryProofResponseDTO verifyOtp(UUID orderId, UUID driverId, String code) {
        DeliveryProof proof = proofRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("No OTP generated for this order"));

        validateDriverOwnership(proof.getOrder(), driverId);

        if (proof.getOtpCode() == null) {
            throw new BusinessException("No OTP generated for this order");
        }

        if (Boolean.TRUE.equals(proof.getOtpVerified())) {
            throw new BusinessException("OTP already verified");
        }

        if (!proof.getOtpCode().equals(code)) {
            log.warn("Invalid OTP attempt for order {} by driver {}", orderId, driverId);
            throw new BusinessException("Invalid OTP code");
        }

        proof.setOtpVerified(true);
        proof = proofRepository.save(proof);
        log.info("OTP verified for order {} by driver {}", orderId, driverId);

        // Record timeline
        eventTimelineService.recordEvent(orderId, driverId, "PROOF_OTP_VERIFIED",
                Map.of("proofId", proof.getId().toString()));

        // Notify customer
        notificationService.createNotification(
                proof.getOrder().getCustomer().getId(),
                NotificationType.ORDER_UPDATE,
                "Delivery Confirmed",
                "Your delivery has been confirmed via OTP verification.",
                orderId
        );

        return toDTO(proof);
    }

    // ── Query ────────────────────────────────────────────

    /**
     * Get proof for an order (if any).
     */
    @Transactional(readOnly = true)
    public Optional<DeliveryProofResponseDTO> getProofByOrderId(UUID orderId) {
        return proofRepository.findByOrderId(orderId).map(this::toDTO);
    }

    /**
     * Check if the order requires proof before marking delivered.
     * Returns true if any proof feature flag is enabled and no proof is yet submitted.
     */
    public boolean isProofRequired(UUID orderId) {
        boolean photoRequired = featureFlagService.isEnabled("delivery-photo-required");
        boolean otpRequired = featureFlagService.isEnabled("delivery-otp-required");

        if (!photoRequired && !otpRequired) return false;

        Optional<DeliveryProof> existing = proofRepository.findByOrderId(orderId);
        if (existing.isEmpty()) return true;

        DeliveryProof proof = existing.get();
        if (photoRequired && proof.getPhotoUrl() == null) return true;
        if (otpRequired && !Boolean.TRUE.equals(proof.getOtpVerified())) return true;

        return false;
    }

    // ── Helpers ──────────────────────────────────────────

    private void validateDriverOwnership(Order order, UUID driverId) {
        if (order.getDriver() == null || !order.getDriver().getId().equals(driverId)) {
            throw new BusinessException("You are not assigned to this order");
        }
    }

    private void validateOrderForProof(Order order) {
        OrderStatus status = order.getStatus();
        // Proof can be submitted when order is PICKED_UP, ENROUTE, or just before DELIVERED
        if (status != OrderStatus.PICKED_UP && status != OrderStatus.ENROUTE) {
            throw new BusinessException("Proof can only be submitted for orders in PICKED_UP or ENROUTE status");
        }
    }

    private Order order(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found: " + orderId));
    }

    private String generateOtpCode() {
        int code = RANDOM.nextInt(900000) + 100000; // 100000-999999
        return String.valueOf(code);
    }

    private DeliveryProofResponseDTO toDTO(DeliveryProof proof) {
        return DeliveryProofResponseDTO.builder()
                .id(proof.getId())
                .orderId(proof.getOrder().getId())
                .driverId(proof.getDriver().getId())
                .proofType(proof.getProofType().name())
                .photoUrl(proof.getPhotoUrl())
                .otpVerified(proof.getOtpVerified())
                .notes(proof.getNotes())
                .lat(proof.getLat())
                .lng(proof.getLng())
                .submittedAt(proof.getSubmittedAt())
                .createdAt(proof.getCreatedAt())
                .build();
    }
}
