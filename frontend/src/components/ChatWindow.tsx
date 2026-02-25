/**
 * ChatWindow — real-time chat panel for customer ↔ driver/vendor communication.
 * Phase 4.4: Uses REST polling (STOMP subscription optional upgrade).
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import { chatService } from '../services/chat.service';
import type { ChatMessageDTO, ChatRoomDTO } from '../types/phase4.types';
import { useAuthStore } from '../store';

export interface ChatWindowProps {
  orderId: string;
  otherUserId?: string;
  otherUserName?: string;
  roomType?: 'CUSTOMER_DRIVER' | 'CUSTOMER_VENDOR';
  onClose?: () => void;
}

export default function ChatWindow({ orderId, otherUserId, otherUserName, roomType = 'CUSTOMER_DRIVER', onClose }: ChatWindowProps) {
  const user = useAuthStore((s) => s.user);
  const [room, setRoom] = useState<ChatRoomDTO | null>(null);
  const [messages, setMessages] = useState<ChatMessageDTO[]>([]);
  const [newMsg, setNewMsg] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const loadMessages = useCallback(async (roomId: string) => {
    try {
      const data = await chatService.getMessages(roomId, 0, 50);
      setMessages(data.content.reverse());
      chatService.markRead(roomId).catch(() => {});
    } catch {
      // silent
    }
  }, []);

  // Initialize room
  useEffect(() => {
    const init = async () => {
      try {
        const r = await chatService.createRoom({ orderId, otherUserId: otherUserId || '', roomType });
        setRoom(r);
        await loadMessages(r.id);
      } catch {
        // fallback
      } finally {
        setLoading(false);
      }
    };
    init();
  }, [orderId, otherUserId, roomType, loadMessages]);

  // Poll for new messages every 5s
  useEffect(() => {
    if (!room) return;
    const interval = setInterval(() => loadMessages(room.id), 5000);
    return () => clearInterval(interval);
  }, [room, loadMessages]);

  // Scroll on new messages
  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSend = async () => {
    if (!room || !newMsg.trim() || sending) return;
    setSending(true);
    try {
      const msg = await chatService.sendMessage(room.id, newMsg.trim());
      setMessages((prev) => [...prev, msg]);
      setNewMsg('');
    } catch {
      // silent
    } finally {
      setSending(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-lg p-4 w-80 h-96 flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-lg w-80 h-96 flex flex-col border">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 bg-primary-600 text-white rounded-t-lg">
        <div>
          <p className="font-medium text-sm">{otherUserName || 'Chat'}</p>
          <p className="text-xs opacity-75">{roomType === 'CUSTOMER_DRIVER' ? 'Driver' : 'Vendor'}</p>
        </div>
        {onClose && (
          <button onClick={onClose} className="text-white hover:text-gray-200">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        )}
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-3 space-y-2">
        {messages.length === 0 && (
          <p className="text-center text-gray-400 text-sm mt-8">No messages yet. Say hello!</p>
        )}
        {messages.map((msg) => {
          const isMe = msg.senderId === user?.id;
          return (
            <div key={msg.id} className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}>
              <div
                className={`max-w-[70%] px-3 py-2 rounded-lg text-sm ${
                  isMe
                    ? 'bg-primary-600 text-white rounded-br-none'
                    : 'bg-gray-100 text-gray-900 rounded-bl-none'
                }`}
              >
                <p>{msg.content}</p>
                <p className={`text-xs mt-1 ${isMe ? 'text-primary-200' : 'text-gray-400'}`}>
                  {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </p>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <div className="border-t p-2 flex gap-2">
        <input
          type="text"
          value={newMsg}
          onChange={(e) => setNewMsg(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Type a message..."
          className="flex-1 px-3 py-2 text-sm border rounded-full focus:outline-none focus:ring-2 focus:ring-primary-500"
          disabled={sending}
        />
        <button
          onClick={handleSend}
          disabled={!newMsg.trim() || sending}
          className="p-2 rounded-full bg-primary-600 text-white disabled:opacity-50 hover:bg-primary-700"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
          </svg>
        </button>
      </div>
    </div>
  );
}
