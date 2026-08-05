import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, register } from '../api/auth';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export function LoginPage() {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const { setUser } = useAuth();
  const navigate = useNavigate();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const user =
        mode === 'login'
          ? await login(email, password)
          : await register(email, password, displayName);
      setUser(user);
      navigate('/planner');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login-page">
      <div className="login-hero">
        <h1>Turapp</h1>
        <p>Planlegg ruter på PC — følg dem på mobilen.</p>
      </div>

      <form className="login-card" onSubmit={onSubmit}>
        <h2>{mode === 'login' ? 'Logg inn' : 'Registrer deg'}</h2>

        <label>
          E-post
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="email"
          />
        </label>

        {mode === 'register' && (
          <label>
            Visningsnavn
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              required
              autoComplete="name"
            />
          </label>
        )}

        <label>
          Passord
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          />
        </label>

        {error && <p className="error-text">{error}</p>}

        <button className="btn-primary" type="submit" disabled={busy}>
          {busy ? 'Vent litt…' : mode === 'login' ? 'Logg inn' : 'Registrer'}
        </button>

        <button
          type="button"
          className="btn-ghost"
          onClick={() => {
            setMode(mode === 'login' ? 'register' : 'login');
            setError(null);
          }}
        >
          {mode === 'login' ? 'Ny bruker? Registrer deg' : 'Har du konto? Logg inn'}
        </button>
      </form>
    </div>
  );
}
