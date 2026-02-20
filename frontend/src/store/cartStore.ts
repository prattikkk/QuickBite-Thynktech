/**
 * Cart store using Zustand
 * Manages shopping cart state
 */

import { create } from 'zustand';
import { CartItem } from '../types';

interface CartState {
  items: CartItem[];
  vendorId: string | null;
  vendorName: string | null;
}

interface CartActions {
  addItem: (item: Omit<CartItem, 'quantity'>) => void;
  removeItem: (menuItemId: string) => void;
  updateQuantity: (menuItemId: string, quantity: number) => void;
  clearCart: () => void;
  getSubtotalCents: () => number;
  getTotalItems: () => number;
}

type CartStore = CartState & CartActions;

const CART_STORAGE_KEY = 'quickbite_cart';

// Helper to save cart to localStorage
const saveToStorage = (items: CartItem[], vendorId: string | null, vendorName: string | null) => {
  localStorage.setItem(
    CART_STORAGE_KEY,
    JSON.stringify({ items, vendorId, vendorName })
  );
};

// Helper to load cart from localStorage
const loadFromStorage = (): CartState => {
  try {
    const stored = localStorage.getItem(CART_STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      return {
        items: parsed.items || [],
        vendorId: parsed.vendorId || null,
        vendorName: parsed.vendorName || null,
      };
    }
  } catch (error) {
    console.error('Failed to load cart from storage:', error);
  }
  return { items: [], vendorId: null, vendorName: null };
};

export const useCartStore = create<CartStore>((set, get) => ({
  // Load initial state from localStorage
  ...loadFromStorage(),

  // Actions
  addItem: (newItem) => {
    const state = get();
    const existingItemIndex = state.items.findIndex(
      (item) => item.menuItemId === newItem.menuItemId
    );

    let updatedItems: CartItem[];

    if (existingItemIndex >= 0) {
      // Item exists, increment quantity
      updatedItems = state.items.map((item, index) =>
        index === existingItemIndex ? { ...item, quantity: item.quantity + 1 } : item
      );
    } else {
      // New item, check if from same vendor
      if (state.vendorId && state.vendorId !== newItem.vendorId) {
        // Different vendor - warn user or clear cart
        // For now, we'll clear the cart and add the new item
        if (
          !confirm(
            `Your cart contains items from ${state.vendorName}. Clear cart to add items from ${newItem.vendorName}?`
          )
        ) {
          return;
        }
        updatedItems = [{ ...newItem, quantity: 1 }];
      } else {
        updatedItems = [...state.items, { ...newItem, quantity: 1 }];
      }
    }

    const vendorId = newItem.vendorId;
    const vendorName = newItem.vendorName;

    set({ items: updatedItems, vendorId, vendorName });
    saveToStorage(updatedItems, vendorId, vendorName);
  },

  removeItem: (menuItemId) => {
    const state = get();
    const updatedItems = state.items.filter((item) => item.menuItemId !== menuItemId);
    const vendorId = updatedItems.length > 0 ? state.vendorId : null;
    const vendorName = updatedItems.length > 0 ? state.vendorName : null;

    set({ items: updatedItems, vendorId, vendorName });
    saveToStorage(updatedItems, vendorId, vendorName);
  },

  updateQuantity: (menuItemId, quantity) => {
    if (quantity <= 0) {
      get().removeItem(menuItemId);
      return;
    }

    const state = get();
    const updatedItems = state.items.map((item) =>
      item.menuItemId === menuItemId ? { ...item, quantity } : item
    );

    set({ items: updatedItems });
    saveToStorage(updatedItems, state.vendorId, state.vendorName);
  },

  clearCart: () => {
    set({ items: [], vendorId: null, vendorName: null });
    localStorage.removeItem(CART_STORAGE_KEY);
  },

  getSubtotalCents: () => {
    const state = get();
    return state.items.reduce((total, item) => total + item.priceCents * item.quantity, 0);
  },

  getTotalItems: () => {
    const state = get();
    return state.items.reduce((total, item) => total + item.quantity, 0);
  },
}));
