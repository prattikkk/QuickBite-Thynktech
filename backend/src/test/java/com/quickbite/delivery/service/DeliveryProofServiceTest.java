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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeliveryProofService — Phase 3 proof-of-delivery.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryProofServiceTest {

    @Mock private DeliveryProofRepository proofRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private FeatureFlagService featureFlagService;
    @Mock private NotificationService notificationService;
    @Mock private EventTimelineService eventTimelineService;

    private DeliveryProofService proofService;

    private UUID orderId;
    private UUID driverId;
    private UUID customerId;
    private Order order;
    private User driver;
    private User customer;

    @BeforeEach
    void setUp() {
        proofService = new DeliveryProofService(
                proofRepository, orderRepository, userRepository,
                fileStorageService, featureFlagService,
                notificationService, eventTimelineService
        );

        orderId = UUID.randomUUID();
        driverId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        customer = User.builder().id(customerId).name("Customer").email("cust@test.com").build();
        driver = User.builder().id(driverId).name("Driver").email("driver@test.com").build();
        order = Order.builder()
                .id(orderId)
                .orderNumber("QB-001")
                .customer(customer)
                .driver(driver)
                .status(OrderStatus.ENROUTE)
                .build();
    }

    // ── Photo Proof Tests ────────────────────────────────

    @Nested
    @DisplayName("submitPhotoProof")
    class SubmitPhotoProofTests {

        @Test
        @DisplayName("success — stores photo and returns DTO")
        void submitPhotoProof_success() {
            when(featureFlagService.isEnabled("proof-of-delivery")).thenReturn(true);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(proofRepository.existsByOrderId(orderId)).thenReturn(false);
            when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
            when(fileStorageService.store(any(), eq(orderId.toString())))
                    .thenReturn("/uploads/proofs/" + orderId + "/photo.jpg");

            DeliveryProof saved = DeliveryProof.builder()
                    .id(UUID.randomUUID())
                    .order(order).driver(driver)
                    .proofType(ProofType.PHOTO)
                    .photoUrl("/uploads/proofs/" + orderId + "/photo.jpg")
                    .notes("Left at door")
                    .lat(BigDecimal.valueOf(28.6139))
                    .lng(BigDecimal.valueOf(77.2090))
                    .submittedAt(OffsetDateTime.now())
                    .createdAt(OffsetDateTime.now())
                    .build();
            when(proofRepository.save(any())).thenReturn(saved);

            MultipartFile mockFile = mock(MultipartFile.class);

            DeliveryProofResponseDTO result = proofService.submitPhotoProof(
                    orderId, driverId, mockFile, "Left at door",
                    BigDecimal.valueOf(28.6139), BigDecimal.valueOf(77.2090));

            assertThat(result).isNotNull();
            assertThat(result.getProofType()).isEqualTo("PHOTO");
            assertThat(result.getPhotoUrl()).contains("photo.jpg");
            assertThat(result.getNotes()).isEqualTo("Left at door");

            verify(fileStorageService).store(mockFile, orderId.toString());
            verify(proofRepository).save(any());
            verify(notificationService).createNotification(
                    eq(customerId), eq(NotificationType.ORDER_UPDATE),
                    anyString(), anyString(), eq(orderId));
            verify(eventTimelineService).recordEvent(eq(orderId), eq(driverId),
                    eq("PROOF_PHOTO_SUBMITTED"), anyMap());
        }

        @Test
        @DisplayName("throws when feature disabled")
        void submitPhotoProof_featureDisabled() {
            when(featureFlagService.isEnabled("proof-of-delivery")).thenReturn(false);

            assertThatThrownBy(() ->
                    proofService.submitPhotoProof(orderId, driverId, mock(MultipartFile.class),
                            null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("disabled");
        }

        @Test
        @DisplayName("throws when driver not assigned to order")
        void submitPhotoProof_wrongDriver() {
            when(featureFlagService.isEnabled("proof-of-delivery")).thenReturn(true);
            Order otherOrder = Order.builder()
                    .id(orderId)
                    .customer(customer)
                    .driver(User.builder().id(UUID.randomUUID()).build())
                    .status(OrderStatus.ENROUTE)
                    .build();
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(otherOrder));

            assertThatThrownBy(() ->
                    proofService.submitPhotoProof(orderId, driverId, mock(MultipartFile.class),
                            null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not assigned");
        }

        @Test
        @DisplayName("throws when order in wrong status")
        void submitPhotoProof_wrongStatus() {
            when(featureFlagService.isEnabled("proof-of-delivery")).thenReturn(true);
            order.setStatus(OrderStatus.PLACED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() ->
                    proofService.submitPhotoProof(orderId, driverId, mock(MultipartFile.class),
                            null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PICKED_UP or ENROUTE");
        }

        @Test
        @DisplayName("throws when proof already submitted")
        void submitPhotoProof_duplicate() {
            when(featureFlagService.isEnabled("proof-of-delivery")).thenReturn(true);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(proofRepository.existsByOrderId(orderId)).thenReturn(true);

            assertThatThrownBy(() ->
                    proofService.submitPhotoProof(orderId, driverId, mock(MultipartFile.class),
                            null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already submitted");
        }
    }

    // ── OTP Tests ────────────────────────────────────────

    @Nested
    @DisplayName("OTP flow")
    class OtpTests {

        @Test
        @DisplayName("generateOtp — creates record and notifies customer")
        void generateOtp_success() {
            when(featureFlagService.isEnabled("delivery-otp-required")).thenReturn(true);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
            when(proofRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

            DeliveryProof saved = DeliveryProof.builder()
                    .id(UUID.randomUUID())
                    .order(order).driver(driver)
                    .proofType(ProofType.OTP)
                    .otpCode("123456")
                    .otpVerified(false)
                    .submittedAt(OffsetDateTime.now())
                    .createdAt(OffsetDateTime.now())
                    .build();
            when(proofRepository.save(any())).thenReturn(saved);

            DeliveryProofResponseDTO result = proofService.generateOtp(orderId, driverId);

            assertThat(result).isNotNull();
            assertThat(result.getProofType()).isEqualTo("OTP");
            assertThat(result.getOtpVerified()).isFalse();

            verify(notificationService).createNotification(
                    eq(customerId), eq(NotificationType.ORDER_UPDATE),
                    eq("Delivery OTP"), contains("OTP"), eq(orderId));
        }

        @Test
        @DisplayName("generateOtp — throws when feature disabled")
        void generateOtp_featureDisabled() {
            when(featureFlagService.isEnabled("delivery-otp-required")).thenReturn(false);

            assertThatThrownBy(() -> proofService.generateOtp(orderId, driverId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("disabled");
        }

        @Test
        @DisplayName("verifyOtp — success with correct code")
        void verifyOtp_success() {
            DeliveryProof existing = DeliveryProof.builder()
                    .id(UUID.randomUUID())
                    .order(order).driver(driver)
                    .proofType(ProofType.OTP)
                    .otpCode("654321")
                    .otpVerified(false)
                    .submittedAt(OffsetDateTime.now())
                    .createdAt(OffsetDateTime.now())
                    .build();
            when(proofRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));
            when(proofRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DeliveryProofResponseDTO result = proofService.verifyOtp(orderId, driverId, "654321");

            assertThat(result.getOtpVerified()).isTrue();
            verify(eventTimelineService).recordEvent(eq(orderId), eq(driverId),
                    eq("PROOF_OTP_VERIFIED"), anyMap());
        }

        @Test
        @DisplayName("verifyOtp — throws on wrong code")
        void verifyOtp_wrongCode() {
            DeliveryProof existing = DeliveryProof.builder()
                    .id(UUID.randomUUID())
                    .order(order).driver(driver)
                    .proofType(ProofType.OTP)
                    .otpCode("654321")
                    .otpVerified(false)
                    .submittedAt(OffsetDateTime.now())
                    .createdAt(OffsetDateTime.now())
                    .build();
            when(proofRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> proofService.verifyOtp(orderId, driverId, "000000"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid OTP");
        }

        @Test
        @DisplayName("verifyOtp — throws if already verified")
        void verifyOtp_alreadyVerified() {
            DeliveryProof existing = DeliveryProof.builder()
                    .id(UUID.randomUUID())
                    .order(order).driver(driver)
                    .proofType(ProofType.OTP)
                    .otpCode("654321")
                    .otpVerified(true)
                    .submittedAt(OffsetDateTime.now())
                    .createdAt(OffsetDateTime.now())
                    .build();
            when(proofRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> proofService.verifyOtp(orderId, driverId, "654321"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already verified");
        }
    }

    // ── isProofRequired ──────────────────────────────────

    @Nested
    @DisplayName("isProofRequired")
    class ProofRequiredTests {

        @Test
        @DisplayName("returns false when all feature flags disabled")
        void noFeaturesEnabled() {
            when(featureFlagService.isEnabled("delivery-photo-required")).thenReturn(false);
            when(featureFlagService.isEnabled("delivery-otp-required")).thenReturn(false);

            assertThat(proofService.isProofRequired(orderId)).isFalse();
        }

        @Test
        @DisplayName("returns true when photo required and no proof exists")
        void photoRequired_noProof() {
            when(featureFlagService.isEnabled("delivery-photo-required")).thenReturn(true);
            when(featureFlagService.isEnabled("delivery-otp-required")).thenReturn(false);
            when(proofRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

            assertThat(proofService.isProofRequired(orderId)).isTrue();
        }

        @Test
        @DisplayName("returns false when photo required and proof with photo exists")
        void photoRequired_hasProof() {
            when(featureFlagService.isEnabled("delivery-photo-required")).thenReturn(true);
            when(featureFlagService.isEnabled("delivery-otp-required")).thenReturn(false);
            DeliveryProof proof = DeliveryProof.builder()
                    .photoUrl("/uploads/proofs/photo.jpg")
                    .otpVerified(false)
                    .build();
            when(proofRepository.findByOrderId(orderId)).thenReturn(Optional.of(proof));

            assertThat(proofService.isProofRequired(orderId)).isFalse();
        }

        @Test
        @DisplayName("returns true when OTP required and not verified")
        void otpRequired_notVerified() {
            when(featureFlagService.isEnabled("delivery-photo-required")).thenReturn(false);
            when(featureFlagService.isEnabled("delivery-otp-required")).thenReturn(true);
            DeliveryProof proof = DeliveryProof.builder()
                    .otpVerified(false)
                    .build();
            when(proofRepository.findByOrderId(orderId)).thenReturn(Optional.of(proof));

            assertThat(proofService.isProofRequired(orderId)).isTrue();
        }
    }

    // ── getProofByOrderId ────────────────────────────────

    @Test
    @DisplayName("getProofByOrderId — returns empty when no proof exists")
    void getProofByOrderId_empty() {
        when(proofRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        assertThat(proofService.getProofByOrderId(orderId)).isEmpty();
    }

    @Test
    @DisplayName("getProofByOrderId — returns DTO when proof exists")
    void getProofByOrderId_found() {
        DeliveryProof proof = DeliveryProof.builder()
                .id(UUID.randomUUID())
                .order(order).driver(driver)
                .proofType(ProofType.PHOTO)
                .photoUrl("/uploads/proofs/photo.jpg")
                .otpVerified(false)
                .submittedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
        when(proofRepository.findByOrderId(orderId)).thenReturn(Optional.of(proof));

        var result = proofService.getProofByOrderId(orderId);
        assertThat(result).isPresent();
        assertThat(result.get().getProofType()).isEqualTo("PHOTO");
    }
}
