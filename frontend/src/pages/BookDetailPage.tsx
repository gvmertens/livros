import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { getBook } from '../api/books';
import { listReadings, createReading } from '../api/readings';
import { useAuth } from '../auth/AuthContext';
import Navbar from '../components/Navbar';
import { theme } from '../theme';
import type { ErrorResponse } from '../types';
import type { AxiosError } from 'axios';

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
  const { t } = useTranslation();
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
      queryClient.invalidateQueries({ queryKey: ['readings'] });
      setAddError('');
    },
    onError: (err) => {
      const axiosErr = err as AxiosError<ErrorResponse>;
      setAddError(
        axiosErr.response?.data?.message ?? t('books.addFailed'),
      );
    },
  });

  const backLink = (
    <Link
      to="/books"
      style={{ fontSize: theme.fontSizes.sm, color: theme.colors.neutral400, transition: 'color 0.15s' }}
      onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
      onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.neutral400; }}
    >
      {t('books.backToCatalog')}
    </Link>
  );

  if (bookLoading || readingsLoading) {
    return (
      <div style={{ minHeight: '100vh', background: theme.colors.black }}>
        <Navbar />
        <main style={{ maxWidth: 720, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
          <p aria-live="polite" style={{ color: theme.colors.neutral400 }}>{t('common.loading')}</p>
        </main>
      </div>
    );
  }

  if (bookError || !book) {
    return (
      <div style={{ minHeight: '100vh', background: theme.colors.black }}>
        <Navbar />
        <main style={{ maxWidth: 720, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
          <p role="alert" style={{ color: theme.colors.danger, marginBottom: theme.spacing.base }}>
            {t('books.notFound')}
          </p>
          {backLink}
        </main>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', background: theme.colors.black }}>
      <Navbar />

      <main style={{ maxWidth: 720, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
        {/* Back link */}
        {backLink}

        {/* Book info */}
        <h1
          style={{
            color: theme.colors.white,
            fontSize: 28,
            fontWeight: 700,
            marginTop: theme.spacing.base,
            marginBottom: 4,
          }}
        >
          {book.title}
        </h1>
        <p style={{ margin: '0 0 4px', fontSize: theme.fontSizes.lg, color: theme.colors.neutral400 }}>
          {t('books.by')} {book.author.name}
        </p>
        <p style={{ margin: '0 0 4px', fontSize: theme.fontSizes.md, color: theme.colors.neutral400 }}>
          {t('books.publisher')}: {book.publisher.name}
        </p>
        <p style={{ margin: `0 0 ${theme.spacing.xl}px`, fontSize: theme.fontSizes.sm, fontFamily: 'monospace', color: theme.colors.neutral400 }}>
          {t('books.isbn')}: {book.isbn}
        </p>

        {/* Admin edit link */}
        {user?.role === 'ADMIN' && (
          <p style={{ marginBottom: theme.spacing.xl }}>
            <Link
              to={`/admin/books/${book.id}/edit`}
              style={{ fontSize: theme.fontSizes.sm, color: theme.colors.primary }}
              onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primaryLight; }}
              onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
            >
              {t('books.editBook')}
            </Link>
          </p>
        )}

        {/* Divider */}
        <div style={{ borderTop: `1px solid ${theme.colors.neutral800}`, marginBottom: theme.spacing.xl }} />

        {/* Reading section */}
        <section aria-label={t('books.yourReadingRecord')}>
          {existingReading ? (
            <div
              style={{
                background: '#1E1E1E',
                border: `1px solid ${theme.colors.neutral800}`,
                borderRadius: theme.radius.md,
                padding: theme.spacing.lg,
              }}
            >
              <h2 style={{ color: theme.colors.white, fontSize: theme.fontSizes.lg, marginBottom: theme.spacing.md }}>
                {t('books.yourReadingRecord')}
              </h2>
              <p style={{ color: theme.colors.neutral400, marginBottom: 6 }}>
                <strong style={{ color: theme.colors.white }}>{t('books.status')}:</strong>{' '}
                {t(`readings.statusLabels.${existingReading.status}`, { defaultValue: existingReading.status })}
              </p>
              {existingReading.rating !== null && (
                <p style={{ color: theme.colors.neutral400, marginBottom: 6 }}>
                  <strong style={{ color: theme.colors.white }}>{t('books.rating')}:</strong>{' '}
                  <span style={{ color: theme.colors.primary, fontWeight: 700 }}>{existingReading.rating}</span>
                  <span style={{ color: theme.colors.neutral600 }}> {t('common.rating.outOf')}</span>
                </p>
              )}
              {existingReading.review && (
                <p style={{ color: theme.colors.neutral400, marginBottom: 6 }}>
                  <strong style={{ color: theme.colors.white }}>{t('books.review')}:</strong> {existingReading.review}
                </p>
              )}
              {existingReading.startedAt && (
                <p style={{ color: theme.colors.neutral400, marginBottom: 6 }}>
                  <strong style={{ color: theme.colors.white }}>{t('books.started')}:</strong>{' '}
                  {new Date(existingReading.startedAt).toLocaleDateString()}
                </p>
              )}
              {existingReading.finishedAt && (
                <p style={{ color: theme.colors.neutral400, marginBottom: 6 }}>
                  <strong style={{ color: theme.colors.white }}>{t('books.finished')}:</strong>{' '}
                  {new Date(existingReading.finishedAt).toLocaleDateString()}
                </p>
              )}
              <Link
                to={`/readings/${existingReading.id}`}
                style={{
                  display: 'inline-block',
                  marginTop: theme.spacing.md,
                  color: theme.colors.primary,
                  fontSize: theme.fontSizes.sm,
                }}
                onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primaryLight; }}
                onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
              >
                {t('books.editReadingRecord')}
              </Link>
            </div>
          ) : (
            <div>
              <p style={{ color: theme.colors.neutral400, marginBottom: theme.spacing.base }}>
                {t('books.notInLibrary')}
              </p>
              {addError && (
                <p role="alert" style={{ color: theme.colors.danger, marginBottom: theme.spacing.md }}>
                  {addError}
                </p>
              )}
              <button
                onClick={() => addMutation.mutate()}
                disabled={addMutation.isPending}
                style={{
                  background: theme.colors.primary,
                  color: theme.colors.white,
                  border: 'none',
                  borderRadius: theme.radius.md,
                  padding: '10px 20px',
                  fontSize: theme.fontSizes.md,
                  fontWeight: 600,
                  cursor: addMutation.isPending ? 'not-allowed' : 'pointer',
                  opacity: addMutation.isPending ? 0.7 : 1,
                  transition: 'background 0.15s',
                }}
                onMouseEnter={(e) => {
                  if (!addMutation.isPending) (e.currentTarget as HTMLElement).style.background = theme.colors.primaryDark;
                }}
                onMouseLeave={(e) => {
                  (e.currentTarget as HTMLElement).style.background = theme.colors.primary;
                }}
              >
                {addMutation.isPending ? t('books.adding') : t('books.addToLibrary')}
              </button>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
