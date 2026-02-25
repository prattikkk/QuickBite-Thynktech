package com.quickbite.chat.entity;

import com.quickbite.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * ChatRoom entity representing a conversation between two participants
 * in the context of an order (customer-driver or customer-vendor).
 */
@Entity
@Table(name = "chat_rooms", indexes = {
    @Index(name = "idx_chatroom_order", columnList = "order_id"),
    @Index(name = "idx_chatroom_order_type", columnList = "order_id, room_type"),
    @Index(name = "idx_chatroom_participant1", columnList = "participant1_id"),
    @Index(name = "idx_chatroom_participant2", columnList = "participant2_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant1_id", nullable = false)
    private User participant1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant2_id", nullable = false)
    private User participant2;

    @Column(name = "room_type", nullable = false, length = 30)
    private String roomType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean closed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
