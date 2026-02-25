/**
 * Inventory service — Phase 4.6
 */

import api from './api';
import type { InventoryItem } from '../types/phase4.types';

export const inventoryService = {
  /** Get all items' inventory for a vendor */
  async getInventory(vendorId: string): Promise<InventoryItem[]> {
    return api.get(`/vendors/${vendorId}/inventory`) as Promise<InventoryItem[]>;
  },

  /** Update stock for a specific menu item */
  async updateStock(vendorId: string, itemId: string, data: {
    stockCount: number;
    lowStockThreshold?: number;
    autoDisableOnZero?: boolean;
  }): Promise<unknown> {
    return api.put(`/vendors/${vendorId}/inventory/${itemId}`, data) as Promise<unknown>;
  },

  /** Reset daily stock for all items */
  async resetDailyStock(vendorId: string): Promise<void> {
    await api.post(`/vendors/${vendorId}/inventory/reset-daily`);
  },
};
