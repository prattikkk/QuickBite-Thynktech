/**
 * Chat service — Phase 4.4
 */

import api from './api';
import type { ChatRoomDTO, ChatMessageDTO, ChatRoomCreateRequest } from '../types/phase4.types';

export const chatService = {
  /** Create or get existing chat room for an order */
  async createRoom(data: ChatRoomCreateRequest): Promise<ChatRoomDTO> {
    return api.post('/chat/rooms', data) as Promise<ChatRoomDTO>;
  },

  /** Get current user's chat rooms */
  async getRooms(): Promise<ChatRoomDTO[]> {
    return api.get('/chat/rooms') as Promise<ChatRoomDTO[]>;
  },

  /** Get paginated messages for a room */
  async getMessages(roomId: string, page = 0, size = 20): Promise<{
    content: ChatMessageDTO[];
    totalElements: number;
    totalPages: number;
  }> {
    return api.get(`/chat/rooms/${roomId}/messages`, { params: { page, size } }) as any;
  },

  /** Send a message to a chat room */
  async sendMessage(roomId: string, content: string): Promise<ChatMessageDTO> {
    return api.post(`/chat/rooms/${roomId}/messages`, { content }) as Promise<ChatMessageDTO>;
  },

  /** Mark all messages in a room as read */
  async markRead(roomId: string): Promise<void> {
    await api.put(`/chat/rooms/${roomId}/read`);
  },
};
