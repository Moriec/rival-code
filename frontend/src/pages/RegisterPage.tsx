import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Panel } from '../components/Panel';
import { FieldError } from '../components/StateBlocks';
import { useAuth } from '../features/auth/AuthProvider';

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string>();
  const [loading, setLoading] = useState(false);

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(undefined);
    try {
      await register({ email, username, displayName, password });
      navigate('/problems', { replace: true });
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : 'Registration failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <Panel title="Register" className="auth-panel">
      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          Email
          <input value={email} onChange={(event) => setEmail(event.target.value)} type="email" required autoComplete="email" />
        </label>
        <label>
          Username
          <input value={username} onChange={(event) => setUsername(event.target.value)} required autoComplete="username" />
        </label>
        <label>
          Display name
          <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
        </label>
        <label>
          Password
          <input value={password} onChange={(event) => setPassword(event.target.value)} type="password" required autoComplete="new-password" />
        </label>
        <FieldError>{error}</FieldError>
        <button className="button-primary" type="submit" disabled={loading}>
          {loading ? 'Creating...' : 'Create account'}
        </button>
        <span className="muted">
          Already registered? <Link to="/login">Login</Link>
        </span>
      </form>
    </Panel>
  );
}
