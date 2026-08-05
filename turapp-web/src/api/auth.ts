import { api } from './client';
import type { UserResponse } from './types';

export async function login(email: string, password: string): Promise<UserResponse> {
  const res = await api.post<UserResponse>('/users/login', { email, password });
  return res.data;
}

export async function register(
  email: string,
  password: string,
  displayName: string,
): Promise<UserResponse> {
  const res = await api.post<UserResponse>('/users', { email, password, displayName });
  return res.data;
}
