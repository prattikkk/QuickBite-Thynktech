/**
 * Header component with role-aware navigation
 */

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks';
import CartWidget from './CartWidget';
import NotificationBell from './NotificationBell';

export default function Header() {
  const { isAuthenticated, user, logout, isCustomer, isVendor, isDriver, isAdmin } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    setMobileMenuOpen(false);
  };

  return (
    <header className="bg-white shadow-md sticky top-0 z-40" role="banner">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center py-4">
          {/* Logo */}
          <Link to="/" className="text-2xl font-bold text-primary-600 flex items-center gap-2" aria-label="QuickBite Home">
            <svg className="w-8 h-8" fill="currentColor" viewBox="0 0 20 20">
              <path d="M3 1a1 1 0 000 2h1.22l.305 1.222a.997.997 0 00.01.042l1.358 5.43-.893.892C3.74 11.846 4.632 14 6.414 14H15a1 1 0 000-2H6.414l1-1H14a1 1 0 00.894-.553l3-6A1 1 0 0017 3H6.28l-.31-1.243A1 1 0 005 1H3zM16 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM6.5 18a1.5 1.5 0 100-3 1.5 1.5 0 000 3z" />
            </svg>
            QuickBite
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center space-x-6" aria-label="Main navigation">
            {isAuthenticated ? (
              <>
                {/* Customer Navigation */}
                {isCustomer() && (
                  <>
                    <Link
                      to="/vendors"
                      className="text-gray-700 hover:text-primary-600 transition-colors font-medium"
                    >
                      Restaurants
                    </Link>
                    <Link
                      to="/favorites"
                      className="text-gray-700 hover:text-primary-600 transition-colors font-medium"
                    >
                      Favorites
                    </Link>
                    <Link
                      to="/orders"
                      className="text-gray-700 hover:text-primary-600 transition-colors font-medium"
                    >
                      My Orders
                    </Link>
                    <NotificationBell />
                    <CartWidget />
                  </>
                )}

                {/* Vendor Navigation */}
                {isVendor() && (
                  <Link
                    to="/vendor/dashboard"
                    className="text-gray-700 hover:text-primary-600 transition-colors font-medium"
                  >
                    Dashboard
                  </Link>
                )}

                {/* Driver Navigation */}
                {isDriver() && (
                  <Link
                    to="/driver/dashboard"
                    className="text-gray-700 hover:text-primary-600 transition-colors font-medium"
                  >
                    Dashboard
                  </Link>
                )}

                {/* Admin Navigation */}
                {isAdmin() && (
                  <>
                    <Link
                      to="/admin/health"
                      className="text-gray-700 hover:text-primary-600 transition-colors font-medium"
                    >
                      Health
                    </Link>
                    <Link
                      to="/admin/management"
                      className="text-gray-700 hover:text-primary-600 transition-colors font-medium"
                    >
                      Management
                    </Link>
                    <Link
                      to="/admin/reports"
                      className="text-gray-700 hover:text-primary-600 transition-colors font-medium"
                    >
                      Reports
                    </Link>
                    <Link
                      to="/admin/refunds"
                      className="text-gray-700 hover:text-primary-600 transition-colors font-medium"
                    >
                      Refunds
                    </Link>
                    <Link
                      to="/admin/promos"
                      className="text-gray-700 hover:text-primary-600 transition-colors font-medium"
                    >
                      Promos
                    </Link>
                    <Link
                      to="/admin/commissions"
                      className="text-gray-700 hover:text-primary-600 transition-colors font-medium"
                    >
                      Commissions
                    </Link>
                    <Link
                      to="/admin/reviews"
                      className="text-gray-700 hover:text-primary-600 transition-colors font-medium"
                    >
                      Reviews
                    </Link>
                  </>
                )}

                {/* User Menu */}
                <div className="flex items-center gap-3 border-l pl-4">
                  <Link
                    to="/profile"
                    className="text-sm text-gray-700 hover:text-primary-600 transition-colors"
                    title="Profile"
                  >
                    Hi, <span className="font-medium">{user?.fullName?.split(' ')[0]}</span>
                  </Link>
                  <Link
                    to="/settings"
                    className="text-gray-500 hover:text-primary-600 transition-colors"
                    aria-label="Settings"
                    title="Settings"
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  </Link>
                  <button
                    onClick={handleLogout}
                    className="bg-primary-600 text-white px-4 py-2 rounded-lg hover:bg-primary-700 transition-colors font-medium"
                  >
                    Logout
                  </button>
                </div>
              </>
            ) : (
              <div className="flex items-center gap-3">
                <Link
                  to="/login"
                  className="text-gray-700 hover:text-primary-600 transition-colors font-medium"
                >
                  Login
                </Link>
                <Link
                  to="/register"
                  className="bg-primary-600 text-white px-4 py-2 rounded-lg hover:bg-primary-700 transition-colors font-medium"
                >
                  Sign Up
                </Link>
              </div>
            )}
          </nav>

          {/* Mobile Menu Button */}
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden p-2 rounded-lg hover:bg-gray-100 transition-colors"
            aria-label="Toggle menu"
            aria-expanded={mobileMenuOpen}
            aria-controls="mobile-menu"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {mobileMenuOpen ? (
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M6 18L18 6M6 6l12 12"
                />
              ) : (
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 6h16M4 12h16M4 18h16"
                />
              )}
            </svg>
          </button>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <nav id="mobile-menu" className="md:hidden border-t py-4 space-y-2" aria-label="Mobile navigation">
            {!isAuthenticated && (
              <Link
                to="/vendors"
                className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                onClick={() => setMobileMenuOpen(false)}
              >
                Restaurants
              </Link>
            )}

            {isAuthenticated ? (
              <>
                {isCustomer() && (
                  <>
                    <Link
                      to="/vendors"
                      className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      Restaurants
                    </Link>
                    <Link
                      to="/orders"
                      className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      My Orders
                    </Link>
                    <Link
                      to="/cart"
                      className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      Cart
                    </Link>
                  </>
                )}

                {isVendor() && (
                  <Link
                    to="/vendor/dashboard"
                    className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    Dashboard
                  </Link>
                )}

                {isDriver() && (
                  <Link
                    to="/driver/dashboard"
                    className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    Dashboard
                  </Link>
                )}

                {isAdmin() && (
                  <>
                    <Link
                      to="/admin/health"
                      className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      Health
                    </Link>
                    <Link
                      to="/admin/management"
                      className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      Management
                    </Link>
                    <Link
                      to="/admin/reports"
                      className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      Reports
                    </Link>
                    <Link
                      to="/admin/refunds"
                      className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      Refunds
                    </Link>
                    <Link
                      to="/admin/promos"
                      className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      Promos
                    </Link>
                    <Link
                      to="/admin/commissions"
                      className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      Commissions
                    </Link>
                    <Link
                      to="/admin/reviews"
                      className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      Reviews
                    </Link>
                  </>
                )}

                <div className="px-4 py-2 text-sm text-gray-700 border-t">
                  <p className="font-medium">{user?.fullName}</p>
                  <p className="text-xs text-gray-500">{user?.email}</p>
                </div>

                <Link
                  to="/profile"
                  className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Profile
                </Link>
                <Link
                  to="/settings"
                  className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Settings
                </Link>

                <button
                  onClick={handleLogout}
                  className="block w-full text-left px-4 py-2 text-primary-600 hover:bg-gray-100 rounded-lg font-medium"
                >
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  className="block px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Login
                </Link>
                <Link
                  to="/register"
                  className="block px-4 py-2 bg-primary-600 text-white hover:bg-primary-700 rounded-lg text-center"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Sign Up
                </Link>
              </>
            )}
          </nav>
        )}
      </div>
    </header>
  );
}
