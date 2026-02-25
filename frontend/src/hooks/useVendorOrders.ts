/**
 * useVendorOrders — STOMP WebSocket hook for real-time vendor KDS order updates (M4)
 * Falls back to REST polling. Plays audio alert on new PLACED orders.
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { OrderDTO } from '../types';

interface UseVendorOrdersOptions {
  vendorId: string | null;
  onNewOrder?: (order: OrderDTO) => void;
  onOrderUpdate?: (order: OrderDTO) => void;
  enabled?: boolean;
}

const USE_WEBSOCKET = import.meta.env.VITE_USE_WEBSOCKET === 'true';
const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

export function useVendorOrders({ vendorId, onNewOrder, onOrderUpdate, enabled = true }: UseVendorOrdersOptions) {
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const subRef = useRef<StompSubscription | null>(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  // Initialize audio for new order alert
  useEffect(() => {
    if (typeof window !== 'undefined') {
      try {
        // Use a simple beep via Web Audio API
        audioRef.current = null; // Will use Web Audio API below
      } catch {
        // audio not supported
      }
    }
  }, []);

  const playAlert = useCallback(() => {
    try {
      const ctx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.frequency.value = 880;
      osc.type = 'sine';
      gain.gain.value = 0.3;
      osc.start();
      osc.stop(ctx.currentTime + 0.2);
      // Double beep
      setTimeout(() => {
        try {
          const osc2 = ctx.createOscillator();
          const gain2 = ctx.createGain();
          osc2.connect(gain2);
          gain2.connect(ctx.destination);
          osc2.frequency.value = 1100;
          osc2.type = 'sine';
          gain2.gain.value = 0.3;
          osc2.start();
          osc2.stop(ctx.currentTime + 0.2);
        } catch { /* ignore */ }
      }, 250);
    } catch {
      // Web Audio not available
    }
  }, []);

  const connect = useCallback(() => {
    if (!vendorId || !USE_WEBSOCKET || !enabled) return;

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
        const sub = client.subscribe(`/topic/vendors.${vendorId}.orders`, (message) => {
          try {
            const order: OrderDTO = JSON.parse(message.body);
            onOrderUpdate?.(order);
            if (order.status === 'PLACED') {
              playAlert();
              onNewOrder?.(order);
            }
          } catch { /* ignore */ }
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
  }, [vendorId, onNewOrder, onOrderUpdate, enabled, playAlert]);

  const disconnect = useCallback(() => {
    subRef.current?.unsubscribe();
    subRef.current = null;
    clientRef.current?.deactivate();
    clientRef.current = null;
    setConnected(false);
  }, []);

  useEffect(() => {
    if (USE_WEBSOCKET && enabled && vendorId) {
      connect();
      return () => disconnect();
    }
  }, [connect, disconnect, enabled, vendorId]);

  return { connected: USE_WEBSOCKET ? connected : false, playAlert };
}

export default useVendorOrders;
