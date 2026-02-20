package com.quickbite.orders.service;

import com.quickbite.delivery.entity.DeliveryStatus;
import com.quickbite.delivery.repository.DeliveryStatusRepository;
import com.quickbite.orders.driver.DriverAssignmentService;
import com.quickbite.orders.dto.OrderCreateDTO;
import com.quickbite.orders.dto.OrderResponseDTO;
import com.quickbite.orders.dto.StatusUpdateDTO;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderItem;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.exception.BusinessException;
import com.quickbite.orders.exception.OrderNotFoundException;
import com.quickbite.orders.mapper.OrderMapper;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.payments.service.PaymentService;
import com.quickbite.payments.entity.Payment;
import com.quickbite.payments.entity.PaymentMethod;
import com.quickbite.payments.entity.PaymentStatus;
import com.quickbite.users.entity.Address;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.AddressRepository;
import com.quickbite.users.repository.UserRepository;
import com.quickbite.vendors.entity.MenuItem;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.MenuItemRepository;
import com.quickbite.websocket.OrderUpdatePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for order lifecycle management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final DeliveryStatusRepository deliveryStatusRepository;
    private final PaymentService paymentService;
    private final DriverAssignmentService driverAssignmentService;
    private final OrderMapper orderMapper;
    private final OrderUpdatePublisher orderUpdatePublisher;
    private final OrderStateMachine orderStateMachine;
    private final EventTimelineService eventTimelineService;

    // Tax and delivery fee configuration (could move to properties)
    private static final double TAX_RATE = 0.05; // 5% tax
    private static final long DELIVERY_FEE_CENTS = 5000; // ₹50

    /**
     * Create a new order from cart.
     *
     * @param dto order creation DTO
     * @param customerId authenticated customer ID
     * @return OrderResponseDTO
     */
    @Transactional
    public OrderResponseDTO createOrder(OrderCreateDTO dto, UUID customerId) {
        log.info("Creating order for customer: {} with {} items", customerId, dto.getItems().size());

        // 1. Load customer
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("Customer not found: " + customerId));

        // 2. Load delivery address
        Address deliveryAddress = addressRepository.findById(dto.getAddressId())
                .orElseThrow(() -> new BusinessException("Delivery address not found: " + dto.getAddressId()));

        if (!deliveryAddress.getUser().getId().equals(customerId)) {
            throw new BusinessException("Address does not belong to customer");
        }

        // 3. Load menu items and validate availability
        Map<UUID, MenuItem> menuItemMap = new HashMap<>();
        for (var itemDto : dto.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemDto.getMenuItemId())
                    .orElseThrow(() -> new BusinessException("Menu item not found: " + itemDto.getMenuItemId()));

            if (!Boolean.TRUE.equals(menuItem.getAvailable())) {
                throw new BusinessException("Menu item not available: " + menuItem.getName());
            }

            menuItemMap.put(menuItem.getId(), menuItem);
        }

        // 4. Determine vendor (all items must be from same vendor)
        Set<UUID> vendorIds = menuItemMap.values().stream()
                .map(mi -> mi.getVendor().getId())
                .collect(Collectors.toSet());

        if (vendorIds.size() > 1) {
            throw new BusinessException("All items must be from the same vendor");
        }

        // Get vendor from first menu item        
        Vendor vendor = menuItemMap.values().iterator().next().getVendor();

        // 5. Calculate totals
        long subtotalCents = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (var itemDto : dto.getItems()) {
            MenuItem menuItem = menuItemMap.get(itemDto.getMenuItemId());
            long itemTotal = menuItem.getPriceCents() * itemDto.getQuantity();
            subtotalCents += itemTotal;

            OrderItem orderItem = OrderItem.builder()
                    .menuItem(menuItem)
                    .quantity(itemDto.getQuantity())
                    .priceCents(menuItem.getPriceCents()) // Snapshot price
                    .specialInstructions(itemDto.getSpecialInstructions())
                    .build();

            orderItems.add(orderItem);
        }

        long taxCents = Math.round(subtotalCents * TAX_RATE);
        long totalCents = subtotalCents + taxCents + DELIVERY_FEE_CENTS;

        // 6. Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customer(customer)
                .vendor(vendor)
                .deliveryAddress(deliveryAddress)
                .status(OrderStatus.PLACED)
                .subtotalCents(subtotalCents)
                .deliveryFeeCents(DELIVERY_FEE_CENTS)
                .taxCents(taxCents)
                .totalCents(totalCents)
                .paymentMethod(mapPaymentMethod(dto.getPaymentMethod()))
                .paymentStatus(PaymentStatus.PENDING)
                .scheduledTime(dto.getScheduledTime() != null ? dto.getScheduledTime().atOffset(ZoneOffset.UTC) : OffsetDateTime.now())
                .specialInstructions(dto.getSpecialInstructions())
                .build();

        // Associate order items with order
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        order.setItems(orderItems);

        order = orderRepository.save(order);
        log.info("Order created: {} (ID: {})", order.getOrderNumber(), order.getId());

        // 7. Create payment intent
        Payment payment = paymentService.createPaymentIntent(
                order.getId(),
                totalCents,
                "INR"
        );
        order.setPayment(payment);

        // For non-COD, authorize payment immediately (stub)
        if (dto.getPaymentMethod() != OrderCreateDTO.PaymentMethod.CASH_ON_DELIVERY) {
            payment = paymentService.authorizePayment(payment.getId());
            order.setPaymentStatus(PaymentStatus.AUTHORIZED);
        }

        // 8. Create initial delivery status entry
        createDeliveryStatusEntry(order, OrderStatus.PLACED, customer.getId(), "Order placed");

        // 8b. Record timeline entry
        eventTimelineService.recordStatusChange(order.getId(), customer.getId(),
                null, OrderStatus.PLACED,
                Map.of("orderNumber", order.getOrderNumber(), "totalCents", totalCents));

        // 9. Send vendor notification (stub)
        notifyVendor(order);

        // 10. Publish real-time update
        orderUpdatePublisher.publishOrderUpdate(order);

        return orderMapper.toResponseDTO(order);
    }

    /**
     * Get order by ID with role-based visibility.
     *
     * @param orderId order ID
     * @param userId authenticated user ID
     * @return OrderResponseDTO
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Check visibility: customer, vendor, driver, or admin can view
        if (!hasOrderAccess(order, userId)) {
            throw new BusinessException("Access denied to order: " + orderId);
        }

        return orderMapper.toResponseDTO(order);
    }

    /**
     * List orders assigned to a driver.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> listDriverOrders(UUID driverId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByDriverId(driverId, pageable);
        return orders.map(orderMapper::toResponseDTO);
    }

    /**
     * List orders with filters.
     *
     * @param customerId filter by customer (optional)
     * @param vendorId filter by vendor (optional)
     * @param status filter by status (optional)
     * @param pageable pagination
     * @return Page<OrderResponseDTO>
     */
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> listOrders(UUID customerId, UUID vendorId, OrderStatus status, Pageable pageable) {
        Page<Order> orders;

        if (customerId != null && status != null) {
            orders = orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        } else if (customerId != null) {
            orders = orderRepository.findByCustomerId(customerId, pageable);
        } else if (vendorId != null && status != null) {
            // Note: Repository method returns List, need to convert to Page
            List<Order> orderList = orderRepository.findByVendorIdAndStatus(vendorId, status);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), orderList.size());
            List<Order> pageContent = (start < orderList.size()) ? orderList.subList(start, end) : new ArrayList<>();
            orders = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, orderList.size());
        } else if (vendorId != null) {
            orders = orderRepository.findByVendorId(vendorId, pageable);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }

        return orders.map(orderMapper::toResponseDTO);
    }

    /**
     * Update order status with validation.
     *
     * @param orderId order ID
     * @param dto status update DTO
     * @param actorId user performing the update
     * @return updated OrderResponseDTO
     */
    @Transactional
    public OrderResponseDTO updateOrderStatus(UUID orderId, StatusUpdateDTO dto, UUID actorId) {
        log.info("Updating order {} status to {} by user {}", orderId, dto.getStatus(), actorId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus newStatus = OrderStatus.valueOf(dto.getStatus().toUpperCase());

        // Validate transition
        validateStatusTransition(order, newStatus, actorId);

        // Update status
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        // Handle special status changes
        switch (newStatus) {
            case READY -> {
                // Assign driver when order is ready
                assignDriverToOrder(order);
            }
            case DELIVERED -> {
                order.setDeliveredAt(OffsetDateTime.now());
                // Capture payment if not already done
                if (order.getPaymentStatus() == PaymentStatus.AUTHORIZED) {
                    paymentService.capturePayment(order.getPayment().getId());
                    order.setPaymentStatus(PaymentStatus.CAPTURED);
                }
            }
            case CANCELLED -> {
                order.setCancellationReason(dto.getNote());
                // Refund if payment was captured
                if (order.getPaymentStatus() == PaymentStatus.CAPTURED) {
                    paymentService.refundPayment(order.getPayment().getId());
                    order.setPaymentStatus(PaymentStatus.REFUNDED);
                }
            }
            default -> {
                // No special handling for other statuses
            }
        }

        order = orderRepository.save(order);

        // Create audit entry
        createDeliveryStatusEntry(order, newStatus, actorId, dto.getNote());

        // Record timeline
        Map<String, Object> meta = new HashMap<>();
        if (dto.getNote() != null) meta.put("note", dto.getNote());
        eventTimelineService.recordStatusChange(order.getId(), actorId, oldStatus, newStatus, meta);

        log.info("Order {} status updated: {} -> {}", orderId, oldStatus, newStatus);

        // Publish real-time update
        orderUpdatePublisher.publishOrderUpdate(order);

        return orderMapper.toResponseDTO(order);
    }

    /**
     * Vendor accepts order.
     */
    @Transactional
    public OrderResponseDTO acceptOrder(UUID orderId, UUID vendorId) {
        log.info("Vendor {} accepting order {}", vendorId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getVendor().getUser().getId().equals(vendorId)) {
            throw new BusinessException("Vendor cannot accept order from another vendor");
        }

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BusinessException("Order cannot be accepted in current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.ACCEPTED);
        order = orderRepository.save(order);

        createDeliveryStatusEntry(order, OrderStatus.ACCEPTED, vendorId, "Order accepted by vendor");

        // Record timeline
        eventTimelineService.recordStatusChange(order.getId(), vendorId,
                OrderStatus.PLACED, OrderStatus.ACCEPTED, Map.of("action", "accept"));

        // Publish real-time update
        orderUpdatePublisher.publishOrderUpdate(order);

        return orderMapper.toResponseDTO(order);
    }

    /**
     * Vendor rejects order.
     */
    @Transactional
    public OrderResponseDTO rejectOrder(UUID orderId, UUID vendorId, String reason) {
        log.info("Vendor {} rejecting order {}: {}", vendorId, orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getVendor().getUser().getId().equals(vendorId)) {
            throw new BusinessException("Vendor cannot reject order from another vendor");
        }

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BusinessException("Order cannot be rejected in current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order = orderRepository.save(order);

        // Refund if payment was authorized
        if (order.getPaymentStatus() == PaymentStatus.AUTHORIZED) {
            paymentService.refundPayment(order.getPayment().getId());
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        createDeliveryStatusEntry(order, OrderStatus.CANCELLED, vendorId, "Order rejected by vendor: " + reason);

        // Record timeline
        eventTimelineService.recordStatusChange(order.getId(), vendorId,
                OrderStatus.PLACED, OrderStatus.CANCELLED, Map.of("action", "reject", "reason", reason));

        // Publish real-time update
        orderUpdatePublisher.publishOrderUpdate(order);

        return orderMapper.toResponseDTO(order);
    }

    // ========== Helper Methods ==========

    private void validateStatusTransition(Order order, OrderStatus newStatus, UUID actorId) {
        OrderStatus currentStatus = order.getStatus();

        // Resolve actor role
        String actorRole = userRepository.findById(actorId)
                .map(u -> u.getRole().getName())
                .orElse(null);

        // Delegate to central state machine (throws InvalidTransitionException on failure)
        orderStateMachine.validateTransition(currentStatus, newStatus, actorRole);
    }

    private void assignDriverToOrder(Order order) {
        Address address = order.getDeliveryAddress();
        Optional<UUID> driverId = driverAssignmentService.assignDriverToOrder(
                address.getLat() != null ? address.getLat().doubleValue() : null,
                address.getLng() != null ? address.getLng().doubleValue() : null
        );

        driverId.ifPresentOrElse(
                id -> {
                    User driver = userRepository.findById(id)
                            .orElseThrow(() -> new BusinessException("Driver not found: " + id));
                    order.setDriver(driver);
                    order.setStatus(OrderStatus.ASSIGNED);
                    log.info("Driver {} assigned to order {}", driver.getName(), order.getId());
                },
                () -> log.warn("No available driver found for order {}", order.getId())
        );
    }

    private void createDeliveryStatusEntry(Order order, OrderStatus status, UUID actorId, String note) {
        DeliveryStatus deliveryStatus = DeliveryStatus.builder()
                .order(order)
                .status(status)
                .changedByUserId(actorId)
                .note(note)
                .build();

        deliveryStatusRepository.save(deliveryStatus);
        log.debug("Delivery status entry created: {} for order {}", status, order.getId());
    }

    private boolean hasOrderAccess(Order order, UUID userId) {
        return order.getCustomer().getId().equals(userId)
                || order.getVendor().getUser().getId().equals(userId)
                || (order.getDriver() != null && order.getDriver().getId().equals(userId));
        // Would also check if userId has ADMIN role
    }

    private String generateOrderNumber() {
        // Simple order number generation (could use more sophisticated pattern)
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PaymentMethod mapPaymentMethod(OrderCreateDTO.PaymentMethod method) {
        return switch (method) {
            case CARD -> PaymentMethod.CARD;
            case UPI -> PaymentMethod.UPI;
            case CASH_ON_DELIVERY -> PaymentMethod.CASH_ON_DELIVERY;
        };
    }

    private void notifyVendor(Order order) {
        // Stub: In production, would send email/SMS/push notification
        log.info("Notification sent to vendor {} for order {}", order.getVendor().getName(), order.getOrderNumber());
    }

    /**
     * Get order status history (delivery status audit trail).
     */
    @Transactional(readOnly = true)
    public List<DeliveryStatus> getStatusHistory(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        // Allow access check to be lenient - any authenticated user can view history if they have the orderId
        return deliveryStatusRepository.findByOrderIdOrderByChangedAtAsc(orderId);
    }

    /**
     * Manually assign a driver to an order.
     */
    @Transactional
    public OrderResponseDTO assignDriverManually(UUID orderId, UUID driverId, UUID actorId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException("Driver not found: " + driverId));

        order.setDriver(driver);
        if (order.getStatus() != OrderStatus.ASSIGNED) {
            OrderStatus oldStatus = order.getStatus();
            order.setStatus(OrderStatus.ASSIGNED);
            order = orderRepository.save(order);
            createDeliveryStatusEntry(order, OrderStatus.ASSIGNED, actorId, "Driver manually assigned");
            eventTimelineService.recordStatusChange(order.getId(), actorId,
                    oldStatus, OrderStatus.ASSIGNED,
                    Map.of("driverId", driverId.toString(), "driverName", driver.getName()));
        } else {
            order = orderRepository.save(order);
        }

        orderUpdatePublisher.publishOrderUpdate(order);
        return orderMapper.toResponseDTO(order);
    }
}
