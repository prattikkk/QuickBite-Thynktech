/**
 * Checkout page with Stripe payment integration
 */

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { loadStripe } from '@stripe/stripe-js';
import { Elements, CardElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { useCartStore, useToastStore } from '../store';
import { addressService, orderService, promoService } from '../services';
import { AddressDTO, PaymentMethod } from '../types';
import { formatCurrencyCompact } from '../utils';
import { LoadingSpinner } from '../components';

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

  // Inline "Add Address" form state
  const [showAddressForm, setShowAddressForm] = useState(false);
  const [newAddress, setNewAddress] = useState<Omit<AddressDTO, 'id'>>(EMPTY_ADDRESS);
  const [savingAddress, setSavingAddress] = useState(false);

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

    if (!selectedAddressId) {
      showError('Please select a delivery address');
      return;
    }

    try {
      setSubmitting(true);

      // 1. Create the order on the backend
      const orderData = {
        items: items.map((item) => ({
          menuItemId: item.menuItemId,
          quantity: item.quantity,
        })),
        addressId: selectedAddressId,
        paymentMethod,
        specialInstructions: specialInstructions.trim() || undefined,
        promoCode: promoApplied ? promoCode.trim() : undefined,
      };

      const order = await orderService.createOrder(orderData);

      // 2. For CARD — confirm the Stripe PaymentIntent on the client
      if (paymentMethod === 'CARD' && order.paymentClientSecret) {
        if (!stripe || !elements) {
          showError('Stripe is still loading — please try again.');
          return;
        }

        const cardElement = elements.getElement(CardElement);
        if (!cardElement) {
          showError('Card element not found');
          return;
        }

        const { error } = await stripe.confirmCardPayment(order.paymentClientSecret, {
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
  const deliveryFee = 5000;
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
          {/* Delivery Address */}
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
                <h3 className="font-semibold text-gray-800">New Address</h3>
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

          {/* Stripe Card Element — visible only for CARD */}
          {paymentMethod === 'CARD' && (
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
              disabled={submitting || addresses.length === 0 || (paymentMethod === 'CARD' && !stripe)}
              className="w-full bg-primary-600 text-white py-3 rounded-lg hover:bg-primary-700 transition-colors font-medium disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {submitting && <LoadingSpinner size="sm" />}
              {submitting
                ? 'Processing...'
                : paymentMethod === 'CARD'
                  ? 'Pay & Place Order'
                  : 'Place Order'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
