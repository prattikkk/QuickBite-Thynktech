/**
 * Admin Order Timeline page
 * Displays the full audit trail for a given order.
 * Accessible only to users with ADMIN role.
 */

import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { adminService, paymentService } from '../services';
import { TimelineEntry } from '../types';
import { LoadingSpinner } from '../components';
import { useToastStore } from '../store';
import { formatDateTime } from '../utils';

export default function AdminOrderTimeline() {
  const { orderId } = useParams<{ orderId: string }>();
  const [entries, setEntries] = useState<TimelineEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { success: showSuccess, error: showError } = useToastStore();

  // Refund modal state
  const [showRefundModal, setShowRefundModal] = useState(false);
  const [refundPaymentId, setRefundPaymentId] = useState('');
  const [refundAmount, setRefundAmount] = useState('');
  const [refundReason, setRefundReason] = useState('');
  const [refunding, setRefunding] = useState(false);

  useEffect(() => {
    if (orderId) loadTimeline();
  }, [orderId]);

  const loadTimeline = async () => {
    if (!orderId) return;
    try {
      const data = await adminService.getOrderTimeline(orderId);
      setEntries(data);
    } catch (err: any) {
      const msg = err.message || 'Failed to load timeline';
      setError(msg);
      showError(msg);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  const handleRefund = async () => {
    if (!refundPaymentId.trim()) {
      showError('Payment ID is required');
      return;
    }
    try {
      setRefunding(true);
      await paymentService.refundPayment(
        refundPaymentId.trim(),
        refundAmount ? Math.round(parseFloat(refundAmount) * 100) : undefined,
        refundReason || undefined
      );
      showSuccess('Refund processed successfully');
      setShowRefundModal(false);
      setRefundPaymentId('');
      setRefundAmount('');
      setRefundReason('');
      // Reload timeline to show refund event
      loadTimeline();
    } catch (err: any) {
      showError(err.message || 'Refund failed');
    } finally {
      setRefunding(false);
    }
  };

  if (error) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-12">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
          <p className="text-red-600 font-medium">{error}</p>
          <Link to="/" className="text-primary-600 mt-4 inline-block hover:underline">
            Go back
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Order Timeline</h1>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowRefundModal(true)}
            className="px-4 py-2 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700 transition"
          >
            Issue Refund
          </button>
          <Link
            to="/admin/dashboard"
            className="text-sm text-primary-600 hover:text-primary-700"
          >
            &larr; Back to Admin
          </Link>
        </div>
      </div>

      {/* Refund Modal */}
      {showRefundModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md shadow-xl">
            <h2 className="text-lg font-bold text-gray-900 mb-4">Issue Refund</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Provider Payment ID *
                </label>
                <input
                  type="text"
                  value={refundPaymentId}
                  onChange={(e) => setRefundPaymentId(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  placeholder="pi_..."
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Amount (optional, in ₹ — leave blank for full refund)
                </label>
                <input
                  type="number"
                  step="0.01"
                  value={refundAmount}
                  onChange={(e) => setRefundAmount(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  placeholder="e.g. 150.00"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Reason
                </label>
                <textarea
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  rows={3}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  placeholder="Reason for refund..."
                />
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button
                onClick={() => setShowRefundModal(false)}
                className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleRefund}
                disabled={refunding}
                className="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {refunding ? 'Processing...' : 'Confirm Refund'}
              </button>
            </div>
          </div>
        </div>
      )}

      <p className="text-sm text-gray-500 mb-6">
        Order <span className="font-mono font-medium text-gray-700">{orderId}</span>
      </p>

      {entries.length === 0 ? (
        <p className="text-gray-500 text-center py-8">No timeline entries yet.</p>
      ) : (
        <ol className="relative border-l border-gray-300 ml-4 space-y-6">
          {entries.map((entry) => (
            <li key={entry.id} className="ml-6">
              <span className="absolute -left-2 flex items-center justify-center w-4 h-4 rounded-full ring-4 ring-white bg-primary-500" />
              <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-sm font-semibold text-gray-900">
                    {formatEventType(entry.eventType)}
                  </span>
                  <time className="text-xs text-gray-400">{formatDateTime(entry.createdAt)}</time>
                </div>

                {entry.oldStatus && entry.newStatus && (
                  <p className="text-sm text-gray-600">
                    <StatusBadge status={entry.oldStatus} /> &rarr; <StatusBadge status={entry.newStatus} />
                  </p>
                )}

                <p className="text-xs text-gray-500 mt-1">
                  By <span className="font-medium">{entry.actorRole}</span>
                  {entry.actorId && (
                    <span className="ml-1 font-mono text-gray-400">({entry.actorId.slice(0, 8)}…)</span>
                  )}
                </p>

                {entry.meta && Object.keys(entry.meta).length > 0 && (
                  <details className="mt-2 text-xs text-gray-500">
                    <summary className="cursor-pointer hover:text-gray-700">Details</summary>
                    <pre className="mt-1 bg-gray-50 rounded p-2 overflow-auto text-xs">
                      {JSON.stringify(entry.meta, null, 2)}
                    </pre>
                  </details>
                )}
              </div>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

/** Humanise the event type constant */
function formatEventType(type: string): string {
  return type
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());
}

function StatusBadge({ status }: { status: string }) {
  const colorMap: Record<string, string> = {
    PLACED: 'bg-yellow-100 text-yellow-800',
    ACCEPTED: 'bg-blue-100 text-blue-800',
    PREPARING: 'bg-purple-100 text-purple-800',
    READY: 'bg-indigo-100 text-indigo-800',
    ASSIGNED: 'bg-indigo-100 text-indigo-800',
    PICKED_UP: 'bg-cyan-100 text-cyan-800',
    ENROUTE: 'bg-teal-100 text-teal-800',
    DELIVERED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-red-100 text-red-800',
  };
  return (
    <span className={`inline-block px-2 py-0.5 text-xs font-medium rounded-full ${colorMap[status] || 'bg-gray-100 text-gray-700'}`}>
      {status}
    </span>
  );
}
