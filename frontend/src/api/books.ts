import client from './client';
import type { Book, PageResponse } from '../types';

export interface BookRequest {
  isbn: string;
  title: string;
  authorId: string;
  publisherId: string;
}

/** GET /books */
export async function listBooks(
  page = 0,
  size = 20,
  search = '',
): Promise<PageResponse<Book>> {
  const res = await client.get<PageResponse<Book>>('/books', {
    params: { page, size, ...(search ? { search } : {}) },
  });
  return res.data;
}

/** GET /books/:id */
export async function getBook(id: string): Promise<Book> {
  const res = await client.get<Book>(`/books/${id}`);
  return res.data;
}

/** POST /books */
export async function createBook(data: BookRequest): Promise<Book> {
  const res = await client.post<Book>('/books', data);
  return res.data;
}

/** PUT /books/:id */
export async function updateBook(id: string, data: BookRequest): Promise<Book> {
  const res = await client.put<Book>(`/books/${id}`, data);
  return res.data;
}

/** DELETE /books/:id */
export async function deleteBook(id: string): Promise<void> {
  await client.delete(`/books/${id}`);
}
