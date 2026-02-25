/**
 * Modifier service — Phase 4.12
 */

import api from './api';
import type { ModifierGroupDTO } from '../types/phase4.types';

export const modifierService = {
  /** Get modifier groups with modifiers for a menu item */
  async getModifiers(menuItemId: string): Promise<ModifierGroupDTO[]> {
    return api.get(`/menu-items/${menuItemId}/modifiers`) as Promise<ModifierGroupDTO[]>;
  },

  /** Create a modifier group */
  async createGroup(menuItemId: string, data: { name: string; required: boolean; minSelections: number; maxSelections: number }): Promise<ModifierGroupDTO> {
    return api.post(`/menu-items/${menuItemId}/modifier-groups`, data) as Promise<ModifierGroupDTO>;
  },

  /** Add a modifier to a group */
  async addModifier(groupId: string, data: { name: string; priceCents: number; available: boolean }): Promise<unknown> {
    return api.post(`/modifier-groups/${groupId}/modifiers`, data) as Promise<unknown>;
  },

  /** Delete a modifier group */
  async deleteGroup(groupId: string): Promise<void> {
    await api.delete(`/modifier-groups/${groupId}`);
  },

  /** Delete a modifier */
  async deleteModifier(modifierId: string): Promise<void> {
    await api.delete(`/modifiers/${modifierId}`);
  },

  /** Update a modifier group */
  async updateGroup(groupId: string, data: { name: string; required: boolean; minSelections: number; maxSelections: number }): Promise<ModifierGroupDTO> {
    return api.put(`/modifier-groups/${groupId}`, data) as Promise<ModifierGroupDTO>;
  },

  /** Update a modifier */
  async updateModifier(modifierId: string, data: { name: string; priceCents: number; available: boolean }): Promise<unknown> {
    return api.put(`/modifiers/${modifierId}`, data) as Promise<unknown>;
  },
};
