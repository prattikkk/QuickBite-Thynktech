/**
 * Checkout page with Stripe payment integration
 */

import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { loadStripe } from '@stripe/stripe-js';
import { Elements, CardElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { useCartStore, useToastStore } from '../store';
import { addressService, orderService, promoService } from '../services';
import api from '../services/api';
import { AddressDTO, PaymentMethod } from '../types';
import { formatCurrencyCompact } from '../utils';
import { LoadingSpinner, MapAddressPicker } from '../components';
import type { PickedLocation } from '../components/MapAddressPicker';

const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY || '');

/** Wrapper that provides Stripe Elements context. */
export default function Checkout() {
  return (
    <Elements stripe={stripePromise}>
      <CheckoutForm />
    </Elements>
  );
}

const CARD_ELEMENT_OPTIONS = {
  style: {
    base: {
      fontSize: '16px',
      color: '#374151',
      '::placeholder': { color: '#9CA3AF' },
    },
    invalid: { color: '#EF4444' },
  },
};

const EMPTY_ADDRESS: Omit<AddressDTO, 'id'> = {
  line1: '',
  line2: '',
  city: '',
  state: '',
  postal: '',
  country: 'IN',
};

function CheckoutForm() {
  const navigate = useNavigate();
  const stripe = useStripe();
  const elements = useElements();
  const { items, clearCart, getSubtotalCents } = useCartStore();
  const vendorId = useCartStore((s) => s.vendorId);
  const { success, error: showError } = useToastStore();

  const [addresses, setAddresses] = useState<AddressDTO[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState<string | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CARD');
  const [specialInstructions, setSpecialInstructions] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // Promo code state
  const [promoCode, setPromoCode] = useState('');
  const [promoApplied, setPromoApplied] = useState(false);
  const [promoDiscount, setPromoDiscount] = useState(0);
  const [promoMessage, setPromoMessage] = useState('');
  const [validatingPromo, setValidatingPromo] = useState(false);

  // Scheduled order state
  const [scheduleOrder, setScheduleOrder] = useState(false);
  const [scheduledTime, setScheduledTime] = useState('');

  // Delivery type (PICKUP vs DELIVERY)
  const [deliveryType, setDeliveryType] = useState<'DELIVERY' | 'PICKUP'>('DELIVERY');

  // Inline "Add Address" form state
  const [showAddressForm, setShowAddressForm] = useState(false);
  const [newAddress, setNewAddress] = useState<Omit<AddressDTO, 'id'>>(EMPTY_ADDRESS);
  const [savingAddress, setSavingAddress] = useState(false);
  const [useMapPicker, setUseMapPicker] = useState(true);

  // Out-of-range delivery warning
  const [rangeWarning, setRangeWarning] = useState<string | null>(null);
  const [checkingRange, setCheckingRange] = useState(false);

  const checkDeliveryRange = useCallback(async (lat: number | undefined, lng: number | undefined) => {
    if (!lat || !lng || !vendorId) {
      setRangeWarning(null);
      return;
    }
    try {
      setCheckingRange(true);
      const res = await api.get<any, any>(`/vendors/${vendorId}/delivery-check?lat=${lat}&lng=${lng}`);
      const data = res?.data ?? res;
      if (!data.vendorLocationSet) {
        setRangeWarning(null);
      } else if (!data.inRange) {
        setRangeWarning(`This address is ${data.distanceKm?.toFixed(1) || '?'} km from the restaurant (max ${data.radiusKm || 10} km). Delivery may not be available.`);
      } else {
        setRangeWarning(null);
      }
    } catch {
      setRangeWarning(null);
    } finally {
      setCheckingRange(false);
    }
  }, [vendorId]);

  // Check range when selected address changes
  useEffect(() => {
    if (deliveryType !== 'DELIVERY' || !selectedAddressId) {
      setRangeWarning(null);
      return;
    }
    const addr = addresses.find((a) => a.id === selectedAddressId);
    if (addr?.lat && addr?.lng) {
      checkDeliveryRange(addr.lat, addr.lng);
    } else {
      setRangeWarning(null);
    }
  }, [selectedAddressId, deliveryType, addresses, checkDeliveryRange]);

  useEffect(() => {
    loadAddresses();
  }, []);

  useEffect(() => {
    if (items.length === 0) {
      navigate('/cart');
    }
  }, [items]);

  const loadAddresses = async () => {
    try {
      const data = await addressService.getMyAddresses();
      setAddresses(data);
      if (data.length > 0) {
        setSelectedAddressId(data[0].id!);
      }
    } catch (err: any) {
      showError(err.message || 'Failed to load addresses');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveAddress = async () => {
    if (!newAddress.line1.trim() || !newAddress.city.trim() || !newAddress.state.trim() || !newAddress.postal.trim()) {
      showError('Please fill in all required address fields');
      return;
    }
    try {
      setSavingAddress(true);
      const saved = await addressService.createAddress(newAddress as AddressDTO);
      setAddresses((prev) => [...prev, saved]);
      setSelectedAddressId(saved.id!);
      setShowAddressForm(false);
      setNewAddress(EMPTY_ADDRESS);
      success('Address saved!');
    } catch (err: any) {
      showError(err.message || 'Failed to save address');
    } finally {
      setSavingAddress(false);
    }
  };

  const handleApplyPromo = async () => {
    if (!promoCode.trim()) return;
    try {
      setValidatingPromo(true);
      setPromoMessage('');
      const subtotal = getSubtotalCents();
      const result = await promoService.validatePromo(promoCode.trim(), subtotal);
      if (result.valid) {
        setPromoApplied(true);
        setPromoDiscount(result.discountCents ?? 0);
        setPromoMessage(result.message || 'Promo applied!');
      } else {
        setPromoApplied(false);
        setPromoDiscount(0);
        setPromoMessage(result.message || 'Invalid promo code');
      }
    } catch (err: any) {
      setPromoApplied(false);
      setPromoDiscount(0);
      setPromoMessage(err.message || 'Failed to validate promo');
    } finally {
      setValidatingPromo(false);
    }
  };

  const handleRemovePromo = () => {
    setPromoCode('');
    setPromoApplied(false);
    setPromoDiscount(0);
    setPromoMessage('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (deliveryType === 'DELIVERY' && !selectedAddressId) {
      showError('Please select a delivery address');
      return;
    }

    // Guard: validate Stripe is ready BEFORE creating the order
    // so a missing/invalid publishable key doesn't create orphaned orders.
    if (paymentMethod === 'CARD' || paymentMethod === 'UPI') {
      if (!stripe || !elements) {
        showError('Stripe is not loaded yet — please refresh the page and try again.');
        return;
      }
      if (!elements.getElement(CardElement)) {
        showError('Card input not found — please refresh and try again.');
        return;
      }
    }

    try {
      setSubmitting(true);

      // 0. Validate scheduled time if scheduling
      if (scheduleOrder && scheduledTime && vendorId) {
        try {
          const validation = await orderService.validateSchedule(scheduledTime, vendorId);
          if (!validation.valid) {
            showError(validation.message || 'Scheduled time is invalid');
            setSubmitting(false);
            return;
          }
        } catch (err: any) {
          showError(err.message || 'Failed to validate scheduled time');
          setSubmitting(false);
          return;
        }
      }

      // 1. Create the order on the backend
      const orderData = {
        items: items.map((item) => ({
          menuItemId: item.menuItemId,
          quantity: item.quantity,
        })),
        addressId: deliveryType === 'DELIVERY' ? selectedAddressId! : (selectedAddressId || undefined),
        paymentMethod,
        specialInstructions: specialInstructions.trim() || undefined,
        promoCode: promoApplied ? promoCode.trim() : undefined,
        scheduledTime: scheduleOrder && scheduledTime ? scheduledTime : undefined,
        deliveryType,
      };

      const order = await orderService.createOrder(orderData);

      // 2. For CARD/UPI — confirm the Stripe PaymentIntent on the client
      if ((paymentMethod === 'CARD' || paymentMethod === 'UPI') && order.paymentClientSecret) {
        const cardElement = elements!.getElement(CardElement)!;

        const { error } = await stripe!.confirmCardPayment(order.paymentClientSecret, {
          payment_method: { card: cardElement },
        });

        if (error) {
          showError(error.message || 'Payment failed');
          // Order is already placed but payment failed — navigate anyway so user can retry/track
          navigate(`/orders/${order.id}`);
          return;
        }
      }

      // 3. Done — clear cart and go to order tracking
      clearCart();
      success('Order placed successfully!');
      navigate(`/orders/${order.id}`);
    } catch (err: any) {
      showError(err.message || 'Failed to place order');
    } finally {
      setSubmitting(false);
    }
  };

  const subtotal = getSubtotalCents();
  const deliveryFee = deliveryType === 'PICKUP' ? 0 : 5000;
  const tax = Math.round(subtotal * 0.05);
  const total = subtotal + deliveryFee + tax - promoDiscount;

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
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Checkout</h1>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Delivery Type Toggle */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-bold mb-4">Order Type</h2>
            <div className="grid grid-cols-2 gap-3">
              {(['DELIVERY', 'PICKUP'] as const).map((type) => (
                <button
                  key={type}
                  type="button"
                  onClick={() => setDeliveryType(type)}
                  className={`flex items-center justify-center gap-2 p-4 rounded-lg border-2 transition-colors font-medium ${
                    deliveryType === type
                      ? 'border-primary-600 bg-primary-50 text-primary-700'
                      : 'border-gray-200 hover:border-gray-300 text-gray-600'
                  }`}
                >
                  <span className="text-xl">{type === 'DELIVERY' ? '🛵' : '🏪'}</span>
                  <span>{type === 'DELIVERY' ? 'Delivery' : 'Pickup'}</span>
                </button>
              ))}
            </div>
            {deliveryType === 'PICKUP' && (
              <p className="text-sm text-gray-500 mt-3">
                Pick up your order directly from the restaurant. No delivery fee!
              </p>
            )}
          </div>

          {/* Delivery Address */}
          {deliveryType === 'DELIVERY' && (
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold">Delivery Address</h2>
              {!showAddressForm && (
                <button
                  type="button"
                  onClick={() => setShowAddressForm(true)}
                  className="text-sm text-primary-600 hover:text-primary-700 font-medium"
                >
                  + Add New Address
                </button>
              )}
            </div>

            {/* Existing addresses */}
            {addresses.length > 0 && (
              <div className="space-y-3 mb-4">
                {addresses.map((address) => (
                  <label
                    key={address.id}
                    className="flex items-start gap-3 p-4 border rounded-lg cursor-pointer hover:border-primary-500 transition-colors"
                  >
                    <input
                      type="radio"
                      name="address"
                      value={address.id}
                      checked={selectedAddressId === address.id}
                      onChange={() => setSelectedAddressId(address.id!)}
                      className="mt-1"
                    />
                    <div>
                      <p className="font-medium">{address.line1}</p>
                      {address.line2 && <p className="text-sm text-gray-600">{address.line2}</p>}
                      <p className="text-sm text-gray-600">
                        {address.city}, {address.state} {address.postal}
                      </p>
                    </div>
                  </label>
                ))}
              </div>
            )}

            {addresses.length === 0 && !showAddressForm && (
              <p className="text-gray-600 mb-2">
                No saved addresses.{' '}
                <button
                  type="button"
                  onClick={() => setShowAddressForm(true)}
                  className="text-primary-600 hover:underline font-medium"
                >
                  Add one to continue
                </button>
              </p>
            )}

            {/* Inline Add Address Form */}
            {showAddressForm && (
              <div className="border border-primary-200 bg-primary-50 rounded-lg p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="font-semibold text-gray-800">New Address</h3>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => setUseMapPicker(true)}
                      className={`text-xs px-3 py-1 rounded-full font-medium transition-colors ${
                        useMapPicker
                          ? 'bg-primary-600 text-white'
                          : 'bg-gray-200 text-gray-600 hover:bg-gray-300'
                      }`}
                    >
                      🗺️ Map
                    </button>
                    <button
                      type="button"
                      onClick={() => setUseMapPicker(false)}
                      className={`text-xs px-3 py-1 rounded-full font-medium transition-colors ${
                        !useMapPicker
                          ? 'bg-primary-600 text-white'
                          : 'bg-gray-200 text-gray-600 hover:bg-gray-300'
                      }`}
                    >
                      ✏️ Manual
                    </button>
                  </div>
                </div>

                {/* Map picker */}
                {useMapPicker && (
                  <MapAddressPicker
                    label="Pin your delivery location"
                    height="260px"
                    onLocationSelect={(loc: PickedLocation) => {
                      setNewAddress({
                        line1: loc.line1,
                        line2: loc.line2 || '',
                        city: loc.city,
                        state: loc.state,
                        postal: loc.postal,
                        country: loc.country || 'IN',
                        lat: loc.lat,
                        lng: loc.lng,
                      });
                    }}
                  />
                )}

                {/* Address fields (auto-filled from map or manual entry) */}
                <input
                  type="text"
                  placeholder="Address Line 1 *"
                  value={newAddress.line1}
                  onChange={(e) => setNewAddress({ ...newAddress, line1: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
                <input
                  type="text"
                  placeholder="Address Line 2 (Optional)"
                  value={newAddress.line2 || ''}
                  onChange={(e) => setNewAddress({ ...newAddress, line2: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
                <div className="grid grid-cols-2 gap-3">
                  <input
                    type="text"
                    placeholder="City *"
                    value={newAddress.city}
                    onChange={(e) => setNewAddress({ ...newAddress, city: e.target.value })}
                    className="px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                  <input
                    type="text"
                    placeholder="State *"
                    value={newAddress.state}
                    onChange={(e) => setNewAddress({ ...newAddress, state: e.target.value })}
                    className="px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <input
                    type="text"
                    placeholder="Postal Code *"
                    value={newAddress.postal}
                    onChange={(e) => setNewAddress({ ...newAddress, postal: e.target.value })}
                    className="px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                  <input
                    type="text"
                    placeholder="Country"
                    value={newAddress.country}
                    onChange={(e) => setNewAddress({ ...newAddress, country: e.target.value })}
                    className="px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
                <div className="flex gap-3 pt-1">
                  <button
                    type="button"
                    onClick={handleSaveAddress}
                    disabled={savingAddress}
                    className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 text-sm font-medium"
                  >
                    {savingAddress ? 'Saving...' : 'Save Address'}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setShowAddressForm(false);
                      setNewAddress(EMPTY_ADDRESS);
                    }}
                    className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-100 text-sm font-medium"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}
          </div>
          )}

          {/* Out-of-range delivery warning */}
          {rangeWarning && deliveryType === 'DELIVERY' && (
            <div className="bg-amber-50 border border-amber-300 rounded-lg p-4 flex items-start gap-3">
              <span className="text-xl flex-shrink-0 mt-0.5">⚠️</span>
              <div>
                <p className="text-amber-800 font-semibold text-sm">Delivery may not be available</p>
                <p className="text-amber-700 text-sm mt-0.5">{rangeWarning}</p>
              </div>
            </div>
          )}
          {checkingRange && deliveryType === 'DELIVERY' && (
            <div className="text-xs text-gray-500 flex items-center gap-1.5 px-1">
              <svg className="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
              </svg>
              Checking delivery range...
            </div>
          )}

          {/* Payment Method */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-bold mb-4">Payment Method</h2>
            <div className="space-y-2">
              {(['CARD', 'UPI', 'CASH_ON_DELIVERY'] as PaymentMethod[]).map((method) => (
                <label
                  key={method}
                  className="flex items-center gap-3 p-4 border rounded-lg cursor-pointer hover:border-primary-500 transition-colors"
                >
                  <input
                    type="radio"
                    name="payment"
                    value={method}
                    checked={paymentMethod === method}
                    onChange={() => setPaymentMethod(method)}
                  />
                  <span className="font-medium">
                    {method === 'CASH_ON_DELIVERY'
                      ? 'Cash on Delivery'
                      : method === 'CARD'
                        ? 'Credit / Debit Card'
                        : method}
                  </span>
                </label>
              ))}
            </div>
          </div>

          {/* Stripe Card Element — visible for CARD and UPI */}
          {(paymentMethod === 'CARD' || paymentMethod === 'UPI') && (
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-xl font-bold mb-4">Card Details</h2>
              <div className="p-4 border rounded-lg">
                <CardElement options={CARD_ELEMENT_OPTIONS} />
              </div>
              <p className="mt-2 text-xs text-gray-500">
                Powered by Stripe. Your card details are never stored on our servers.
              </p>
            </div>
          )}

          {/* Promo Code */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-bold mb-4">Promo Code</h2>
            {promoApplied ? (
              <div className="flex items-center justify-between p-3 bg-green-50 border border-green-200 rounded-lg">
                <div>
                  <span className="text-green-700 font-medium">{promoCode.toUpperCase()}</span>
                  <span className="text-green-600 text-sm ml-2">-{formatCurrencyCompact(promoDiscount)}</span>
                </div>
                <button
                  type="button"
                  onClick={handleRemovePromo}
                  className="text-sm text-red-600 hover:text-red-700 font-medium"
                >
                  Remove
                </button>
              </div>
            ) : (
              <div className="flex gap-2">
                <input
                  type="text"
                  value={promoCode}
                  onChange={(e) => setPromoCode(e.target.value.toUpperCase())}
                  placeholder="Enter promo code"
                  className="flex-1 px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
                <button
                  type="button"
                  onClick={handleApplyPromo}
                  disabled={validatingPromo || !promoCode.trim()}
                  className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 font-medium text-sm"
                >
                  {validatingPromo ? 'Checking...' : 'Apply'}
                </button>
              </div>
            )}
            {promoMessage && !promoApplied && (
              <p className="text-sm text-red-600 mt-2">{promoMessage}</p>
            )}
          </div>

          {/* Special Instructions */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-bold mb-4">Special Instructions</h2>
            <textarea
              value={specialInstructions}
              onChange={(e) => setSpecialInstructions(e.target.value)}
              placeholder="Any special requests for the restaurant..."
              rows={3}
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          {/* Schedule Order — Phase 4 */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold">Schedule Order</h2>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={scheduleOrder}
                  onChange={(e) => {
                    setScheduleOrder(e.target.checked);
                    if (!e.target.checked) setScheduledTime('');
                  }}
                  className="h-4 w-4 text-primary-600 rounded border-gray-300"
                />
                <span className="text-sm text-gray-700">Schedule for later</span>
              </label>
            </div>
            {scheduleOrder && (
              <div>
                <input
                  type="datetime-local"
                  value={scheduledTime}
                  onChange={(e) => setScheduledTime(e.target.value)}
                  min={new Date(Date.now() + 30 * 60000).toISOString().slice(0, 16)}
                  className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
                <p className="text-xs text-gray-500 mt-1">Must be at least 30 minutes from now</p>
              </div>
            )}
          </div>

          {/* Order Summary */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-bold mb-4">Order Summary</h2>
            <div className="space-y-2 mb-4">
              <div className="flex justify-between">
                <span>Subtotal</span>
                <span>{formatCurrencyCompact(subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span>Delivery Fee</span>
                <span>{formatCurrencyCompact(deliveryFee)}</span>
              </div>
              <div className="flex justify-between">
                <span>Tax</span>
                <span>{formatCurrencyCompact(tax)}</span>
              </div>
              {promoDiscount > 0 && (
                <div className="flex justify-between text-green-600">
                  <span>Discount</span>
                  <span>-{formatCurrencyCompact(promoDiscount)}</span>
                </div>
              )}
              <div className="border-t pt-2 flex justify-between text-lg font-bold">
                <span>Total</span>
                <span className="text-primary-600">{formatCurrencyCompact(total)}</span>
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting || (deliveryType === 'DELIVERY' && addresses.length === 0) || ((paymentMethod === 'CARD' || paymentMethod === 'UPI') && !stripe)}
              className="w-full bg-primary-600 text-white py-3 rounded-lg hover:bg-primary-700 transition-colors font-medium disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {submitting && <LoadingSpinner size="sm" />}
              {submitting
                ? 'Processing...'
                : (paymentMethod === 'CARD' || paymentMethod === 'UPI')
                  ? 'Pay & Place Order'
                  : 'Place Order'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
