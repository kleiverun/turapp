import { TileLayer } from 'react-leaflet';

/** Kartverkets topografiske kart — samme kartgrunnlag som mobilappen. */
export function KartverketTileLayer() {
  return (
    <TileLayer
      url="https://cache.kartverket.no/v1/wmts/1.0.0/topo/default/webmercator/{z}/{y}/{x}.png"
      attribution='&copy; <a href="https://www.kartverket.no/">Kartverket</a>'
      maxZoom={18}
    />
  );
}
