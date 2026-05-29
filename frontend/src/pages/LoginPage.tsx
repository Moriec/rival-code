import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Panel } from '../components/Panel';
import { FieldError } from '../components/StateBlocks';
import { useAuth } from '../features/auth/AuthProvider';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string>();
  const [loading, setLoading] = useState(false);

  const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? '/problems';

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(undefined);
    try {
      await login({ usernameOrEmail, password });
      navigate(from, { replace: true });
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <Panel title="Login" className="auth-panel">
      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          Username or email
          <input value={usernameOrEmail} onChange={(event) => setUsernameOrEmail(event.target.value)} required autoComplete="username" />
        </label>
        <label>
          Password
          <input value={password} onChange={(event) => setPassword(event.target.value)} required type="password" autoComplete="current-password" />
        </label>
        <FieldError>{error}</FieldError>
        <button className="button-primary" type="submit" disabled={loading}>
          {loading ? 'Signing in...' : 'Login'}
        </button>
        <span className="muted">
          No account yet? <Link to="/register">Register</Link>
        </span>
      </form>
    </Panel>
  );
}
