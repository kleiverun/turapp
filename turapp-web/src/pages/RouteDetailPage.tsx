import { useEffect, useMemo, useState } from 'react';
import { MapContainer, Polyline, CircleMarker } from 'react-leaflet';
import { useNavigate, useParams } from 'react-router-dom';
import type { RoutePoint, RouteResponse } from '../api/types';
import { deleteRoute, getRoutePoints, listRoutes, updateRoute } from '../api/routes';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { KartverketTileLayer } from '../components/KartverketTileLayer';
import { totalDistanceMeters } from '../utils/geo';
import {
  ACTIVITY_TYPES,
  estimatedSeconds,
  formatDistance,
  formatDuration,
} from '../utils/pace';
import { FitBounds } from '../components/FitBounds';

export function RouteDetailPage() {
  const { routeId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [route, setRoute] = useState<RouteResponse | null>(null);
  const [points, setPoints] = useState<RoutePoint[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const id = Number(routeId);

  useEffect(() => {
    if (!user || !id) return;
    Promise.all([listRoutes(user.id), getRoutePoints(id)])
      .then(([routes, pts]) => {
        const found = routes.find((r) => r.id === id) ?? null;
        setRoute(found);
        if (found) {
          setName(found.name);
          setDescription(found.description ?? '');
        }
        setPoints(pts);
      })
      .catch((err) => setError(errorMessage(err)));
  }, [user, id]);

  const latLngs = useMemo(
    () => points.map((p) => ({ latitude: p.latitude, longitude: p.longitude })),
    [points],
  );
  const positions = points.map((p) => [p.latitude, p.longitude] as [number, number]);
  const distance = totalDistanceMeters(latLngs);
  const walking = ACTIVITY_TYPES[0];

  async function onSaveEdit() {
    if (!user || !route) return;
    try {
      const updated = await updateRoute(user.id, route.id, name, description || null);
      setRoute(updated);
      setEditing(false);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function onDelete() {
    if (!user || !route) return;
    if (!window.confirm(`Slette «${route.name}» for godt?`)) return;
    try {
      await deleteRoute(user.id, route.id);
      navigate('/routes');
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="detail-layout">
      <aside className="detail-sidebar">
        {error && <p className="error-text">{error}</p>}
        {route && !editing && (
          <>
            <h1>{route.name}</h1>
            <p className="muted">
              Lagret {new Date(route.createdAt).toLocaleDateString('nb-NO')}
              {route.description ? ` — ${route.description}` : ''}
            </p>

            <div className="stats-grid">
              <div className="stat">
                <span className="stat-label">Distanse</span>
                <span className="stat-value">{formatDistance(distance)}</span>
              </div>
              <div className="stat">
                <span className="stat-label">Est. tid (gange)</span>
                <span className="stat-value">
                  {formatDuration(estimatedSeconds(distance, walking, null))}
                </span>
              </div>
              <div className="stat">
                <span className="stat-label">Punkter</span>
                <span className="stat-value">{points.length}</span>
              </div>
            </div>

            <p className="muted small">
              Åpne mobilappen og velg «Følg rute» for å bruke denne ruten på tur.
            </p>

            <div className="sidebar-actions column">
              <button className="btn-secondary" onClick={() => setEditing(true)}>
                Endre navn/beskrivelse
              </button>
              <button className="btn-danger" onClick={onDelete}>
                Slett rute
              </button>
            </div>
          </>
        )}

        {route && editing && (
          <div className="save-form">
            <h2>Endre rute</h2>
            <label>
              Navn
              <input value={name} onChange={(e) => setName(e.target.value)} />
            </label>
            <label>
              Beskrivelse
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={3}
              />
            </label>
            <div className="sidebar-actions">
              <button className="btn-primary" onClick={onSaveEdit} disabled={!name.trim()}>
                Lagre
              </button>
              <button className="btn-ghost" onClick={() => setEditing(false)}>
                Avbryt
              </button>
            </div>
          </div>
        )}
      </aside>

      <div className="detail-map">
        <MapContainer center={[59.913, 10.752]} zoom={12} style={{ height: '100%', width: '100%' }}>
          <KartverketTileLayer />
          {positions.length > 1 && (
            <>
              <Polyline positions={positions} pathOptions={{ color: '#1E88E5', weight: 5 }} />
              <FitBounds positions={positions} />
              <CircleMarker
                center={positions[0]}
                radius={8}
                pathOptions={{ color: '#fff', weight: 2, fillColor: '#2E7D32', fillOpacity: 1 }}
              />
              <CircleMarker
                center={positions[positions.length - 1]}
                radius={8}
                pathOptions={{ color: '#fff', weight: 2, fillColor: '#C62828', fillOpacity: 1 }}
              />
            </>
          )}
        </MapContainer>
      </div>
    </div>
  );
}
