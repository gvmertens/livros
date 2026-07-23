import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/AuthContext';
import { login } from '../api/auth';
import Logo from '../components/Logo';
import { theme } from '../theme';
import type { ErrorResponse } from '../types';
import type { AxiosError } from 'axios';

const inputStyle: React.CSSProperties = {
  width: '100%',
  background: theme.colors.black,
  border: `1px solid ${theme.colors.neutral800}`,
  color: theme.colors.white,
  borderRadius: theme.radius.sm,
  padding: '10px 12px',
  fontSize: theme.fontSizes.md,
  outline: 'none',
  transition: 'border-color 0.15s',
};

/**
 * Login page — email + password form.
 *
 * Client-side validation: non-empty fields, email format.
 * On success: stores JWT via AuthContext.login() and redirects to /books.
 * Requirements: 2.1
 */
export default function LoginPage() {
  const { t } = useTranslation();
  const { login: authLogin } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (!email.trim()) errs.email = t('common.validation.emailRequired');
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errs.email = t('common.validation.emailInvalid');
    if (!password) errs.password = t('common.validation.passwordRequired');
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setServerError('');
    if (!validate()) return;

    setLoading(true);
    try {
      const response = await login({ email, password });
      authLogin(response.token);
      navigate('/books', { replace: true });
    } catch (err) {
      const axiosErr = err as AxiosError<ErrorResponse>;
      setServerError(axiosErr.response?.data?.message ?? t('auth.loginFailed'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        background: theme.colors.black,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: `0 ${theme.spacing.base}px`,
      }}
    >
      <div
        style={{
          background: theme.colors.neutral900,
          border: `1px solid ${theme.colors.neutral800}`,
          borderRadius: theme.radius.lg,
          padding: theme.spacing['3xl'],
          width: '100%',
          maxWidth: 400,
        }}
      >
        {/* Logo */}
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: theme.spacing.xl }}>
          <Logo variant="horizontal" size={40} />
        </div>

        {/* Title */}
        <h1
          style={{
            color: theme.colors.white,
            fontSize: theme.fontSizes.xl,
            fontWeight: 700,
            textAlign: 'center',
            marginBottom: theme.spacing.xl,
          }}
        >
          {t('auth.signIn')}
        </h1>

        {serverError && (
          <p
            role="alert"
            style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm, marginBottom: theme.spacing.base }}
          >
            {serverError}
          </p>
        )}

        <form onSubmit={handleSubmit} noValidate>
          {/* Email */}
          <div style={{ marginBottom: theme.spacing.base }}>
            <label
              htmlFor="email"
              style={{ display: 'block', color: theme.colors.neutral400, fontSize: theme.fontSizes.sm, marginBottom: 6 }}
            >
              {t('auth.email')}
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
              style={inputStyle}
              onFocus={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.primary; }}
              onBlur={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800; }}
              aria-describedby={errors.email ? 'email-error' : undefined}
              aria-invalid={!!errors.email}
            />
            {errors.email && (
              <span id="email-error" style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm }}>
                {errors.email}
              </span>
            )}
          </div>

          {/* Password */}
          <div style={{ marginBottom: theme.spacing.xl }}>
            <label
              htmlFor="password"
              style={{ display: 'block', color: theme.colors.neutral400, fontSize: theme.fontSizes.sm, marginBottom: 6 }}
            >
              {t('auth.password')}
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              style={inputStyle}
              onFocus={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.primary; }}
              onBlur={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800; }}
              aria-describedby={errors.password ? 'password-error' : undefined}
              aria-invalid={!!errors.password}
            />
            {errors.password && (
              <span id="password-error" style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm }}>
                {errors.password}
              </span>
            )}
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              background: theme.colors.primary,
              color: theme.colors.white,
              border: 'none',
              borderRadius: theme.radius.md,
              padding: '12px',
              fontSize: theme.fontSizes.md,
              fontWeight: 600,
              cursor: loading ? 'not-allowed' : 'pointer',
              opacity: loading ? 0.7 : 1,
              transition: 'background 0.15s',
            }}
            onMouseEnter={(e) => {
              if (!loading) (e.currentTarget as HTMLElement).style.background = theme.colors.primaryDark;
            }}
            onMouseLeave={(e) => {
              (e.currentTarget as HTMLElement).style.background = theme.colors.primary;
            }}
          >
            {loading ? t('auth.signingIn') : t('auth.signIn')}
          </button>
        </form>

        <p style={{ marginTop: theme.spacing.base, textAlign: 'center', fontSize: theme.fontSizes.sm, color: theme.colors.neutral400 }}>
          {t('auth.noAccount')}{' '}
          <Link
            to="/register"
            style={{ color: theme.colors.primary }}
            onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primaryLight; }}
            onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
          >
            {t('auth.register')}
          </Link>
        </p>
      </div>
    </div>
  );
}
