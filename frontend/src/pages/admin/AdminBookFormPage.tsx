import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getBook, createBook, updateBook } from '../../api/books';
import { listAuthors } from '../../api/authors';
import { listPublishers } from '../../api/publishers';
import Navbar from '../../components/Navbar';
import type { ErrorResponse } from '../../types';
import type { AxiosError } from 'axios';

interface BookFormState {
  isbn: string;
  title: string;
  authorId: string;
  publisherId: string;
}

const EMPTY_FORM: BookFormState = { isbn: '', title: '', authorId: '', publisherId: '' };

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
        axiosErr.response?.data?.message ?? 'Failed to save book.',
      );
    },
  });

  function validate(): boolean {
    const errs: Partial<BookFormState> = {};
    if (!form.isbn.trim()) errs.isbn = 'ISBN is required.';
    else if (form.isbn.trim().length > 13) errs.isbn = 'ISBN must be 13 characters or fewer.';
    if (!form.title.trim()) errs.title = 'Title is required.';
    if (!form.authorId) errs.authorId = 'Author is required.';
    if (!form.publisherId) errs.publisherId = 'Publisher is required.';
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
      <div>
        <Navbar />
        <main style={{ maxWidth: 600, margin: '0 auto', padding: '24px 16px' }}>
          <p aria-live="polite">Loading…</p>
        </main>
      </div>
    );
  }

  const authors = authorsPage?.content ?? [];
  const publishers = publishersPage?.content ?? [];

  return (
    <div>
      <Navbar />

      <main style={{ maxWidth: 600, margin: '0 auto', padding: '24px 16px' }}>
        <Link to="/books" style={{ fontSize: 14, opacity: 0.7 }}>
          ← Back to Books
        </Link>

        <h1 style={{ marginTop: 16, marginBottom: 24 }}>
          {isEdit ? 'Edit Book' : 'New Book'}
        </h1>

        {serverError && (
          <p role="alert" style={{ color: 'red', marginBottom: 16 }}>
            {serverError}
          </p>
        )}

        <form onSubmit={handleSubmit} noValidate>
          {/* ISBN */}
          <div style={{ marginBottom: 20 }}>
            <label htmlFor="book-isbn" style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>
              ISBN <span aria-hidden="true">*</span>
            </label>
            <input
              id="book-isbn"
              type="text"
              value={form.isbn}
              onChange={(e) => handleChange('isbn', e.target.value)}
              maxLength={13}
              style={{ width: '100%', padding: '8px 12px', fontSize: 15 }}
              aria-required="true"
              aria-describedby={errors.isbn ? 'isbn-error' : undefined}
              aria-invalid={!!errors.isbn}
              disabled={isEdit} // ISBN is immutable after creation
            />
            {isEdit && (
              <span style={{ fontSize: 12, opacity: 0.55 }}>ISBN cannot be changed after creation.</span>
            )}
            {errors.isbn && (
              <span id="isbn-error" style={{ color: 'red', fontSize: 13, display: 'block' }}>
                {errors.isbn}
              </span>
            )}
          </div>

          {/* Title */}
          <div style={{ marginBottom: 20 }}>
            <label htmlFor="book-title" style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>
              Title <span aria-hidden="true">*</span>
            </label>
            <input
              id="book-title"
              type="text"
              value={form.title}
              onChange={(e) => handleChange('title', e.target.value)}
              style={{ width: '100%', padding: '8px 12px', fontSize: 15 }}
              aria-required="true"
              aria-describedby={errors.title ? 'title-error' : undefined}
              aria-invalid={!!errors.title}
            />
            {errors.title && (
              <span id="title-error" style={{ color: 'red', fontSize: 13, display: 'block' }}>
                {errors.title}
              </span>
            )}
          </div>

          {/* Author */}
          <div style={{ marginBottom: 20 }}>
            <label htmlFor="book-author" style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>
              Author <span aria-hidden="true">*</span>
            </label>
            {authorsError ? (
              <p style={{ color: '#c00', fontSize: 14 }}>
                Failed to load authors. Check your connection and{' '}
                <button type="button" onClick={() => queryClient.invalidateQueries({ queryKey: ['authors-all'] })} style={{ fontSize: 14, padding: 0, background: 'none', border: 'none', color: '#1565c0', cursor: 'pointer', textDecoration: 'underline' }}>
                  try again
                </button>.
              </p>
            ) : authors.length === 0 ? (
              <p style={{ color: '#c00', fontSize: 14 }}>
                No authors available.{' '}
                <Link to="/admin/authors/new">Create an author first.</Link>
              </p>
            ) : (
              <select
                id="book-author"
                value={form.authorId}
                onChange={(e) => handleChange('authorId', e.target.value)}
                style={{ width: '100%', padding: '8px 12px', fontSize: 15 }}
                aria-required="true"
                aria-describedby={errors.authorId ? 'author-error' : undefined}
                aria-invalid={!!errors.authorId}
              >
                <option value="">— Select an author —</option>
                {authors.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                  </option>
                ))}
              </select>
            )}
            {errors.authorId && (
              <span id="author-error" style={{ color: 'red', fontSize: 13, display: 'block' }}>
                {errors.authorId}
              </span>
            )}
          </div>

          {/* Publisher */}
          <div style={{ marginBottom: 28 }}>
            <label htmlFor="book-publisher" style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>
              Publisher <span aria-hidden="true">*</span>
            </label>
            {publishersError ? (
              <p style={{ color: '#c00', fontSize: 14 }}>
                Failed to load publishers. Check your connection and{' '}
                <button type="button" onClick={() => queryClient.invalidateQueries({ queryKey: ['publishers-all'] })} style={{ fontSize: 14, padding: 0, background: 'none', border: 'none', color: '#1565c0', cursor: 'pointer', textDecoration: 'underline' }}>
                  try again
                </button>.
              </p>
            ) : publishers.length === 0 ? (
              <p style={{ color: '#c00', fontSize: 14 }}>
                No publishers available.{' '}
                <Link to="/admin/publishers/new">Create a publisher first.</Link>
              </p>
            ) : (
              <select
                id="book-publisher"
                value={form.publisherId}
                onChange={(e) => handleChange('publisherId', e.target.value)}
                style={{ width: '100%', padding: '8px 12px', fontSize: 15 }}
                aria-required="true"
                aria-describedby={errors.publisherId ? 'publisher-error' : undefined}
                aria-invalid={!!errors.publisherId}
              >
                <option value="">— Select a publisher —</option>
                {publishers.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            )}
            {errors.publisherId && (
              <span id="publisher-error" style={{ color: 'red', fontSize: 13, display: 'block' }}>
                {errors.publisherId}
              </span>
            )}
          </div>

          <div style={{ display: 'flex', gap: 12 }}>
            <button type="submit" disabled={mutation.isPending} style={{ padding: '10px 20px' }}>
              {mutation.isPending ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Book'}
            </button>
            <Link to="/books">
              <button type="button" style={{ padding: '10px 20px' }}>
                Cancel
              </button>
            </Link>
          </div>
        </form>
      </main>
    </div>
  );
}
