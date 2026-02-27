/**
 * OrderTrack page - track order status
 */

import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { orderService } from '../services';
import { useOrderUpdates } from '../hooks';
import { OrderDTO, OrderStatus } from '../types';
import { LoadingSpinner, DeliveryProofDisplay, ReviewForm } from '../components';
import LiveMapView from '../components/LiveMapView';
import ChatWindow from '../components/ChatWindow';
import { formatCurrencyCompact, formatDateTime } from '../utils';
import { formatDistance } from '../utils/geo';
import { useToastStore } from '../store';

export default function OrderTrack() {
  const { id } = useParams<{ id: string }>();
  const [order, setOrder] = useState<OrderDTO | null>(null);
  const [statusHistory, setStatusHistory] = useState<Array<{ status: string; timestamp: string; note: string }>>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [cancelling, setCancelling] = useState(false);
  const [showChat, setShowChat] = useState(false);
  const { success: showSuccess, error: showError } = useToastStore();

  // Live ETA from road-based route
  const [liveEta, setLiveEta] = useState<{ distanceKm: number; durationMin: number } | null>(null);

  // Tip state
  const [tipAmount, setTipAmount] = useState<number | null>(null);
  const [customTip, setCustomTip] = useState('');
  const [submittingTip, setSubmittingTip] = useState(false);
  const [tipSent, setTipSent] = useState(false);

  useEffect(() => {
    if (id) {
      loadOrder();
    }
  }, [id]);

  // Setup order updates (WebSocket or polling)
  useOrderUpdates({
    orderId: id || '',  // Order IDs are UUIDs (strings)
    onUpdate: (updatedOrder) => {
      setOrder(updatedOrder);
    },
    onError: (err) => {
      console.error('Order updates error:', err);
    },
    enabled: !!id && !!order,
  });

  const loadOrder = async () => {
    if (!id) return;

    try {
      const data = await orderService.getOrderById(id);
      setOrder(data);
      // Load status history
      try {
        const history = await orderService.getOrderStatusHistory(id);
        setStatusHistory(history);
      } catch {
        // Non-critical — silently ignore
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load order');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async () => {
    if (!id || !confirm('Are you sure you want to cancel this order?')) return;
    try {
      setCancelling(true);
      const updated = await orderService.cancelOrder(id, 'Cancelled by customer');
      setOrder(updated);
      showSuccess('Order cancelled');
    } catch (err: any) {
      showError(err.message || 'Failed to cancel order');
    } finally {
      setCancelling(false);
    }
  };

  const handleTipSubmit = async () => {
    const cents = tipAmount ?? (customTip ? Math.round(parseFloat(customTip) * 100) : 0);
    if (!id || cents <= 0) {
      showError('Please select or enter a tip amount');
      return;
    }
    try {
      setSubmittingTip(true);
      const updated = await orderService.tipDriver(id, cents);
      setOrder(updated);
      setTipSent(true);
      showSuccess(`Tip of ${formatCurrencyCompact(cents)} sent to driver!`);
    } catch (err: any) {
      showError(err.message || 'Failed to send tip');
    } finally {
      setSubmittingTip(false);
    }
  };

  const getStatusColor = (status: OrderStatus) => {
    const colors: Record<string, string> = {
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
    return colors[status] || 'bg-gray-100 text-gray-800';
  };

  const statusSteps: OrderStatus[] = ['PLACED', 'ACCEPTED', 'PREPARING', 'READY', 'PICKED_UP', 'ENROUTE', 'DELIVERED'];

  const getCurrentStepIndex = (status: OrderStatus) => {
    // ASSIGNED means driver was assigned after READY, map to READY's position
    if (status === 'ASSIGNED') return statusSteps.indexOf('READY');
    return statusSteps.indexOf(status);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Error</h2>
          <p className="text-gray-600 mb-4">{error || 'Order not found'}</p>
          <Link to="/orders" className="text-primary-600 hover:text-primary-700 font-medium">
            View all orders
          </Link>
        </div>
      </div>
    );
  }

  const currentStep = getCurrentStepIndex(order.status);

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-6">
          <Link to="/orders" className="text-primary-600 hover:text-primary-700 font-medium inline-flex items-center">
            <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back to orders
          </Link>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex justify-between items-start mb-6">
            <div>
              <h1 className="text-2xl font-bold text-gray-900 mb-1">
                Order #{order.orderNumber || order.id.substring(0, 8).toUpperCase()}
              </h1>
              <p className="text-gray-600">Placed {formatDateTime(order.createdAt)}</p>
            </div>
            <div className="flex items-center gap-3">
              {order.status === 'PLACED' && (
                <button
                  onClick={handleCancel}
                  disabled={cancelling}
                  className="px-4 py-2 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700 disabled:opacity-50"
                >
                  {cancelling ? 'Cancelling...' : 'Cancel Order'}
                </button>
              )}
              <span className={`px-4 py-2 rounded-full text-sm font-medium ${getStatusColor(order.status)}`}>
                {order.status}
              </span>
              {order.deliveryType === 'PICKUP' && (
                <span className="px-3 py-1 rounded-full text-xs font-medium bg-orange-100 text-orange-700">
                  🏪 Pickup
                </span>
              )}
            </div>
          </div>

          {/* Progress Bar */}
          {!['CANCELLED', 'REJECTED'].includes(order.status) && (
            <div className="mb-8">
              <div className="flex justify-between mb-2">
                {statusSteps.map((step, index) => (
                  <div key={step} className={`flex-1 ${index < statusSteps.length - 1 ? 'mr-2' : ''}`}>
                    <div
                      className={`h-2 rounded-full ${
                        index <= currentStep ? 'bg-primary-600' : 'bg-gray-200'
                      }`}
                    />
                    <p className={`text-xs mt-1 text-center ${index <= currentStep ? 'text-primary-600 font-medium' : 'text-gray-500'}`}>
                      {step}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* ETA Display */}
          {order.estimatedDeliveryAt && !['DELIVERED', 'CANCELLED', 'REJECTED'].includes(order.status) && (
            <div className="mb-6 p-4 bg-primary-50 border border-primary-200 rounded-lg">
              <div className="flex items-center gap-2">
                <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span className="text-primary-800 font-medium">
                  Estimated Delivery: {new Date(order.estimatedDeliveryAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </span>
                {order.estimatedPrepMins && (
                  <span className="text-sm text-primary-600 ml-2">
                    (~{order.estimatedPrepMins} min prep)
                  </span>
                )}
              </div>
              {liveEta && (
                <div className="mt-2 flex items-center gap-2 text-sm text-primary-700">
                  <span className="relative flex h-2 w-2">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary-400 opacity-75" />
                    <span className="relative inline-flex rounded-full h-2 w-2 bg-primary-500" />
                  </span>
                  <span>
                    Live: driver is {formatDistance(liveEta.distanceKm)} away · ~{liveEta.durationMin} min by road
                  </span>
                </div>
              )}
            </div>
          )}

          {/* Status History Timeline */}
          {statusHistory.length > 0 && (
            <div className="mb-6">
              <h3 className="font-semibold text-gray-900 mb-3">Status History</h3>
              <div className="relative pl-6 space-y-4">
                <div className="absolute left-2 top-1 bottom-1 w-0.5 bg-gray-200" />
                {statusHistory.map((entry, idx) => (
                  <div key={idx} className="relative flex items-start">
                    <div className={`absolute -left-4 mt-1 w-3 h-3 rounded-full border-2 border-white ${
                      idx === statusHistory.length - 1 ? 'bg-primary-600' : 'bg-gray-400'
                    }`} />
                    <div>
                      <p className="font-medium text-sm text-gray-900">{entry.status}</p>
                      <p className="text-xs text-gray-500">{formatDateTime(entry.timestamp)}</p>
                      {entry.note && <p className="text-xs text-gray-600 mt-0.5">{entry.note}</p>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Order Details */}
          <div className="grid md:grid-cols-2 gap-6">
            <div>
              <h3 className="font-semibold mb-2">Restaurant</h3>
              <p className="text-gray-900">{order.vendorName}</p>
            </div>

            <div>
              <h3 className="font-semibold mb-2">Delivery Address</h3>
              <p className="text-gray-900">{order.deliveryAddress.line1}</p>
              {order.deliveryAddress.line2 && (
                <p className="text-gray-600">{order.deliveryAddress.line2}</p>
              )}
              <p className="text-gray-600">
                {order.deliveryAddress.city}, {order.deliveryAddress.state} {order.deliveryAddress.postal}
              </p>
            </div>

            <div>
              <h3 className="font-semibold mb-2">Payment</h3>
              <p className="text-gray-900">{order.paymentMethod}</p>
              <p className="text-sm text-gray-600">{order.paymentStatus}</p>
              {order.refundStatus && (
                <span className="inline-block mt-1 px-2 py-0.5 text-xs font-medium rounded-full bg-purple-100 text-purple-700">
                  {order.refundStatus}
                </span>
              )}
            </div>

            {order.driverName && (
              <div>
                <h3 className="font-semibold mb-2">Driver</h3>
                <p className="text-gray-900">{order.driverName}</p>
              </div>
            )}
          </div>

          {/* Delivery Proof — Phase 3 */}
          {(order.status === 'DELIVERED' || order.status === 'ENROUTE') && id && (
            <DeliveryProofDisplay orderId={id} />
          )}

          {/* Map View — Phase 3.6 + Live Driver Marker */}
          {!['CANCELLED', 'REJECTED'].includes(order.status) && id && (
            <div className="mt-6">
              <h3 className="font-semibold text-gray-900 mb-3">Delivery Map</h3>
              <LiveMapView
                orderId={id}
                deliveryLat={order.deliveryAddress?.lat}
                deliveryLng={order.deliveryAddress?.lng}
                vendorLat={order.vendorLat}
                vendorLng={order.vendorLng}
                vendorName={order.vendorName}
                onRouteUpdate={(info) => setLiveEta(info ? { distanceKm: info.distanceKm, durationMin: info.durationMin } : null)}
                className="h-64"
              />
            </div>
          )}

          {/* Review Form — shows for delivered orders */}
          {order.status === 'DELIVERED' && id && (
            <div className="mt-6">
              <ReviewForm orderId={id} />
            </div>
          )}

          {/* Tip the Driver — shows for delivered orders */}
          {order.status === 'DELIVERED' && order.driverName && (
            <div className="mt-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              {(order.tipCents ?? 0) > 0 || tipSent ? (
                <div className="flex items-center gap-2 text-yellow-800">
                  <span className="text-xl">💰</span>
                  <span className="font-medium">
                    Tip of {formatCurrencyCompact(order.tipCents ?? tipAmount ?? 0)} sent to {order.driverName}. Thank you!
                  </span>
                </div>
              ) : (
                <div>
                  <h3 className="font-semibold text-gray-900 mb-2 flex items-center gap-2">
                    <span className="text-xl">💰</span> Tip your driver
                  </h3>
                  <p className="text-sm text-gray-600 mb-3">
                    Show appreciation for {order.driverName}'s service
                  </p>
                  <div className="flex flex-wrap gap-2 mb-3">
                    {[50, 100, 200, 500].map((cents) => (
                      <button
                        key={cents}
                        type="button"
                        onClick={() => { setTipAmount(cents); setCustomTip(''); }}
                        className={`px-4 py-2 rounded-lg border-2 font-medium text-sm transition-colors ${
                          tipAmount === cents
                            ? 'border-yellow-500 bg-yellow-100 text-yellow-800'
                            : 'border-gray-200 hover:border-yellow-300 text-gray-700'
                        }`}
                      >
                        {formatCurrencyCompact(cents)}
                      </button>
                    ))}
                    <input
                      type="number"
                      placeholder="Custom"
                      value={customTip}
                      onChange={(e) => { setCustomTip(e.target.value); setTipAmount(null); }}
                      className="w-24 px-3 py-2 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:border-yellow-500"
                      min="0.01"
                      step="0.01"
                    />
                  </div>
                  <button
                    onClick={handleTipSubmit}
                    disabled={submittingTip || (!tipAmount && !customTip)}
                    className="px-6 py-2 bg-yellow-500 text-white font-medium rounded-lg hover:bg-yellow-600 disabled:opacity-50 transition-colors text-sm"
                  >
                    {submittingTip ? 'Sending...' : 'Send Tip'}
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Chat with Driver/Vendor — Phase 4 */}
          {!['DELIVERED', 'CANCELLED', 'REJECTED'].includes(order.status) && id && (
            <div className="mt-6">
              {showChat ? (
                <div>
                  <div className="flex justify-between items-center mb-2">
                    <h3 className="font-semibold text-gray-900">Live Chat</h3>
                    <button
                      onClick={() => setShowChat(false)}
                      className="text-sm text-gray-500 hover:text-gray-700"
                    >
                      Close
                    </button>
                  </div>
                  <ChatWindow 
                    orderId={id} 
                    otherUserId={order.driverId || order.vendorId}
                    otherUserName={order.driverName || order.vendorName}
                    roomType={order.driverId ? 'CUSTOMER_DRIVER' : 'CUSTOMER_VENDOR'}
                  />
                </div>
              ) : (
                <button
                  onClick={() => setShowChat(true)}
                  className="w-full py-3 bg-primary-50 text-primary-600 font-medium rounded-lg border border-primary-200 hover:bg-primary-100 transition-colors flex items-center justify-center gap-2"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                  Chat with {order.driverName ? `Driver ${order.driverName}` : `Restaurant`}
                </button>
              )}
            </div>
          )}
        </div>

        {/* Order Items */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-xl font-bold mb-4">Order Items</h2>
          <div className="space-y-3">
            {order.items.map((item) => (
              <div key={item.id} className="flex justify-between items-center py-2 border-b last:border-0">
                <div className="flex-1">
                  <p className="font-medium">{item.name}</p>
                  <p className="text-sm text-gray-600">Qty: {item.quantity}</p>
                </div>
                <p className="font-medium">{formatCurrencyCompact(item.totalCents)}</p>
              </div>
            ))}
          </div>

          <div className="mt-4 pt-4 border-t space-y-2">
            <div className="flex justify-between text-gray-600">
              <span>Subtotal</span>
              <span>{formatCurrencyCompact(order.subtotalCents)}</span>
            </div>
            <div className="flex justify-between text-gray-600">
              <span>Delivery Fee</span>
              <span>{formatCurrencyCompact(order.deliveryFeeCents)}</span>
            </div>
            <div className="flex justify-between text-gray-600">
              <span>Tax</span>
              <span>{formatCurrencyCompact(order.taxCents)}</span>
            </div>
            {(order.discountCents ?? 0) > 0 && (
              <div className="flex justify-between text-green-600">
                <span>Discount {order.promoCode && <span className="text-xs">({order.promoCode})</span>}</span>
                <span>-{formatCurrencyCompact(order.discountCents!)}</span>
              </div>
            )}
            <div className="flex justify-between text-lg font-bold">
              <span>Total</span>
              <span className="text-primary-600">{formatCurrencyCompact(order.totalCents)}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
