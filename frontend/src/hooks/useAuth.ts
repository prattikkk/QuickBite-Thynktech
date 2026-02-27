/**
 * useAuth hook
 * Provides authentication utilities and user info
 */

import { useAuthStore } from '../store';
import { authService } from '../services';
import { useToastStore } from '../store/toastStore';
import { LoginRequest, RegisterRequest, UserDTO, UserRole } from '../types';
import { useNavigate } from 'react-router-dom';

export const useAuth = () => {
  const { user, token, isAuthenticated, isLoading, setAuth, logout: storeLogout, loadFromStorage } = useAuthStore();
  const { success, error } = useToastStore();
  const navigate = useNavigate();

  /**
   * Login user
   */
  const login = async (credentials: LoginRequest) => {
    try {
      const response = await authService.login(credentials);
      // Convert flat response to UserDTO
      const user: UserDTO = {
        id: response.userId,
        email: response.email,
        fullName: response.name,
        phone: '',
        role: response.role as UserRole,
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
      };
      setAuth(user, response.accessToken);
      success('Login successful!');
      return user;
    } catch (err: any) {
      // If the error is EMAIL_NOT_VERIFIED, throw with a special marker
      const msg: string = err.message || 'Login failed';
      if (msg.includes('EMAIL_NOT_VERIFIED')) {
        error('Please verify your email before logging in. A new verification link has been sent.');
        throw new Error('EMAIL_NOT_VERIFIED');
      }
      error(msg);
      throw err;
    }
  };

  /**
   * Register new user
   */
  const register = async (data: RegisterRequest) => {
    try {
      const response = await authService.register(data);
      // Registration no longer returns tokens — user must verify email first
      // Return a minimal user object for the UI to show the verification notice
      const user: UserDTO = {
        id: response.userId,
        email: response.email,
        fullName: response.name,
        phone: data.phone,
        role: response.role as UserRole,
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
      };
      // Do NOT call setAuth — no token was issued
      success('Registration successful! Please check your email to verify your account.');
      return user;
    } catch (err: any) {
      const errorMessage = err.message || err.response?.data?.message || 'Registration failed';
      error(errorMessage);
      throw err;
    }
  };

  /**
   * Logout user
   */
  const logout = () => {
    authService.logout();
    storeLogout();
    success('Logged out successfully');
    navigate('/login');
  };

  /**
   * Check if user has a specific role
   */
  const hasRole = (role: string): boolean => {
    return user?.role === role;
  };

  /**
   * Check if user is customer
   */
  const isCustomer = (): boolean => hasRole('CUSTOMER');

  /**
   * Check if user is vendor
   */
  const isVendor = (): boolean => hasRole('VENDOR');

  /**
   * Check if user is driver
   */
  const isDriver = (): boolean => hasRole('DRIVER');

  /**
   * Check if user is admin
   */
  const isAdmin = (): boolean => hasRole('ADMIN');

  return {
    user,
    token,
    isAuthenticated,
    isLoading,
    login,
    register,
    logout,
    loadFromStorage,
    hasRole,
    isCustomer,
    isVendor,
    isDriver,
    isAdmin,
  };
};

export default useAuth;
