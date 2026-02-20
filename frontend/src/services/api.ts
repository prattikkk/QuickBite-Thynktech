/**
 * Axios instance with interceptors for auth, idempotency, and error handling
 */

import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiError } from '../types';

/**
 * Generate a unique idempotency key (UUID v4).
 * Used to prevent duplicate order/payment creation on retries.
 */
function generateIdempotencyKey(): string {
  return crypto.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

/** Endpoints that require an Idempotency-Key header on POST */
const IDEMPOTENT_ENDPOINTS = ['/orders', '/payments/intent'];

// Create axios instance with default config
const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT token and idempotency key
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('quickbite_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Attach Idempotency-Key for critical POST endpoints
    if (
      config.method === 'post' &&
      config.headers &&
      IDEMPOTENT_ENDPOINTS.some((ep) => config.url?.startsWith(ep))
    ) {
      if (!config.headers['Idempotency-Key']) {
        config.headers['Idempotency-Key'] = generateIdempotencyKey();
      }
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => {
    // Backend wraps responses in ApiResponse { success, message, data }
    // Extract the actual data payload
    return response.data?.data !== undefined ? response.data.data : response.data;
  },
  async (error: AxiosError<any>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Handle 401 Unauthorized - attempt token refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // TODO: Implement refresh token mechanism when backend supports it
        // For now, just redirect to login
        localStorage.removeItem('quickbite_token');
        localStorage.removeItem('quickbite_auth');
        window.location.href = '/login';
        return Promise.reject(error);
      } catch (refreshError) {
        // Refresh failed - redirect to login
        localStorage.removeItem('quickbite_token');
        localStorage.removeItem('quickbite_auth');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    // Format error for consistent handling
    const apiError: ApiError = {
      message: error.response?.data?.message || error.message || 'An error occurred',
      status: error.response?.status || 500,
      errors: error.response?.data?.errors,
      timestamp: error.response?.data?.timestamp,
      path: error.response?.data?.path,
    };

    return Promise.reject(apiError);
  }
);

export default api;
