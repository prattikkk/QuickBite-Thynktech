/**
 * MyOrders page - list all orders for the current customer
 */

import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { orderService } from '../services';
import { OrderDTO } from '../types';
import { LoadingSpinner } from '../components';
import { formatCurrencyCompact, formatDateTime } from '../utils';
import { useToastStore } from '../store';

const STATUS_COLORS: Record<string, string> = {
  PLACED: 'bg-yellow-100 text-yellow-800',
  ACCEPTED: 'bg-blue-100 text-blue-800',
  PREPARING: 'bg-purple-100 text-purple-800',
  READY: 'bg-indigo-100 text-indigo-800',
  ASSIGNED: 'bg-indigo-100 text-indigo-800',
  PICKED_UP: 'bg-cyan-100 text-cyan-800',
  ENROUTE: 'bg-teal-100 text-teal-800',
  DELIVERED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
  REJECTED: 'bg-red-100 text-red-800',
};

export default function MyOrders() {
  const [orders, setOrders] = useState<OrderDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [reordering, setReordering] = useState<string | null>(null);
  const { error: showError, success } = useToastStore();
  const navigate = useNavigate();

  useEffect(() => {
    loadOrders();
  }, []);

  const loadOrders = async () => {
    try {
      const data = await orderService.getMyOrders({ page: 0, size: 50 });
      const list = (data as any)?.content || data || [];
      setOrders(Array.isArray(list) ? list : []);
    } catch (err: any) {
      showError(err.message || 'Failed to load orders');
    } finally {
      setLoading(false);
    }
  };

  const handleReorder = async (e: React.MouseEvent, orderId: string) => {
    e.preventDefault();
    e.stopPropagation();
    try {
      setReordering(orderId);
      const newOrder = await orderService.reorder(orderId);
      success('Reorder placed successfully!');
      navigate(`/orders/${newOrder.id}`);
    } catch (err: any) {
      showError(err.message || 'Failed to reorder');
    } finally {
      setReordering(null);
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
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">My Orders</h1>

        {orders.length === 0 ? (
          <div className="bg-white rounded-lg shadow-md p-12 text-center">
            <p className="text-gray-600 mb-4">No orders yet</p>
            <Link
              to="/vendors"
              className="inline-block px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
            >
              Browse Restaurants
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            {orders.map((order) => (
              <Link
                key={order.id}
                to={`/orders/${order.id}`}
                className="block bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow"
              >
                <div className="flex justify-between items-start mb-3">
                  <div>
                    <h3 className="text-lg font-bold text-gray-900">
                      Order #{order.orderNumber || order.id.substring(0, 8).toUpperCase()}
                    </h3>
                    <p className="text-sm text-gray-600">{order.vendorName}</p>
                    <p className="text-xs text-gray-500 mt-1">{formatDateTime(order.createdAt)}</p>
                  </div>
                  <span
                    className={`px-3 py-1 rounded-full text-sm font-medium ${
                      STATUS_COLORS[order.status] || 'bg-gray-100 text-gray-800'
                    }`}
                  >
                    {order.status}
                  </span>
                </div>

                {/* Scheduled order indicator */}
                {order.scheduledTime && (
                  <div className="flex items-center gap-1.5 mt-2 text-sm text-indigo-600 bg-indigo-50 px-3 py-1.5 rounded-lg w-fit ml-auto">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <span>Scheduled: {new Date(order.scheduledTime).toLocaleString()}</span>
                  </div>
                )}

                {/* Refund status */}
                {order.refundStatus && (
                  <div className="mt-2 text-sm px-3 py-1 bg-purple-50 text-purple-700 rounded-lg w-fit ml-auto">
                    {order.refundStatus === 'REFUNDED' ? '💰 Refunded' : order.refundStatus}
                  </div>
                )}

                <div className="flex justify-between items-center pt-3 border-t">
                  <p className="text-sm text-gray-600">
                    {order.items.length} item{order.items.length !== 1 ? 's' : ''}
                  </p>
                  <div className="flex items-center gap-3">
                    {['DELIVERED', 'CANCELLED', 'REJECTED'].includes(order.status) && (
                      <button
                        onClick={(e) => handleReorder(e, order.id)}
                        disabled={reordering === order.id}
                        className="px-3 py-1 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 transition-colors"
                      >
                        {reordering === order.id ? 'Reordering...' : 'Reorder'}
                      </button>
                    )}
                    <p className="text-lg font-bold text-primary-600">
                      {formatCurrencyCompact(order.totalCents)}
                    </p>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
