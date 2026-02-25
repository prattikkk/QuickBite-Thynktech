/**
 * Authentication service
 */

import api from './api';
import {
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  UserDTO,
} from '../types';

export const authService = {
  /**
   * Login user
   */
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await api.post<any, any>('/auth/login', credentials);
    return response;
  },

  /**
   * Register new user
   */
  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await api.post<any, any>('/auth/register', data);
    return response;
  },

  /**
   * Refresh access token
   * Refresh token is sent automatically via HttpOnly cookie
   */
  refreshToken: async (): Promise<AuthResponse> => {
    // No need to send refreshToken in body - it's in HttpOnly cookie
    const response = await api.post<any, AuthResponse>('/auth/refresh', {});
    return response;
  },

  /**
   * Get current user profile
   */
  getCurrentUser: async (): Promise<UserDTO> => {
    const response = await api.get<any, UserDTO>('/auth/me');
    return response;
  },

  /**
   * Request a password reset link.
   */
  forgotPassword: async (email: string): Promise<string | null> => {
    const response = await api.post<any, any>('/auth/forgot-password', { email });
    return response; // raw token (for dev/testing); in prod, user receives email
  },

  /**
   * Reset password using the token received via email/link.
   */
  resetPassword: async (token: string, newPassword: string): Promise<void> => {
    await api.post('/auth/reset-password', { token, newPassword });
  },

  /**
   * Verify email address using the token received via email/link.
   */
  verifyEmail: async (token: string): Promise<void> => {
    await api.post('/auth/verify-email', { token });
  },

  /**
   * Resend email verification link.
   */
  resendVerification: async (email: string): Promise<void> => {
    await api.post('/auth/resend-verification', { email });
  },

  /**
   * Logout — revoke refresh token on server, then clear local storage.
   * Refresh token is sent automatically via HttpOnly cookie.
   */
  logout: async () => {
    try {
      // Server reads refreshToken from HttpOnly cookie
      await api.post('/auth/logout', {});
    } catch {
      // Best-effort: server may be unreachable, proceed with local cleanup
    } finally {
      localStorage.removeItem('quickbite_token');
      localStorage.removeItem('quickbite_auth');
    }
  },
};

export default authService;
