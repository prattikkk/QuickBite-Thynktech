/**
 * AdminManagement page — user ban/activate, vendor approve/deactivate
 */

import { useState, useEffect } from 'react';
import { adminService } from '../services';
import { LoadingSpinner } from '../components';
import { useToastStore } from '../store';

interface UserRow {
  id: string;
  email: string;
  name: string;
  role: string;
  active: boolean;
  createdAt: string;
}

interface VendorRow {
  id: string;
  name: string;
  active: boolean;
  ownerEmail: string;
}

type Tab = 'users' | 'vendors';

export default function AdminManagement() {
  const [tab, setTab] = useState<Tab>('users');
  const [users, setUsers] = useState<UserRow[]>([]);
  const [vendors, setVendors] = useState<VendorRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const { success, error: showError } = useToastStore();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [usersData, vendorsData] = await Promise.all([
        adminService.listUsers({ page: 0, size: 100 }),
        adminService.listVendors(),
      ]);
      setUsers(Array.isArray(usersData?.content) ? usersData.content : Array.isArray(usersData) ? usersData : []);
      setVendors(Array.isArray(vendorsData) ? vendorsData : []);
    } catch (err: any) {
      showError(err.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleUser = async (userId: string, currentActive: boolean) => {
    try {
      setActionLoading(userId);
      await adminService.setUserStatus(userId, !currentActive);
      success(`User ${currentActive ? 'banned' : 'activated'}`);
      setUsers((prev) =>
        prev.map((u) => (u.id === userId ? { ...u, active: !currentActive } : u))
      );
    } catch (err: any) {
      showError(err.message || 'Failed to update user');
    } finally {
      setActionLoading(null);
    }
  };

  const handleToggleVendor = async (vendorId: string, currentActive: boolean) => {
    try {
      setActionLoading(vendorId);
      await adminService.approveVendor(vendorId, !currentActive);
      success(`Vendor ${currentActive ? 'deactivated' : 'approved'}`);
      setVendors((prev) =>
        prev.map((v) => (v.id === vendorId ? { ...v, active: !currentActive } : v))
      );
    } catch (err: any) {
      showError(err.message || 'Failed to update vendor');
    } finally {
      setActionLoading(null);
    }
  };

  const filteredUsers = users.filter(
    (u) =>
      !search ||
      u.email.toLowerCase().includes(search.toLowerCase()) ||
      u.name?.toLowerCase().includes(search.toLowerCase())
  );

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">Admin Management</h1>

        {/* Tabs */}
        <div className="flex space-x-1 bg-gray-200 rounded-lg p-1 mb-6">
          <button
            onClick={() => setTab('users')}
            className={`flex-1 py-2 px-4 text-sm font-medium rounded-md transition-colors ${
              tab === 'users' ? 'bg-white text-primary-700 shadow' : 'text-gray-600 hover:text-gray-800'
            }`}
          >
            Users ({users.length})
          </button>
          <button
            onClick={() => setTab('vendors')}
            className={`flex-1 py-2 px-4 text-sm font-medium rounded-md transition-colors ${
              tab === 'vendors' ? 'bg-white text-primary-700 shadow' : 'text-gray-600 hover:text-gray-800'
            }`}
          >
            Vendors ({vendors.length})
          </button>
        </div>

        {/* Users tab */}
        {tab === 'users' && (
          <>
            <div className="mb-4">
              <input
                type="text"
                placeholder="Search users by name or email..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
            </div>
            <div className="bg-white rounded-lg shadow overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Role</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {filteredUsers.map((user) => (
                    <tr key={user.id}>
                      <td className="px-6 py-4 text-sm text-gray-900">{user.name || '—'}</td>
                      <td className="px-6 py-4 text-sm text-gray-600">{user.email}</td>
                      <td className="px-6 py-4">
                        <span className="px-2 py-1 text-xs font-medium bg-blue-100 text-blue-800 rounded-full">
                          {user.role}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <span
                          className={`px-2 py-1 text-xs font-medium rounded-full ${
                            user.active
                              ? 'bg-green-100 text-green-800'
                              : 'bg-red-100 text-red-800'
                          }`}
                        >
                          {user.active ? 'Active' : 'Banned'}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <button
                          onClick={() => handleToggleUser(user.id, user.active)}
                          disabled={actionLoading === user.id}
                          className={`px-3 py-1 text-sm rounded-lg font-medium disabled:opacity-50 ${
                            user.active
                              ? 'bg-red-100 text-red-700 hover:bg-red-200'
                              : 'bg-green-100 text-green-700 hover:bg-green-200'
                          }`}
                        >
                          {user.active ? 'Ban' : 'Activate'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* Vendors tab */}
        {tab === 'vendors' && (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {vendors.map((vendor) => (
                  <tr key={vendor.id}>
                    <td className="px-6 py-4 text-sm text-gray-900">{vendor.name}</td>
                    <td className="px-6 py-4">
                      <span
                        className={`px-2 py-1 text-xs font-medium rounded-full ${
                          vendor.active
                            ? 'bg-green-100 text-green-800'
                            : 'bg-yellow-100 text-yellow-800'
                        }`}
                      >
                        {vendor.active ? 'Active' : 'Pending'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => handleToggleVendor(vendor.id, vendor.active)}
                        disabled={actionLoading === vendor.id}
                        className={`px-3 py-1 text-sm rounded-lg font-medium disabled:opacity-50 ${
                          vendor.active
                            ? 'bg-red-100 text-red-700 hover:bg-red-200'
                            : 'bg-green-100 text-green-700 hover:bg-green-200'
                        }`}
                      >
                        {vendor.active ? 'Deactivate' : 'Approve'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
