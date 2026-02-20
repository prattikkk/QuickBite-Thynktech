package com.quickbite.orders.service;

import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.exception.InvalidTransitionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Centralised order state machine.
 * Defines which transitions are legal and which role(s) can perform each transition.
 *
 * <pre>
 * PLACED  ─→ ACCEPTED  (VENDOR)
 *         ─→ CANCELLED (CUSTOMER, VENDOR, ADMIN)
 * ACCEPTED ─→ PREPARING (VENDOR)
 *           ─→ CANCELLED (VENDOR, ADMIN)
 * PREPARING ─→ READY     (VENDOR)
 *            ─→ CANCELLED (VENDOR, ADMIN)
 * READY ─→ ASSIGNED  (SYSTEM, ADMIN)
 *       ─→ PICKED_UP (DRIVER) — if no explicit ASSIGNED step
 *       ─→ CANCELLED (ADMIN)
 * ASSIGNED ─→ PICKED_UP (DRIVER)
 *           ─→ CANCELLED (ADMIN)
 * PICKED_UP ─→ ENROUTE   (DRIVER)
 *            ─→ CANCELLED (ADMIN)
 * ENROUTE ─→ DELIVERED  (DRIVER)
 *         ─→ CANCELLED (ADMIN)
 * </pre>
 */
@Slf4j
@Service
public class OrderStateMachine {

    /**
     * Transition definition: from-status → Set<to-status>.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PLACED,    Set.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED),
            OrderStatus.ACCEPTED,  Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING, Set.of(OrderStatus.READY, OrderStatus.CANCELLED),
            OrderStatus.READY,     Set.of(OrderStatus.ASSIGNED, OrderStatus.PICKED_UP, OrderStatus.CANCELLED),
            OrderStatus.ASSIGNED,  Set.of(OrderStatus.PICKED_UP, OrderStatus.CANCELLED),
            OrderStatus.PICKED_UP, Set.of(OrderStatus.ENROUTE, OrderStatus.CANCELLED),
            OrderStatus.ENROUTE,   Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED)
    );

    /**
     * Role → set of transitions that role may trigger (expressed as "FROM→TO" keys).
     * A null role or ADMIN bypasses role checks.
     */
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
            "VENDOR",   Set.of(
                    key(OrderStatus.PLACED, OrderStatus.ACCEPTED),
                    key(OrderStatus.PLACED, OrderStatus.CANCELLED),
                    key(OrderStatus.ACCEPTED, OrderStatus.PREPARING),
                    key(OrderStatus.ACCEPTED, OrderStatus.CANCELLED),
                    key(OrderStatus.PREPARING, OrderStatus.READY),
                    key(OrderStatus.PREPARING, OrderStatus.CANCELLED)
            ),
            "DRIVER",   Set.of(
                    key(OrderStatus.READY, OrderStatus.PICKED_UP),
                    key(OrderStatus.ASSIGNED, OrderStatus.PICKED_UP),
                    key(OrderStatus.PICKED_UP, OrderStatus.ENROUTE),
                    key(OrderStatus.ENROUTE, OrderStatus.DELIVERED)
            ),
            "CUSTOMER", Set.of(
                    key(OrderStatus.PLACED, OrderStatus.CANCELLED)
            ),
            "ADMIN",    Set.of()  // ADMIN can do anything — checked separately
    );

    /**
     * Validate whether the transition from {@code current} to {@code target} is allowed,
     * optionally checking the actor's role.
     *
     * @param current   current order status (must not be null)
     * @param target    desired next status (must not be null)
     * @param actorRole the Spring Security role name (e.g. "VENDOR", "DRIVER", "CUSTOMER", "ADMIN").
     *                  May be null to skip role check.
     * @throws InvalidTransitionException if the transition is not permitted
     */
    public void validateTransition(OrderStatus current, OrderStatus target, String actorRole) {
        // 1. Terminal states cannot transition
        if (current == OrderStatus.DELIVERED || current == OrderStatus.CANCELLED) {
            throw new InvalidTransitionException(current.name(), target.name(),
                    "Order is in terminal status " + current);
        }

        // 2. Check the transition is structurally valid
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidTransitionException(current.name(), target.name(),
                    "Transition not in allowed set: " + allowed);
        }

        // 3. ADMIN may perform any structurally-valid transition
        if ("ADMIN".equals(actorRole) || actorRole == null) {
            log.debug("Transition {}->{} permitted for role={}", current, target, actorRole);
            return;
        }

        // 4. Role-based check
        Set<String> permitted = ROLE_PERMISSIONS.getOrDefault(actorRole, Set.of());
        String transitionKey = key(current, target);
        if (!permitted.contains(transitionKey)) {
            throw new InvalidTransitionException(current.name(), target.name(),
                    "Role " + actorRole + " is not permitted for this transition");
        }

        log.debug("Transition {}->{} permitted for role={}", current, target, actorRole);
    }

    /**
     * Check whether the transition is allowed (no exception).
     */
    public boolean isAllowed(OrderStatus current, OrderStatus target, String actorRole) {
        try {
            validateTransition(current, target, actorRole);
            return true;
        } catch (InvalidTransitionException e) {
            return false;
        }
    }

    private static String key(OrderStatus from, OrderStatus to) {
        return from.name() + "->" + to.name();
    }
}
