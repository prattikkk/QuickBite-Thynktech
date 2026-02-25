/**
 * DriverDashboard page — Phase 2 enhanced with live location, shift management, and GPS indicator.
 */

import { useState, useEffect, FormEvent, useRef } from 'react';
import { driverService, type DriverOrderSummary, type DriverProfileDTO } from '../services/driver.service';
import { userService } from '../services/user.service';
import { OrderDTO } from '../types';
import { LoadingSpinner } from '../components';
import LiveMapView from '../components/LiveMapView';
import ProofCaptureModal from '../components/ProofCaptureModal';
import ChatWindow from '../components/ChatWindow';
import { formatCurrencyCompact, formatDateTime } from '../utils';
import { useToastStore } from '../store';
import { useDriverLocation } from '../hooks';
import DriverRatings from '../components/DriverRatings';

type Tab = 'assigned' | 'available' | 'history' | 'ratings' | 'profile';

export default function DriverDashboard() {
  const [orders, setOrders] = useState<OrderDTO[]>([]);
  const [availableOrders, setAvailableOrders] = useState<DriverOrderSummary[]>([]);
  const [historyOrders, setHistoryOrders] = useState<DriverOrderSummary[]>([]);
  const [profile, setProfile] = useState<DriverProfileDTO | null>(null);
  const [tab, setTab] = useState<Tab>('assigned');
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [statusToggling, setStatusToggling] = useState(false);
  const [shiftLoading, setShiftLoading] = useState(false);
  const [proofOrderId, setProofOrderId] = useState<string | null>(null);
  const [chatOrderId, setChatOrderId] = useState<string | null>(null);
  const { success, error: showError } = useToastStore();

  // Shift-based live location tracking
  const isShiftActive = profile?.shiftStartedAt != null && profile?.shiftEndedAt == null && profile?.isOnline;
  const locationState = useDriverLocation({ enabled: !!isShiftActive });

  // Profile edit form state
  const [editVehicle, setEditVehicle] = useState('');
  const [editPlate, setEditPlate] = useState('');
  const [profileSaving, setProfileSaving] = useState(false);
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const avatarInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    loadOrders();
    loadProfile();
    // Auto-refresh every 10s for new assignments
    const interval = setInterval(loadOrders, 10000);
    return () => clearInterval(interval);
  }, []);

  const loadProfile = async () => {
    try {
      const p = await driverService.getProfile();
      setProfile(p);
      setEditVehicle(p.vehicleType || '');
      setEditPlate(p.licensePlate || '');
    } catch {
      // Profile endpoint may 404 on first load; ignore
    }
    try {
      const up = await userService.getProfile();
      setAvatarUrl(up.avatarUrl ?? null);
    } catch { /* ignore */ }
  };

  const handleAvatarUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingAvatar(true);
    try {
      const updated = await userService.uploadAvatar(file);
      setAvatarUrl(updated.avatarUrl ?? null);
      success('Profile photo updated!');
    } catch {
      showError('Failed to upload photo.');
    } finally {
      setUploadingAvatar(false);
      if (avatarInputRef.current) avatarInputRef.current.value = '';
    }
  };

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
    if (tab === 'profile' && !profile) {
      loadProfile();
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
    // Open proof capture modal instead of directly marking delivered
    setProofOrderId(orderId);
  };

  const handleDeliverConfirmed = async (orderId: string) => {
    try {
      setActionLoading(orderId);
      await driverService.markDelivered(orderId);
      success('Order delivered successfully!');
      setProofOrderId(null);
      loadOrders();
    } catch (err: any) {
      showError(err.message || 'Failed to update order');
    } finally {
      setActionLoading(null);
    }
  };

  const handleToggleStatus = async () => {
    if (!profile) return;
    try {
      setStatusToggling(true);
      await driverService.toggleStatus(!profile.isOnline);
      setProfile({ ...profile, isOnline: !profile.isOnline });
      success(profile.isOnline ? 'You are now offline' : 'You are now online');
    } catch (err: any) {
      showError(err.message || 'Failed to toggle status');
    } finally {
      setStatusToggling(false);
    }
  };

  const handleShiftToggle = async () => {
    if (!profile) return;
    try {
      setShiftLoading(true);
      if (isShiftActive) {
        const updated = await driverService.endShift();
        setProfile(updated);
        success('Shift ended — GPS sharing stopped');
      } else {
        // Check location permission before starting shift
        if (locationState.permissionState === 'denied') {
          showError('Location permission denied. Please enable it in browser settings.');
          return;
        }
        const updated = await driverService.startShift();
        setProfile(updated);
        success('Shift started — GPS sharing active');
      }
    } catch (err: any) {
      showError(err.message || 'Failed to toggle shift');
    } finally {
      setShiftLoading(false);
    }
  };

  const handleProfileSave = async (e: FormEvent) => {
    e.preventDefault();
    try {
      setProfileSaving(true);
      await driverService.updateProfile(editVehicle, editPlate);
      success('Profile updated');
      loadProfile();
    } catch (err: any) {
      showError(err.message || 'Failed to update profile');
    } finally {
      setProfileSaving(false);
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
        {/* Header with shift toggle and GPS indicator */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-6 gap-4">
          <h1 className="text-3xl font-bold text-gray-900">Driver Dashboard</h1>
          <div className="flex items-center gap-3">
            {/* GPS location indicator */}
            {isShiftActive && (
              <div className="flex items-center gap-1.5 text-xs">
                {locationState.isTracking ? (
                  <>
                    <span className="relative flex h-3 w-3">
                      <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75" />
                      <span className="relative inline-flex rounded-full h-3 w-3 bg-green-500" />
                    </span>
                    <span className="text-green-700 font-medium">
                      GPS Active{locationState.accuracy ? ` (±${Math.round(locationState.accuracy)}m)` : ''}
                    </span>
                  </>
                ) : locationState.error ? (
                  <>
                    <span className="h-3 w-3 rounded-full bg-red-500" />
                    <span className="text-red-600 font-medium">{locationState.error}</span>
                  </>
                ) : (
                  <>
                    <span className="h-3 w-3 rounded-full bg-yellow-400 animate-pulse" />
                    <span className="text-yellow-700 font-medium">Acquiring GPS...</span>
                  </>
                )}
              </div>
            )}

            {/* Shift Start/Stop button */}
            {profile && (
              <button
                onClick={handleShiftToggle}
                disabled={shiftLoading}
                className={`flex items-center gap-2 px-5 py-2 rounded-full font-semibold text-sm transition-colors shadow ${
                  isShiftActive
                    ? 'bg-red-500 text-white hover:bg-red-600'
                    : 'bg-green-500 text-white hover:bg-green-600'
                } disabled:opacity-50`}
              >
                <span className={`h-3 w-3 rounded-full ${isShiftActive ? 'bg-red-200 animate-pulse' : 'bg-green-200'}`} />
                {shiftLoading ? 'Updating...' : isShiftActive ? 'End Shift' : 'Start Shift'}
              </button>
            )}

            {/* Legacy online/offline toggle (kept as secondary control) */}
            {profile && !isShiftActive && (
              <button
                onClick={handleToggleStatus}
                disabled={statusToggling}
                className={`flex items-center gap-2 px-4 py-2 rounded-full font-medium text-xs transition-colors ${
                  profile.isOnline
                    ? 'bg-green-100 text-green-700 hover:bg-green-200'
                    : 'bg-gray-200 text-gray-600 hover:bg-gray-300'
                } disabled:opacity-50`}
              >
                {statusToggling ? '...' : profile.isOnline ? 'Online' : 'Offline'}
              </button>
            )}
          </div>
        </div>

        {/* Stats bar */}
        {profile && (
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
            <div className="bg-white rounded-lg shadow p-4 text-center">
              <p className="text-2xl font-bold text-primary-600">{profile.totalDeliveries}</p>
              <p className="text-xs text-gray-500">Total Deliveries</p>
            </div>
            <div className="bg-white rounded-lg shadow p-4 text-center">
              <p className="text-2xl font-bold text-green-600">{profile.successRate?.toFixed(1)}%</p>
              <p className="text-xs text-gray-500">Success Rate</p>
            </div>
            <div className="bg-white rounded-lg shadow p-4 text-center">
              <p className="text-2xl font-bold text-blue-600">{orders.length}</p>
              <p className="text-xs text-gray-500">Active Orders</p>
            </div>
            <div className="bg-white rounded-lg shadow p-4 text-center">
              <p className="text-2xl font-bold text-orange-600">{availableOrders.length}</p>
              <p className="text-xs text-gray-500">Available</p>
            </div>
          </div>
        )}

        {/* Tabs */}
        <div className="flex space-x-1 bg-gray-200 rounded-lg p-1 mb-6">
          {([
            { key: 'assigned' as Tab, label: 'My Orders', count: orders.length },
            { key: 'available' as Tab, label: 'Available', count: availableOrders.length },
            { key: 'history' as Tab, label: 'History', count: null },
            { key: 'ratings' as Tab, label: 'Ratings', count: null },
            { key: 'profile' as Tab, label: 'Profile', count: null },
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

                    {/* Live Map for active deliveries */}
                    {['PICKED_UP', 'ENROUTE'].includes(order.status) && (
                      <div className="mb-4">
                        <LiveMapView
                          orderId={order.id}
                          deliveryLat={order.deliveryAddress?.lat}
                          deliveryLng={order.deliveryAddress?.lng}
                          vendorLat={order.vendorLat}
                          vendorLng={order.vendorLng}
                          vendorName={order.vendorName}
                          className="h-52"
                        />
                      </div>
                    )}

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

                    {/* Chat with Customer */}
                    <div className="mt-4 pt-4 border-t">
                      {chatOrderId === order.id ? (
                        <div>
                          <div className="flex justify-between items-center mb-2">
                            <h4 className="font-semibold text-gray-900">Chat with Customer</h4>
                            <button
                              onClick={() => setChatOrderId(null)}
                              className="text-sm text-gray-500 hover:text-gray-700"
                            >
                              Close
                            </button>
                          </div>
                          <ChatWindow
                            orderId={order.id}
                            otherUserId={order.customerId}
                            otherUserName={order.customerName}
                            roomType="CUSTOMER_DRIVER"
                          />
                        </div>
                      ) : (
                        <button
                          onClick={() => setChatOrderId(order.id)}
                          className="w-full py-2 px-4 bg-blue-50 text-blue-600 font-medium rounded-lg border border-blue-200 hover:bg-blue-100 transition-colors flex items-center justify-center gap-2"
                        >
                          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                          </svg>
                          Chat with {order.customerName}
                        </button>
                      )}
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

        {/* Ratings tab */}
        {tab === 'ratings' && profile && (
          <DriverRatings driverId={profile.id || ''} />
        )}

        {/* Profile tab */}
        {tab === 'profile' && (
          <div className="max-w-lg mx-auto">
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-xl font-bold text-gray-900 mb-4">Driver Profile</h2>
              {profile ? (
                <form onSubmit={handleProfileSave} className="space-y-4">
                  {/* Avatar upload */}
                  <div className="flex flex-col items-center mb-2">
                    <div
                      className="relative w-24 h-24 rounded-full cursor-pointer group"
                      onClick={() => !uploadingAvatar && avatarInputRef.current?.click()}
                    >
                      {avatarUrl ? (
                        <img src={avatarUrl} alt="avatar" className="w-24 h-24 rounded-full object-cover border-2 border-primary-300" />
                      ) : (
                        <div className="w-24 h-24 rounded-full bg-primary-100 flex items-center justify-center text-3xl font-bold text-primary-600 border-2 border-primary-200">
                          {profile.name?.charAt(0).toUpperCase() || 'D'}
                        </div>
                      )}
                      <div className="absolute inset-0 rounded-full bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                        {uploadingAvatar ? (
                          <svg className="animate-spin h-6 w-6 text-white" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/></svg>
                        ) : (
                          <svg className="h-7 w-7 text-white" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"/><path strokeLinecap="round" strokeLinejoin="round" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                        )}
                      </div>
                    </div>
                    <p className="text-xs text-gray-500 mt-1">Click to change photo</p>
                    <input ref={avatarInputRef} type="file" accept="image/jpeg,image/png,image/webp" className="hidden" onChange={handleAvatarUpload} />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
                    <p className="text-gray-900">{profile.name}</p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                    <p className="text-gray-900">{profile.email}</p>
                  </div>
                  <div>
                    <label htmlFor="vehicleType" className="block text-sm font-medium text-gray-700 mb-1">Vehicle Type</label>
                    <select
                      id="vehicleType"
                      value={editVehicle}
                      onChange={(e) => setEditVehicle(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-primary-500 focus:border-primary-500"
                    >
                      <option value="">Select vehicle</option>
                      <option value="BIKE">Bike</option>
                      <option value="MOTORCYCLE">Motorcycle</option>
                      <option value="CAR">Car</option>
                      <option value="VAN">Van</option>
                    </select>
                  </div>
                  <div>
                    <label htmlFor="licensePlate" className="block text-sm font-medium text-gray-700 mb-1">License Plate</label>
                    <input
                      id="licensePlate"
                      type="text"
                      value={editPlate}
                      onChange={(e) => setEditPlate(e.target.value)}
                      placeholder="ABC-1234"
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-primary-500 focus:border-primary-500"
                    />
                  </div>
                  <button
                    type="submit"
                    disabled={profileSaving}
                    className="w-full py-2 px-4 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 font-medium"
                  >
                    {profileSaving ? 'Saving...' : 'Save Profile'}
                  </button>
                </form>
              ) : (
                <LoadingSpinner />
              )}
            </div>
          </div>
        )}
      </div>

      {/* Proof-of-delivery modal (Phase 3) */}
      {proofOrderId && (
        <ProofCaptureModal
          orderId={proofOrderId}
          onProofSubmitted={() => {
            success('Proof submitted! Marking order as delivered...');
            handleDeliverConfirmed(proofOrderId);
          }}
          onCancel={() => setProofOrderId(null)}
          onDeliverWithoutProof={() => handleDeliverConfirmed(proofOrderId)}
        />
      )}
    </div>
  );
}
