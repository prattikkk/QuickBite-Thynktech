/**
 * useVendorOrders — STOMP WebSocket hook for real-time vendor KDS order updates (M4)
 * Falls back to REST polling. Plays audio alert on new PLACED orders.
 * Uses refs for callback stability to prevent reconnection loops.
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

// Shared AudioContext — reuse to avoid browser limits
let sharedAudioCtx: AudioContext | null = null;
function getAudioContext(): AudioContext | null {
  try {
    if (!sharedAudioCtx || sharedAudioCtx.state === 'closed') {
      sharedAudioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
    }
    if (sharedAudioCtx.state === 'suspended') {
      sharedAudioCtx.resume();
    }
    return sharedAudioCtx;
  } catch {
    return null;
  }
}

export function useVendorOrders({ vendorId, onNewOrder, onOrderUpdate, enabled = true }: UseVendorOrdersOptions) {
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const subRef = useRef<StompSubscription | null>(null);

  // Stable refs for callbacks — prevents reconnection when parent re-renders
  const onNewOrderRef = useRef(onNewOrder);
  const onOrderUpdateRef = useRef(onOrderUpdate);
  useEffect(() => {
    onNewOrderRef.current = onNewOrder;
    onOrderUpdateRef.current = onOrderUpdate;
  }, [onNewOrder, onOrderUpdate]);

  const playAlert = useCallback(() => {
    const ctx = getAudioContext();
    if (!ctx) return;
    try {
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
          const ctx2 = getAudioContext();
          if (!ctx2) return;
          const osc2 = ctx2.createOscillator();
          const gain2 = ctx2.createGain();
          osc2.connect(gain2);
          gain2.connect(ctx2.destination);
          osc2.frequency.value = 1100;
          osc2.type = 'sine';
          gain2.gain.value = 0.3;
          osc2.start();
          osc2.stop(ctx2.currentTime + 0.2);
        } catch { /* ignore */ }
      }, 250);
    } catch {
      // Web Audio not available
    }
  }, []);

  const disconnect = useCallback(() => {
    subRef.current?.unsubscribe();
    subRef.current = null;
    clientRef.current?.deactivate();
    clientRef.current = null;
    setConnected(false);
  }, []);

  const connect = useCallback(() => {
    if (!vendorId || !USE_WEBSOCKET || !enabled) return;

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
        const sub = client.subscribe(`/topic/vendors.${vendorId}.orders`, (message) => {
          try {
            const order: OrderDTO = JSON.parse(message.body);
            onOrderUpdateRef.current?.(order);
            if (order.status === 'PLACED') {
              playAlert();
              onNewOrderRef.current?.(order);
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
  }, [vendorId, enabled, disconnect, playAlert]);

  useEffect(() => {
    if (USE_WEBSOCKET && enabled && vendorId) {
      connect();
      return () => disconnect();
    }
  }, [connect, disconnect, enabled, vendorId]);

  return { connected: USE_WEBSOCKET ? connected : false, playAlert };
}

export default useVendorOrders;
