/**
 * AdminCommissions — manage per-vendor commission rates (H2)
 * Backend: GET /admin/commissions/{vendorId}, PUT /admin/commissions/{vendorId}
 */

import { useState, useEffect } from 'react';
import { adminService } from '../services/admin.service';
import { useToastStore } from '../store';
import Breadcrumbs from '../components/Breadcrumbs';
import { SkeletonTable } from '../components/Skeleton';
import EmptyState from '../components/EmptyState';

interface VendorCommission {
  vendorId: string;
  vendorName: string;
  commissionBps: number; // basis points (e.g. 1500 = 15.00%)
  active: boolean;
}

export default function AdminCommissions() {
  const [vendors, setVendors] = useState<VendorCommission[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editValue, setEditValue] = useState('');
  const [saving, setSaving] = useState(false);
  const [search, setSearch] = useState('');
  const { success, error: showError } = useToastStore();

  useEffect(() => {
    loadVendors();
  }, []);

  const loadVendors = async () => {
    try {
      setLoading(true);
      // List all vendors with their commission info
      const data = await adminService.listVendors();
      const vendorList = Array.isArray(data) ? data : data?.content || [];
      const mapped: VendorCommission[] = vendorList.map((v: any) => ({
        vendorId: v.id,
        vendorName: v.name || v.businessName || 'Unknown',
        commissionBps: v.commissionBps ?? v.commissionRate ?? 1500,
        active: v.active ?? true,
      }));
      setVendors(mapped);
    } catch (err: any) {
      showError(err.message || 'Failed to load vendors');
    } finally {
      setLoading(false);
    }
  };

  const startEdit = (vendor: VendorCommission) => {
    setEditingId(vendor.vendorId);
    setEditValue((vendor.commissionBps / 100).toFixed(2));
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditValue('');
  };

  const handleSave = async (vendorId: string) => {
    const pct = parseFloat(editValue);
    if (isNaN(pct) || pct < 0 || pct > 100) {
      showError('Commission must be between 0% and 100%');
      return;
    }
    const bps = Math.round(pct * 100);
    try {
      setSaving(true);
      await adminService.setCommission(vendorId, bps);
      setVendors((prev) =>
        prev.map((v) => (v.vendorId === vendorId ? { ...v, commissionBps: bps } : v))
      );
      success('Commission updated');
      setEditingId(null);
    } catch (err: any) {
      showError(err.message || 'Failed to update commission');
    } finally {
      setSaving(false);
    }
  };

  const filtered = vendors.filter((v) =>
    v.vendorName.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <Breadcrumbs items={[{ label: 'Admin', to: '/admin/health' }, { label: 'Commissions' }]} />

      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Vendor Commissions</h1>
      </div>

      <div className="mb-4">
        <input
          type="text"
          placeholder="Search vendors..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Search vendors"
          className="w-full max-w-xs border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
        />
      </div>

      {loading ? (
        <div className="bg-white rounded-lg shadow-sm p-6">
          <SkeletonTable rows={6} cols={4} />
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          icon="💰"
          title="No vendors found"
          description={search ? 'No vendors match your search.' : 'No vendors have been registered yet.'}
        />
      ) : (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th scope="col" className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Vendor</th>
                <th scope="col" className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Status</th>
                <th scope="col" className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Commission Rate</th>
                <th scope="col" className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filtered.map((vendor) => (
                <tr key={vendor.vendorId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{vendor.vendorName}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${vendor.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'}`}>
                      {vendor.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    {editingId === vendor.vendorId ? (
                      <div className="flex items-center gap-2">
                        <input
                          type="number"
                          step="0.01"
                          min="0"
                          max="100"
                          value={editValue}
                          onChange={(e) => setEditValue(e.target.value)}
                          className="w-24 border border-gray-300 rounded px-2 py-1 text-sm focus:ring-2 focus:ring-orange-500"
                          autoFocus
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') handleSave(vendor.vendorId);
                            if (e.key === 'Escape') cancelEdit();
                          }}
                        />
                        <span className="text-sm text-gray-500">%</span>
                      </div>
                    ) : (
                      <span className="text-sm font-mono text-gray-700">
                        {(vendor.commissionBps / 100).toFixed(2)}%
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    {editingId === vendor.vendorId ? (
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleSave(vendor.vendorId)}
                          disabled={saving}
                          className="text-sm text-green-600 hover:text-green-800 font-medium disabled:opacity-50"
                        >
                          {saving ? 'Saving...' : 'Save'}
                        </button>
                        <button
                          onClick={cancelEdit}
                          className="text-sm text-gray-500 hover:text-gray-700 font-medium"
                        >
                          Cancel
                        </button>
                      </div>
                    ) : (
                      <button
                        onClick={() => startEdit(vendor)}
                        className="text-sm text-blue-600 hover:text-blue-800 font-medium"
                      >
                        Edit
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Info card */}
      <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
        <p className="text-sm text-blue-800">
          <strong>Note:</strong> Commission rates are in percentage. For example, 15.00% means the platform retains 15% of each order's subtotal for vendors with this rate.
        </p>
      </div>
    </div>
  );
}
