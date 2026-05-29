import { Link } from 'react-router-dom';
import { Panel } from '../components/Panel';

export function NotFoundPage() {
  return (
    <Panel title="Not found">
      <p className="muted">This page does not exist.</p>
      <Link to="/problems">Back to problems</Link>
    </Panel>
  );
}
