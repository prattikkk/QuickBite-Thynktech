/**
 * Favorite service — customer favorite vendors
 */

import api from './api';
import { FavoriteDTO } from '../types';

export const favoriteService = {
  /** Add vendor to favorites */
  addFavorite: async (vendorId: string): Promise<FavoriteDTO> => {
    const res = await api.post<any, any>(`/favorites/${vendorId}`);
    return res?.data ?? res;
  },

  /** Remove vendor from favorites */
  removeFavorite: async (vendorId: string): Promise<void> => {
    await api.delete(`/favorites/${vendorId}`);
  },

  /** List all favorites */
  getFavorites: async (): Promise<FavoriteDTO[]> => {
    const res = await api.get<any, any>('/favorites');
    return res?.data ?? res ?? [];
  },

  /** Check if vendor is a favorite */
  isFavorite: async (vendorId: string): Promise<boolean> => {
    const res = await api.get<any, any>(`/favorites/${vendorId}/check`);
    const data = res?.data ?? res;
    return data?.isFavorite ?? false;
  },
};

export default favoriteService;
