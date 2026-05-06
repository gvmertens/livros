import axios from 'axios';

/**
 * Shared Axios instance for all API calls.
 *
 * - Base URL: VITE_API_BASE_URL env var, falling back to the Vite dev proxy path.
 * - Request interceptor: attaches the JWT from localStorage as a Bearer token.
 * - Response interceptor: redirects to /login on 401 Unauthorized.
 *
 * Requirements: 2.4, 2.5
 */
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

// ── Request interceptor — attach JWT ─────────────────────────────────────────
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ── Response interceptor — handle 401 globally ───────────────────────────────
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('jwt');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  },
);

export default client;
