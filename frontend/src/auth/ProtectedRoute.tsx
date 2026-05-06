import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext';

interface Props {
  /** When set, only users with this role can access the route. */
  requiredRole?: 'ADMIN';
}

/**
 * Route guard that enforces authentication and optional role-based access.
 *
 * Behaviour:
 * - Not authenticated → redirect to /login
 * - Authenticated but wrong role → redirect to /books
 * - Authenticated and role matches (or no role required) → render <Outlet />
 *
 * Requirements: 3.1, 3.4
 */
export default function ProtectedRoute({ requiredRole }: Props) {
  const { user } = useAuth();

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole && user.role !== requiredRole) {
    return <Navigate to="/books" replace />;
  }

  return <Outlet />;
}
