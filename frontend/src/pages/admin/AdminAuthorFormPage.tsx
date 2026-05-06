import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getAuthor, createAuthor, updateAuthor } from '../../api/authors';
import Navbar from '../../components/Navbar';
import type { ErrorResponse } from '../../types';
import type { AxiosError } from 'axios';

/**
 * Admin author form page — handles both create and edit.
 *
 * - No :id param → create mode (POST /authors)
 * - With :id param → edit mode (GET /authors/:id then PUT /authors/:id)
 *
 * Requirements: 5.1, 5.2, 5.7
 */
export default function AdminAuthorFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id;
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [name, setName] = useState('');
  const [fieldError, setFieldError] = useState('');
  const [serverError, setServerError] = useState('');

  // Load existing author in edit mode
  const { data: existing, isLoading } = useQuery({
    queryKey: ['author', id],
    queryFn: () => getAuthor(id!),
    enabled: isEdit,
  });

  useEffect(() => {
    if (existing) setName(existing.name);
  }, [existing]);

  const mutation = useMutation({
    mutationFn: (payload: { name: string }) =>
      isEdit ? updateAuthor(id!, payload) : createAuthor(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['authors'] });
      navigate('/admin/authors');
    },
    onError: (err) => {
      const axiosErr = err as AxiosError<ErrorResponse>;
      setServerError(
        axiosErr.response?.data?.message ?? 'Failed to save author.',
      );
    },
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setFieldError('');
    setServerError('');
    if (!name.trim()) {
      setFieldError('Name is required.');
      return;
    }
    mutation.mutate({ name: name.trim() });
  }

  if (isEdit && isLoading) {
    return (
      <div>
        <Navbar />
        <main style={{ maxWidth: 500, margin: '0 auto', padding: '24px 16px' }}>
          <p aria-live="polite">Loading…</p>
        </main>
      </div>
    );
  }

  return (
    <div>
      <Navbar />

      <main style={{ maxWidth: 500, margin: '0 auto', padding: '24px 16px' }}>
        <Link to="/admin/authors" style={{ fontSize: 14, opacity: 0.7 }}>
          ← Back to Authors
        </Link>

        <h1 style={{ marginTop: 16, marginBottom: 24 }}>
          {isEdit ? 'Edit Author' : 'New Author'}
        </h1>

        {serverError && (
          <p role="alert" style={{ color: 'red', marginBottom: 16 }}>
            {serverError}
          </p>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div style={{ marginBottom: 20 }}>
            <label htmlFor="author-name" style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>
              Name <span aria-hidden="true">*</span>
            </label>
            <input
              id="author-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              style={{ width: '100%', padding: '8px 12px', fontSize: 15 }}
              aria-required="true"
              aria-describedby={fieldError ? 'name-error' : undefined}
              aria-invalid={!!fieldError}
            />
            {fieldError && (
              <span id="name-error" style={{ color: 'red', fontSize: 13 }}>
                {fieldError}
              </span>
            )}
          </div>

          <div style={{ display: 'flex', gap: 12 }}>
            <button type="submit" disabled={mutation.isPending} style={{ padding: '10px 20px' }}>
              {mutation.isPending ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Author'}
            </button>
            <Link to="/admin/authors">
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
