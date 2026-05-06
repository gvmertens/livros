import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { listReadings } from '../api/readings';
import Navbar from '../components/Navbar';
import Pagination from '../components/Pagination';
import ReadingStatusBadge from '../components/ReadingStatusBadge';
import type { ReadingStatus } from '../types';

const PAGE_SIZE = 20;

const STATUS_TABS: { label: string; value: ReadingStatus | '' }[] = [
  { label: 'All',          value: '' },
  { label: 'Want to Read', value: 'WANT_TO_READ' },
  { label: 'Reading',      value: 'READING' },
  { label: 'Finished',     value: 'FINISHED' },
  { label: 'Abandoned',    value: 'ABANDONED' },
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
    <div>
      <Navbar />

      <main style={{ maxWidth: 800, margin: '0 auto', padding: '24px 16px' }}>
        <h1 style={{ marginBottom: 20 }}>My Readings</h1>

        {/* Status filter tabs */}
        <div
          role="tablist"
          aria-label="Filter by reading status"
          style={{ display: 'flex', gap: 4, marginBottom: 24, flexWrap: 'wrap' }}
        >
          {STATUS_TABS.map((tab) => (
            <button
              key={tab.value}
              role="tab"
              aria-selected={statusFilter === tab.value}
              onClick={() => handleTabChange(tab.value)}
              style={{
                padding: '6px 14px',
                borderRadius: 20,
                border: '1px solid #ccc',
                cursor: 'pointer',
                fontWeight: statusFilter === tab.value ? 700 : 400,
                background: statusFilter === tab.value ? '#1565c0' : 'transparent',
                color: statusFilter === tab.value ? '#fff' : 'inherit',
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Loading */}
        {isLoading && <p aria-live="polite">Loading readings…</p>}

        {/* Error */}
        {isError && (
          <p role="alert" style={{ color: 'red' }}>
            Failed to load readings.
          </p>
        )}

        {/* Empty state */}
        {!isLoading && !isError && data?.content.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px 0', opacity: 0.6 }}>
            <p>
              {statusFilter
                ? `No readings with status "${STATUS_TABS.find((t) => t.value === statusFilter)?.label}".`
                : 'You haven\'t added any books to your library yet.'}
            </p>
            <Link to="/books">Browse the catalog →</Link>
          </div>
        )}

        {/* Readings list */}
        {data && data.content.length > 0 && (
          <>
            <p style={{ fontSize: 13, opacity: 0.6, marginBottom: 16 }}>
              {data.totalElements} reading{data.totalElements !== 1 ? 's' : ''}
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              {data.content.map((reading) => (
                <Link
                  key={reading.id}
                  to={`/readings/${reading.id}`}
                  style={{ textDecoration: 'none', color: 'inherit' }}
                  aria-label={`View reading record for ${reading.book.title}`}
                >
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 16,
                      padding: '14px 16px',
                      border: '1px solid #eee',
                      borderRadius: 8,
                      marginBottom: 8,
                      cursor: 'pointer',
                    }}
                    onMouseEnter={(e) => {
                      (e.currentTarget as HTMLElement).style.background = '#f9f9f9';
                    }}
                    onMouseLeave={(e) => {
                      (e.currentTarget as HTMLElement).style.background = 'transparent';
                    }}
                  >
                    {/* Book title */}
                    <span style={{ flex: 1, fontWeight: 500 }}>
                      {reading.book.title ?? 'Unknown Book'}
                    </span>

                    {/* Status badge */}
                    <ReadingStatusBadge status={reading.status} />

                    {/* Rating */}
                    <span style={{ fontSize: 13, opacity: 0.65, minWidth: 60, textAlign: 'right' }}>
                      {reading.rating !== null ? `${reading.rating} / 10` : '—'}
                    </span>
                  </div>
                </Link>
              ))}
            </div>

            <div style={{ marginTop: 24 }}>
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
