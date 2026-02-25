/**
 * ModifierSelector — UI for selecting modifiers when adding items to cart.
 * Phase 4.12
 */

import { useState, useEffect } from 'react';
import { modifierService } from '../services/modifier.service';
import type { ModifierGroupDTO, SelectedModifier } from '../types/phase4.types';

interface ModifierSelectorProps {
  menuItemId: string;
  onSelectModifiers: (modifiers: SelectedModifier[], totalExtraCents: number) => void;
}

export default function ModifierSelector({ menuItemId, onSelectModifiers }: ModifierSelectorProps) {
  const [groups, setGroups] = useState<ModifierGroupDTO[]>([]);
  const [selected, setSelected] = useState<Record<string, SelectedModifier[]>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    modifierService
      .getModifiers(menuItemId)
      .then((data) => {
        setGroups(data);
        // Initialize selections
        const init: Record<string, SelectedModifier[]> = {};
        data.forEach((g) => { init[g.id] = []; });
        setSelected(init);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [menuItemId]);

  const handleToggle = (groupId: string, mod: { id: string; name: string; priceCents: number }, group: ModifierGroupDTO) => {
    setSelected((prev) => {
      const current = prev[groupId] || [];
      const exists = current.find((m) => m.modifierId === mod.id);

      let updated: SelectedModifier[];
      if (exists) {
        updated = current.filter((m) => m.modifierId !== mod.id);
      } else {
        if (group.maxSelections === 1) {
          // Single select — replace
          updated = [{ modifierId: mod.id, name: mod.name, priceCents: mod.priceCents }];
        } else if (current.length < group.maxSelections) {
          updated = [...current, { modifierId: mod.id, name: mod.name, priceCents: mod.priceCents }];
        } else {
          return prev; // max reached
        }
      }

      const newSelected = { ...prev, [groupId]: updated };

      // Notify parent
      const allMods = Object.values(newSelected).flat();
      const totalExtra = allMods.reduce((sum, m) => sum + m.priceCents, 0);
      onSelectModifiers(allMods, totalExtra);

      return newSelected;
    });
  };

  if (loading || groups.length === 0) return null;

  return (
    <div className="space-y-4 mt-3">
      {groups.map((group) => (
        <div key={group.id} className="border rounded-lg p-3">
          <div className="flex items-center justify-between mb-2">
            <h4 className="text-sm font-semibold text-gray-900">
              {group.name}
              {group.required && <span className="text-red-500 ml-1">*</span>}
            </h4>
            <span className="text-xs text-gray-500">
              {group.maxSelections === 1 ? 'Choose 1' : `Choose up to ${group.maxSelections}`}
            </span>
          </div>
          <div className="space-y-1">
            {group.modifiers
              .filter((m) => m.available)
              .map((mod) => {
                const isSelected = (selected[group.id] || []).some((s) => s.modifierId === mod.id);
                return (
                  <button
                    key={mod.id}
                    onClick={() => handleToggle(group.id, mod, group)}
                    className={`w-full flex items-center justify-between px-3 py-2 rounded-md text-sm transition-colors ${
                      isSelected
                        ? 'bg-primary-50 border border-primary-500 text-primary-700'
                        : 'bg-gray-50 border border-transparent hover:bg-gray-100 text-gray-700'
                    }`}
                  >
                    <span>{mod.name}</span>
                    {mod.priceCents > 0 && (
                      <span className="text-xs text-gray-500">+${(mod.priceCents / 100).toFixed(2)}</span>
                    )}
                  </button>
                );
              })}
          </div>
        </div>
      ))}
    </div>
  );
}
