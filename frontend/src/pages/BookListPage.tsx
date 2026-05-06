import { useState, useEffect, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { listBooks } from '../api/books';
import Navbar from '../components/Navbar';
import BookCard from '../components/BookCard';
import Pagination from '../components/Pagination';

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
    <div>
      <Navbar />

      <main style={{ maxWidth: 900, margin: '0 auto', padding: '24px 16px' }}>
        <h1 style={{ marginBottom: 20 }}>Books</h1>

        {/* Search */}
        <div style={{ marginBottom: 24 }}>
          <label htmlFor="book-search" style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>
            Search by title or author
          </label>
          <input
            id="book-search"
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="e.g. Tolkien, Dune…"
            style={{ width: '100%', maxWidth: 400, padding: '8px 12px', fontSize: 15 }}
            aria-label="Search books"
          />
        </div>

        {/* Loading state */}
        {isLoading && <p aria-live="polite">Loading books…</p>}

        {/* Error state */}
        {isError && (
          <p role="alert" style={{ color: 'red' }}>
            Failed to load books:{' '}
            {error instanceof Error ? error.message : 'Unknown error'}
          </p>
        )}

        {/* Empty state */}
        {!isLoading && !isError && data?.content.length === 0 && (
          <p style={{ opacity: 0.6 }}>
            {debouncedSearch
              ? `No books found matching "${debouncedSearch}".`
              : 'No books in the catalog yet.'}
          </p>
        )}

        {/* Book grid */}
        {data && data.content.length > 0 && (
          <>
            <p style={{ fontSize: 13, opacity: 0.6, marginBottom: 16 }}>
              {data.totalElements} book{data.totalElements !== 1 ? 's' : ''} found
              {debouncedSearch ? ` for "${debouncedSearch}"` : ''}
            </p>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
                gap: 16,
                marginBottom: 32,
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
