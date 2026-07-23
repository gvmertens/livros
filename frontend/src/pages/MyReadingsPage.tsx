import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { listReadings } from '../api/readings';
import Navbar from '../components/Navbar';
import Pagination from '../components/Pagination';
import ReadingStatusBadge from '../components/ReadingStatusBadge';
import { theme } from '../theme';
import type { ReadingStatus } from '../types';

const PAGE_SIZE = 20;

type StatusTab = { labelKey: string; value: ReadingStatus | '' };

const STATUS_TABS: StatusTab[] = [
  { labelKey: 'readings.statusTabs.all',        value: '' },
  { labelKey: 'readings.statusTabs.wantToRead',  value: 'WANT_TO_READ' },
  { labelKey: 'readings.statusTabs.reading',     value: 'READING' },
  { labelKey: 'readings.statusTabs.finished',    value: 'FINISHED' },
  { labelKey: 'readings.statusTabs.abandoned',   value: 'ABANDONED' },
];

/**
 * My Readings page — paginated list of the current user's reading records.
 *
 * - Status filter tabs to narrow results
 * - Each row shows book title, status badge, rating, and a link to the detail page
 *
 * Requirements: 8.6, 11.1, 11.5
 */
export default function MyReadingsPage() {
  const { t } = useTranslation();
  const [statusFilter, setStatusFilter] = useState<ReadingStatus | ''>('');
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['readings', statusFilter, page],
    queryFn: () =>
      listReadings(page, PAGE_SIZE, statusFilter || undefined),
    placeholderData: (prev) => prev,
  });

  function handleTabChange(value: ReadingStatus | '') {
    setStatusFilter(value);
    setPage(0);
  }

  return (
    <div style={{ minHeight: '100vh', background: theme.colors.black }}>
      <Navbar />

      <main style={{ maxWidth: 840, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
        <h1
          style={{
            color: theme.colors.white,
            fontSize: 28,
            fontWeight: 700,
            marginBottom: theme.spacing.xl,
          }}
        >
          {t('readings.title')}
        </h1>

        {/* Status filter tabs */}
        <div
          role="tablist"
          aria-label={t('readings.filterAriaLabel')}
          style={{ display: 'flex', gap: theme.spacing.sm, marginBottom: theme.spacing.xl, flexWrap: 'wrap' }}
        >
          {STATUS_TABS.map((tab) => {
            const isActive = statusFilter === tab.value;
            return (
              <button
                key={tab.value}
                role="tab"
                aria-selected={isActive}
                onClick={() => handleTabChange(tab.value)}
                style={{
                  padding: '6px 14px',
                  borderRadius: 20,
                  border: `1px solid ${isActive ? theme.colors.primary : theme.colors.neutral800}`,
                  cursor: 'pointer',
                  fontWeight: isActive ? 700 : 400,
                  background: isActive ? theme.colors.primary : theme.colors.neutral900,
                  color: isActive ? theme.colors.white : theme.colors.neutral400,
                  fontSize: theme.fontSizes.sm,
                  transition: 'all 0.15s',
                }}
              >
                {t(tab.labelKey)}
              </button>
            );
          })}
        </div>

        {/* Loading */}
        {isLoading && (
          <p aria-live="polite" style={{ color: theme.colors.neutral400 }}>
            {t('readings.loading')}
          </p>
        )}

        {/* Error */}
        {isError && (
          <p role="alert" style={{ color: theme.colors.danger }}>
            {t('readings.loadError')}
          </p>
        )}

        {/* Empty state */}
        {!isLoading && !isError && data?.content.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px 0', color: theme.colors.neutral600 }}>
            <p style={{ marginBottom: theme.spacing.md }}>
              {statusFilter
                ? t('readings.emptyStatus', { status: t(`readings.statusTabs.${statusFilter === 'WANT_TO_READ' ? 'wantToRead' : statusFilter.toLowerCase()}`) })
                : t('readings.emptyAll')}
            </p>
            <Link
              to="/books"
              style={{ color: theme.colors.primary }}
              onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primaryLight; }}
              onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
            >
              {t('readings.browseCatalog')}
            </Link>
          </div>
        )}

        {/* Readings list */}
        {data && data.content.length > 0 && (
          <>
            <p style={{ fontSize: theme.fontSizes.sm, color: theme.colors.neutral600, marginBottom: theme.spacing.base }}>
              {t('readings.count', { count: data.totalElements })}
            </p>

            <div style={{ overflowX: 'auto', border: `1px solid ${theme.colors.neutral800}`, borderRadius: theme.radius.md }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', background: theme.colors.neutral900 }}>
                <thead>
                  <tr style={{ borderBottom: `1px solid ${theme.colors.neutral800}` }}>
                    {['bookColumn', 'publisherColumn', 'statusColumn', 'ratingColumn'].map((key) => (
                      <th key={key} scope="col" style={{ padding: '12px 16px', textAlign: key === 'ratingColumn' ? 'right' : 'left', color: theme.colors.neutral400, fontSize: theme.fontSizes.sm }}>
                        {t(`readings.${key}`)}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((reading) => (
                    <tr key={reading.id} style={{ borderBottom: `1px solid ${theme.colors.neutral800}` }}>
                      <td style={{ padding: '14px 16px', fontWeight: 500 }}>
                        <Link to={`/readings/${reading.id}`} style={{ color: theme.colors.white, textDecoration: 'none' }}>
                          {reading.book.title ?? t('readings.unknownBook')}
                        </Link>
                      </td>
                      <td style={{ padding: '14px 16px', color: theme.colors.neutral400 }}>
                        {reading.book.publisher ?? '—'}
                      </td>
                      <td style={{ padding: '14px 16px' }}><ReadingStatusBadge status={reading.status} /></td>
                      <td style={{ padding: '14px 16px', textAlign: 'right', color: theme.colors.neutral400, whiteSpace: 'nowrap' }}>
                        {reading.rating !== null ? `${reading.rating.toFixed(1)} ${t('common.rating.outOf')}` : '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div style={{ marginTop: theme.spacing.xl }}>
              <Pagination
                page={data.page}
                totalPages={data.totalPages}
                onPageChange={setPage}
              />
            </div>
          </>
        )}
      </main>
    </div>
  );
}
