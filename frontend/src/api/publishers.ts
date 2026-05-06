import client from './client';
import type { Publisher, PageResponse } from '../types';

export interface PublisherRequest {
  name: string;
}

/** GET /publishers */
export async function listPublishers(page = 0, size = 20): Promise<PageResponse<Publisher>> {
  const res = await client.get<PageResponse<Publisher>>('/publishers', { params: { page, size } });
  return res.data;
}

/** GET /publishers/:id */
export async function getPublisher(id: string): Promise<Publisher> {
  const res = await client.get<Publisher>(`/publishers/${id}`);
  return res.data;
}

/** POST /publishers */
export async function createPublisher(data: PublisherRequest): Promise<Publisher> {
  const res = await client.post<Publisher>('/publishers', data);
  return res.data;
}

/** PUT /publishers/:id */
export async function updatePublisher(id: string, data: PublisherRequest): Promise<Publisher> {
  const res = await client.put<Publisher>(`/publishers/${id}`, data);
  return res.data;
}

/** DELETE /publishers/:id */
export async function deletePublisher(id: string): Promise<void> {
  await client.delete(`/publishers/${id}`);
}
