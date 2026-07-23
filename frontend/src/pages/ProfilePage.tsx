import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/AuthContext';
import Navbar from '../components/Navbar';
import { theme } from '../theme';

/**
 * Profile page — displays the current user's account information.
 * Requirements: identity module
 */
export default function ProfilePage() {
  const { t } = useTranslation();
  const { user } = useAuth();

  return (
    <div style={{ minHeight: '100vh', background: theme.colors.black }}>
      <Navbar />

      <main style={{ maxWidth: 620, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
        <h1
          style={{
            color: theme.colors.white,
            fontSize: 28,
            fontWeight: 700,
            marginBottom: theme.spacing.xl,
          }}
        >
          {t('profile.title')}
        </h1>

        {user ? (
          <div
            style={{
              background: theme.colors.neutral900,
              border: `1px solid ${theme.colors.neutral800}`,
              borderRadius: theme.radius.md,
              padding: theme.spacing.lg,
            }}
          >
            <div style={{ marginBottom: theme.spacing.md }}>
              <span style={{ color: theme.colors.neutral400, fontSize: theme.fontSizes.sm }}>{t('profile.emailLabel')}</span>
              <p style={{ color: theme.colors.white, fontSize: theme.fontSizes.md, marginTop: 4 }}>
                {user.email}
              </p>
            </div>
            <div style={{ marginBottom: theme.spacing.md }}>
              <span style={{ color: theme.colors.neutral400, fontSize: theme.fontSizes.sm }}>{t('profile.roleLabel')}</span>
              <p style={{ color: theme.colors.white, fontSize: theme.fontSizes.md, marginTop: 4 }}>
                {user.role}
              </p>
            </div>
            <div>
              <span style={{ color: theme.colors.neutral400, fontSize: theme.fontSizes.sm }}>{t('profile.userIdLabel')}</span>
              <p
                style={{
                  color: theme.colors.neutral600,
                  fontSize: theme.fontSizes.xs,
                  fontFamily: 'monospace',
                  marginTop: 4,
                }}
              >
                {user.id}
              </p>
            </div>
          </div>
        ) : (
          <p style={{ color: theme.colors.neutral400 }}>{t('profile.notLoggedIn')}</p>
        )}
      </main>
    </div>
  );
}
