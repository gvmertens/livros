import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

/**
 * Top navigation bar.
 *
 * Shows:
 * - App name linking to /books
 * - Catalog link (all authenticated users)
 * - My Readings link (all authenticated users)
 * - Admin links (ADMIN role only)
 * - User email and logout button
 *
 * Requirements: 11.1
 */
export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <nav
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 24,
        padding: '12px 24px',
        borderBottom: '1px solid #e0e0e0',
        flexWrap: 'wrap',
      }}
    >
      {/* Brand */}
      <Link
        to="/books"
        style={{ fontWeight: 700, fontSize: 18, textDecoration: 'none', marginRight: 8 }}
      >
        📚 Library
      </Link>

      {/* Navigation links */}
      <Link to="/books" style={{ textDecoration: 'none' }}>
        Books
      </Link>

      {user && (
        <Link to="/readings" style={{ textDecoration: 'none' }}>
          My Readings
        </Link>
      )}

      {user && (
        <Link to="/recommendations" style={{ textDecoration: 'none' }}>
          Recommendations
        </Link>
      )}

      {user?.role === 'ADMIN' && (
        <>
          <Link to="/admin/authors" style={{ textDecoration: 'none' }}>
            Authors
          </Link>
          <Link to="/admin/publishers" style={{ textDecoration: 'none' }}>
            Publishers
          </Link>
          <Link to="/admin/books/new" style={{ textDecoration: 'none' }}>
            + Add Book
          </Link>
        </>
      )}

      {/* Spacer */}
      <span style={{ flex: 1 }} />

      {/* User info + logout */}
      {user && (
        <span style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ fontSize: 14, opacity: 0.8 }} aria-label="Logged in as">
            {user.email}
          </span>
          <button
            onClick={handleLogout}
            style={{ padding: '4px 12px', fontSize: 14 }}
            aria-label="Log out"
          >
            Log out
          </button>
        </span>
      )}
    </nav>
  );
}
