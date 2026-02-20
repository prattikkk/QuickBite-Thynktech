/**
 * VendorDashboard page
 */

import { useState, useEffect } from 'react';
import { vendorService } from '../services';
import { OrderDTO } from '../types';
import { LoadingSpinner } from '../components';
import { formatCurrencyCompact, formatDateTime } from '../utils';
import { useToastStore } from '../store';

export default function VendorDashboard() {
  const [orders, setOrders] = useState<OrderDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const { success, error: showError } = useToastStore();

  useEffect(() => {
    loadOrders();
    // Auto-refresh every 10s for new orders
    const interval = setInterval(loadOrders, 10000);
    return () => clearInterval(interval);
  }, []);

  const loadOrders = async () => {
    try {
      const data = await vendorService.getVendorOrders({ page: 0, size: 50 });
      // Backend returns Page<OrderDTO>, extract content
      const orderList = (data as any)?.content || data || [];
      setOrders(Array.isArray(orderList) ? orderList : []);
    } catch (err: any) {
      showError(err.message || 'Failed to load orders');
    } finally {
      setLoading(false);
    }
  };

  const handleAccept = async (orderId: string) => {
    try {
      setActionLoading(orderId);
      await vendorService.acceptOrder(orderId);
      success('Order accepted');
      loadOrders();
    } catch (err: any) {
      showError(err.message || 'Failed to accept order');
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async (orderId: string) => {
    if (!confirm('Are you sure you want to reject this order?')) return;

    try {
      setActionLoading(orderId);
      await vendorService.rejectOrder(orderId, 'Rejected by vendor');
      success('Order rejected');
      loadOrders();
    } catch (err: any) {
      showError(err.message || 'Failed to reject order');
    } finally {
      setActionLoading(null);
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
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Vendor Dashboard</h1>

        {orders.length === 0 ? (
          <div className="bg-white rounded-lg shadow-md p-12 text-center">
            <p className="text-gray-600">No orders yet</p>
          </div>
        ) : (
          <div className="space-y-4">
            {orders.map((order) => (
              <div key={order.id} className="bg-white rounded-lg shadow-md p-6">
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <h3 className="text-lg font-bold">Order #{order.orderNumber || order.id.substring(0, 8)}</h3>
                    <p className="text-sm text-gray-600">{formatDateTime(order.createdAt)}</p>
                    <p className="text-sm text-gray-900 mt-1">{order.customerName}</p>
                  </div>
                  <span className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm font-medium">
                    {order.status}
                  </span>
                </div>

                <div className="mb-4">
                  <h4 className="font-medium mb-2">Items:</h4>
                  <ul className="text-sm space-y-1">
                    {order.items.map((item) => (
                      <li key={item.id} className="text-gray-700">
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
                          onClick={() => handleAccept(order.id)}
                          disabled={actionLoading === order.id}
                          className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
                        >
                          Accept
                        </button>
                        <button
                          onClick={() => handleReject(order.id)}
                          disabled={actionLoading === order.id}
                          className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
                        >
                          Reject
                        </button>
                      </>
                    )}

                    {order.status === 'ACCEPTED' && (
                      <button
                        onClick={() => handleMarkPreparing(order.id)}
                        disabled={actionLoading === order.id}
                        className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:opacity-50"
                      >
                        Start Preparing
                      </button>
                    )}

                    {order.status === 'PREPARING' && (
                      <button
                        onClick={() => handleMarkReady(order.id)}
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
        )}
      </div>
    </div>
  );
}
