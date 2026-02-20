/**
 * User and Authentication DTOs
 */

export interface UserDTO {
  id: string;
  email: string;
  fullName: string;
  phone: string;
  role: UserRole;
  status: UserStatus;
  createdAt: string;
}

export type UserRole = 'CUSTOMER' | 'VENDOR' | 'DRIVER' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  phone: string;
  roleName?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  email: string;
  name: string;
  role: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}
