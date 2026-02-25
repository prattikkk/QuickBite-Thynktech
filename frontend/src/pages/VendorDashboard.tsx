/**
 * VendorDashboard — tabbed layout: Orders | KDS | Menu | Scheduled | Analytics | Inventory | Profile
 */

import { useState, useEffect, useRef } from 'react';
import { vendorService } from '../services';
import { OrderDTO, VendorDTO } from '../types';
import { LoadingSpinner } from '../components';
import EmptyState from '../components/EmptyState';
import { formatCurrencyCompact, formatDateTime } from '../utils';
import { useToastStore } from '../store';
import { useVendorOrders } from '../hooks/useVendorOrders';
import VendorMenuManagement from './VendorMenuManagement';
import VendorProfile from './VendorProfile';
import VendorAnalytics from '../components/VendorAnalytics';
import InventoryManagement from '../components/InventoryManagement';

type Tab = 'orders' | 'kds' | 'menu' | 'scheduled' | 'analytics' | 'inventory' | 'profile';

// ── Canned stock messages ─────────────────────────────────────────────────
const CANNED_MESSAGES = [
  'Item out of stock',
  'Too busy right now',
  'Closing soon',
  'Kitchen issue — please reorder later',
  'Delivery area not covered',
  'Custom reason…',
];

// ── Driver type ───────────────────────────────────────────────────────────
interface OnlineDriver {
  driverId: string;
  name: string;
  vehicleType: string;
  licensePlate: string | null;
}

export default function VendorDashboard() {
  const [tab, setTab] = useState<Tab>('orders');
  const [vendor, setVendor] = useState<VendorDTO | null>(null);
  const [vendorLoading, setVendorLoading] = useState(true);
  const [orders, setOrders] = useState<OrderDTO[]>([]);
  const [ordersLoading, setOrdersLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [rejectTarget, setRejectTarget] = useState<string | null>(null);
  const [rejecting, setRejecting] = useState(false);
  const [rejectMessage, setRejectMessage] = useState('');
  const [rejectCustom, setRejectCustom] = useState('');
  // Runner assignment
  const [assignTarget, setAssignTarget] = useState<string | null>(null);
  const [onlineDrivers, setOnlineDrivers] = useState<OnlineDriver[]>([]);
  const [assigningDriver, setAssigningDriver] = useState(false);
  // Persistent alert: track order IDs that are PLACED and need acknowledgement
  const [pendingAlertIds, setPendingAlertIds] = useState<Set<string>>(new Set());
  const alertIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const { success, error: showError } = useToastStore();

  // WebSocket hook for real-time KDS updates (M4)
  const { playAlert } = useVendorOrders({
    vendorId: vendor?.id ?? null,
    enabled: !!vendor,
    onNewOrder: (order) => {
      success('New order received!');
      setPendingAlertIds((prev) => new Set(prev).add(order.id));
      loadOrders();
    },
    onOrderUpdate: () => loadOrders(),
  });

  // Load vendor profile
  useEffect(() => {
    loadVendorProfile();
  }, []);

  // Load orders initially, then auto-refresh only on orders/kds tabs
  useEffect(() => {
    loadOrders();
  }, []);

  useEffect(() => {
    if (tab === 'orders' || tab === 'kds') {
      const interval = setInterval(loadOrders, 10000);
      return () => clearInterval(interval);
    }
  }, [tab]);

  // Persistent alert: beep every 3s while there are unacknowledged PLACED orders
  useEffect(() => {
    if (pendingAlertIds.size > 0) {
      if (!alertIntervalRef.current) {
        alertIntervalRef.current = setInterval(() => {
          playAlert();
        }, 3000);
      }
    } else {
      if (alertIntervalRef.current) {
        clearInterval(alertIntervalRef.current);
        alertIntervalRef.current = null;
      }
    }
    return () => {
      if (alertIntervalRef.current) {
        clearInterval(alertIntervalRef.current);
        alertIntervalRef.current = null;
      }
    };
  }, [pendingAlertIds.size, playAlert]);

  const loadVendorProfile = async () => {
    try {
      const data = await vendorService.getMyVendorProfile();
      setVendor(data);
      // If no restaurant profile yet, show profile tab right away
      if (!data) setTab('profile');
    } catch (err: any) {
      showError(err.message || 'Failed to load vendor profile');
    } finally {
      setVendorLoading(false);
    }
  };

  const loadOrders = async () => {
    try {
      const data = await vendorService.getVendorOrders({ page: 0, size: 50 });
      const orderList = (data as any)?.content || data || [];
      setOrders(Array.isArray(orderList) ? orderList : []);
    } catch (err: any) {
      // silently fail on auto-refresh
      if (ordersLoading) showError(err.message || 'Failed to load orders');
    } finally {
      setOrdersLoading(false);
    }
  };

  const handleAccept = async (orderId: string) => {
    try {
      setActionLoading(orderId);
      await vendorService.acceptOrder(orderId);
      // Dismiss persistent alert for this order
      setPendingAlertIds((prev) => {
        const next = new Set(prev);
        next.delete(orderId);
        return next;
      });
      success('Order accepted');
      loadOrders();
    } catch (err: any) {
      showError(err.message || 'Failed to accept order');
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async (orderId: string) => {
    setRejectMessage('');
    setRejectCustom('');
    setRejectTarget(orderId);
  };

  const confirmReject = async () => {
    if (!rejectTarget) return;
    const reason = rejectMessage === 'Custom reason…' || rejectMessage === ''
      ? (rejectCustom.trim() || 'Rejected by vendor')
      : rejectMessage;
    try {
      setRejecting(true);
      setActionLoading(rejectTarget);
      await vendorService.rejectOrder(rejectTarget, reason);
      setPendingAlertIds((prev) => {
        const next = new Set(prev);
        next.delete(rejectTarget);
        return next;
      });
      success('Order rejected');
      loadOrders();
    } catch (err: any) {
      showError(err.message || 'Failed to reject order');
    } finally {
      setActionLoading(null);
      setRejecting(false);
      setRejectTarget(null);
    }
  };

  // Open runner assignment modal
  const handleOpenAssign = async (orderId: string) => {
    setAssignTarget(orderId);
    try {
      const drivers = await vendorService.getOnlineDrivers();
      setOnlineDrivers(drivers);
    } catch {
      setOnlineDrivers([]);
    }
  };

  const handleAssignDriver = async (driverId: string) => {
    if (!assignTarget) return;
    try {
      setAssigningDriver(true);
      await vendorService.assignDriverToOrder(assignTarget, driverId);
      success('Runner assigned!');
      setAssignTarget(null);
      loadOrders();
    } catch (err: any) {
      showError(err.message || 'Failed to assign runner');
    } finally {
      setAssigningDriver(false);
    }
  };

  const handleMarkPreparing = async (orderId: string) => {
    try {
      setActionLoading(orderId);
      await vendorService.markOrderPreparing(orderId);
      success('Order marked as preparing');
      loadOrders();
    } catch (err: any) {
      showError(err.message || 'Failed to update order');
    } finally {
      setActionLoading(null);
    }
  };

  const handleMarkReady = async (orderId: string) => {
    try {
      setActionLoading(orderId);
      await vendorService.markOrderReady(orderId);
      success('Order marked as ready');
      loadOrders();
    } catch (err: any) {
      showError(err.message || 'Failed to update order');
    } finally {
      setActionLoading(null);
    }
  };

  if (vendorLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  const tabs: { key: Tab; label: string; icon: string }[] = [
    { key: 'orders', label: 'Orders', icon: '📋' },
    { key: 'kds', label: 'Kitchen', icon: '🍳' },
    { key: 'menu', label: 'Menu', icon: '🍽️' },
    { key: 'scheduled', label: 'Scheduled', icon: '📅' },
    { key: 'analytics', label: 'Analytics', icon: '📊' },
    { key: 'inventory', label: 'Inventory', icon: '📦' },
    { key: 'profile', label: 'Profile', icon: '⚙️' },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
            {vendor ? vendor.name : 'Vendor Dashboard'}
          </h1>
          {vendor && (
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
              {vendor.active ? '🟢 Active' : '🔴 Inactive'} · {vendor.menuItemCount} menu items
            </p>
          )}
        </div>

        {/* Tab navigation */}
        <div className="border-b border-gray-200 dark:border-gray-700 mb-6">
          <nav className="flex gap-6">
            {tabs.map((t) => (
              <button
                key={t.key}
                onClick={() => setTab(t.key)}
                className={`pb-3 px-1 text-sm font-medium border-b-2 transition-colors ${
                  tab === t.key
                    ? 'border-primary-600 text-primary-600'
                    : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <span className="mr-1.5">{t.icon}</span>
                {t.label}
                {t.key === 'orders' && orders.filter((o) => o.status === 'PLACED').length > 0 && (
                  <span className="ml-2 px-2 py-0.5 bg-red-500 text-white text-xs rounded-full">
                    {orders.filter((o) => o.status === 'PLACED').length}
                  </span>
                )}
              </button>
            ))}
          </nav>
        </div>

        {/* Tab content */}
        {tab === 'orders' && (
          <OrdersTab
            orders={orders}
            loading={ordersLoading}
            actionLoading={actionLoading}
            vendor={vendor}
            pendingAlertIds={pendingAlertIds}
            onAccept={handleAccept}
            onReject={handleReject}
            onMarkPreparing={handleMarkPreparing}
            onMarkReady={handleMarkReady}
            onAssignRunner={handleOpenAssign}
          />
        )}

        {tab === 'kds' && vendor && (
          <KDSView
            orders={orders}
            actionLoading={actionLoading}
            onAccept={handleAccept}
            onMarkPreparing={handleMarkPreparing}
            onMarkReady={handleMarkReady}
          />
        )}

        {tab === 'kds' && !vendor && (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-12 text-center">
            <p className="text-gray-600 dark:text-gray-300">Create your restaurant profile first.</p>
          </div>
        )}

        {tab === 'menu' && vendor && <VendorMenuManagement vendor={vendor} />}

        {tab === 'menu' && !vendor && (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-12 text-center">
            <p className="text-gray-600 dark:text-gray-300 mb-4">Create your restaurant profile first before adding menu items.</p>
            <button
              onClick={() => setTab('profile')}
              className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
            >
              Go to Profile
            </button>
          </div>
        )}

        {tab === 'analytics' && vendor && (
          <VendorAnalytics vendorId={vendor.id} />
        )}

        {tab === 'scheduled' && vendor && (
          <ScheduledOrdersTab vendorId={vendor.id} />
        )}

        {tab === 'inventory' && vendor && (
          <InventoryManagement vendorId={vendor.id} />
        )}

        {tab === 'profile' && (
          <VendorProfile
            vendor={vendor}
            onProfileUpdated={(updated) => {
              setVendor(updated);
              success('Profile saved!');
              // If they just created their restaurant, switch to menu tab
              if (!vendor) setTab('menu');
            }}
          />
        )}

        {/* Reject Confirmation Dialog with stock messages */}
        {rejectTarget && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl max-w-sm w-full p-6 space-y-4">
              <h3 className="text-lg font-bold text-gray-900 dark:text-white">Reject Order</h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Choose a stock message or enter a custom reason:
              </p>
              <div className="space-y-2">
                {CANNED_MESSAGES.map((msg) => (
                  <button
                    key={msg}
                    onClick={() => setRejectMessage(msg)}
                    className={`w-full text-left px-3 py-2 rounded-lg border text-sm transition-colors ${
                      rejectMessage === msg
                        ? 'border-red-500 bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300'
                        : 'border-gray-200 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300'
                    }`}
                  >
                    {msg}
                  </button>
                ))}
              </div>
              {(rejectMessage === 'Custom reason…' || (rejectMessage === '' && true)) && (
                <input
                  type="text"
                  placeholder="Custom reason (optional)"
                  value={rejectCustom}
                  onChange={(e) => setRejectCustom(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg text-sm dark:bg-gray-700 dark:text-white"
                />
              )}
              <div className="flex gap-2 pt-2">
                <button
                  onClick={confirmReject}
                  disabled={rejecting}
                  className="flex-1 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 disabled:opacity-50 text-sm"
                >
                  {rejecting ? 'Rejecting…' : 'Confirm Reject'}
                </button>
                <button
                  onClick={() => setRejectTarget(null)}
                  className="flex-1 py-2 bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg font-medium hover:bg-gray-300 text-sm"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Runner assignment modal */}
        {assignTarget && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl max-w-sm w-full p-6 space-y-4">
              <h3 className="text-lg font-bold text-gray-900 dark:text-white">Assign Runner</h3>
              {onlineDrivers.length === 0 ? (
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  No online runners available right now. Runners must start their shift to appear here.
                </p>
              ) : (
                <div className="space-y-2 max-h-64 overflow-y-auto">
                  {onlineDrivers.map((d) => (
                    <button
                      key={d.driverId}
                      onClick={() => handleAssignDriver(d.driverId)}
                      disabled={assigningDriver}
                      className="w-full flex items-center gap-3 px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-lg hover:bg-green-50 dark:hover:bg-green-900/20 disabled:opacity-50 transition-colors text-left"
                    >
                      <div className="w-8 h-8 rounded-full bg-green-100 dark:bg-green-900 flex items-center justify-center text-green-700 font-semibold text-sm flex-shrink-0">
                        {d.name?.charAt(0)?.toUpperCase() || 'D'}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-gray-900 dark:text-white text-sm">{d.name}</p>
                        <p className="text-xs text-gray-500 dark:text-gray-400">
                          {d.vehicleType}{d.licensePlate ? ` · ${d.licensePlate}` : ''}
                        </p>
                      </div>
                      <span className="flex items-center gap-1 text-xs text-green-600">
                        <span className="w-2 h-2 rounded-full bg-green-500" />
                        Online
                      </span>
                    </button>
                  ))}
                </div>
              )}
              <button
                onClick={() => setAssignTarget(null)}
                className="w-full py-2 bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg font-medium hover:bg-gray-300 text-sm"
              >
                Close
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ── Orders sub-component ───────────────────────────────────────────────

interface OrdersTabProps {
  orders: OrderDTO[];
  loading: boolean;
  actionLoading: string | null;
  vendor: VendorDTO | null;
  pendingAlertIds: Set<string>;
  onAccept: (id: string) => void;
  onReject: (id: string) => void;
  onMarkPreparing: (id: string) => void;
  onMarkReady: (id: string) => void;
  onAssignRunner: (id: string) => void;
}

function OrdersTab({
  orders,
  loading,
  actionLoading,
  vendor,
  pendingAlertIds,
  onAccept,
  onReject,
  onMarkPreparing,
  onMarkReady,
  onAssignRunner,
}: OrdersTabProps) {
  if (!vendor) {
    return (
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-12 text-center">
        <p className="text-gray-600 dark:text-gray-300">Create your restaurant profile first to start receiving orders.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (orders.length === 0) {
    return (
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-12 text-center">
        <div className="text-5xl mb-4">📭</div>
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">No orders yet</h3>
        <p className="text-gray-500 dark:text-gray-400">
          Orders from customers will appear here. Make sure you have menu items available!
        </p>
      </div>
    );
  }

  // Group by status for better UX
  const statusOrder = ['PLACED', 'ACCEPTED', 'PREPARING', 'READY', 'ASSIGNED', 'PICKED_UP', 'ENROUTE', 'DELIVERED', 'CANCELLED', 'REJECTED'];
  const sorted = [...orders].sort(
    (a, b) => statusOrder.indexOf(a.status) - statusOrder.indexOf(b.status)
  );

  return (
    <div className="space-y-4">
      {sorted.map((order) => (
        <div
          key={order.id}
          className={`bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 transition-all ${
            pendingAlertIds.has(order.id) ? 'ring-2 ring-red-500 animate-pulse-border' : ''
          }`}
        >
          <div className="flex justify-between items-start mb-4">
            <div>
              <h3 className="text-lg font-bold flex items-center gap-2">
                Order #{order.orderNumber || order.id.substring(0, 8)}
                {pendingAlertIds.has(order.id) && (
                  <span className="px-2 py-0.5 bg-red-500 text-white text-xs rounded-full animate-pulse">
                    NEW
                  </span>
                )}
              </h3>
              <p className="text-sm text-gray-600 dark:text-gray-300">{formatDateTime(order.createdAt)}</p>
              <p className="text-sm text-gray-900 dark:text-white mt-1">{order.customerName}</p>
            </div>
            <span
              className={`px-3 py-1 rounded-full text-sm font-medium ${
                order.status === 'PLACED'
                  ? 'bg-yellow-100 text-yellow-800'
                  : order.status === 'ACCEPTED'
                  ? 'bg-blue-100 text-blue-800'
                  : order.status === 'PREPARING'
                  ? 'bg-purple-100 text-purple-800'
                  : order.status === 'READY'
                  ? 'bg-green-100 text-green-800'
                  : order.status === 'CANCELLED' || order.status === 'REJECTED'
                  ? 'bg-red-100 text-red-800'
                  : 'bg-gray-100 text-gray-800'
              }`}
            >
              {order.status}
            </span>
          </div>

          <div className="mb-4">
            <h4 className="font-medium mb-2">Items:</h4>
            <ul className="text-sm space-y-1">
              {order.items.map((item) => (
                <li key={item.id} className="text-gray-700 dark:text-gray-200">
                  {item.quantity}x {item.name}
                </li>
              ))}
            </ul>
          </div>

          <div className="flex justify-between items-center pt-4 border-t">
            <p className="text-lg font-bold text-primary-600">
              {formatCurrencyCompact(order.totalCents)}
            </p>

            <div className="flex gap-2">
              {order.status === 'PLACED' && (
                <>
                  <button
                    onClick={() => onAccept(order.id)}
                    disabled={actionLoading === order.id}
                    className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
                  >
                    Accept
                  </button>
                  <button
                    onClick={() => onReject(order.id)}
                    disabled={actionLoading === order.id}
                    className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
                  >
                    Reject
                  </button>
                </>
              )}

              {order.status === 'ACCEPTED' && (
                <button
                  onClick={() => onMarkPreparing(order.id)}
                  disabled={actionLoading === order.id}
                  className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:opacity-50"
                >
                  Start Preparing
                </button>
              )}

              {(order.status === 'READY' || order.status === 'ACCEPTED') && !order.driverId && (
                <button
                  onClick={() => onAssignRunner(order.id)}
                  disabled={actionLoading === order.id}
                  className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
                >
                  🛵 Assign Runner
                </button>
              )}

              {order.status === 'PREPARING' && (
                <button
                  onClick={() => onMarkReady(order.id)}
                  disabled={actionLoading === order.id}
                  className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
                >
                  Mark Ready
                </button>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

// ── KDS (Kitchen Display System) ───────────────────────────────────────

interface KDSViewProps {
  orders: OrderDTO[];
  actionLoading: string | null;
  onAccept: (id: string) => void;
  onMarkPreparing: (id: string) => void;
  onMarkReady: (id: string) => void;
}

function KDSView({ orders, actionLoading, onAccept, onMarkPreparing, onMarkReady }: KDSViewProps) {
  // Only show active kitchen-relevant orders
  const activeStatuses = ['PLACED', 'ACCEPTED', 'PREPARING', 'READY'];
  const kdsOrders = orders.filter((o) => activeStatuses.includes(o.status));

  // Group by status columns
  const columns: { status: string; label: string; color: string; bg: string }[] = [
    { status: 'PLACED', label: 'New', color: 'border-yellow-500', bg: 'bg-yellow-50' },
    { status: 'ACCEPTED', label: 'Accepted', color: 'border-blue-500', bg: 'bg-blue-50' },
    { status: 'PREPARING', label: 'Preparing', color: 'border-purple-500', bg: 'bg-purple-50' },
    { status: 'READY', label: 'Ready', color: 'border-green-500', bg: 'bg-green-50' },
  ];

  if (kdsOrders.length === 0) {
    return (
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-12 text-center">
        <div className="text-5xl mb-4">🍳</div>
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">Kitchen is clear</h3>
        <p className="text-gray-500 dark:text-gray-400">Active orders will appear here in real time.</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      {columns.map((col) => {
        const colOrders = kdsOrders.filter((o) => o.status === col.status);
        return (
          <div key={col.status}>
            <h3 className={`text-sm font-bold uppercase tracking-wider mb-3 text-gray-600 dark:text-gray-300`}>
              {col.label}{' '}
              <span className="text-gray-400 dark:text-gray-500">({colOrders.length})</span>
            </h3>
            <div className="space-y-3">
              {colOrders.map((order) => (
                <div
                  key={order.id}
                  className={`rounded-lg shadow p-4 border-l-4 ${col.color} ${col.bg}`}
                >
                  <div className="flex justify-between items-start mb-2">
                    <span className="text-lg font-bold">
                      #{order.orderNumber || order.id.substring(0, 6)}
                    </span>
                    <span className="text-xs text-gray-500 dark:text-gray-400">
                      {formatDateTime(order.createdAt)}
                    </span>
                  </div>
                  <ul className="text-sm space-y-1 mb-3">
                    {order.items.map((item) => (
                      <li key={item.id} className="font-medium">
                        {item.quantity}x {item.name}
                      </li>
                    ))}
                  </ul>
                  {order.status === 'PLACED' && (
                    <button
                      onClick={() => onAccept(order.id)}
                      disabled={actionLoading === order.id}
                      className="w-full py-1.5 bg-green-600 text-white rounded text-sm font-medium hover:bg-green-700 disabled:opacity-50"
                    >
                      Accept
                    </button>
                  )}
                  {order.status === 'ACCEPTED' && (
                    <button
                      onClick={() => onMarkPreparing(order.id)}
                      disabled={actionLoading === order.id}
                      className="w-full py-1.5 bg-purple-600 text-white rounded text-sm font-medium hover:bg-purple-700 disabled:opacity-50"
                    >
                      Start Preparing
                    </button>
                  )}
                  {order.status === 'PREPARING' && (
                    <button
                      onClick={() => onMarkReady(order.id)}
                      disabled={actionLoading === order.id}
                      className="w-full py-1.5 bg-primary-600 text-white rounded text-sm font-medium hover:bg-primary-700 disabled:opacity-50"
                    >
                      Mark Ready
                    </button>
                  )}
                </div>
              ))}
              {colOrders.length === 0 && (
                <p className="text-sm text-gray-400 dark:text-gray-500 text-center py-4">—</p>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

// ── Scheduled Orders Tab ───────────────────────────────────────────────

function ScheduledOrdersTab({ vendorId }: { vendorId: string }) {
  const [orders, setOrders] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    vendorService
      .getScheduledOrders(vendorId)
      .then(setOrders)
      .catch(() => setOrders([]))
      .finally(() => setLoading(false));
  }, [vendorId]);

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (orders.length === 0) {
    return (
      <EmptyState
        icon="📅"
        title="No scheduled orders"
        description="Upcoming scheduled orders will appear here."
      />
    );
  }

  return (
    <div className="space-y-4">
      {orders.map((order: any) => (
        <div key={order.id} className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
          <div className="flex justify-between items-start mb-3">
            <div>
              <h3 className="text-lg font-bold">
                Order #{order.orderNumber || order.id?.substring(0, 8)}
              </h3>
              <p className="text-sm text-gray-900 dark:text-white mt-1">{order.customerName}</p>
            </div>
            <div className="text-right">
              <p className="text-sm font-medium text-orange-600">
                📅 {order.scheduledTime ? new Date(order.scheduledTime).toLocaleString() : 'N/A'}
              </p>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                {order.status}
              </span>
            </div>
          </div>
          <div className="mb-3">
            <ul className="text-sm space-y-1">
              {(order.items || []).map((item: any, i: number) => (
                <li key={i} className="text-gray-700 dark:text-gray-200">
                  {item.quantity}x {item.name}
                </li>
              ))}
            </ul>
          </div>
          <p className="text-lg font-bold text-primary-600">
            {formatCurrencyCompact(order.totalCents || 0)}
          </p>
        </div>
      ))}
    </div>
  );
}