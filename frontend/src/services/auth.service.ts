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
    // Interceptor already unwraps ApiResponse.data
    return response;
  },

  /**
   * Register new user
   */
  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await api.post<any, any>('/auth/register', data);
    // Interceptor already unwraps ApiResponse.data
    return response;
  },

  /**
   * Refresh access token
   * TODO: Implement when backend supports refresh tokens
   */
  refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
    const response = await api.post<any, AuthResponse>('/auth/refresh', { refreshToken });
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
   * Logout (client-side only for now)
   */
  logout: () => {
    localStorage.removeItem('quickbite_token');
    localStorage.removeItem('quickbite_auth');
  },
};

export default authService;
