/**
 * Auth store using Zustand
 * Manages authentication state (user, token)
 */

import { create } from 'zustand';
import { UserDTO } from '../types';

interface AuthState {
  user: UserDTO | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

interface AuthActions {
  setAuth: (user: UserDTO, token: string) => void;
  logout: () => void;
  setLoading: (loading: boolean) => void;
  loadFromStorage: () => void;
}

type AuthStore = AuthState & AuthActions;

const AUTH_STORAGE_KEY = 'quickbite_auth';
const TOKEN_STORAGE_KEY = 'quickbite_token';

export const useAuthStore = create<AuthStore>((set) => ({
  // Initial state
  user: null,
  token: null,
  isAuthenticated: false,
  isLoading: true,

  // Actions
  setAuth: (user, token) => {
    // Store in localStorage
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user));
    localStorage.setItem(TOKEN_STORAGE_KEY, token);

    set({
      user,
      token,
      isAuthenticated: true,
      isLoading: false,
    });
  },

  logout: () => {
    // Clear localStorage
    localStorage.removeItem(AUTH_STORAGE_KEY);
    localStorage.removeItem(TOKEN_STORAGE_KEY);

    set({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
    });
  },

  setLoading: (loading) => {
    set({ isLoading: loading });
  },

  loadFromStorage: () => {
    try {
      const userStr = localStorage.getItem(AUTH_STORAGE_KEY);
      const token = localStorage.getItem(TOKEN_STORAGE_KEY);

      if (userStr && token) {
        const user = JSON.parse(userStr) as UserDTO;
        set({
          user,
          token,
          isAuthenticated: true,
          isLoading: false,
        });
      } else {
        set({ isLoading: false });
      }
    } catch (error) {
      console.error('Failed to load auth from storage:', error);
      set({ isLoading: false });
    }
  },
}));
