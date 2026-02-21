/**
 * User profile service — view, edit, export, delete account
 */

import api from './api';

export interface UserProfile {
  id: string;
  email: string;
  name: string;
  phone: string | null;
  role: string;
  active: boolean;
  createdAt: string;
}

export interface ProfileUpdateRequest {
  name?: string;
  phone?: string;
}

export interface DataExport {
  profile: UserProfile;
  addresses: Array<Record<string, unknown>>;
  orders: Array<Record<string, unknown>>;
  favorites: Array<Record<string, unknown>>;
  exportedAt: string;
}

export const userService = {
  /** GET /api/users/me */
  getProfile: async (): Promise<UserProfile> => {
    const response = await api.get<any, UserProfile>('/users/me');
    return response;
  },

  /** PUT /api/users/me */
  updateProfile: async (dto: ProfileUpdateRequest): Promise<UserProfile> => {
    const response = await api.put<any, UserProfile>('/users/me', dto);
    return response;
  },

  /** GET /api/users/me/export */
  exportData: async (): Promise<DataExport> => {
    const response = await api.get<any, DataExport>('/users/me/export');
    return response;
  },

  /** DELETE /api/users/me */
  deleteAccount: async (): Promise<void> => {
    await api.delete('/users/me');
  },
};

export default userService;
