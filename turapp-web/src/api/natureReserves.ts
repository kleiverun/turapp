import { api } from './client';

export interface NatureReserveBoundaryPoint {
  latitude: number;
  longitude: number;
}

export interface NatureReserve {
  id: number;
  name: string;
  boundary: NatureReserveBoundaryPoint[];
}

export async function listNatureReserves(): Promise<NatureReserve[]> {
  const res = await api.get<NatureReserve[]>('/nature-reserves');
  return res.data;
}
