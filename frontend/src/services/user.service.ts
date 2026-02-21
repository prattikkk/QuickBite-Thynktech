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
    const res = await api.get<any>('/users/me');
    return res.data?.data ?? res.data;
  },

  /** PUT /api/users/me */
  updateProfile: async (dto: ProfileUpdateRequest): Promise<UserProfile> => {
    const res = await api.put<any>('/users/me', dto);
    return res.data?.data ?? res.data;
  },

  /** GET /api/users/me/export */
  exportData: async (): Promise<DataExport> => {
    const res = await api.get<any>('/users/me/export');
    return res.data?.data ?? res.data;
  },

  /** DELETE /api/users/me */
  deleteAccount: async (): Promise<void> => {
    await api.delete('/users/me');
  },
};

export default userService;
