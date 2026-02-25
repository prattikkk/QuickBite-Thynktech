/**
 * AdminRefunds page — search orders, view payment status, process refunds
 */

import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import { paymentService } from '../services';
import { LoadingSpinner } from '../components';
import type { OrderDTO } from '../types';

type RefundFilter = 'CANCELLED' | 'DELIVERED' | 'ALL';

export default function AdminRefunds() {
  const [orders, setOrders] = useState<OrderDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<RefundFilter>('CANCELLED');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [refundModal, setRefundModal] = useState<OrderDTO | null>(null);
  const [refundReason, setRefundReason] = useState('');
  const [refundAmount, setRefundAmount] = useState('');
  const [isPartial, setIsPartial] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const loadOrders = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const params: Record<string, any> = { page, size: 15, sortBy: 'createdAt', sortDir: 'DESC' };
      if (filter !== 'ALL') {
        params.status = filter;
      }
      const res = await api.get<any, any>('/orders', { params });
      const data = res?.data ?? res;
      setOrders(data?.content ?? []);
      setTotalPages(data?.totalPages ?? 0);
    } catch {
      setError('Failed to load orders');
    } finally {
      setLoading(false);
    }
  }, [filter, page]);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  const filteredOrders = search
    ? orders.filter(
        (o) =>
          o.orderNumber?.toLowerCase().includes(search.toLowerCase()) ||
          o.customerName?.toLowerCase().includes(search.toLowerCase())
      )
    : orders;

  const handleRefund = async () => {
    if (!refundModal?.providerPaymentId) {
      setError('No payment ID found for this order');
      return;
    }
    try {
      setProcessing(true);
      setError(null);
      const amountCents = isPartial && refundAmount ? Math.round(parseFloat(refundAmount) * 100) : undefined;
      await paymentService.refundPayment(refundModal.providerPaymentId, amountCents, refundReason || undefined);
      setSuccessMsg(`Refund processed for order ${refundModal.orderNumber}`);
      setRefundModal(null);
      setRefundReason('');
      setRefundAmount('');
      setIsPartial(false);
      setTimeout(() => setSuccessMsg(null), 4000);
      loadOrders();
    } catch (err: any) {
      setError(err.message || 'Refund failed');
    } finally {
      setProcessing(false);
    }
  };

  const formatCurrency = (cents: number) => `₹${(cents / 100).toFixed(2)}`;

  const statusBadge = (status: string) => {
    const colors: Record<string, string> = {
      CAPTURED: 'bg-green-100 text-green-700',
      REFUNDED: 'bg-purple-100 text-purple-700',
      PENDING: 'bg-yellow-100 text-yellow-700',
      FAILED: 'bg-red-100 text-red-700',
      CANCELLED: 'bg-gray-100 text-gray-700',
    };
    return (
      <span className={`px-2 py-1 text-xs font-medium rounded-full ${colors[status] || 'bg-gray-100 text-gray-700'}`}>
        {status}
      </span>
    );
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Refund Management</h1>
          <p className="text-gray-500">Review orders and process refunds</p>
        </div>
        <Link
          to="/admin/reports"
          className="text-primary-600 hover:text-primary-700 text-sm font-medium"
        >
          ← Back to Reports
        </Link>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-lg border border-red-200" role="alert">
          {error}
        </div>
      )}
      {successMsg && (
        <div className="mb-4 p-3 bg-green-50 text-green-700 rounded-lg border border-green-200" role="status">
          {successMsg}
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 mb-6">
        <div className="flex gap-2">
          {(['CANCELLED', 'DELIVERED', 'ALL'] as RefundFilter[]).map((f) => (
            <button
              key={f}
              onClick={() => { setFilter(f); setPage(0); }}
              className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${
                filter === f
                  ? 'bg-primary-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {f === 'ALL' ? 'All Orders' : f.charAt(0) + f.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
        <input
          type="text"
          placeholder="Search by order # or customer name..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          aria-label="Search orders"
        />
      </div>

      {/* Orders Table */}
      {loading ? (
        <div className="flex justify-center py-12">
          <LoadingSpinner size="lg" />
        </div>
      ) : filteredOrders.length === 0 ? (
        <div className="text-center py-12 text-gray-500">No orders found</div>
      ) : (
        <>
          <div className="bg-white rounded-lg shadow overflow-x-auto">
            <table className="w-full" role="table">
              <thead>
                <tr className="bg-gray-50 text-left text-sm text-gray-500">
                  <th className="px-4 py-3 font-medium">Order #</th>
                  <th className="px-4 py-3 font-medium">Customer</th>
                  <th className="px-4 py-3 font-medium">Vendor</th>
                  <th className="px-4 py-3 font-medium">Total</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Payment</th>
                  <th className="px-4 py-3 font-medium">Date</th>
                  <th className="px-4 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filteredOrders.map((order) => (
                  <tr key={order.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 text-sm font-mono">
                      <Link to={`/admin/orders/${order.id}/timeline`} className="text-primary-600 hover:underline">
                        {order.orderNumber}
                      </Link>
                    </td>
                    <td className="px-4 py-3 text-sm">{order.customerName}</td>
                    <td className="px-4 py-3 text-sm">{order.vendorName}</td>
                    <td className="px-4 py-3 text-sm font-medium">{formatCurrency(order.totalCents)}</td>
                    <td className="px-4 py-3">{statusBadge(order.status)}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        {statusBadge(order.paymentStatus)}
                        {order.refundStatus && (
                          <span className="px-2 py-1 text-xs font-medium rounded-full bg-purple-100 text-purple-700">
                            {order.refundStatus}
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">
                      {new Date(order.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-4 py-3">
                      {order.paymentStatus !== 'REFUNDED' && order.providerPaymentId && (
                        <button
                          onClick={() => setRefundModal(order)}
                          className="px-3 py-1.5 text-sm font-medium text-red-600 bg-red-50 hover:bg-red-100 rounded-lg transition-colors"
                          aria-label={`Refund order ${order.orderNumber}`}
                        >
                          Refund
                        </button>
                      )}
                      {order.paymentStatus === 'REFUNDED' && (
                        <span className="text-xs text-purple-600 font-medium">Refunded</span>
                      )}
                      {!order.providerPaymentId && order.paymentMethod === 'COD' && (
                        <span className="text-xs text-gray-400">COD</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center gap-2 mt-6">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Previous
              </button>
              <span className="px-3 py-1.5 text-sm text-gray-600">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}

      {/* Refund Modal */}
      {refundModal && (
        <div
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          role="dialog"
          aria-modal="true"
          aria-label="Process refund"
          onClick={(e) => { if (e.target === e.currentTarget) setRefundModal(null); }}
        >
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6">
            <h2 className="text-lg font-bold text-gray-900 mb-1">Process Refund</h2>
            <p className="text-sm text-gray-500 mb-4">
              Order <span className="font-mono font-medium">{refundModal.orderNumber}</span> —{' '}
              {formatCurrency(refundModal.totalCents)}
            </p>

            <div className="space-y-4">
              {/* Full or Partial */}
              <div className="flex items-center gap-4">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="refundType"
                    checked={!isPartial}
                    onChange={() => setIsPartial(false)}
                    className="text-primary-600 focus:ring-primary-500"
                  />
                  <span className="text-sm">Full refund</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="refundType"
                    checked={isPartial}
                    onChange={() => setIsPartial(true)}
                    className="text-primary-600 focus:ring-primary-500"
                  />
                  <span className="text-sm">Partial refund</span>
                </label>
              </div>

              {/* Partial amount */}
              {isPartial && (
                <div>
                  <label htmlFor="refundAmount" className="block text-sm font-medium text-gray-700 mb-1">
                    Refund Amount (₹)
                  </label>
                  <input
                    id="refundAmount"
                    type="number"
                    step="0.01"
                    min="0.01"
                    max={(refundModal.totalCents / 100).toFixed(2)}
                    value={refundAmount}
                    onChange={(e) => setRefundAmount(e.target.value)}
                    placeholder="0.00"
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                  />
                </div>
              )}

              {/* Reason */}
              <div>
                <label htmlFor="refundReason" className="block text-sm font-medium text-gray-700 mb-1">
                  Reason
                </label>
                <textarea
                  id="refundReason"
                  rows={3}
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  placeholder="Why is this refund being issued?"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                />
              </div>
            </div>

            <div className="flex justify-end gap-3 mt-6">
              <button
                onClick={() => { setRefundModal(null); setRefundReason(''); setRefundAmount(''); setIsPartial(false); }}
                className="px-4 py-2 text-sm text-gray-700 border rounded-lg hover:bg-gray-50 transition-colors"
                disabled={processing}
              >
                Cancel
              </button>
              <button
                onClick={handleRefund}
                disabled={processing || (isPartial && !refundAmount)}
                className="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {processing ? 'Processing...' : 'Confirm Refund'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
