package com.quickbite.orders;

import com.quickbite.delivery.entity.DeliveryStatus;
import com.quickbite.delivery.repository.DeliveryStatusRepository;
import com.quickbite.orders.driver.DriverAssignmentService;
import com.quickbite.orders.dto.OrderCreateDTO;
import com.quickbite.orders.dto.OrderItemDTO;
import com.quickbite.orders.dto.OrderResponseDTO;
import com.quickbite.orders.dto.StatusUpdateDTO;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderItem;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.exception.BusinessException;
import com.quickbite.orders.exception.OrderNotFoundException;
import com.quickbite.orders.exception.InvalidTransitionException;
import com.quickbite.orders.mapper.OrderMapper;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.orders.service.EventTimelineService;
import com.quickbite.orders.service.OrderService;
import com.quickbite.orders.service.OrderStateMachine;
import com.quickbite.payments.service.PaymentService;
import com.quickbite.payments.entity.Payment;
import com.quickbite.payments.entity.PaymentMethod;
import com.quickbite.payments.entity.PaymentStatus;
import com.quickbite.users.entity.Address;
import com.quickbite.users.entity.Role;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.AddressRepository;
import com.quickbite.users.repository.UserRepository;
import com.quickbite.vendors.entity.MenuItem;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.MenuItemRepository;
import com.quickbite.websocket.OrderUpdatePublisher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private DeliveryStatusRepository deliveryStatusRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private DriverAssignmentService driverAssignmentService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderUpdatePublisher orderUpdatePublisher;

    @Mock
    private OrderStateMachine orderStateMachine;

    @Mock
    private EventTimelineService eventTimelineService;

    private OrderService orderService;

    private UUID customerId;
    private UUID vendorUserId;
    private UUID vendorEntityId;
    private UUID addressId;
    private UUID menuItemId;
    private User customer;
    private User vendorUser;
    private Vendor vendorEntity;
    private Address address;
    private MenuItem menuItem;
    private Payment payment;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository, menuItemRepository, userRepository,
                addressRepository, deliveryStatusRepository, paymentService,
                driverAssignmentService, orderMapper, orderUpdatePublisher,
                orderStateMachine, eventTimelineService,
                new SimpleMeterRegistry());

        customerId = UUID.randomUUID();
        vendorUserId = UUID.randomUUID();
        vendorEntityId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        menuItemId = UUID.randomUUID();

        // Setup customer
        customer = User.builder()
                .id(customerId)
                .email("customer@test.com")
                .name("Test Customer")
                .phone("+1234567890")
                .active(true)
                .build();

        // Setup vendor
        vendorUser = User.builder()
                .id(vendorUserId)
                .email("vendor@test.com")
                .name("Test Vendor")
                .phone("+1234567891")
                .active(true)
                .build();

        vendorEntity = Vendor.builder()
                .id(vendorEntityId)
                .user(vendorUser)
                .name("Test Restaurant")
                .build();

        // Setup address
        address = Address.builder()
                .id(addressId)
                .user(customer)
                .line1("123 Main St")
                .city("Test City")
                .state("TS")
                .postal("12345")
                .country("IN")
                .lat(BigDecimal.valueOf(12.9716))
                .lng(BigDecimal.valueOf(77.5946))
                .isDefault(true)
                .build();

        customer.setAddresses(List.of(address));

        // Setup menu item
        menuItem = MenuItem.builder()
                .id(menuItemId)
                .vendor(vendorEntity)
                .name("Test Item")
                .priceCents(50000L) // ₹500
                .available(true)
                .build();

        // Setup payment
        payment = Payment.builder()
                .id(UUID.randomUUID())
                .providerPaymentId("stub_pi_123")
                .status(PaymentStatus.PENDING)
                .amountCents(55000L)
                .currency("INR")
                .build();
    }

    @Test
    void createOrder_happyPath_success() {
        // Arrange
        OrderItemDTO itemDto = OrderItemDTO.builder()
                .menuItemId(menuItemId)
                .quantity(2)
                .specialInstructions("No onions")
                .build();

        OrderCreateDTO createDto = OrderCreateDTO.builder()
                .items(List.of(itemDto))
                .addressId(addressId)
                .paymentMethod(OrderCreateDTO.PaymentMethod.CARD)
                .specialInstructions("Leave at door")
                .build();

        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));
        when(paymentService.createPaymentIntent(any(), anyLong(), eq("INR"))).thenReturn(payment);
        when(paymentService.authorizePayment(any())).thenReturn(payment);
        when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(new OrderResponseDTO());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        // Act
        OrderResponseDTO result = orderService.createOrder(createDto, customerId);

        // Assert
        assertThat(result).isNotNull();

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getCustomer()).isEqualTo(customer);
        assertThat(savedOrder.getVendor()).isEqualTo(vendorEntity);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(savedOrder.getSubtotalCents()).isEqualTo(100000L); // 2 * 50000
        assertThat(savedOrder.getTaxCents()).isEqualTo(5000L); // 5% of 100000
        assertThat(savedOrder.getDeliveryFeeCents()).isEqualTo(5000L);
        assertThat(savedOrder.getTotalCents()).isEqualTo(110000L); // 100000 + 5000 + 5000
        assertThat(savedOrder.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(savedOrder.getSpecialInstructions()).isEqualTo("Leave at door");
        assertThat(savedOrder.getItems()).hasSize(1);

        OrderItem savedItem = savedOrder.getItems().get(0);
        assertThat(savedItem.getMenuItem()).isEqualTo(menuItem);
        assertThat(savedItem.getQuantity()).isEqualTo(2);
        assertThat(savedItem.getPriceCents()).isEqualTo(50000L);
        assertThat(savedItem.getSpecialInstructions()).isEqualTo("No onions");

        verify(paymentService).createPaymentIntent(any(), eq(110000L), eq("INR"));
        verify(paymentService).authorizePayment(any());
        verify(deliveryStatusRepository).save(any(DeliveryStatus.class));
    }

    @Test
    void createOrder_menuItemNotAvailable_throwsException() {
        // Arrange
        menuItem.setAvailable(false);

        OrderItemDTO itemDto = OrderItemDTO.builder()
                .menuItemId(menuItemId)
                .quantity(1)
                .build();

        OrderCreateDTO createDto = OrderCreateDTO.builder()
                .items(List.of(itemDto))
                .addressId(addressId)
                .paymentMethod(OrderCreateDTO.PaymentMethod.CARD)
                .build();

        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(createDto, customerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_addressNotBelongToCustomer_throwsException() {
        // Arrange
        Address wrongAddress = Address.builder()
                .id(addressId)
                .user(User.builder().id(UUID.randomUUID()).build())
                .build();

        OrderItemDTO itemDto = OrderItemDTO.builder()
                .menuItemId(menuItemId)
                .quantity(1)
                .build();

        OrderCreateDTO createDto = OrderCreateDTO.builder()
                .items(List.of(itemDto))
                .addressId(addressId)
                .paymentMethod(OrderCreateDTO.PaymentMethod.CARD)
                .build();

        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(wrongAddress));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(createDto, customerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong to customer");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void acceptOrder_happyPath_success() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .vendor(vendorEntity)
                .status(OrderStatus.PLACED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDTO(any())).thenReturn(new OrderResponseDTO());

        // Act — pass vendorUserId, not vendorEntityId, because the service checks
        // order.getVendor().getUser().getId().equals(vendorId)
        OrderResponseDTO result = orderService.acceptOrder(orderId, vendorUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        verify(deliveryStatusRepository).save(any(DeliveryStatus.class));
    }

    @Test
    void rejectOrder_happyPath_success() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .vendor(vendorEntity)
                .status(OrderStatus.PLACED)
                .payment(payment)
                .paymentStatus(PaymentStatus.AUTHORIZED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDTO(any())).thenReturn(new OrderResponseDTO());
        when(paymentService.refundPayment(any())).thenReturn(payment);

        String reason = "Out of stock";

        // Act — pass vendorUserId, not vendorEntityId, because the service checks
        // order.getVendor().getUser().getId().equals(vendorId)
        OrderResponseDTO result = orderService.rejectOrder(orderId, vendorUserId, reason);

        // Assert
        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).isEqualTo(reason);
        verify(paymentService).refundPayment(payment.getId());
        verify(deliveryStatusRepository).save(any(DeliveryStatus.class));
    }

    @Test
    void rejectOrder_wrongVendor_throwsException() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID wrongVendorId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .vendor(vendorEntity)
                .status(OrderStatus.PLACED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> orderService.rejectOrder(orderId, wrongVendorId, "Reason"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot reject order from another vendor");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_invalidTransition_throwsException() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PLACED)
                .build();

        StatusUpdateDTO updateDto = StatusUpdateDTO.builder()
                .status("DELIVERED")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(actorId)).thenReturn(Optional.empty());
        doThrow(new InvalidTransitionException("PLACED", "DELIVERED", "Transition not in allowed set"))
                .when(orderStateMachine).validateTransition(OrderStatus.PLACED, OrderStatus.DELIVERED, null);

        // Act & Assert
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, updateDto, actorId))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("PLACED");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_toReady_assignsDriver() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        User actor = User.builder().id(actorId).name("Actor").role(Role.builder().name("VENDOR").build()).build();
        User driver = User.builder().id(driverId).name("Driver").build();

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PREPARING)
                .deliveryAddress(address)
                .build();

        StatusUpdateDTO updateDto = StatusUpdateDTO.builder()
                .status("READY")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(driverAssignmentService.assignDriverToOrder(any(), any())).thenReturn(Optional.of(driverId));
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(orderMapper.toResponseDTO(any())).thenReturn(new OrderResponseDTO());

        // Act
        OrderResponseDTO result = orderService.updateOrderStatus(orderId, updateDto, actorId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(order.getDriver()).isEqualTo(driver);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
        verify(driverAssignmentService).assignDriverToOrder(
                address.getLat().doubleValue(), address.getLng().doubleValue());
        verify(deliveryStatusRepository).save(any(DeliveryStatus.class));
    }

    @Test
    void updateOrderStatus_toDelivered_capturesPayment() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        User actor = User.builder().id(actorId).name("Driver Actor").role(Role.builder().name("DRIVER").build()).build();
        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.ENROUTE)
                .payment(payment)
                .paymentStatus(PaymentStatus.AUTHORIZED)
                .build();

        StatusUpdateDTO updateDto = StatusUpdateDTO.builder()
                .status("DELIVERED")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(paymentService.capturePayment(any())).thenReturn(payment);
        when(orderMapper.toResponseDTO(any())).thenReturn(new OrderResponseDTO());

        // Act
        OrderResponseDTO result = orderService.updateOrderStatus(orderId, updateDto, actorId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();
        verify(paymentService).capturePayment(payment.getId());
        verify(deliveryStatusRepository).save(any(DeliveryStatus.class));
    }

    @Test
    void getOrder_notFound_throwsException() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrder(orderId, customerId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void listOrders_withFilters_success() {
        // Arrange
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.PLACED)
                .customer(customer)
                .vendor(vendorEntity)
                .build();

        Page<Order> page = new PageImpl<>(List.of(order));
        Pageable pageable = PageRequest.of(0, 20);

        when(orderRepository.findByCustomerIdAndStatus(customerId, OrderStatus.PLACED, pageable))
                .thenReturn(page);
        when(orderMapper.toResponseDTO(any())).thenReturn(new OrderResponseDTO());

        // Act
        Page<OrderResponseDTO> result = orderService.listOrders(customerId, null, OrderStatus.PLACED, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findByCustomerIdAndStatus(customerId, OrderStatus.PLACED, pageable);
    }
}
