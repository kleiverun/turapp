import { useEffect, useMemo, useState } from 'react';
import { MapContainer, Polygon, Polyline, CircleMarker, Tooltip, useMapEvents } from 'react-leaflet';
import { useNavigate } from 'react-router-dom';
import type { LatLng } from '../api/types';
import { createRoute } from '../api/routes';
import { listTrips } from '../api/trips';
import { listNationalParks, type NationalPark } from '../api/nationalParks';
import { listNatureReserves, type NatureReserve } from '../api/natureReserves';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { KartverketTileLayer } from '../components/KartverketTileLayer';
import { totalDistanceMeters } from '../utils/geo';
import {
  ACTIVITY_TYPES,
  effectivePaceMps,
  estimatedSeconds,
  formatDistance,
  formatDuration,
  formatPace,
  personalPaceMps,
} from '../utils/pace';

const OSLO: [number, number] = [59.913, 10.752];

function ClickCapture({ onClick }: { onClick: (p: LatLng) => void }) {
  useMapEvents({
    click(e) {
      onClick({ latitude: e.latlng.lat, longitude: e.latlng.lng });
    },
  });
  return null;
}

export function PlannerPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [waypoints, setWaypoints] = useState<LatLng[]>([]);
  const [activityKey, setActivityKey] = useState('WALKING');
  const [personal, setPersonal] = useState<number | null>(null);
  const [tripCount, setTripCount] = useState(0);
  const [name, setName] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [nationalParks, setNationalParks] = useState<NationalPark[]>([]);
  const [natureReserves, setNatureReserves] = useState<NatureReserve[]>([]);

  useEffect(() => {
    if (!user) return;
    listTrips(user.id)
      .then((trips) => {
        setPersonal(personalPaceMps(trips));
        setTripCount(trips.length);
      })
      .catch(() => {
        /* historikk er valgfri — standardtempo brukes */
      });
  }, [user]);

  useEffect(() => {
    listNationalParks().then(setNationalParks).catch(() => {});
    listNatureReserves().then(setNatureReserves).catch(() => {});
  }, []);

  const activity = ACTIVITY_TYPES.find((a) => a.key === activityKey) ?? ACTIVITY_TYPES[0];
  const distance = useMemo(() => totalDistanceMeters(waypoints), [waypoints]);
  const seconds = estimatedSeconds(distance, activity, personal);
  const paceMps = effectivePaceMps(activity, personal);

  function addWaypoint(p: LatLng) {
    setWaypoints((prev) => [...prev, p]);
  }

  function removeWaypoint(index: number) {
    setWaypoints((prev) => prev.filter((_, i) => i !== index));
  }

  async function save() {
    if (!user || waypoints.length < 2 || !name.trim()) return;
    setSaving(true);
    setError(null);
    try {
      await createRoute(user.id, name.trim(), notes.trim() || null, waypoints);
      navigate('/routes');
    } catch (err) {
      setError(errorMessage(err));
      setSaving(false);
    }
  }

  const positions = waypoints.map((p) => [p.latitude, p.longitude] as [number, number]);

  return (
    <div className="planner-layout">
      <aside className="planner-sidebar">
        <h2>Planlegg rute</h2>
        <p className="muted">Klikk på kartet for å legge til veipunkter.</p>

        <div className="chip-row">
          {ACTIVITY_TYPES.map((a) => (
            <button
              key={a.key}
              className={a.key === activityKey ? 'chip chip-active' : 'chip'}
              onClick={() => setActivityKey(a.key)}
            >
              {a.label}
            </button>
          ))}
        </div>

        <div className="stats-grid">
          <div className="stat">
            <span className="stat-label">Distanse</span>
            <span className="stat-value">{formatDistance(distance)}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Est. tid</span>
            <span className="stat-value">{formatDuration(seconds)}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Tempo</span>
            <span className="stat-value">{formatPace(paceMps)}</span>
          </div>
        </div>

        <p className="muted small">
          {waypoints.length} veipunkter
          {personal != null
            ? ` • tempo basert på ${tripCount} turer`
            : ' • standardtempo (for lite turhistorikk)'}
        </p>

        {waypoints.length > 0 && (
          <ol className="waypoint-list">
            {waypoints.map((p, i) => (
              <li key={`${p.latitude}-${p.longitude}-${i}`}>
                <span>
                  {p.latitude.toFixed(5)}, {p.longitude.toFixed(5)}
                </span>
                <button className="btn-ghost small" onClick={() => removeWaypoint(i)}>
                  Fjern
                </button>
              </li>
            ))}
          </ol>
        )}

        <div className="sidebar-actions">
          <button
            className="btn-ghost"
            disabled={waypoints.length === 0}
            onClick={() => setWaypoints((prev) => prev.slice(0, -1))}
          >
            ↩ Angre
          </button>
          <button
            className="btn-ghost"
            disabled={waypoints.length === 0}
            onClick={() => setWaypoints([])}
          >
            ✕ Tøm
          </button>
        </div>

        <div className="save-form">
          <label>
            Navn på ruten
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="F.eks. Søndagstur" />
          </label>
          <label>
            Beskrivelse (valgfritt)
            <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} />
          </label>
          {error && <p className="error-text">{error}</p>}
          <button
            className="btn-primary"
            disabled={saving || waypoints.length < 2 || !name.trim()}
            onClick={save}
          >
            {saving ? 'Lagrer…' : 'Lagre rute'}
          </button>
        </div>
      </aside>

      <div className="planner-map">
        <MapContainer center={OSLO} zoom={12} style={{ height: '100%', width: '100%' }}>
          <KartverketTileLayer />
          <ClickCapture onClick={addWaypoint} />
          {natureReserves.map((r) => (
            <Polygon
              key={r.id}
              positions={r.boundary.map((p) => [p.latitude, p.longitude] as [number, number])}
              pathOptions={{ color: '#E65100', weight: 1, fillColor: '#FF9800', fillOpacity: 0.18 }}
            >
              <Tooltip sticky>{r.name}</Tooltip>
            </Polygon>
          ))}
          {nationalParks.map((park) => (
            <Polygon
              key={park.id}
              positions={park.boundary.map((p) => [p.latitude, p.longitude] as [number, number])}
              pathOptions={{ color: '#1B5E20', weight: 2.5, fillColor: '#4CAF50', fillOpacity: 0.25 }}
            >
              <Tooltip sticky>{park.name}</Tooltip>
            </Polygon>
          ))}
          {positions.length > 1 && (
            <Polyline positions={positions} pathOptions={{ color: '#1E88E5', weight: 5 }} />
          )}
          {positions.map((pos, i) => (
            <CircleMarker
              key={i}
              center={pos}
              radius={7}
              pathOptions={{
                color: '#ffffff',
                weight: 2,
                fillColor: i === 0 ? '#2E7D32' : i === positions.length - 1 ? '#C62828' : '#1E88E5',
                fillOpacity: 1,
              }}
              eventHandlers={{ click: () => removeWaypoint(i) }}
            />
          ))}
        </MapContainer>
      </div>
    </div>
  );
}
