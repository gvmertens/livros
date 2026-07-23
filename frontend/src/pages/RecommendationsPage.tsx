import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { getRecommendations } from '../api/recommendations';
import Navbar from '../components/Navbar';
import { theme } from '../theme';

/**
 * Recommendations page — displays personalised book suggestions from the
 * recommendation service (powered by OpenAI).
 *
 * - Shows a loading state while fetching
 * - Shows an empty state with a prompt to rate books when no history exists
 * - Lists recommendations as book cards when available
 *
 * Requirements: 10.4
 */
export default function RecommendationsPage() {
  const { t } = useTranslation();
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['recommendations'],
    queryFn: getRecommendations,
    staleTime: 5 * 60 * 1000, // cache for 5 minutes
  });

  return (
    <div style={{ minHeight: '100vh', background: theme.colors.black }}>
      <Navbar />

      <main style={{ maxWidth: 720, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.base, marginBottom: theme.spacing.xl }}>
          <h1 style={{ margin: 0, color: theme.colors.white, fontSize: 28, fontWeight: 700 }}>
            {t('recommendations.title')}
          </h1>
          <button
            onClick={() => refetch()}
            disabled={isLoading}
            style={{
              padding: '6px 14px',
              fontSize: theme.fontSizes.sm,
              background: 'transparent',
              border: `1px solid ${theme.colors.primary}`,
              color: theme.colors.primary,
              borderRadius: theme.radius.sm,
              cursor: isLoading ? 'not-allowed' : 'pointer',
              opacity: isLoading ? 0.6 : 1,
              transition: 'background 0.15s, color 0.15s',
            }}
            onMouseEnter={(e) => {
              if (!isLoading) {
                (e.currentTarget as HTMLElement).style.background = theme.colors.primary;
                (e.currentTarget as HTMLElement).style.color = theme.colors.white;
              }
            }}
            onMouseLeave={(e) => {
              (e.currentTarget as HTMLElement).style.background = 'transparent';
              (e.currentTarget as HTMLElement).style.color = theme.colors.primary;
            }}
            aria-label={t('recommendations.refreshAriaLabel')}
          >
            {isLoading ? t('recommendations.loading') : t('recommendations.refresh')}
          </button>
        </div>

        {/* Loading */}
        {isLoading && (
          <p aria-live="polite" style={{ color: theme.colors.neutral400 }}>
            {t('recommendations.loading')}
          </p>
        )}

        {/* Error */}
        {isError && (
          <p role="alert" style={{ color: theme.colors.danger }}>
            {t('recommendations.loadError')}
          </p>
        )}

        {/* Empty state */}
        {!isLoading && !isError && data?.recommendations.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <p style={{ fontSize: theme.fontSizes.xl, marginBottom: theme.spacing.sm }}>📚</p>
            <p style={{ fontSize: theme.fontSizes.lg, color: theme.colors.white, marginBottom: theme.spacing.sm }}>
              {t('recommendations.emptyTitle')}
            </p>
            <p style={{ color: theme.colors.neutral600, fontSize: theme.fontSizes.sm, marginBottom: theme.spacing.lg }}>
              {t('recommendations.emptyDescription')}
            </p>
            <Link to="/readings">
              <button
                style={{
                  padding: '10px 20px',
                  background: theme.colors.primary,
                  color: theme.colors.white,
                  border: 'none',
                  borderRadius: theme.radius.md,
                  fontSize: theme.fontSizes.md,
                  fontWeight: 600,
                  cursor: 'pointer',
                  transition: 'background 0.15s',
                }}
                onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.background = theme.colors.primaryDark; }}
                onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.background = theme.colors.primary; }}
              >
                {t('recommendations.goToReadings')}
              </button>
            </Link>
          </div>
        )}

        {/* Recommendations list */}
        {data && data.recommendations.length > 0 && (
          <>
            <p style={{ color: theme.colors.neutral600, fontSize: theme.fontSizes.sm, marginBottom: theme.spacing.lg }}>
              {t('recommendations.poweredByAI')}
            </p>
            {data.criteriaSummary && (
              <section style={{ padding: theme.spacing.lg, marginBottom: theme.spacing.lg, background: theme.colors.neutral900, border: `1px solid ${theme.colors.neutral800}`, borderRadius: theme.radius.md }}>
                <h2 style={{ margin: `0 0 ${theme.spacing.sm}px`, color: theme.colors.white, fontSize: theme.fontSizes.md }}>
                  {t('recommendations.criteriaTitle')}
                </h2>
                <p style={{ margin: 0, color: theme.colors.neutral400 }}>{data.criteriaSummary}</p>
              </section>
            )}
            <ol style={{ paddingLeft: 0, listStyle: 'none', display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
              {data.recommendations.map((rec, idx) => (
                <li
                  key={idx}
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: theme.spacing.base,
                    padding: `${theme.spacing.base}px ${theme.spacing.lg}px`,
                    background: theme.colors.neutral900,
                    border: `1px solid ${theme.colors.neutral800}`,
                    borderRadius: theme.radius.md,
                  }}
                >
                  <span
                    style={{
                      minWidth: 28,
                      height: 28,
                      borderRadius: '50%',
                      background: theme.colors.primary,
                      color: theme.colors.white,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: theme.fontSizes.sm,
                      fontWeight: 700,
                      flexShrink: 0,
                    }}
                    aria-hidden="true"
                  >
                    {idx + 1}
                  </span>
                  <div>
                    <div style={{ fontSize: theme.fontSizes.md, color: theme.colors.white, fontWeight: 600 }}>{rec.title}</div>
                    <div style={{ marginTop: 4, fontSize: theme.fontSizes.sm, color: theme.colors.neutral400 }}>{rec.publisher}</div>
                  </div>
                </li>
              ))}
            </ol>
          </>
        )}
      </main>
    </div>
  );
}
