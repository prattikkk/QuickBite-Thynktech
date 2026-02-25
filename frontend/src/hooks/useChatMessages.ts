/**
 * useChatMessages — STOMP WebSocket hook for real-time chat messages (M3)
 * Falls back to REST polling if WebSocket is unavailable.
 * Uses refs for callback stability to prevent reconnection loops.
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

  // Stable ref for the onMessage callback — prevents reconnection when parent re-renders
  const onMessageRef = useRef(onMessage);
  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);

  const disconnect = useCallback(() => {
    subRef.current?.unsubscribe();
    subRef.current = null;
    clientRef.current?.deactivate();
    clientRef.current = null;
    setConnected(false);
  }, []);

  const connect = useCallback(() => {
    if (!roomId || !USE_WEBSOCKET || !enabled) return;

    // Disconnect any existing connection first
    if (clientRef.current) {
      disconnect();
    }

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
            onMessageRef.current?.(msg);
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
  }, [roomId, enabled, disconnect]);

  useEffect(() => {
    if (USE_WEBSOCKET && enabled && roomId) {
      connect();
      return () => disconnect();
    }
  }, [connect, disconnect, enabled, roomId]);

  return { connected: USE_WEBSOCKET ? connected : false, isWebSocket: USE_WEBSOCKET };
}

export default useChatMessages;
