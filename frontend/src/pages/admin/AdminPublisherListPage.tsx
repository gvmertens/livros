import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listPublishers, deletePublisher } from '../../api/publishers';
import Navbar from '../../components/Navbar';
import Pagination from '../../components/Pagination';
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
        axiosErr.response?.data?.message ?? 'Failed to delete publisher.',
      );
    },
  });

  function handleDelete(id: string, name: string) {
    if (!window.confirm(`Delete publisher "${name}"? This cannot be undone.`)) return;
    setDeleteError('');
    deleteMutation.mutate(id);
  }

  return (
    <div>
      <Navbar />

      <main style={{ maxWidth: 800, margin: '0 auto', padding: '24px 16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <h1 style={{ margin: 0 }}>Publishers</h1>
          <Link to="/admin/publishers/new">
            <button style={{ padding: '8px 16px' }}>+ New Publisher</button>
          </Link>
        </div>

        {deleteError && (
          <p role="alert" style={{ color: 'red', marginBottom: 16 }}>
            {deleteError}
          </p>
        )}

        {isLoading && <p aria-live="polite">Loading publishers…</p>}

        {isError && (
          <p role="alert" style={{ color: 'red' }}>
            Failed to load publishers.
          </p>
        )}

        {!isLoading && !isError && data?.content.length === 0 && (
          <p style={{ opacity: 0.6 }}>No publishers yet. Create one to get started.</p>
        )}

        {data && data.content.length > 0 && (
          <>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #ddd', textAlign: 'left' }}>
                  <th style={{ padding: '8px 12px' }}>Name</th>
                  <th style={{ padding: '8px 12px' }}>Created</th>
                  <th style={{ padding: '8px 12px', width: 140 }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((publisher) => (
                  <tr key={publisher.id} style={{ borderBottom: '1px solid #eee' }}>
                    <td style={{ padding: '10px 12px' }}>{publisher.name}</td>
                    <td style={{ padding: '10px 12px', fontSize: 13, opacity: 0.6 }}>
                      {new Date(publisher.createdAt).toLocaleDateString()}
                    </td>
                    <td style={{ padding: '10px 12px' }}>
                      <Link
                        to={`/admin/publishers/${publisher.id}/edit`}
                        style={{ marginRight: 12, fontSize: 14 }}
                      >
                        Edit
                      </Link>
                      <button
                        onClick={() => handleDelete(publisher.id, publisher.name)}
                        disabled={deleteMutation.isPending}
                        style={{ fontSize: 14, color: '#c00', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
                        aria-label={`Delete ${publisher.name}`}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div style={{ marginTop: 24 }}>
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
