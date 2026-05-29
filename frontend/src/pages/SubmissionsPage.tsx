import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { submissionApi } from '../api/endpoints';
import { DataTable } from '../components/DataTable';
import { Panel } from '../components/Panel';
import { ErrorState, LoadingState } from '../components/StateBlocks';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../features/auth/AuthProvider';
import type { SubmissionSummaryDto } from '../types/contracts';
import { formatDate } from '../utils/format';

export function SubmissionsPage() {
  const { user } = useAuth();
  const submissionsQuery = useQuery({
    queryKey: ['submissions', user?.userId],
    queryFn: () => submissionApi.byUser(user?.userId ?? ''),
    enabled: Boolean(user?.userId),
    refetchInterval: 5000
  });

  return (
    <Panel title="My submissions">
      {submissionsQuery.isLoading ? (
        <LoadingState />
      ) : submissionsQuery.isError ? (
        <ErrorState error={submissionsQuery.error} />
      ) : (
        <DataTable<SubmissionSummaryDto>
          columns={[
            { key: 'createdAt', header: 'Created', render: (item) => formatDate(item.createdAt) },
            { key: 'problem', header: 'Problem', render: (item) => <Link to={`/problems/${item.problemId}`}>{item.problemId}</Link> },
            { key: 'language', header: 'Language', render: (item) => item.language },
            { key: 'mode', header: 'Mode', render: (item) => item.mode },
            { key: 'status', header: 'Status', render: (item) => <StatusBadge status={item.overallStatus ?? item.status} /> },
            { key: 'time', header: 'Time', render: (item) => `${item.maxTimeMs ?? 0} ms`, align: 'right' },
            {
              key: 'open',
              header: '',
              render: (item) => <Link to={`/submissions/${item.submissionId}`}>open</Link>,
              align: 'right'
            }
          ]}
          data={submissionsQuery.data ?? []}
          getRowKey={(item) => item.submissionId}
          emptyText="No submissions yet"
        />
      )}
    </Panel>
  );
}
