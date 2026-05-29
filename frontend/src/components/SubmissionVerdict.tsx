import type { PublicSubmissionVerdict } from '../types/contracts';
import { DataTable } from './DataTable';
import { StatusBadge } from './StatusBadge';

export function SubmissionVerdict({ verdict }: { verdict?: PublicSubmissionVerdict }) {
  if (!verdict) {
    return <p className="muted">Verdict is not ready yet.</p>;
  }
  return (
    <div className="verdict">
      <div className="meta-grid">
        <div>
          <span className="meta-label">Status</span>
          <StatusBadge status={verdict.overallStatus} />
        </div>
        <div>
          <span className="meta-label">Tests</span>
          <strong>
            {verdict.passedTests ?? 0}/{verdict.totalTests ?? 0}
          </strong>
        </div>
        <div>
          <span className="meta-label">Max time</span>
          <strong>{verdict.maxTimeMs ?? 0} ms</strong>
        </div>
        <div>
          <span className="meta-label">Max memory</span>
          <strong>{verdict.maxMemoryKb ?? 0} KB</strong>
        </div>
      </div>

      {verdict.compilationError && <pre className="diagnostic">{verdict.compilationError}</pre>}

      <DataTable
        columns={[
          { key: 'orderNo', header: '#', render: (row) => row.orderNo, align: 'right' },
          { key: 'status', header: 'Status', render: (row) => <StatusBadge status={row.status} /> },
          { key: 'time', header: 'Time', render: (row) => `${row.timeMs ?? 0} ms`, align: 'right' },
          { key: 'memory', header: 'Memory', render: (row) => `${row.memoryKb ?? 0} KB`, align: 'right' },
          { key: 'message', header: 'Message', render: (row) => row.message ?? <span className="muted">hidden</span> }
        ]}
        data={verdict.visibleTests ?? []}
        getRowKey={(row) => String(row.orderNo)}
        emptyText="No visible test details"
      />
    </div>
  );
}
