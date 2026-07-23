import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { getPublisher, createPublisher, updatePublisher } from '../../api/publishers';
import Navbar from '../../components/Navbar';
import { theme } from '../../theme';
import type { ErrorResponse } from '../../types';
import type { AxiosError } from 'axios';

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
 * Admin publisher form page — handles both create and edit.
 *
 * - No :id param → create mode (POST /publishers)
 * - With :id param → edit mode (GET /publishers/:id then PUT /publishers/:id)
 *
 * Requirements: 6.1, 6.2, 6.7
 */
export default function AdminPublisherFormPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id;
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [name, setName] = useState('');
  const [fieldError, setFieldError] = useState('');
  const [serverError, setServerError] = useState('');

  // Load existing publisher in edit mode
  const { data: existing, isLoading } = useQuery({
    queryKey: ['publisher', id],
    queryFn: () => getPublisher(id!),
    enabled: isEdit,
  });

  useEffect(() => {
    if (existing) setName(existing.name);
  }, [existing]);

  const mutation = useMutation({
    mutationFn: (payload: { name: string }) =>
      isEdit ? updatePublisher(id!, payload) : createPublisher(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['publishers'] });
      navigate('/admin/publishers');
    },
    onError: (err) => {
      const axiosErr = err as AxiosError<ErrorResponse>;
      setServerError(
        axiosErr.response?.data?.message ?? t('admin.publishers.saveFailed'),
      );
    },
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setFieldError('');
    setServerError('');
    if (!name.trim()) {
      setFieldError(t('admin.publishers.nameRequired'));
      return;
    }
    mutation.mutate({ name: name.trim() });
  }

  if (isEdit && isLoading) {
    return (
      <div style={{ minHeight: '100vh', background: theme.colors.black }}>
        <Navbar />
        <main style={{ maxWidth: 520, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
          <p aria-live="polite" style={{ color: theme.colors.neutral400 }}>{t('common.loading')}</p>
        </main>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', background: theme.colors.black }}>
      <Navbar />

      <main style={{ maxWidth: 520, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
        <Link
          to="/admin/publishers"
          style={{ fontSize: theme.fontSizes.sm, color: theme.colors.neutral400, transition: 'color 0.15s' }}
          onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
          onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.neutral400; }}
        >
          {t('admin.publishers.backToList')}
        </Link>

        <h1 style={{ color: theme.colors.white, fontSize: 28, fontWeight: 700, marginTop: theme.spacing.base, marginBottom: theme.spacing.xl }}>
          {isEdit ? t('admin.publishers.editTitle') : t('admin.publishers.newTitle')}
        </h1>

        {serverError && (
          <p role="alert" style={{ color: theme.colors.danger, marginBottom: theme.spacing.base }}>
            {serverError}
          </p>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div style={{ marginBottom: theme.spacing.lg }}>
            <label htmlFor="publisher-name" style={labelStyle}>
              {t('admin.publishers.nameLabel')} <span aria-hidden="true">*</span>
            </label>
            <input
              id="publisher-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={t('admin.publishers.namePlaceholder')}
              style={darkInputStyle}
              onFocus={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.primary; }}
              onBlur={(e) => { (e.currentTarget as HTMLElement).style.borderColor = theme.colors.neutral800; }}
              aria-required="true"
              aria-describedby={fieldError ? 'name-error' : undefined}
              aria-invalid={!!fieldError}
            />
            {fieldError && (
              <span id="name-error" style={{ color: theme.colors.danger, fontSize: theme.fontSizes.sm }}>
                {fieldError}
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
                ? t('admin.publishers.saving')
                : isEdit
                  ? t('admin.publishers.saveButton')
                  : t('admin.publishers.createButton')}
            </button>
            <Link to="/admin/publishers">
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
