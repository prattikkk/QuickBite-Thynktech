/**
 * DriverDashboard page
 */

import { useState, useEffect } from 'react';
import { driverService } from '../services';
import { OrderDTO } from '../types';
import { LoadingSpinner } from '../components';
import { formatCurrencyCompact, formatDateTime } from '../utils';
import { useToastStore } from '../store';

export default function DriverDashboard() {
  const [orders, setOrders] = useState<OrderDTO[]>([]);
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
      const data = await driverService.getAssignedOrders({ page: 0, size: 50 });
      setOrders(data.content || []);
    } catch (err: any) {
      showError(err.message || 'Failed to load orders');
    } finally {
      setLoading(false);
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
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Driver Dashboard</h1>

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
      </div>
    </div>
  );
}
