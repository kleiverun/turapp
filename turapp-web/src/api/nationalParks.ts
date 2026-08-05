import { api } from './client';

export interface NationalParkBoundaryPoint {
  latitude: number;
  longitude: number;
}

export interface NationalPark {
  id: number;
  name: string;
  boundary: NationalParkBoundaryPoint[];
}

export async function listNationalParks(): Promise<NationalPark[]> {
  const res = await api.get<NationalPark[]>('/national-parks');
  return res.data;
}
