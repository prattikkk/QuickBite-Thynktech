/**
 * Main App component with routing
 */

import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { useEffect } from 'react';
import { useAuthStore } from './store';
import { Header, Footer, Toast, ProtectedRoute, RoleBasedRedirect } from './components';

// Pages
import Login from './pages/Login';
import Register from './pages/Register';
import VendorList from './pages/VendorList';
import VendorDetail from './pages/VendorDetail';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';
import OrderTrack from './pages/OrderTrack';
import MyOrders from './pages/MyOrders';
import VendorDashboard from './pages/VendorDashboard';
import DriverDashboard from './pages/DriverDashboard';
import AdminOrderTimeline from './pages/AdminOrderTimeline';
import AdminHealth from './pages/AdminHealth';
import Favorites from './pages/Favorites';

function App() {
  const loadFromStorage = useAuthStore((state) => state.loadFromStorage);

  // Load auth from localStorage on app start
  useEffect(() => {
    loadFromStorage();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <BrowserRouter>
      <div className="min-h-screen bg-gray-50 flex flex-col">
        <Header />
        <Toast />
        
        <main className="flex-1">
          <Routes>
            {/* Public routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
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

            {/* Default route - redirect based on role */}
            <Route path="/" element={<RoleBasedRedirect />} />
            
            {/* 404 */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </main>

        <Footer />
      </div>
    </BrowserRouter>
  );
}

// Simple 404 component
function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-gray-900 mb-2">404</h1>
        <p className="text-gray-600 mb-4">Page not found</p>
        <a href="/" className="text-primary-600 hover:text-primary-700 font-medium">
          Go home
        </a>
      </div>
    </div>
  );
}

export default App;
