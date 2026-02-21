/**
 * Address service
 */

import api from './api';
import { AddressDTO } from '../types';

export const addressService = {
  /**
   * Get user addresses
   */
  getMyAddresses: async (): Promise<AddressDTO[]> => {
    const response = await api.get<any, AddressDTO[]>('/addresses');
    return response;
  },

  /**
   * Create new address
   */
  createAddress: async (address: Omit<AddressDTO, 'id'>): Promise<AddressDTO> => {
    const response = await api.post<any, AddressDTO>('/addresses', address);
    return response;
  },

  /**
   * Update address
   */
  updateAddress: async (id: string, address: Partial<AddressDTO>): Promise<AddressDTO> => {
    const response = await api.put<any, AddressDTO>(`/addresses/${id}`, address);
    return response;
  },

  /**
   * Delete address
   */
  deleteAddress: async (id: string): Promise<void> => {
    await api.delete(`/addresses/${id}`);
  },
};

export default addressService;
