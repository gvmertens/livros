import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getBook } from '../api/books';
import { listReadings, createReading } from '../api/readings';
import { useAuth } from '../auth/AuthContext';
import Navbar from '../components/Navbar';
import type { ErrorResponse } from '../types';
import type { AxiosError } from 'axios';

const STATUS_LABELS: Record<string, string> = {
  WANT_TO_READ: 'Want to Read',
  READING: 'Reading',
  FINISHED: 'Finished',
  ABANDONED: 'Abandoned',
};

/**
 * Book detail page.
 *
 * - Fetches GET /books/:id
 * - Checks whether the current user already has a reading record for this book
 * - If no reading: shows "Add to Library" button → POST /readings
 * - If reading exists: shows status, rating, review with a link to the reading detail
 *
 * Requirements: 7.6, 8.1
 */
export default function BookDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [addError, setAddError] = useState('');

  // Fetch book details
  const {
    data: book,
    isLoading: bookLoading,
    isError: bookError,
  } = useQuery({
    queryKey: ['book', id],
    queryFn: () => getBook(id!),
    enabled: !!id,
  });

  // Fetch user's readings to check if this book is already tracked
  // We fetch a large page to find the reading; in practice the user's list is small
  const { data: readingsPage, isLoading: readingsLoading } = useQuery({
    queryKey: ['readings', 'all'],
    queryFn: () => listReadings(0, 100),
    enabled: !!user,
  });

  const existingReading = readingsPage?.content.find((r) => r.book.id === id);

  // Mutation: add book to library
  const addMutation = useMutation({
    mutationFn: () => createReading({ bookId: id! }),
    onSuccess: () => {
      // Invalidate readings cache so the new record appears
      queryClient.invalidateQueries({ queryKey: ['readings'] });
      setAddError('');
    },
    onError: (err) => {
      const axiosErr = err as AxiosError<ErrorResponse>;
      setAddError(
        axiosErr.response?.data?.message ?? 'Failed to add book to library.',
      );
    },
  });

  if (bookLoading || readingsLoading) {
    return (
      <div>
        <Navbar />
        <main style={{ maxWidth: 700, margin: '0 auto', padding: '24px 16px' }}>
          <p aria-live="polite">Loading…</p>
        </main>
      </div>
    );
  }

  if (bookError || !book) {
    return (
      <div>
        <Navbar />
        <main style={{ maxWidth: 700, margin: '0 auto', padding: '24px 16px' }}>
          <p role="alert" style={{ color: 'red' }}>
            Book not found.
          </p>
          <Link to="/books">← Back to catalog</Link>
        </main>
      </div>
    );
  }

  return (
    <div>
      <Navbar />

      <main style={{ maxWidth: 700, margin: '0 auto', padding: '24px 16px' }}>
        {/* Back link */}
        <Link to="/books" style={{ fontSize: 14, opacity: 0.7 }}>
          ← Back to catalog
        </Link>

        {/* Book info */}
        <h1 style={{ marginTop: 16, marginBottom: 4 }}>{book.title}</h1>
        <p style={{ margin: '0 0 4px', fontSize: 16, opacity: 0.8 }}>
          by {book.author.name}
        </p>
        <p style={{ margin: '0 0 4px', fontSize: 14, opacity: 0.6 }}>
          Publisher: {book.publisher.name}
        </p>
        <p style={{ margin: '0 0 24px', fontSize: 13, fontFamily: 'monospace', opacity: 0.55 }}>
          ISBN: {book.isbn}
        </p>

        {/* Admin edit link */}
        {user?.role === 'ADMIN' && (
          <p style={{ marginBottom: 24 }}>
            <Link to={`/admin/books/${book.id}/edit`} style={{ fontSize: 14 }}>
              ✏️ Edit this book
            </Link>
          </p>
        )}

        <hr style={{ marginBottom: 24, opacity: 0.2 }} />

        {/* Reading section */}
        <section aria-label="Reading record">
          {existingReading ? (
            <div>
              <h2 style={{ marginBottom: 12 }}>Your Reading Record</h2>
              <p>
                <strong>Status:</strong>{' '}
                {STATUS_LABELS[existingReading.status] ?? existingReading.status}
              </p>
              {existingReading.rating !== null && (
                <p>
                  <strong>Rating:</strong> {existingReading.rating} / 10
                </p>
              )}
              {existingReading.review && (
                <p>
                  <strong>Review:</strong> {existingReading.review}
                </p>
              )}
              {existingReading.startedAt && (
                <p>
                  <strong>Started:</strong>{' '}
                  {new Date(existingReading.startedAt).toLocaleDateString()}
                </p>
              )}
              {existingReading.finishedAt && (
                <p>
                  <strong>Finished:</strong>{' '}
                  {new Date(existingReading.finishedAt).toLocaleDateString()}
                </p>
              )}
              <Link
                to={`/readings/${existingReading.id}`}
                style={{ display: 'inline-block', marginTop: 12 }}
              >
                Edit reading record →
              </Link>
            </div>
          ) : (
            <div>
              <p style={{ opacity: 0.7, marginBottom: 16 }}>
                You haven't added this book to your library yet.
              </p>
              {addError && (
                <p role="alert" style={{ color: 'red', marginBottom: 12 }}>
                  {addError}
                </p>
              )}
              <button
                onClick={() => addMutation.mutate()}
                disabled={addMutation.isPending}
                style={{ padding: '10px 20px' }}
              >
                {addMutation.isPending ? 'Adding…' : '+ Add to Library'}
              </button>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
