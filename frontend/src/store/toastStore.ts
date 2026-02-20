/**
 * Toast store using Zustand
 * Manages notification toasts
 */

import { create } from 'zustand';
import { ToastMessage } from '../types';

interface ToastState {
  toasts: ToastMessage[];
}

interface ToastActions {
  addToast: (toast: Omit<ToastMessage, 'id'>) => void;
  removeToast: (id: string) => void;
  success: (message: string, duration?: number) => void;
  error: (message: string, duration?: number) => void;
  warning: (message: string, duration?: number) => void;
  info: (message: string, duration?: number) => void;
}

type ToastStore = ToastState & ToastActions;

export const useToastStore = create<ToastStore>((set, get) => ({
  toasts: [],

  addToast: (toast) => {
    const id = `toast-${Date.now()}-${Math.random()}`;
    const newToast: ToastMessage = { ...toast, id };

    set((state) => ({
      toasts: [...state.toasts, newToast],
    }));

    // Auto-remove toast after duration
    const duration = toast.duration || 5000;
    setTimeout(() => {
      get().removeToast(id);
    }, duration);
  },

  removeToast: (id) => {
    set((state) => ({
      toasts: state.toasts.filter((toast) => toast.id !== id),
    }));
  },

  success: (message, duration) => {
    get().addToast({ type: 'success', message, duration });
  },

  error: (message, duration) => {
    get().addToast({ type: 'error', message, duration });
  },

  warning: (message, duration) => {
    get().addToast({ type: 'warning', message, duration });
  },

  info: (message, duration) => {
    get().addToast({ type: 'info', message, duration });
  },
}));
