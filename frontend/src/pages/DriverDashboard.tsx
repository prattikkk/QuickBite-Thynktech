/**
 * DriverDashboard page
 */

import { useState, useEffect } from 'react';
import { driverService, type DriverOrderSummary } from '../services/driver.service';
import { OrderDTO } from '../types';
import { LoadingSpinner } from '../components';
import { formatCurrencyCompact, formatDateTime } from '../utils';
import { useToastStore } from '../store';

type Tab = 'assigned' | 'available' | 'history';

export default function DriverDashboard() {
  const [orders, setOrders] = useState<OrderDTO[]>([]);
  const [availableOrders, setAvailableOrders] = useState<DriverOrderSummary[]>([]);
  const [historyOrders, setHistoryOrders] = useState<DriverOrderSummary[]>([]);
  const [tab, setTab] = useState<Tab>('assigned');
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const { success, error: showError } = useToastStore();

  useEffect(() => {
    loadOrders();
    // Auto-refresh every 10s for new assignments
    const interval = setInterval(loadOrders, 10000);
    return () => clearInterval(interval);
  }, []);

  const loadOrders = async () => {
    try {
      const [assigned, available] = await Promise.all([
        driverService.getAssignedOrders({ page: 0, size: 50 }),
        driverService.getAvailableOrders(),
      ]);
      setOrders(assigned.content || []);
      setAvailableOrders(available || []);
    } catch (err: any) {
      showError(err.message || 'Failed to load orders');
    } finally {
      setLoading(false);
    }
  };

  const loadHistory = async () => {
    try {
      const history = await driverService.getDeliveryHistory(0, 50);
      setHistoryOrders(history || []);
    } catch (err: any) {
      showError(err.message || 'Failed to load history');
    }
  };

  useEffect(() => {
    if (tab === 'history' && historyOrders.length === 0) {
      loadHistory();
    }
  }, [tab]);

  const handleAcceptOrder = async (orderId: string) => {
    try {
      setActionLoading(orderId);
      await driverService.acceptAvailableOrder(orderId);
      success('Order accepted!');
      loadOrders();
    } catch (err: any) {
      showError(err.message || 'Failed to accept order');
    } finally {
      setActionLoading(null);
    }
  };

  const handlePickup = async (orderId: string) => {
    try {
      setActionLoading(orderId);
      await driverService.markPickedUp(orderId);
      success('Order marked as picked up');
      loadOrders();
    } catch (err: any) {
      showError(err.message || 'Failed to update order');
    } finally {
      setActionLoading(null);
    }
  };

  const handleEnroute = async (orderId: string) => {
    try {
      setActionLoading(orderId);
      await driverService.markEnroute(orderId);
      success('Marked as on the way');
      loadOrders();
    } catch (err: any) {
      showError(err.message || 'Failed to update order');
    } finally {
      setActionLoading(null);
    }
  };

  const handleDeliver = async (orderId: string) => {
    try {
      setActionLoading(orderId);
      await driverService.markDelivered(orderId);
      success('Order delivered successfully!');
      loadOrders();
    } catch (err: any) {
      showError(err.message || 'Failed to update order');
    } finally {
      setActionLoading(null);
    }
  };

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
        <h1 className="text-3xl font-bold text-gray-900 mb-6">Driver Dashboard</h1>

        {/* Tabs */}
        <div className="flex space-x-1 bg-gray-200 rounded-lg p-1 mb-6">
          {([
            { key: 'assigned' as Tab, label: 'My Orders', count: orders.length },
            { key: 'available' as Tab, label: 'Available', count: availableOrders.length },
            { key: 'history' as Tab, label: 'History', count: null },
          ]).map(({ key, label, count }) => (
            <button
              key={key}
              onClick={() => setTab(key)}
              className={`flex-1 py-2 px-4 text-sm font-medium rounded-md transition-colors ${
                tab === key
                  ? 'bg-white text-primary-700 shadow'
                  : 'text-gray-600 hover:text-gray-800'
              }`}
            >
              {label}{count !== null ? ` (${count})` : ''}
            </button>
          ))}
        </div>

        {/* Assigned orders tab */}
        {tab === 'assigned' && (
          <>
            {orders.length === 0 ? (
              <div className="bg-white rounded-lg shadow-md p-12 text-center">
                <p className="text-gray-600">No assigned orders</p>
              </div>
            ) : (
              <div className="space-y-4">
                {orders.map((order) => (
                  <div key={order.id} className="bg-white rounded-lg shadow-md p-6">
                    <div className="flex justify-between items-start mb-4">
                      <div>
                        <h3 className="text-lg font-bold">Order #{order.id}</h3>
                        <p className="text-sm text-gray-600">{formatDateTime(order.createdAt)}</p>
                      </div>
                      <span className="px-3 py-1 bg-purple-100 text-purple-800 rounded-full text-sm font-medium">
                        {order.status}
                      </span>
                    </div>

                    <div className="grid md:grid-cols-2 gap-4 mb-4">
                      <div>
                        <h4 className="font-medium mb-1">Pickup</h4>
                        <p className="text-sm text-gray-900">{order.vendorName}</p>
                      </div>
                      <div>
                        <h4 className="font-medium mb-1">Delivery</h4>
                        <p className="text-sm text-gray-900">{order.deliveryAddress?.line1}</p>
                        {order.deliveryAddress?.line2 && (
                          <p className="text-sm text-gray-600">{order.deliveryAddress.line2}</p>
                        )}
                        <p className="text-sm text-gray-600">
                          {order.deliveryAddress?.city}, {order.deliveryAddress?.postal}
                        </p>
                      </div>
                    </div>

                    <div className="flex justify-between items-center pt-4 border-t">
                      <p className="text-lg font-bold text-primary-600">
                        {formatCurrencyCompact(order.totalCents)}
                      </p>
                      <div className="flex gap-2">
                        {(order.status === 'READY' || order.status === 'ASSIGNED') && (
                          <button
                            onClick={() => handlePickup(order.id)}
                            disabled={actionLoading === order.id}
                            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
                          >
                            Mark Picked Up
                          </button>
                        )}
                        {order.status === 'PICKED_UP' && (
                          <button
                            onClick={() => handleEnroute(order.id)}
                            disabled={actionLoading === order.id}
                            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                          >
                            On The Way
                          </button>
                        )}
                        {order.status === 'ENROUTE' && (
                          <button
                            onClick={() => handleDeliver(order.id)}
                            disabled={actionLoading === order.id}
                            className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
                          >
                            Mark Delivered
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}

        {/* Available orders tab */}
        {tab === 'available' && (
          <>
            {availableOrders.length === 0 ? (
              <div className="bg-white rounded-lg shadow-md p-12 text-center">
                <p className="text-gray-600">No orders available for pickup right now</p>
              </div>
            ) : (
              <div className="space-y-4">
                {availableOrders.map((order) => (
                  <div key={order.orderId} className="bg-white rounded-lg shadow-md p-6 border-l-4 border-green-500">
                    <div className="flex justify-between items-start mb-4">
                      <div>
                        <h3 className="text-lg font-bold">Order #{order.orderId}</h3>
                        <p className="text-sm text-gray-600">{formatDateTime(order.createdAt)}</p>
                      </div>
                      <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-sm font-medium">
                        {order.status}
                      </span>
                    </div>

                    <div className="grid md:grid-cols-2 gap-4 mb-4">
                      <div>
                        <h4 className="font-medium mb-1">Pickup From</h4>
                        <p className="text-sm text-gray-900">{order.vendorName}</p>
                      </div>
                      {order.deliveryAddress && (
                        <div>
                          <h4 className="font-medium mb-1">Deliver To</h4>
                          <p className="text-sm text-gray-900">{order.deliveryAddress.line1}</p>
                          <p className="text-sm text-gray-600">
                            {order.deliveryAddress.city}, {order.deliveryAddress.postal}
                          </p>
                        </div>
                      )}
                    </div>

                    <div className="flex justify-between items-center pt-4 border-t">
                      <p className="text-lg font-bold text-primary-600">
                        {formatCurrencyCompact(order.totalCents)}
                      </p>
                      <button
                        onClick={() => handleAcceptOrder(order.orderId)}
                        disabled={actionLoading === order.orderId}
                        className="px-6 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 font-medium"
                      >
                        {actionLoading === order.orderId ? 'Accepting...' : 'Accept Order'}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}

        {/* History tab */}
        {tab === 'history' && (
          <>
            {historyOrders.length === 0 ? (
              <div className="bg-white rounded-lg shadow-md p-12 text-center">
                <p className="text-gray-600">No delivery history yet</p>
              </div>
            ) : (
              <div className="space-y-4">
                {historyOrders.map((order) => (
                  <div key={order.orderId} className="bg-white rounded-lg shadow-md p-6">
                    <div className="flex justify-between items-start mb-2">
                      <div>
                        <h3 className="text-lg font-bold">Order #{order.orderId}</h3>
                        <p className="text-sm text-gray-600">{formatDateTime(order.createdAt)}</p>
                      </div>
                      <span className="px-3 py-1 bg-gray-100 text-gray-800 rounded-full text-sm font-medium">
                        {order.status}
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <p className="text-sm text-gray-600">{order.vendorName}</p>
                      <p className="text-lg font-bold text-primary-600">
                        {formatCurrencyCompact(order.totalCents)}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
