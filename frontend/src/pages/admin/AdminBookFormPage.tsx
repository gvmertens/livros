import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { getBook, createBook, updateBook } from '../../api/books';
import { listAuthors } from '../../api/authors';
import { listPublishers } from '../../api/publishers';
import Navbar from '../../components/Navbar';
import { theme } from '../../theme';
import type { ErrorResponse } from '../../types';
import type { AxiosError } from 'axios';

interface BookFormState {
  isbn: string;
  title: string;
  authorId: string;
  publisherId: string;
}

const EMPTY_FORM: BookFormState = { isbn: '', title: '', authorId: '', publisherId: '' };

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
 * Admin book form page — handles both create and edit.
 *
 * - No :id param → create mode (POST /books)
 * - With :id param → edit mode (GET /books/:id then PUT /books/:id)
 *
 * Fetches all authors and publishers to populate <select> dropdowns.
 * Validates required fields client-side before submitting.
 *
 * Requirements: 7.1–7.4
 */
export default function AdminBookFormPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id;
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [form, setForm] = useState<BookFormState>(EMPTY_FORM);
  const [errors, setErrors] = useState<Partial<BookFormState>>({});
  const [serverError, setServerError] = useState('');

  // Load existing book in edit mode
  const { data: existing, isLoading: bookLoading } = useQuery({
    queryKey: ['book', id],
    queryFn: () => getBook(id!),
    enabled: isEdit,
  });

  // Load all authors for the dropdown (fetch up to 100 — backend max page size)
  const { data: authorsPage, isLoading: authorsLoading, isError: authorsError } = useQuery({
    queryKey: ['authors-all'],
    queryFn: () => listAuthors(0, 100),
    retry: 2,
    staleTime: 0,
  });

  // Load all publishers for the dropdown
  const { data: publishersPage, isLoading: publishersLoading, isError: publishersError } = useQuery({
    queryKey: ['publishers-all'],
    queryFn: () => listPublishers(0, 100),
    retry: 2,
    staleTime: 0,
  });

  // Populate form when editing
  useEffect(() => {
    if (existing) {
      setForm({
        isbn: existing.isbn,
        title: existing.title,
        authorId: existing.author.id,
        publisherId: existing.publisher.id,
      });
    }
  }, [existing]);

  const mutation = useMutation({
    mutationFn: (payload: BookFormState) =>
      isEdit ? updateBook(id!, payload) : createBook(payload),
    onSuccess: (book) => {
      queryClient.invalidateQueries({ queryKey: ['books'] });
      queryClient.invalidateQueries({ queryKey: ['book', book.id] });
      navigate('/books');
    },
    onError: (err) => {
      const axiosErr = err as AxiosError<ErrorResponse>;
      setServerError(
        axiosErr.response?.data?.message ?? t('admin.books.saveFailed'),
      );
    },
  });

  function validate(): boolean {
    const errs: Partial<BookFormState> = {};
    if (!form.isbn.trim()) errs.isbn = t('admin.books.isbnRequired');
    else if (form.isbn.trim().length > 13) errs.isbn = t('admin.books.isbnTooLong');
    if (!form.title.trim()) errs.title = t('admin.books.titleRequired');
    if (!form.authorId) errs.authorId = t('admin.books.authorRequired');
    if (!form.publisherId) errs.publisherId = t('admin.books.publisherRequired');
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  function handleChange(field: keyof BookFormState, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) setErrors((prev) => ({ ...prev, [field]: undefined }));
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setServerError('');
    if (!validate()) return;
    mutation.mutate({
      isbn: form.isbn.trim(),
      title: form.title.trim(),
      authorId: form.authorId,
      publisherId: form.publisherId,
    });
  }

  const isPageLoading = (isEdit && bookLoading) || authorsLoading || publishersLoading;

  if (isPageLoading) {
    return (
      <div style={{ minHeight: '100vh', background: theme.colors.black }}>
        <Navbar />
        <main style={{ maxWidth: 520, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
          <p aria-live="polite" style={{ color: theme.colors.neutral400 }}>{t('common.loading')}</p>
        </main>
      </div>
    );
  }

  const authors = authorsPage?.content ?? [];
  const publishers = publishersPage?.content ?? [];

  return (
    <div style={{ minHeight: '100vh', background: theme.colors.black }}>
      <Navbar />

      <main style={{ maxWidth: 520, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
        <Link
          to="/books"
          style={{ fontSize: theme.fontSizes.sm, color: theme.colors.neutral400, transition: 'color 0.15s' }}
          onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
          onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.neutral400; }}
        >
          {t('admin.books.backToBooks')}
        </Link>

        <h1 style={{ color: theme.colors.white, fontSize: 28, fontWeight: 700, marginTop: theme.spacing.base, marginBottom: theme.spacing.xl }}>
          {isEdit ? t('admin.books.editTitle') : t('admin.books.newTitle')}
        </h1>

        {serverError && (
          <p role="alert" style={{ color: theme.colors.danger, marginBottom: theme.spacing.base }}>
            {serverError}
          </p>
        )}

        <form onSubmit={handleSubmit} noValidate>
          {/* ISBN */}
          <div style={{ marginBottom: theme.spacing.lg }}>
            <label htmlFor="book-isbn" style={labelStyle}>
              {t('admin.books.isbnLabel')} <span aria-hidden="true">*</span>
            </label>
            <input
              id="book-isbn"
              type="text"
              value={form.isbn}
              onChange={(e) => handleChange('isbn', e.target.value)}
              maxLength={13}
              style={{
                ...darkInputStyle,
                opacity: isEdit ? 0.5 : 1,
                cursor: isEdit ? 'not-allowed' : 'text',
              }}
              onFocus={(e) => {
                if (!isEdit) (e.currentTarget as HTMLElement).style.borderColor = theme.colors.primary;
              }}
              onBlur={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800; }}
              aria-required="true"
              aria-describedby={errors.isbn ? 'isbn-error' : undefined}
              aria-invalid={!!errors.isbn}
              disabled={isEdit} // ISBN is immutable after creation
            />
            {isEdit && (
              <span style={{ fontSize: theme.fontSizes.xs, color: theme.colors.neutral600 }}>
                {t('admin.books.isbnImmutable')}
              </span>
            )}
            {errors.isbn && (
              <span id="isbn-error" style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm, display: 'block' }}>
                {errors.isbn}
              </span>
            )}
          </div>

          {/* Title */}
          <div style={{ marginBottom: theme.spacing.lg }}>
            <label htmlFor="book-title" style={labelStyle}>
              {t('admin.books.titleLabel')} <span aria-hidden="true">*</span>
            </label>
            <input
              id="book-title"
              type="text"
              value={form.title}
              onChange={(e) => handleChange('title', e.target.value)}
              style={darkInputStyle}
              onFocus={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.primary; }}
              onBlur={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800; }}
              aria-required="true"
              aria-describedby={errors.title ? 'title-error' : undefined}
              aria-invalid={!!errors.title}
            />
            {errors.title && (
              <span id="title-error" style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm, display: 'block' }}>
                {errors.title}
              </span>
            )}
          </div>

          {/* Author */}
          <div style={{ marginBottom: theme.spacing.lg }}>
            <label htmlFor="book-author" style={labelStyle}>
              {t('admin.books.authorLabel')} <span aria-hidden="true">*</span>
            </label>
            {authorsError ? (
              <p style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm }}>
                {t('admin.books.authorLoadError')}{' '}
                <button
                  type="button"
                  onClick={() => queryClient.invalidateQueries({ queryKey: ['authors-all'] })}
                  style={{
                    fontSize: theme.fontSizes.sm,
                    padding: 0,
                    background: 'none',
                    border: 'none',
                    color: theme.colors.primary,
                    cursor: 'pointer',
                    textDecoration: 'underline',
                  }}
                >
                  {t('admin.books.authorLoadErrorRetry')}
                </button>.
              </p>
            ) : authors.length === 0 ? (
              <p style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm }}>
                {t('admin.books.authorEmpty')}{' '}
                <Link to="/admin/authors/new" style={{ color: theme.colors.primary }}>
                  {t('admin.books.authorCreateFirst')}
                </Link>
              </p>
            ) : (
              <select
                id="book-author"
                value={form.authorId}
                onChange={(e) => handleChange('authorId', e.target.value)}
                style={darkInputStyle}
                onFocus={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.primary; }}
                onBlur={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800; }}
                aria-required="true"
                aria-describedby={errors.authorId ? 'author-error' : undefined}
                aria-invalid={!!errors.authorId}
              >
                <option value="">{t('admin.books.authorSelectPlaceholder')}</option>
                {authors.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                  </option>
                ))}
              </select>
            )}
            {errors.authorId && (
              <span id="author-error" style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm, display: 'block' }}>
                {errors.authorId}
              </span>
            )}
          </div>

          {/* Publisher */}
          <div style={{ marginBottom: theme.spacing.xxl }}>
            <label htmlFor="book-publisher" style={labelStyle}>
              {t('admin.books.publisherLabel')} <span aria-hidden="true">*</span>
            </label>
            {publishersError ? (
              <p style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm }}>
                {t('admin.books.publisherLoadError')}{' '}
                <button
                  type="button"
                  onClick={() => queryClient.invalidateQueries({ queryKey: ['publishers-all'] })}
                  style={{
                    fontSize: theme.fontSizes.sm,
                    padding: 0,
                    background: 'none',
                    border: 'none',
                    color: theme.colors.primary,
                    cursor: 'pointer',
                    textDecoration: 'underline',
                  }}
                >
                  {t('admin.books.publisherLoadErrorRetry')}
                </button>.
              </p>
            ) : publishers.length === 0 ? (
              <p style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm }}>
                {t('admin.books.publisherEmpty')}{' '}
                <Link to="/admin/publishers/new" style={{ color: theme.colors.primary }}>
                  {t('admin.books.publisherCreateFirst')}
                </Link>
              </p>
            ) : (
              <select
                id="book-publisher"
                value={form.publisherId}
                onChange={(e) => handleChange('publisherId', e.target.value)}
                style={darkInputStyle}
                onFocus={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.primary; }}
                onBlur={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800; }}
                aria-required="true"
                aria-describedby={errors.publisherId ? 'publisher-error' : undefined}
                aria-invalid={!!errors.publisherId}
              >
                <option value="">{t('admin.books.publisherSelectPlaceholder')}</option>
                {publishers.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            )}
            {errors.publisherId && (
              <span id="publisher-error" style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm, display: 'block' }}>
                {errors.publisherId}
              </span>
            )}
          </div>

          <div style={{ display: 'flex', gap: theme.spacing.md }}>
            <button
              type="submit"
              disabled={mutation.isPending}
              style={{
                background: theme.colors.primary,
                color: theme.colors.white,
                border: 'none',
                borderRadius: theme.radius.md,
                padding: '10px 20px',
                fontSize: theme.fontSizes.md,
                fontWeight: 600,
                cursor: mutation.isPending ? 'not-allowed' : 'pointer',
                opacity: mutation.isPending ? 0.7 : 1,
                transition: 'background 0.15s',
              }}
              onMouseEnter={(e) => {
                if (!mutation.isPending) (e.currentTarget as HTMLElement).style.background = theme.colors.primaryDark;
              }}
              onMouseLeave={(e) => {
                (e.currentTarget as HTMLElement).style.background = theme.colors.primary;
              }}
            >
              {mutation.isPending
                ? t('admin.books.saving')
                : isEdit
                  ? t('admin.books.saveButton')
                  : t('admin.books.createButton')}
            </button>
            <Link to="/books">
              <button
                type="button"
                style={{
                  background: 'transparent',
                  border: `1px solid ${theme.colors.neutral800}`,
                  color: theme.colors.neutral400,
                  borderRadius: theme.radius.md,
                  padding: '10px 20px',
                  fontSize: theme.fontSizes.md,
                  cursor: 'pointer',
                  transition: 'border-color 0.15s, color 0.15s',
                }}
                onMouseEnter={(e) => {
                  (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral400;
                  (e.currentTarget as HTMLElement).style.color = theme.colors.white;
                }}
                onMouseLeave={(e) => {
                  (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800;
                  (e.currentTarget as HTMLElement).style.color = theme.colors.neutral400;
                }}
              >
                {t('common.cancel')}
              </button>
            </Link>
          </div>
        </form>
      </main>
    </div>
  );
}
