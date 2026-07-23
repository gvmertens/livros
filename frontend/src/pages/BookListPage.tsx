import { useState, useEffect, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { listBooks } from '../api/books';
import Navbar from '../components/Navbar';
import BookCard from '../components/BookCard';
import Pagination from '../components/Pagination';
import { theme } from '../theme';

const PAGE_SIZE = 20;
const DEBOUNCE_MS = 350;

/**
 * Book list page — paginated, searchable catalog.
 *
 * - Fetches GET /books?page=&size=&search= via React Query
 * - Debounces the search input by 350 ms to avoid excessive requests
 * - Renders a grid of BookCard components with Pagination
 *
 * Requirements: 7.5, 11.1, 11.4
 */
export default function BookListPage() {
  const { t } = useTranslation();
  const [searchInput, setSearchInput] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [page, setPage] = useState(0);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Debounce search input
  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(searchInput.trim());
      setPage(0); // reset to first page on new search
    }, DEBOUNCE_MS);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [searchInput]);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['books', page, debouncedSearch],
    queryFn: () => listBooks(page, PAGE_SIZE, debouncedSearch),
    placeholderData: (prev) => prev, // keep previous data while fetching
  });

  return (
    <div style={{ minHeight: '100vh', background: theme.colors.black }}>
      <Navbar />

      <main style={{ maxWidth: 960, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
        <h1
          style={{
            color: theme.colors.white,
            fontSize: 28,
            fontWeight: 700,
            marginBottom: theme.spacing.xl,
          }}
        >
          {t('books.title')}
        </h1>

        {/* Search */}
        <div style={{ marginBottom: theme.spacing.xl }}>
          <label
            htmlFor="book-search"
            style={{
              display: 'block',
              marginBottom: 6,
              color: theme.colors.neutral400,
              fontSize: theme.fontSizes.sm,
            }}
          >
            {t('books.searchLabel')}
          </label>
          <input
            id="book-search"
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder={t('books.searchPlaceholder')}
            style={{
              width: '100%',
              maxWidth: 400,
              background: theme.colors.black,
              border: `1px solid ${theme.colors.neutral800}`,
              color: theme.colors.white,
              borderRadius: theme.radius.sm,
              padding: '10px 12px',
              fontSize: theme.fontSizes.md,
              outline: 'none',
            }}
            onFocus={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.primary; }}
            onBlur={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800; }}
            aria-label={t('books.searchLabel')}
          />
        </div>

        {/* Loading state */}
        {isLoading && (
          <p aria-live="polite" style={{ color: theme.colors.neutral400 }}>
            {t('books.loading')}
          </p>
        )}

        {/* Error state */}
        {isError && (
          <p role="alert" style={{ color: theme.colors.danger }}>
            {t('books.loadError')}{' '}
            {error instanceof Error ? error.message : t('common.unknownError')}
          </p>
        )}

        {/* Empty state */}
        {!isLoading && !isError && data?.content.length === 0 && (
          <p style={{ color: theme.colors.neutral600, fontSize: theme.fontSizes.sm }}>
            {debouncedSearch
              ? t('books.emptySearch', { query: debouncedSearch })
              : t('books.emptyCatalog')}
          </p>
        )}

        {/* Book grid */}
        {data && data.content.length > 0 && (
          <>
            <p style={{ fontSize: theme.fontSizes.sm, color: theme.colors.neutral600, marginBottom: theme.spacing.base }}>
              {debouncedSearch
                ? t('books.foundCountSearch', { count: data.totalElements, query: debouncedSearch })
                : t('books.foundCount', { count: data.totalElements })}
            </p>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
                gap: theme.spacing.base,
                marginBottom: theme.spacing.xxl,
              }}
            >
              {data.content.map((book) => (
                <BookCard key={book.id} book={book} />
              ))}
            </div>

            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              onPageChange={setPage}
            />
          </>
        )}
      </main>
    </div>
  );
}
