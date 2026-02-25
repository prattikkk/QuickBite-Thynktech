/**
 * Main App component with routing
 * Uses React.lazy for route-level code splitting
 */

import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { useEffect, lazy, Suspense } from 'react';
import { useAuthStore } from './store';
import { deviceService } from './services';
import { Header, Footer, Toast, ProtectedRoute, RoleBasedRedirect, ErrorBoundary, PWAInstallPrompt, OfflineBanner, LoadingSpinner } from './components';

// Lazy-loaded pages for code splitting
const Login = lazy(() => import('./pages/Login'));
const Register = lazy(() => import('./pages/Register'));
const VendorList = lazy(() => import('./pages/VendorList'));
const VendorDetail = lazy(() => import('./pages/VendorDetail'));
const Cart = lazy(() => import('./pages/Cart'));
const Checkout = lazy(() => import('./pages/Checkout'));
const OrderTrack = lazy(() => import('./pages/OrderTrack'));
const MyOrders = lazy(() => import('./pages/MyOrders'));
const VendorDashboard = lazy(() => import('./pages/VendorDashboard'));
const DriverDashboard = lazy(() => import('./pages/DriverDashboard'));
const AdminOrderTimeline = lazy(() => import('./pages/AdminOrderTimeline'));
const AdminHealth = lazy(() => import('./pages/AdminHealth'));
const AdminManagement = lazy(() => import('./pages/AdminManagement'));
const Favorites = lazy(() => import('./pages/Favorites'));
const Profile = lazy(() => import('./pages/Profile'));
const ForgotPassword = lazy(() => import('./pages/ForgotPassword'));
const ResetPassword = lazy(() => import('./pages/ResetPassword'));
const VerifyEmail = lazy(() => import('./pages/VerifyEmail'));
const Notifications = lazy(() => import('./pages/Notifications'));
const AdminReporting = lazy(() => import('./pages/AdminReporting'));
const Settings = lazy(() => import('./pages/Settings'));
const AdminRefunds = lazy(() => import('./pages/AdminRefunds'));
const AdminPromos = lazy(() => import('./pages/AdminPromos'));
const AdminCommissions = lazy(() => import('./pages/AdminCommissions'));
const AdminReviewModeration = lazy(() => import('./pages/AdminReviewModeration'));

function App() {
  const loadFromStorage = useAuthStore((state) => state.loadFromStorage);
  const user = useAuthStore((state) => state.user);

  // Load auth from localStorage on app start
  useEffect(() => {
    loadFromStorage();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Register device for push notifications when user is authenticated
  useEffect(() => {
    if (user) {
      deviceService.requestAndRegister().catch(() => {
        // silent — push is optional
      });
    }
  }, [user]);

  return (
    <BrowserRouter>
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex flex-col transition-colors">
        <OfflineBanner />
        <Header />
        <Toast />
        
        <main id="main-content" className="flex-1">
          <ErrorBoundary>
          <Suspense fallback={<div className="flex justify-center items-center min-h-[50vh]"><LoadingSpinner size="lg" /></div>}>
          <Routes>
            {/* Public routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route path="/verify-email" element={<VerifyEmail />} />
            <Route
              path="/vendors"
              element={
                <ProtectedRoute requiredRole="CUSTOMER">
                  <VendorList />
                </ProtectedRoute>
              }
            />
            <Route
              path="/vendors/:id"
              element={
                <ProtectedRoute requiredRole="CUSTOMER">
                  <VendorDetail />
                </ProtectedRoute>
              }
            />

            {/* Protected customer routes */}
            <Route
              path="/cart"
              element={
                <ProtectedRoute requiredRole="CUSTOMER">
                  <Cart />
                </ProtectedRoute>
              }
            />
            <Route
              path="/checkout"
              element={
                <ProtectedRoute requiredRole="CUSTOMER">
                  <Checkout />
                </ProtectedRoute>
              }
            />
            <Route
              path="/favorites"
              element={
                <ProtectedRoute requiredRole="CUSTOMER">
                  <Favorites />
                </ProtectedRoute>
              }
            />
            <Route
              path="/orders/:id"
              element={
                <ProtectedRoute>
                  <OrderTrack />
                </ProtectedRoute>
              }
            />
            <Route
              path="/orders"
              element={
                <ProtectedRoute requiredRole="CUSTOMER">
                  <MyOrders />
                </ProtectedRoute>
              }
            />
            <Route
              path="/profile"
              element={
                <ProtectedRoute>
                  <Profile />
                </ProtectedRoute>
              }
            />
            <Route
              path="/notifications"
              element={
                <ProtectedRoute>
                  <Notifications />
                </ProtectedRoute>
              }
            />

            {/* Vendor routes */}
            <Route
              path="/vendor/dashboard"
              element={
                <ProtectedRoute requiredRole="VENDOR">
                  <VendorDashboard />
                </ProtectedRoute>
              }
            />

            {/* Driver routes */}
            <Route
              path="/driver/dashboard"
              element={
                <ProtectedRoute requiredRole="DRIVER">
                  <DriverDashboard />
                </ProtectedRoute>
              }
            />

            {/* Admin routes */}
            <Route
              path="/admin/orders/:orderId/timeline"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <AdminOrderTimeline />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/health"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <AdminHealth />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/management"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <AdminManagement />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/reports"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <AdminReporting />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/refunds"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <AdminRefunds />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/promos"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <AdminPromos />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/commissions"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <AdminCommissions />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/reviews"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <AdminReviewModeration />
                </ProtectedRoute>
              }
            />

            {/* Settings */}
            <Route
              path="/settings"
              element={
                <ProtectedRoute>
                  <Settings />
                </ProtectedRoute>
              }
            />

            {/* Default route - redirect based on role */}
            <Route path="/" element={<RoleBasedRedirect />} />
            
            {/* 404 */}
            <Route path="*" element={<NotFound />} />
          </Routes>
          </Suspense>
          </ErrorBoundary>
        </main>

        <Footer />
        <PWAInstallPrompt />
      </div>
    </BrowserRouter>
  );
}

// Simple 404 component
function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-2">404</h1>
        <p className="text-gray-600 dark:text-gray-400 mb-4">Page not found</p>
        <a href="/" className="text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300 font-medium">
          Go home
        </a>
      </div>
    </div>
  );
}

export default App;
