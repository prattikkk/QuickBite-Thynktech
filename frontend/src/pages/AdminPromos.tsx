/**
 * AdminPromos — Full CRUD management for promo codes (H1)
 */

import { useState, useEffect } from 'react';
import { promoService } from '../services/promo.service';
import type { PromoCodeDTO } from '../types/promo.types';
import { useToastStore } from '../store';
import { formatDateTime } from '../utils';
import Breadcrumbs from '../components/Breadcrumbs';
import ConfirmDialog from '../components/ConfirmDialog';
import EmptyState from '../components/EmptyState';
import { SkeletonTable } from '../components/Skeleton';

interface PromoFormData {
  code: string;
  description: string;
  discountType: 'PERCENT' | 'FIXED';
  discountValue: number;
  minOrderCents: number;
  maxDiscountCents: number;
  maxUses: number;
  maxUsesPerUser: number;
  validFrom: string;
  validUntil: string;
  active: boolean;
}

const EMPTY_FORM: PromoFormData = {
  code: '',
  description: '',
  discountType: 'PERCENT',
  discountValue: 0,
  minOrderCents: 0,
  maxDiscountCents: 0,
  maxUses: 0,
  maxUsesPerUser: 1,
  validFrom: '',
  validUntil: '',
  active: true,
};

export default function AdminPromos() {
  const [promos, setPromos] = useState<PromoCodeDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingPromo, setEditingPromo] = useState<PromoCodeDTO | null>(null);
  const [form, setForm] = useState<PromoFormData>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<PromoCodeDTO | null>(null);
  const [deleting, setDeleting] = useState(false);
  const { success, error: showError } = useToastStore();

  useEffect(() => {
    loadPromos();
  }, []);

  const loadPromos = async () => {
    try {
      setLoading(true);
      const data = await promoService.getPromos();
      setPromos(Array.isArray(data) ? data : []);
    } catch (err: any) {
      showError(err.message || 'Failed to load promo codes');
    } finally {
      setLoading(false);
    }
  };

  const openCreate = () => {
    setEditingPromo(null);
    setForm(EMPTY_FORM);
    setShowModal(true);
  };

  const openEdit = (promo: PromoCodeDTO) => {
    setEditingPromo(promo);
    setForm({
      code: promo.code,
      description: promo.description || '',
      discountType: promo.discountType,
      discountValue: promo.discountValue,
      minOrderCents: promo.minOrderCents,
      maxDiscountCents: promo.maxDiscountCents ?? 0,
      maxUses: promo.maxUses ?? 0,
      maxUsesPerUser: (promo as any).maxUsesPerUser ?? 1,
      validFrom: promo.validFrom ? promo.validFrom.slice(0, 16) : '',
      validUntil: promo.validUntil ? promo.validUntil.slice(0, 16) : '',
      active: promo.active,
    });
    setShowModal(true);
  };

  const handleSave = async () => {
    if (!form.code.trim()) {
      showError('Promo code is required');
      return;
    }
    try {
      setSaving(true);
      const payload = {
        ...form,
        code: form.code.toUpperCase().trim(),
        validFrom: form.validFrom || undefined,
        validUntil: form.validUntil || undefined,
        maxUses: form.maxUses || undefined,
        maxDiscountCents: form.maxDiscountCents || undefined,
      };

      if (editingPromo) {
        await promoService.updatePromo(editingPromo.id, payload);
        success('Promo code updated');
      } else {
        await promoService.createPromo(payload);
        success('Promo code created');
      }
      setShowModal(false);
      loadPromos();
    } catch (err: any) {
      showError(err.message || 'Failed to save promo code');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      setDeleting(true);
      await promoService.deletePromo(deleteTarget.id);
      success('Promo code deleted');
      setDeleteTarget(null);
      loadPromos();
    } catch (err: any) {
      showError(err.message || 'Failed to delete promo code');
    } finally {
      setDeleting(false);
    }
  };

  const getStatusBadge = (promo: PromoCodeDTO) => {
    if (!promo.active) return <span className="px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">Inactive</span>;
    if (promo.validUntil && new Date(promo.validUntil) < new Date()) return <span className="px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">Expired</span>;
    if (promo.maxUses && promo.currentUses >= promo.maxUses) return <span className="px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-800">Depleted</span>;
    return <span className="px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">Active</span>;
  };

  const filtered = promos.filter((p) =>
    p.code.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <Breadcrumbs items={[{ label: 'Admin', to: '/admin/health' }, { label: 'Promo Codes' }]} />

      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Promo Codes</h1>
        <button
          onClick={openCreate}
          className="bg-orange-500 hover:bg-orange-600 text-white font-semibold py-2 px-4 rounded-lg transition-colors"
        >
          + Create Promo
        </button>
      </div>

      {/* Search */}
      <div className="mb-4">
        <input
          type="text"
          placeholder="Search by code..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Search promo codes"
          className="w-full max-w-xs border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
        />
      </div>

      {loading ? (
        <div className="bg-white rounded-lg shadow-sm p-6">
          <SkeletonTable rows={5} cols={6} />
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          icon="🎟️"
          title="No promo codes"
          description={search ? 'No promo codes match your search.' : 'Create your first promo code to get started.'}
          action={!search ? { label: 'Create Promo', onClick: openCreate } : undefined}
        />
      ) : (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th scope="col" className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Code</th>
                <th scope="col" className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Type</th>
                <th scope="col" className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Value</th>
                <th scope="col" className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Usage</th>
                <th scope="col" className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Status</th>
                <th scope="col" className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Expiry</th>
                <th scope="col" className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filtered.map((promo) => (
                <tr key={promo.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-mono text-sm font-medium text-gray-900">{promo.code}</td>
                  <td className="px-4 py-3 text-sm text-gray-700">{promo.discountType}</td>
                  <td className="px-4 py-3 text-sm text-gray-700">
                    {promo.discountType === 'PERCENT'
                      ? `${promo.discountValue}%`
                      : `₹${(promo.discountValue / 100).toFixed(2)}`}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-700">
                    {promo.currentUses}{promo.maxUses ? `/${promo.maxUses}` : ''}
                  </td>
                  <td className="px-4 py-3">{getStatusBadge(promo)}</td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {promo.validUntil ? formatDateTime(promo.validUntil) : '—'}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <button
                        onClick={() => openEdit(promo)}
                        className="text-sm text-blue-600 hover:text-blue-800 font-medium"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => setDeleteTarget(promo)}
                        className="text-sm text-red-600 hover:text-red-800 font-medium"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Create/Edit Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setShowModal(false)}>
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="promo-modal-title"
            className="bg-white rounded-xl shadow-xl max-w-lg w-full mx-4 p-6 max-h-[90vh] overflow-y-auto"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 id="promo-modal-title" className="text-lg font-bold text-gray-900 mb-4">
              {editingPromo ? 'Edit Promo Code' : 'Create Promo Code'}
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Code *</label>
                <input
                  type="text"
                  value={form.code}
                  onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                  placeholder="e.g. SAVE20"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <input
                  type="text"
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                  placeholder="20% off for new users"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Discount Type</label>
                  <select
                    value={form.discountType}
                    onChange={(e) => setForm({ ...form, discountType: e.target.value as 'PERCENT' | 'FIXED' })}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                  >
                    <option value="PERCENT">Percentage</option>
                    <option value="FIXED">Fixed Amount</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    {form.discountType === 'PERCENT' ? 'Discount (%)' : 'Discount (cents)'}
                  </label>
                  <input
                    type="number"
                    value={form.discountValue}
                    onChange={(e) => setForm({ ...form, discountValue: Number(e.target.value) })}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                    min={0}
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Min Order (cents)</label>
                  <input
                    type="number"
                    value={form.minOrderCents}
                    onChange={(e) => setForm({ ...form, minOrderCents: Number(e.target.value) })}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                    min={0}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Max Discount (cents)</label>
                  <input
                    type="number"
                    value={form.maxDiscountCents}
                    onChange={(e) => setForm({ ...form, maxDiscountCents: Number(e.target.value) })}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                    min={0}
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Max Uses (0 = unlimited)</label>
                  <input
                    type="number"
                    value={form.maxUses}
                    onChange={(e) => setForm({ ...form, maxUses: Number(e.target.value) })}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                    min={0}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Per-User Limit</label>
                  <input
                    type="number"
                    value={form.maxUsesPerUser}
                    onChange={(e) => setForm({ ...form, maxUsesPerUser: Number(e.target.value) })}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                    min={1}
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Valid From</label>
                  <input
                    type="datetime-local"
                    value={form.validFrom}
                    onChange={(e) => setForm({ ...form, validFrom: e.target.value })}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Valid Until</label>
                  <input
                    type="datetime-local"
                    value={form.validUntil}
                    onChange={(e) => setForm({ ...form, validUntil: e.target.value })}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                  />
                </div>
              </div>
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={form.active}
                  onChange={(e) => setForm({ ...form, active: e.target.checked })}
                  className="h-4 w-4 text-orange-600 rounded border-gray-300"
                />
                <span className="text-sm text-gray-700">Active</span>
              </label>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button
                onClick={() => setShowModal(false)}
                className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-100 font-medium text-sm"
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="px-4 py-2 bg-orange-500 hover:bg-orange-600 text-white font-semibold rounded-lg text-sm disabled:opacity-50 transition-colors"
              >
                {saving ? 'Saving...' : editingPromo ? 'Update' : 'Create'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete Promo Code"
        message={`Are you sure you want to delete "${deleteTarget?.code}"? This cannot be undone.`}
        confirmLabel="Delete"
        variant="danger"
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
