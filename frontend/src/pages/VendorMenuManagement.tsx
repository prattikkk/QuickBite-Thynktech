/**
 * Vendor Menu Management — full CRUD for menu items
 */

import { useState, useEffect, FormEvent } from 'react';
import { vendorService } from '../services';
import { MenuItemDTO, MenuItemCreateRequest, VendorDTO } from '../types';
import { LoadingSpinner } from '../components';
import { formatCurrencyCompact } from '../utils';
import { useToastStore } from '../store';

interface Props {
  vendor: VendorDTO;
}

export default function VendorMenuManagement({ vendor }: Props) {
  const [items, setItems] = useState<MenuItemDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingItem, setEditingItem] = useState<MenuItemDTO | null>(null);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const { success, error: showError } = useToastStore();

  const emptyForm: MenuItemCreateRequest = {
    name: '',
    description: '',
    priceCents: 0,
    available: true,
    prepTimeMins: 15,
    category: '',
    imageUrl: '',
  };

  const [form, setForm] = useState<MenuItemCreateRequest>(emptyForm);

  useEffect(() => {
    loadMenu();
  }, [vendor.id]);

  const loadMenu = async () => {
    try {
      const data = await vendorService.getVendorMenu(vendor.id, { size: 200 } as any);
      setItems(data);
    } catch (err: any) {
      showError(err.message || 'Failed to load menu');
    } finally {
      setLoading(false);
    }
  };

  const openCreateForm = () => {
    setEditingItem(null);
    setForm(emptyForm);
    setShowForm(true);
  };

  const openEditForm = (item: MenuItemDTO) => {
    setEditingItem(item);
    setForm({
      name: item.name,
      description: item.description || '',
      priceCents: item.priceCents,
      available: item.available,
      prepTimeMins: item.prepTimeMins,
      category: item.category || '',
      imageUrl: item.imageUrl || '',
    });
    setShowForm(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) {
      showError('Item name is required');
      return;
    }
    if (form.priceCents <= 0) {
      showError('Price must be greater than 0');
      return;
    }

    setSaving(true);
    try {
      if (editingItem) {
        await vendorService.updateMenuItem(editingItem.id, form);
        success('Menu item updated');
      } else {
        await vendorService.createMenuItem(vendor.id, form);
        success('Menu item added');
      }
      setShowForm(false);
      setEditingItem(null);
      setForm(emptyForm);
      loadMenu();
    } catch (err: any) {
      showError(err.message || 'Failed to save menu item');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (itemId: string) => {
    if (!confirm('Delete this menu item?')) return;
    setDeletingId(itemId);
    try {
      await vendorService.deleteMenuItem(itemId);
      success('Menu item deleted');
      loadMenu();
    } catch (err: any) {
      showError(err.message || 'Failed to delete');
    } finally {
      setDeletingId(null);
    }
  };

  const handleToggleAvailability = async (item: MenuItemDTO) => {
    try {
      await vendorService.updateMenuItem(item.id, {
        name: item.name,
        priceCents: item.priceCents,
        available: !item.available,
      });
      success(item.available ? 'Item marked unavailable' : 'Item marked available');
      loadMenu();
    } catch (err: any) {
      showError(err.message || 'Failed to update availability');
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  // Group items by category
  const categories = [...new Set(items.map((i) => i.category || 'Uncategorized'))];

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Menu Items</h2>
          <p className="text-sm text-gray-500">{items.length} items total</p>
        </div>
        <button
          onClick={openCreateForm}
          className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 font-medium"
        >
          + Add Item
        </button>
      </div>

      {/* Create / Edit form modal */}
      {showForm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              <h3 className="text-lg font-bold mb-4">
                {editingItem ? 'Edit Menu Item' : 'Add Menu Item'}
              </h3>
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                    placeholder="e.g. Classic Burger"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                  <textarea
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                    rows={2}
                    placeholder="A brief description of this item"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Price (₹) <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      min="0.01"
                      value={(form.priceCents / 100).toFixed(2)}
                      onChange={(e) =>
                        setForm({ ...form, priceCents: Math.round(parseFloat(e.target.value || '0') * 100) })
                      }
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Prep Time (mins)</label>
                    <input
                      type="number"
                      min="1"
                      value={form.prepTimeMins || 15}
                      onChange={(e) => setForm({ ...form, prepTimeMins: parseInt(e.target.value) || 15 })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
                  <input
                    type="text"
                    value={form.category}
                    onChange={(e) => setForm({ ...form, category: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                    placeholder="e.g. Burgers, Drinks, Desserts"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Image URL</label>
                  <input
                    type="url"
                    value={form.imageUrl}
                    onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                    placeholder="https://..."
                  />
                </div>

                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="available"
                    checked={form.available}
                    onChange={(e) => setForm({ ...form, available: e.target.checked })}
                    className="h-4 w-4 text-primary-600 border-gray-300 rounded"
                  />
                  <label htmlFor="available" className="text-sm text-gray-700">
                    Available for ordering
                  </label>
                </div>

                <div className="flex gap-3 pt-2">
                  <button
                    type="submit"
                    disabled={saving}
                    className="flex-1 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 font-medium"
                  >
                    {saving ? 'Saving...' : editingItem ? 'Update Item' : 'Add Item'}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setShowForm(false);
                      setEditingItem(null);
                    }}
                    className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Menu items list */}
      {items.length === 0 ? (
        <div className="bg-white rounded-lg shadow-md p-12 text-center">
          <div className="text-5xl mb-4">🍽️</div>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">No menu items yet</h3>
          <p className="text-gray-500 mb-4">
            Add your first menu item so customers can start ordering!
          </p>
          <button
            onClick={openCreateForm}
            className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 font-medium"
          >
            + Add Your First Item
          </button>
        </div>
      ) : (
        <div className="space-y-6">
          {categories.map((category) => (
            <div key={category}>
              <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">
                {category}
              </h3>
              <div className="space-y-2">
                {items
                  .filter((i) => (i.category || 'Uncategorized') === category)
                  .map((item) => (
                    <div
                      key={item.id}
                      className={`bg-white rounded-lg shadow-sm border p-4 flex justify-between items-center ${
                        !item.available ? 'opacity-60' : ''
                      }`}
                    >
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <h4 className="font-medium text-gray-900 truncate">{item.name}</h4>
                          {!item.available && (
                            <span className="px-2 py-0.5 bg-red-100 text-red-700 text-xs rounded-full">
                              Unavailable
                            </span>
                          )}
                        </div>
                        {item.description && (
                          <p className="text-sm text-gray-500 truncate mt-0.5">{item.description}</p>
                        )}
                        <div className="flex items-center gap-3 mt-1 text-sm text-gray-600">
                          <span className="font-semibold text-gray-900">
                            {formatCurrencyCompact(item.priceCents)}
                          </span>
                          <span>·</span>
                          <span>{item.prepTimeMins} min</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 ml-4">
                        <button
                          onClick={() => handleToggleAvailability(item)}
                          className={`px-3 py-1.5 text-xs font-medium rounded-lg ${
                            item.available
                              ? 'bg-yellow-100 text-yellow-800 hover:bg-yellow-200'
                              : 'bg-green-100 text-green-800 hover:bg-green-200'
                          }`}
                        >
                          {item.available ? 'Mark Unavailable' : 'Mark Available'}
                        </button>
                        <button
                          onClick={() => openEditForm(item)}
                          className="px-3 py-1.5 text-xs font-medium bg-blue-100 text-blue-800 rounded-lg hover:bg-blue-200"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleDelete(item.id)}
                          disabled={deletingId === item.id}
                          className="px-3 py-1.5 text-xs font-medium bg-red-100 text-red-800 rounded-lg hover:bg-red-200 disabled:opacity-50"
                        >
                          {deletingId === item.id ? '...' : 'Delete'}
                        </button>
                      </div>
                    </div>
                  ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
