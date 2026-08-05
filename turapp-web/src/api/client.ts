import axios from 'axios';

export const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export const api = axios.create({
  baseURL: `${API_BASE}/api`,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  try {
    const raw = localStorage.getItem('turapp.user');
    if (raw) {
      const user = JSON.parse(raw) as { token?: string };
      if (user.token) config.headers['Authorization'] = `Bearer ${user.token}`;
    }
  } catch {}
  return config;
});

/** Plukker ut en lesbar feilmelding fra backendens ApiError-format. */
export function errorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const msg = err.response?.data?.message;
    if (typeof msg === 'string' && msg.length > 0) return msg;
    if (err.code === 'ERR_NETWORK') return 'Fikk ikke kontakt med serveren.';
    if (err.response?.status) return `Feil ${err.response.status}: ${err.response?.data?.error ?? err.message}`;
  }
  return 'Noe gikk galt. Prøv igjen.';
}
