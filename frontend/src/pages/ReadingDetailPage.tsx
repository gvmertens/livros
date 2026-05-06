import { useState, useEffect, type FormEvent } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getReading, updateReading, deleteReading } from '../api/readings';
import Navbar from '../components/Navbar';
import ReadingStatusBadge from '../components/ReadingStatusBadge';
import StarRating from '../components/StarRating';
import type { ReadingStatus, ErrorResponse } from '../types';
import type { AxiosError } from 'axios';

const STATUS_OPTIONS: ReadingStatus[] = ['WANT_TO_READ', 'READING', 'FINISHED', 'ABANDONED'];

const STATUS_LABELS: Record<ReadingStatus, string> = {
  WANT_TO_READ: 'Want to Read',
  READING: 'Reading',
  FINISHED: 'Finished',
  ABANDONED: 'Abandoned',
};

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
      setServerError(axiosErr.response?.data?.message ?? 'Failed to save changes.');
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
      setServerError(axiosErr.response?.data?.message ?? 'Failed to delete reading.');
    },
  });

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (rating !== null && (rating < 0 || rating > 10)) {
      errs.rating = 'Rating must be between 0.0 and 10.0.';
    }
    if (status === 'FINISHED' && !startedAt) {
      errs.startedAt = 'Start date is required when status is Finished.';
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
    if (!window.confirm('Remove this book from your library? This cannot be undone.')) return;
    deleteMutation.mutate();
  }

  if (isLoading) {
    return (
      <div>
        <Navbar />
        <main style={{ maxWidth: 600, margin: '0 auto', padding: '24px 16px' }}>
          <p aria-live="polite">Loading…</p>
        </main>
      </div>
    );
  }

  if (isError || !reading) {
    return (
      <div>
        <Navbar />
        <main style={{ maxWidth: 600, margin: '0 auto', padding: '24px 16px' }}>
          <p role="alert" style={{ color: 'red' }}>
            Reading record not found.
          </p>
          <Link to="/readings">← Back to My Readings</Link>
        </main>
      </div>
    );
  }

  return (
    <div>
      <Navbar />

      <main style={{ maxWidth: 600, margin: '0 auto', padding: '24px 16px' }}>
        {/* Back link */}
        <Link to="/readings" style={{ fontSize: 14, opacity: 0.7 }}>
          ← Back to My Readings
        </Link>

        {/* Book title + current status */}
        <div style={{ marginTop: 16, marginBottom: 24 }}>
          <h1 style={{ marginBottom: 8 }}>
            {reading.book.title ?? 'Unknown Book'}
          </h1>
          <ReadingStatusBadge status={reading.status} />
        </div>

        {/* Feedback messages */}
        {serverError && (
          <p role="alert" style={{ color: 'red', marginBottom: 16 }}>
            {serverError}
          </p>
        )}
        {saveSuccess && (
          <p role="status" style={{ color: 'green', marginBottom: 16 }}>
            Changes saved.
          </p>
        )}

        {/* Edit form */}
        <form onSubmit={handleSubmit} noValidate>

          {/* Status */}
          <div style={{ marginBottom: 20 }}>
            <label htmlFor="reading-status" style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>
              Status
            </label>
            <select
              id="reading-status"
              value={status}
              onChange={(e) => setStatus(e.target.value as ReadingStatus)}
              style={{ width: '100%', padding: '8px 12px', fontSize: 15 }}
            >
              {STATUS_OPTIONS.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </div>

          {/* Rating */}
          <div style={{ marginBottom: 20 }}>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>
              Rating
            </label>
            <StarRating value={rating} onChange={setRating} />
            {errors.rating && (
              <span style={{ color: 'red', fontSize: 13, display: 'block', marginTop: 4 }}>
                {errors.rating}
              </span>
            )}
          </div>

          {/* Review */}
          <div style={{ marginBottom: 20 }}>
            <label htmlFor="reading-review" style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>
              Review
            </label>
            <textarea
              id="reading-review"
              value={review}
              onChange={(e) => setReview(e.target.value)}
              rows={5}
              placeholder="Write your thoughts about this book…"
              style={{ width: '100%', padding: '8px 12px', fontSize: 15, resize: 'vertical' }}
            />
          </div>

          {/* Started At */}
          <div style={{ marginBottom: 20 }}>
            <label htmlFor="reading-started" style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>
              Started
            </label>
            <input
              id="reading-started"
              type="date"
              value={startedAt}
              onChange={(e) => setStartedAt(e.target.value)}
              style={{ padding: '8px 12px', fontSize: 15 }}
              aria-describedby={errors.startedAt ? 'started-error' : undefined}
              aria-invalid={!!errors.startedAt}
            />
            {errors.startedAt && (
              <span id="started-error" style={{ color: 'red', fontSize: 13, display: 'block', marginTop: 4 }}>
                {errors.startedAt}
              </span>
            )}
          </div>

          {/* Finished At */}
          <div style={{ marginBottom: 28 }}>
            <label htmlFor="reading-finished" style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>
              Finished
            </label>
            <input
              id="reading-finished"
              type="date"
              value={finishedAt}
              onChange={(e) => setFinishedAt(e.target.value)}
              style={{ padding: '8px 12px', fontSize: 15 }}
            />
          </div>

          {/* Actions */}
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <button
              type="submit"
              disabled={updateMutation.isPending}
              style={{ padding: '10px 20px' }}
            >
              {updateMutation.isPending ? 'Saving…' : 'Save Changes'}
            </button>

            <button
              type="button"
              onClick={handleDelete}
              disabled={deleteMutation.isPending}
              style={{
                padding: '10px 20px',
                color: '#c00',
                background: 'none',
                border: '1px solid #c00',
                cursor: 'pointer',
                borderRadius: 8,
              }}
            >
              {deleteMutation.isPending ? 'Removing…' : 'Remove from Library'}
            </button>
          </div>
        </form>
      </main>
    </div>
  );
}
