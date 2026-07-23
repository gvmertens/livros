import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { listPublishers, deletePublisher } from '../../api/publishers';
import Navbar from '../../components/Navbar';
import Pagination from '../../components/Pagination';
import { theme } from '../../theme';
import type { ErrorResponse } from '../../types';
import type { AxiosError } from 'axios';

const PAGE_SIZE = 20;

/**
 * Admin publisher list page.
 *
 * Displays a paginated list of publishers with create / edit / delete actions.
 *
 * Requirements: 6.1–6.7
 */
export default function AdminPublisherListPage() {
  const { t } = useTranslation();
  const [page, setPage] = useState(0);
  const [deleteError, setDeleteError] = useState('');
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ['publishers', page],
    queryFn: () => listPublishers(page, PAGE_SIZE),
    placeholderData: (prev) => prev,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deletePublisher(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['publishers'] });
      setDeleteError('');
    },
    onError: (err) => {
      const axiosErr = err as AxiosError<ErrorResponse>;
      setDeleteError(
        axiosErr.response?.data?.message ?? t('admin.publishers.deleteFailed'),
      );
    },
  });

  function handleDelete(id: string, name: string) {
    if (!window.confirm(t('admin.publishers.deleteConfirm', { name }))) return;
    setDeleteError('');
    deleteMutation.mutate(id);
  }

  return (
    <div style={{ minHeight: '100vh', background: theme.colors.black }}>
      <Navbar />

      <main style={{ maxWidth: 840, margin: '0 auto', padding: `${theme.spacing.xxl}px ${theme.spacing.xl}px` }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: theme.spacing.xl }}>
          <h1 style={{ margin: 0, color: theme.colors.white, fontSize: 28, fontWeight: 700 }}>
            {t('admin.publishers.title')}
          </h1>
          <Link to="/admin/publishers/new">
            <button
              style={{
                background: theme.colors.primary,
                color: theme.colors.white,
                border: 'none',
                borderRadius: theme.radius.md,
                padding: '8px 16px',
                fontSize: theme.fontSizes.md,
                fontWeight: 600,
                cursor: 'pointer',
                transition: 'background 0.15s',
              }}
              onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.background = theme.colors.primaryDark; }}
              onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.background = theme.colors.primary; }}
            >
              {t('admin.publishers.newButton')}
            </button>
          </Link>
        </div>

        {deleteError && (
          <p role="alert" style={{ color: theme.colors.danger, marginBottom: theme.spacing.base }}>
            {deleteError}
          </p>
        )}

        {isLoading && (
          <p aria-live="polite" style={{ color: theme.colors.neutral400 }}>
            {t('admin.publishers.loading')}
          </p>
        )}

        {isError && (
          <p role="alert" style={{ color: theme.colors.danger }}>
            {t('admin.publishers.loadError')}
          </p>
        )}

        {!isLoading && !isError && data?.content.length === 0 && (
          <p style={{ color: theme.colors.neutral600 }}>{t('admin.publishers.empty')}</p>
        )}

        {data && data.content.length > 0 && (
          <>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: `1px solid ${theme.colors.neutral800}`, textAlign: 'left' }}>
                  <th
                    style={{
                      padding: '10px 16px',
                      background: theme.colors.neutral900,
                      color: theme.colors.neutral400,
                      fontSize: theme.fontSizes.xs,
                      textTransform: 'uppercase',
                      letterSpacing: '0.05em',
                      fontWeight: 600,
                    }}
                  >
                    {t('admin.publishers.colName')}
                  </th>
                  <th
                    style={{
                      padding: '10px 16px',
                      background: theme.colors.neutral900,
                      color: theme.colors.neutral400,
                      fontSize: theme.fontSizes.xs,
                      textTransform: 'uppercase',
                      letterSpacing: '0.05em',
                      fontWeight: 600,
                    }}
                  >
                    {t('admin.publishers.colCreated')}
                  </th>
                  <th
                    style={{
                      padding: '10px 16px',
                      background: theme.colors.neutral900,
                      color: theme.colors.neutral400,
                      fontSize: theme.fontSizes.xs,
                      textTransform: 'uppercase',
                      letterSpacing: '0.05em',
                      fontWeight: 600,
                      width: 140,
                    }}
                  >
                    {t('admin.publishers.colActions')}
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((publisher) => (
                  <tr
                    key={publisher.id}
                    style={{ borderBottom: `1px solid ${theme.colors.neutral900}` }}
                    onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.background = theme.colors.neutral900; }}
                    onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.background = 'transparent'; }}
                  >
                    <td style={{ padding: '12px 16px', color: theme.colors.white, fontSize: theme.fontSizes.md }}>
                      {publisher.name}
                    </td>
                    <td style={{ padding: '12px 16px', fontSize: theme.fontSizes.sm, color: theme.colors.neutral400 }}>
                      {new Date(publisher.createdAt).toLocaleDateString()}
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <Link
                        to={`/admin/publishers/${publisher.id}/edit`}
                        style={{ marginRight: 12, fontSize: 14, color: theme.colors.primary }}
                        onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primaryLight; }}
                        onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.color = theme.colors.primary; }}
                      >
                        {t('admin.publishers.edit')}
                      </Link>
                      <button
                        onClick={() => handleDelete(publisher.id, publisher.name)}
                        disabled={deleteMutation.isPending}
                        style={{
                          fontSize: 14,
                          color: theme.colors.danger,
                          background: 'none',
                          border: 'none',
                          cursor: 'pointer',
                          padding: 0,
                          transition: 'opacity 0.15s',
                        }}
                        onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.opacity = '0.7'; }}
                        onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.opacity = '1'; }}
                        aria-label={t('admin.publishers.deleteAriaLabel', { name: publisher.name })}
                      >
                        {t('admin.publishers.delete')}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div style={{ marginTop: theme.spacing.xl }}>
              <Pagination
                page={data.page}
                totalPages={data.totalPages}
                onPageChange={setPage}
              />
            </div>
          </>
        )}
      </main>
    </div>
  );
}
