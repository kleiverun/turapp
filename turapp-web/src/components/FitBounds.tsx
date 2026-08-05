import { useEffect } from 'react';
import { useMap } from 'react-leaflet';
import L from 'leaflet';

/** Zoomer kartet slik at hele linja er synlig. */
export function FitBounds({ positions }: { positions: [number, number][] }) {
  const map = useMap();
  useEffect(() => {
    if (positions.length > 1) {
      map.fitBounds(L.latLngBounds(positions), { padding: [40, 40] });
    }
  }, [map, positions]);
  return null;
}
