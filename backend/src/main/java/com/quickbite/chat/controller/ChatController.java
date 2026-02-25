package com.quickbite.chat.controller;

import com.quickbite.chat.dto.ChatMessageDTO;
import com.quickbite.chat.dto.ChatRoomDTO;
import com.quickbite.chat.entity.ChatMessage;
import com.quickbite.chat.entity.ChatRoom;
import com.quickbite.chat.service.ChatService;
import com.quickbite.common.dto.ApiResponse;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for chat operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat messaging endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    /**
     * Create or get an existing chat room.
     */
    @PostMapping("/rooms")
    @Operation(summary = "Create or get chat room", description = "Creates a new chat room or returns an existing one for the order")
    public ResponseEntity<ApiResponse<ChatRoomDTO>> createOrGetRoom(
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        UUID orderId = UUID.fromString(body.get("orderId"));
        UUID otherUserId = UUID.fromString(body.get("otherUserId"));
        String roomType = body.get("roomType");

        ChatRoom room = chatService.getOrCreateRoom(orderId, userId, otherUserId, roomType);

        ChatRoomDTO dto = ChatRoomDTO.builder()
                .id(room.getId())
                .orderId(room.getOrderId())
                .otherUserId(otherUserId)
                .roomType(room.getRoomType())
                .closed(Boolean.TRUE.equals(room.getClosed()))
                .createdAt(room.getCreatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(dto));
    }

    /**
     * List all chat rooms for the authenticated user.
     */
    @GetMapping("/rooms")
    @Operation(summary = "List user chat rooms", description = "Returns all chat rooms the authenticated user participates in")
    public ResponseEntity<ApiResponse<List<ChatRoomDTO>>> getUserRooms(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        List<ChatRoomDTO> rooms = chatService.getUserRooms(userId);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    /**
     * Get paginated messages for a chat room.
     */
    @GetMapping("/rooms/{roomId}/messages")
    @Operation(summary = "Get chat messages", description = "Returns paginated messages for a chat room")
    public ResponseEntity<ApiResponse<Page<ChatMessageDTO>>> getMessages(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        Page<ChatMessageDTO> messages = chatService.getMessages(roomId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * Send a message in a chat room.
     */
    @PostMapping("/rooms/{roomId}/messages")
    @Operation(summary = "Send chat message", description = "Sends a message in the specified chat room")
    public ResponseEntity<ApiResponse<ChatMessageDTO>> sendMessage(
            @PathVariable UUID roomId,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        String content = body.get("content");

        ChatMessage message = chatService.sendMessage(roomId, userId, content);

        ChatMessageDTO dto = ChatMessageDTO.builder()
                .id(message.getId())
                .roomId(roomId)
                .senderId(userId)
                .senderName(message.getSender().getName())
                .content(message.getContent())
                .read(false)
                .createdAt(message.getCreatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent", dto));
    }

    /**
     * Mark all messages in a room as read for the authenticated user.
     */
    @PutMapping("/rooms/{roomId}/read")
    @Operation(summary = "Mark messages read", description = "Marks all unread messages in the room as read for the current user")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable UUID roomId,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        chatService.markRead(roomId, userId);
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }

    // ========== Helper Methods ==========

    private UUID extractUserId(Authentication authentication) {
        String principal = authentication.getName();
        try {
            return UUID.fromString(principal);
        } catch (IllegalArgumentException e) {
            // Principal is email — look up user by email
            User user = userRepository.findByEmail(principal)
                    .orElseThrow(() -> new IllegalStateException("User not found for email: " + principal));
            return user.getId();
        }
    }
}
