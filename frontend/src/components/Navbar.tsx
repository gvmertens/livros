import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/AuthContext';
import Logo from './Logo';
import LanguageSwitcher from './LanguageSwitcher';
import { theme } from '../theme';

/**
 * Top navigation bar.
 *
 * Shows:
 * - GMLib logo linking to /books
 * - Catalog link (all authenticated users)
 * - My Readings link (all authenticated users)
 * - Admin links (ADMIN role only)
 * - LanguageSwitcher (PT | EN) before user info
 * - User email and logout button
 *
 * Requirements: 11.1, 13.6, 13.7
 */
export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  const linkStyle: React.CSSProperties = {
    color: theme.colors.neutral400,
    fontSize: theme.fontSizes.md,
    textDecoration: 'none',
    transition: 'color 0.15s',
  };

  return (
    <header
      style={{
        width: '100%',
        background: theme.colors.neutral900,
        borderBottom: `1px solid ${theme.colors.neutral800}`,
        height: 60,
        display: 'flex',
        alignItems: 'center',
        padding: `0 ${theme.spacing.xl}px`,
        gap: theme.spacing.xl,
      }}
    >
      {/* Brand */}
      <Link to="/books" aria-label={t('nav.home')}>
        <Logo variant="horizontal" size={32} />
      </Link>

      {/* Navigation links */}
      <nav
        style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.xl, flex: 1 }}
        aria-label="Main navigation"
      >
        <Link
          to="/books"
          style={linkStyle}
          onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
          onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.neutral400; }}
        >
          {t('nav.books')}
        </Link>

        {user && (
          <Link
            to="/readings"
            style={linkStyle}
            onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
            onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.neutral400; }}
          >
            {t('nav.myReadings')}
          </Link>
        )}

        {user && (
          <Link
            to="/recommendations"
            style={linkStyle}
            onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
            onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.neutral400; }}
          >
            {t('nav.recommendations')}
          </Link>
        )}

        {user?.role === 'ADMIN' && (
          <>
            <Link
              to="/admin/authors"
              style={linkStyle}
              onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
              onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.neutral400; }}
            >
              {t('nav.authors')}
            </Link>
            <Link
              to="/admin/publishers"
              style={linkStyle}
              onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
              onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.neutral400; }}
            >
              {t('nav.publishers')}
            </Link>
            <Link
              to="/admin/books/new"
              style={{ ...linkStyle, color: theme.colors.primary }}
              onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primaryLight; }}
              onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
            >
              {t('nav.addBook')}
            </Link>
          </>
        )}

        {/* Spacer */}
        <span style={{ flex: 1 }} />

        {/* Language switcher — right side, before user info */}
        <LanguageSwitcher />

        {/* User info + logout */}
        {user && (
          <span style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.md }}>
            <span
              style={{ fontSize: theme.fontSizes.sm, color: theme.colors.neutral400 }}
              aria-label={t('nav.loggedInAs')}
            >
              {user.email}
            </span>
            <button
              onClick={handleLogout}
              style={{
                padding: `4px ${theme.spacing.md}px`,
                fontSize: theme.fontSizes.sm,
                background: 'transparent',
                border: `1px solid ${theme.colors.primary}`,
                color: theme.colors.primary,
                borderRadius: theme.radius.sm,
              }}
              onMouseEnter={(e) => {
                (e.currentTarget as HTMLElement).style.background = theme.colors.primary;
                (e.currentTarget as HTMLElement).style.color = theme.colors.white;
              }}
              onMouseLeave={(e) => {
                (e.currentTarget as HTMLElement).style.background = 'transparent';
                (e.currentTarget as HTMLElement).style.color = theme.colors.primary;
              }}
              aria-label={t('nav.logOut')}
            >
              {t('nav.logOut')}
            </button>
          </span>
        )}
      </nav>
    </header>
  );
}
