import { useState, useEffect, type FormEvent } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { getReading, updateReading, deleteReading } from '../api/readings';
import Navbar from '../components/Navbar';
import ReadingStatusBadge from '../components/ReadingStatusBadge';
import StarRating from '../components/StarRating';
import { theme } from '../theme';
import type { ReadingStatus, ErrorResponse } from '../types';
import type { AxiosError } from 'axios';

const STATUS_OPTIONS: ReadingStatus[] = ['WANT_TO_READ', 'READING', 'FINISHED', 'ABANDONED'];

/** Format an ISO-8601 string to a local date input value (YYYY-MM-DD). */
function toDateInputValue(iso: string | null): string {
  if (!iso) return '';
  return iso.slice(0, 10);
}

/** Convert a date input value (YYYY-MM-DD) to an ISO-8601 string at midnight UTC. */
function fromDateInputValue(val: string): string | null {
  if (!val) return null;
  return `${val}T00:00:00Z`;
}

const darkInputStyle: React.CSSProperties = {
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

const labelStyle: React.CSSProperties = {
  display: 'block',
  marginBottom: 6,
  color: theme.colors.neutral400,
  fontSize: theme.fontSizes.sm,
  fontWeight: 500,
};

/**
 * Reading detail page — view and edit a reading record.
 *
 * - Fetches GET /readings/:id
 * - Form for updating status, rating (StarRating slider), review (textarea),
 *   startedAt and finishedAt (date inputs)
 * - Validates: rating 0–10, FINISHED requires startedAt
 * - On submit: PUT /readings/:id
 * - Delete button with confirmation
 *
 * Requirements: 8.3, 8.9, 8.10
 */
export default function ReadingDetailPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Form state
  const [status, setStatus] = useState<ReadingStatus>('WANT_TO_READ');
  const [rating, setRating] = useState<number | null>(null);
  const [review, setReview] = useState('');
  const [startedAt, setStartedAt] = useState('');
  const [finishedAt, setFinishedAt] = useState('');

  // Validation errors
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState('');
  const [saveSuccess, setSaveSuccess] = useState(false);

  const { data: reading, isLoading, isError } = useQuery({
    queryKey: ['reading', id],
    queryFn: () => getReading(id!),
    enabled: !!id,
  });

  // Populate form when reading loads
  useEffect(() => {
    if (reading) {
      setStatus(reading.status);
      setRating(reading.rating);
      setReview(reading.review ?? '');
      setStartedAt(toDateInputValue(reading.startedAt));
      setFinishedAt(toDateInputValue(reading.finishedAt));
    }
  }, [reading]);

  const updateMutation = useMutation({
    mutationFn: () =>
      updateReading(id!, {
        status,
        rating,
        review: review || null,
        startedAt: fromDateInputValue(startedAt),
        finishedAt: fromDateInputValue(finishedAt),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reading', id] });
      queryClient.invalidateQueries({ queryKey: ['readings'] });
      setSaveSuccess(true);
      setServerError('');
      setTimeout(() => setSaveSuccess(false), 3000);
    },
    onError: (err) => {
      const axiosErr = err as AxiosError<ErrorResponse>;
      setServerError(axiosErr.response?.data?.message ?? t('readings.saveFailed'));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteReading(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['readings'] });
      navigate('/readings', { replace: true });
    },
    onError: (err) => {
      const axiosErr = err as AxiosError<ErrorResponse>;
      setServerError(axiosErr.response?.data?.message ?? t('readings.deleteFailed'));
    },
  });

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (rating !== null && (rating < 0 || rating > 10)) {
      errs.rating = t('common.validation.ratingOutOfRange');
    }
    if (status === 'FINISHED' && !startedAt) {
      errs.startedAt = t('common.validation.startedAtRequired');
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setServerError('');
    setSaveSuccess(false);
    if (!validate()) return;
    updateMutation.mutate();
  }

  function handleDelete() {
    if (!window.confirm(t('readings.deleteConfirm'))) return;
    deleteMutation.mutate();
  }

  const backLink = (
    <Link
      to="/readings"
      style={{ fontSize: theme.fontSizes.sm, color: theme.colors.neutral400, transition: 'color 0.15s' }}
      onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
      onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.neutral400; }}
    >
      {t('readings.backToReadings')}
    </Link>
  );

  if (isLoading) {
    return (
      <div style={{ minHeight: '100vh', background: theme.colors.black }}>
        <Navbar />
        <main style={{ maxWidth: 620, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
          <p aria-live="polite" style={{ color: theme.colors.neutral400 }}>{t('common.loading')}</p>
        </main>
      </div>
    );
  }

  if (isError || !reading) {
    return (
      <div style={{ minHeight: '100vh', background: theme.colors.black }}>
        <Navbar />
        <main style={{ maxWidth: 620, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
          <p role="alert" style={{ color: theme.colors.danger, marginBottom: theme.spacing.base }}>
            {t('readings.notFound')}
          </p>
          {backLink}
        </main>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', background: theme.colors.black }}>
      <Navbar />

      <main style={{ maxWidth: 620, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
        {/* Back link */}
        {backLink}

        {/* Book title + current status */}
        <div style={{ marginTop: theme.spacing.base, marginBottom: theme.spacing.xl }}>
          <h1 style={{ color: theme.colors.white, fontSize: 28, fontWeight: 700, marginBottom: theme.spacing.sm }}>
            {reading.book.title ?? t('readings.unknownBook')}
          </h1>
          <ReadingStatusBadge status={reading.status} />
        </div>

        {/* Feedback messages */}
        {serverError && (
          <p role="alert" style={{ color: theme.colors.danger, marginBottom: theme.spacing.base }}>
            {serverError}
          </p>
        )}
        {saveSuccess && (
          <p role="status" style={{ color: theme.colors.success, marginBottom: theme.spacing.base }}>
            {t('readings.changesSaved')}
          </p>
        )}

        {/* Edit form */}
        <form onSubmit={handleSubmit} noValidate>

          {/* Status */}
          <div style={{ marginBottom: theme.spacing.lg }}>
            <label htmlFor="reading-status" style={labelStyle}>
              {t('readings.statusLabel')}
            </label>
            <select
              id="reading-status"
              value={status}
              onChange={(e) => setStatus(e.target.value as ReadingStatus)}
              style={darkInputStyle}
              onFocus={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.primary; }}
              onBlur={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800; }}
            >
              {STATUS_OPTIONS.map((s) => (
                <option key={s} value={s}>
                  {t(`readings.statusLabels.${s}`)}
                </option>
              ))}
            </select>
          </div>

          {/* Rating */}
          <div style={{ marginBottom: theme.spacing.lg }}>
            <label style={labelStyle}>
              {t('readings.ratingLabel')}
            </label>
            <StarRating value={rating} onChange={setRating} />
            {errors.rating && (
              <span style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm, display: 'block', marginTop: 4 }}>
                {errors.rating}
              </span>
            )}
          </div>

          {/* Review */}
          <div style={{ marginBottom: theme.spacing.lg }}>
            <label htmlFor="reading-review" style={labelStyle}>
              {t('readings.reviewLabel')}
            </label>
            <textarea
              id="reading-review"
              value={review}
              onChange={(e) => setReview(e.target.value)}
              rows={5}
              placeholder={t('readings.reviewPlaceholder')}
              style={{ ...darkInputStyle, resize: 'vertical' }}
              onFocus={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.primary; }}
              onBlur={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800; }}
            />
          </div>

          {/* Started At */}
          <div style={{ marginBottom: theme.spacing.lg }}>
            <label htmlFor="reading-started" style={labelStyle}>
              {t('readings.startedLabel')}
            </label>
            <input
              id="reading-started"
              type="date"
              value={startedAt}
              onChange={(e) => setStartedAt(e.target.value)}
              style={{ ...darkInputStyle, width: 'auto' }}
              onFocus={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.primary; }}
              onBlur={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800; }}
              aria-describedby={errors.startedAt ? 'started-error' : undefined}
              aria-invalid={!!errors.startedAt}
            />
            {errors.startedAt && (
              <span id="started-error" style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm, display: 'block', marginTop: 4 }}>
                {errors.startedAt}
              </span>
            )}
          </div>

          {/* Finished At */}
          <div style={{ marginBottom: theme.spacing.xxl }}>
            <label htmlFor="reading-finished" style={labelStyle}>
              {t('readings.finishedLabel')}
            </label>
            <input
              id="reading-finished"
              type="date"
              value={finishedAt}
              onChange={(e) => setFinishedAt(e.target.value)}
              style={{ ...darkInputStyle, width: 'auto' }}
              onFocus={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.primary; }}
              onBlur={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800; }}
            />
          </div>

          {/* Actions */}
          <div style={{ display: 'flex', gap: theme.spacing.md, alignItems: 'center' }}>
            <button
              type="submit"
              disabled={updateMutation.isPending}
              style={{
                background: theme.colors.primary,
                color: theme.colors.white,
                border: 'none',
                borderRadius: theme.radius.md,
                padding: '10px 20px',
                fontSize: theme.fontSizes.md,
                fontWeight: 600,
                cursor: updateMutation.isPending ? 'not-allowed' : 'pointer',
                opacity: updateMutation.isPending ? 0.7 : 1,
                transition: 'background 0.15s',
              }}
              onMouseEnter={(e) => {
                if (!updateMutation.isPending) (e.currentTarget as HTMLElement).style.background = theme.colors.primaryDark;
              }}
              onMouseLeave={(e) => {
                (e.currentTarget as HTMLElement).style.background = theme.colors.primary;
              }}
            >
              {updateMutation.isPending ? t('readings.saving') : t('readings.saveChanges')}
            </button>

            <button
              type="button"
              onClick={handleDelete}
              disabled={deleteMutation.isPending}
              style={{
                padding: '10px 20px',
                color: theme.colors.danger,
                background: 'transparent',
                border: `1px solid ${theme.colors.danger}`,
                borderRadius: theme.radius.md,
                fontSize: theme.fontSizes.md,
                cursor: deleteMutation.isPending ? 'not-allowed' : 'pointer',
                opacity: deleteMutation.isPending ? 0.7 : 1,
                transition: 'background 0.15s, color 0.15s',
              }}
              onMouseEnter={(e) => {
                if (!deleteMutation.isPending) {
                  (e.currentTarget as HTMLElement).style.background = theme.colors.danger;
                  (e.currentTarget as HTMLElement).style.color = theme.colors.white;
                }
              }}
              onMouseLeave={(e) => {
                (e.currentTarget as HTMLElement).style.background = 'transparent';
                (e.currentTarget as HTMLElement).style.color = theme.colors.danger;
              }}
            >
              {deleteMutation.isPending ? t('readings.removing') : t('readings.removeFromLibrary')}
            </button>
          </div>
        </form>
      </main>
    </div>
  );
}
