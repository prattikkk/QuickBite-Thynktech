/**
 * InventoryManagement — stock management for vendor menu items.
 * Phase 4.6
 */

import { useState, useEffect } from 'react';
import { inventoryService } from '../services/inventory.service';
import type { InventoryItem } from '../types/phase4.types';

interface InventoryManagementProps {
  vendorId: string;
}

export default function InventoryManagement({ vendorId }: InventoryManagementProps) {
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editStock, setEditStock] = useState(0);

  const loadInventory = async () => {
    setLoading(true);
    try {
      const data = await inventoryService.getInventory(vendorId);
      setItems(data);
    } catch {
      // silent
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInventory();
  }, [vendorId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleUpdateStock = async (itemId: string) => {
    try {
      await inventoryService.updateStock(vendorId, itemId, { stockCount: editStock });
      setEditingId(null);
      loadInventory();
    } catch {
      // silent
    }
  };

  const handleResetDaily = async () => {
    try {
      await inventoryService.resetDailyStock(vendorId);
      loadInventory();
    } catch {
      // silent
    }
  };

  if (loading) {
    return <div className="flex justify-center py-8"><div className="animate-spin h-8 w-8 border-b-2 border-primary-600 rounded-full" /></div>;
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-gray-900">Inventory</h3>
        <button
          onClick={handleResetDaily}
          className="px-4 py-2 text-sm font-medium text-primary-600 border border-primary-600 rounded-lg hover:bg-primary-50"
        >
          Reset Daily Stock
        </button>
      </div>

      {/* Table */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Item</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Stock</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {items.map((item) => (
              <tr key={item.itemId} className={item.lowStock ? 'bg-yellow-50' : ''}>
                <td className="px-4 py-3 text-sm text-gray-900">{item.name}</td>
                <td className="px-4 py-3 text-sm">
                  {editingId === item.itemId ? (
                    <div className="flex items-center gap-2">
                      <input
                        type="number"
                        value={editStock}
                        onChange={(e) => setEditStock(parseInt(e.target.value) || 0)}
                        className="w-20 px-2 py-1 border rounded text-sm"
                        min={-1}
                      />
                      <button
                        onClick={() => handleUpdateStock(item.itemId)}
                        className="text-green-600 text-sm font-medium"
                      >
                        Save
                      </button>
                      <button
                        onClick={() => setEditingId(null)}
                        className="text-gray-400 text-sm"
                      >
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <span className={`font-medium ${item.stockCount < 0 ? 'text-gray-500' : item.lowStock ? 'text-yellow-600' : 'text-gray-900'}`}>
                      {item.stockCount < 0 ? 'Unlimited' : item.stockCount}
                    </span>
                  )}
                </td>
                <td className="px-4 py-3">
                  {!item.available ? (
                    <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-red-100 text-red-700">Unavailable</span>
                  ) : item.lowStock ? (
                    <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-yellow-100 text-yellow-700">Low Stock</span>
                  ) : (
                    <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-green-100 text-green-700">In Stock</span>
                  )}
                </td>
                <td className="px-4 py-3">
                  <button
                    onClick={() => { setEditingId(item.itemId); setEditStock(item.stockCount); }}
                    className="text-primary-600 hover:text-primary-700 text-sm font-medium"
                  >
                    Edit
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {items.length === 0 && (
          <p className="text-center text-gray-400 py-8">No menu items found.</p>
        )}
      </div>
    </div>
  );
}
