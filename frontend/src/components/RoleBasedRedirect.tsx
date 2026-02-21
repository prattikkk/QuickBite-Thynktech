/**
 * RoleBasedRedirect component
 * Redirects user to appropriate dashboard based on their role
 */

import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks';
import LoadingSpinner from './LoadingSpinner';

export const RoleBasedRedirect: React.FC = () => {
  const { isAuthenticated, isLoading, user } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Redirect based on role
  switch (user?.role) {
    case 'VENDOR':
      return <Navigate to="/vendor/dashboard" replace />;
    case 'DRIVER':
      return <Navigate to="/driver/dashboard" replace />;
    case 'ADMIN':
      return <Navigate to="/admin/health" replace />;
    case 'CUSTOMER':
    default:
      return <Navigate to="/vendors" replace />;
  }
};

export default RoleBasedRedirect;
