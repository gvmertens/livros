import client from './client';
import type { Author, PageResponse } from '../types';

export interface AuthorRequest {
  name: string;
}

/** GET /authors */
export async function listAuthors(page = 0, size = 20): Promise<PageResponse<Author>> {
  const res = await client.get<PageResponse<Author>>('/authors', { params: { page, size } });
  return res.data;
}

/** GET /authors/:id */
export async function getAuthor(id: string): Promise<Author> {
  const res = await client.get<Author>(`/authors/${id}`);
  return res.data;
}

/** POST /authors */
export async function createAuthor(data: AuthorRequest): Promise<Author> {
  const res = await client.post<Author>('/authors', data);
  return res.data;
}

/** PUT /authors/:id */
export async function updateAuthor(id: string, data: AuthorRequest): Promise<Author> {
  const res = await client.put<Author>(`/authors/${id}`, data);
  return res.data;
}

/** DELETE /authors/:id */
export async function deleteAuthor(id: string): Promise<void> {
  await client.delete(`/authors/${id}`);
}
