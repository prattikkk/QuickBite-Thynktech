package com.quickbite.orders.service;

import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.exception.InvalidTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for OrderStateMachine.
 * No Spring context needed — pure logic test.
 */
class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
    }

    // ========== Valid Transitions ==========

    @ParameterizedTest(name = "{0} → {1} by {2} should succeed")
    @CsvSource({
            // Vendor transitions
            "PLACED,    ACCEPTED,   VENDOR",
            "PLACED,    CANCELLED,  VENDOR",
            "ACCEPTED,  PREPARING,  VENDOR",
            "ACCEPTED,  CANCELLED,  VENDOR",
            "PREPARING, READY,      VENDOR",
            "PREPARING, CANCELLED,  VENDOR",
            // Driver transitions
            "READY,     PICKED_UP,  DRIVER",
            "ASSIGNED,  PICKED_UP,  DRIVER",
            "PICKED_UP, ENROUTE,    DRIVER",
            "ENROUTE,   DELIVERED,  DRIVER",
            // Customer transitions
            "PLACED,    CANCELLED,  CUSTOMER",
            // Admin can do any valid structural transition
            "PLACED,    ACCEPTED,   ADMIN",
            "ENROUTE,   DELIVERED,  ADMIN",
            "READY,     CANCELLED,  ADMIN",
            "ASSIGNED,  CANCELLED,  ADMIN",
            "PICKED_UP, CANCELLED,  ADMIN",
            "ENROUTE,   CANCELLED,  ADMIN",
    })
    void validTransition_shouldPass(String from, String to, String role) {
        OrderStatus current = OrderStatus.valueOf(from);
        OrderStatus target = OrderStatus.valueOf(to);
        assertThatCode(() -> stateMachine.validateTransition(current, target, role))
                .doesNotThrowAnyException();
    }

    // ========== Invalid Structural Transitions ==========

    @ParameterizedTest(name = "{0} → {1} is structurally invalid")
    @CsvSource({
            "PLACED,    DELIVERED",
            "PLACED,    READY",
            "PLACED,    ENROUTE",
            "ACCEPTED,  DELIVERED",
            "ACCEPTED,  PICKED_UP",
            "PREPARING, DELIVERED",
            "PREPARING, ASSIGNED",
            "READY,     DELIVERED",
            "READY,     ENROUTE",
            "PICKED_UP, DELIVERED",
    })
    void invalidStructuralTransition_shouldThrow(String from, String to) {
        OrderStatus current = OrderStatus.valueOf(from);
        OrderStatus target = OrderStatus.valueOf(to);

        assertThatThrownBy(() -> stateMachine.validateTransition(current, target, "ADMIN"))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("Transition not in allowed set");
    }

    // ========== Terminal States ==========

    @Test
    void fromDelivered_shouldRejectAllTransitions() {
        assertThatThrownBy(() -> stateMachine.validateTransition(OrderStatus.DELIVERED, OrderStatus.PLACED, "ADMIN"))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("terminal status");
    }

    @Test
    void fromCancelled_shouldRejectAllTransitions() {
        assertThatThrownBy(() -> stateMachine.validateTransition(OrderStatus.CANCELLED, OrderStatus.PLACED, "ADMIN"))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("terminal status");
    }

    // ========== Role-Based Rejection ==========

    @Test
    void vendor_cannotPickUp() {
        assertThatThrownBy(() -> stateMachine.validateTransition(
                OrderStatus.READY, OrderStatus.PICKED_UP, "VENDOR"))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("VENDOR")
                .hasMessageContaining("not permitted");
    }

    @Test
    void driver_cannotAccept() {
        assertThatThrownBy(() -> stateMachine.validateTransition(
                OrderStatus.PLACED, OrderStatus.ACCEPTED, "DRIVER"))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("DRIVER")
                .hasMessageContaining("not permitted");
    }

    @Test
    void customer_canOnlyCancelFromPlaced() {
        // Customer CAN cancel from PLACED
        assertThatCode(() -> stateMachine.validateTransition(
                OrderStatus.PLACED, OrderStatus.CANCELLED, "CUSTOMER"))
                .doesNotThrowAnyException();

        // Customer CANNOT cancel from ACCEPTED (not in their permissions)
        assertThatThrownBy(() -> stateMachine.validateTransition(
                OrderStatus.ACCEPTED, OrderStatus.CANCELLED, "CUSTOMER"))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("CUSTOMER");
    }

    // ========== Null Role = Skip Role Check ==========

    @Test
    void nullRole_shouldBypassRoleCheck() {
        // Null role is treated like ADMIN — bypasses role check for any structurally valid transition
        assertThatCode(() -> stateMachine.validateTransition(
                OrderStatus.PLACED, OrderStatus.ACCEPTED, null))
                .doesNotThrowAnyException();
    }

    // ========== isAllowed helper ==========

    @Test
    void isAllowed_returnsTrue_forValidTransition() {
        assertThat(stateMachine.isAllowed(OrderStatus.PLACED, OrderStatus.ACCEPTED, "VENDOR")).isTrue();
    }

    @Test
    void isAllowed_returnsFalse_forInvalidTransition() {
        assertThat(stateMachine.isAllowed(OrderStatus.PLACED, OrderStatus.DELIVERED, "VENDOR")).isFalse();
    }

    @Test
    void isAllowed_returnsFalse_forUnauthorizedRole() {
        assertThat(stateMachine.isAllowed(OrderStatus.PLACED, OrderStatus.ACCEPTED, "DRIVER")).isFalse();
    }
}
