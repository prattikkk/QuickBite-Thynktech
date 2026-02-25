/**
 * useChatMessages — STOMP WebSocket hook for real-time chat messages (M3)
 * Falls back to REST polling if WebSocket is unavailable.
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { ChatMessageDTO } from '../types/phase4.types';

interface UseChatMessagesOptions {
  roomId: string | null;
  onMessage?: (msg: ChatMessageDTO) => void;
  enabled?: boolean;
}

const USE_WEBSOCKET = import.meta.env.VITE_USE_WEBSOCKET === 'true';
const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

export function useChatMessages({ roomId, onMessage, enabled = true }: UseChatMessagesOptions) {
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const subRef = useRef<StompSubscription | null>(null);

  const connect = useCallback(() => {
    if (!roomId || !USE_WEBSOCKET || !enabled) return;

    try {
      const token = localStorage.getItem('quickbite_token');
      const client = new Client({
        webSocketFactory: () => new SockJS(WS_URL),
        connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      client.onConnect = () => {
        setConnected(true);
        const sub = client.subscribe(`/topic/chat.${roomId}`, (message) => {
          try {
            const msg: ChatMessageDTO = JSON.parse(message.body);
            onMessage?.(msg);
          } catch {
            // ignore parse errors
          }
        });
        subRef.current = sub;
      };

      client.onStompError = () => setConnected(false);
      client.onWebSocketClose = () => setConnected(false);

      client.activate();
      clientRef.current = client;
    } catch {
      setConnected(false);
    }
  }, [roomId, onMessage, enabled]);

  const disconnect = useCallback(() => {
    subRef.current?.unsubscribe();
    subRef.current = null;
    clientRef.current?.deactivate();
    clientRef.current = null;
    setConnected(false);
  }, []);

  useEffect(() => {
    if (USE_WEBSOCKET && enabled && roomId) {
      connect();
      return () => disconnect();
    }
  }, [connect, disconnect, enabled, roomId]);

  return { connected: USE_WEBSOCKET ? connected : false, isWebSocket: USE_WEBSOCKET };
}

export default useChatMessages;
