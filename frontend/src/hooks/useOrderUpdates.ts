/**
 * useOrderUpdates hook
 * WebSocket connection for real-time order updates
 * TODO: Implement STOMP WebSocket connection when backend is ready
 * For now, uses polling as fallback
 */

import { useEffect, useRef, useCallback, useState } from 'react';
import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { OrderDTO } from '../types';
import { orderService } from '../services';

interface UseOrderUpdatesOptions {
  orderId: string;
  onUpdate?: (order: OrderDTO) => void;
  onError?: (error: Error) => void;
  enabled?: boolean;
}

const USE_WEBSOCKET = import.meta.env.VITE_USE_WEBSOCKET === 'true';
const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';
const POLL_INTERVAL = parseInt(import.meta.env.VITE_POLL_INTERVAL || '5000');

export const useOrderUpdates = ({
  orderId,
  onUpdate,
  onError,
  enabled = true,
}: UseOrderUpdatesOptions) => {
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const clientRef = useRef<Client | null>(null);
  const subscriptionRef = useRef<StompSubscription | null>(null);
  const pollIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  /**
   * Connect to WebSocket
   * TODO: Add authentication headers and proper error handling
   */
  const connectWebSocket = useCallback(() => {
    try {
      const token = localStorage.getItem('quickbite_token');
      const client = new Client({
        webSocketFactory: () => new SockJS(WS_URL),
        connectHeaders: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        debug: (str) => {
          console.log('[STOMP]', str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      client.onConnect = () => {
        console.log('WebSocket connected');
        setIsConnected(true);
        setError(null);

        // Subscribe to order updates
        const subscription = client.subscribe(`/topic/orders/${orderId}`, (message) => {
          try {
            const order: OrderDTO = JSON.parse(message.body);
            onUpdate?.(order);
          } catch (err) {
            console.error('Failed to parse order update:', err);
          }
        });

        subscriptionRef.current = subscription;
      };

      client.onStompError = (frame) => {
        console.error('STOMP error:', frame);
        const err = new Error(frame.headers['message'] || 'WebSocket error');
        setError(err);
        onError?.(err);
      };

      client.onWebSocketClose = () => {
        console.log('WebSocket closed');
        setIsConnected(false);
      };

      client.activate();
      clientRef.current = client;
    } catch (err) {
      console.error('Failed to connect WebSocket:', err);
      const error = err instanceof Error ? err : new Error('WebSocket connection failed');
      setError(error);
      onError?.(error);
    }
  }, [orderId, onUpdate, onError]);

  /**
   * Poll order status as fallback
   */
  const startPolling = useCallback(() => {
    const poll = async () => {
      try {
        const order = await orderService.getOrderById(orderId);
        onUpdate?.(order);
      } catch (err) {
        console.error('Failed to poll order:', err);
        const error = err instanceof Error ? err : new Error('Failed to fetch order');
        onError?.(error);
      }
    };

    // Initial poll
    poll();

    // Set up interval
    pollIntervalRef.current = setInterval(poll, POLL_INTERVAL);
  }, [orderId, onUpdate, onError]);

  /**
   * Stop polling
   */
  const stopPolling = useCallback(() => {
    if (pollIntervalRef.current) {
      clearInterval(pollIntervalRef.current);
      pollIntervalRef.current = null;
    }
  }, []);

  /**
   * Disconnect WebSocket
   */
  const disconnectWebSocket = useCallback(() => {
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }

    setIsConnected(false);
  }, []);

  /**
   * Initialize connection
   */
  useEffect(() => {
    if (!enabled) {
      return;
    }

    if (USE_WEBSOCKET) {
      connectWebSocket();
    } else {
      startPolling();
    }

    return () => {
      if (USE_WEBSOCKET) {
        disconnectWebSocket();
      } else {
        stopPolling();
      }
    };
  }, [enabled, USE_WEBSOCKET, connectWebSocket, disconnectWebSocket, startPolling, stopPolling]);

  return {
    isConnected: USE_WEBSOCKET ? isConnected : true,
    error,
    disconnect: USE_WEBSOCKET ? disconnectWebSocket : stopPolling,
  };
};

export default useOrderUpdates;
