import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { submissionApi } from '../api/endpoints';
import { Panel } from '../components/Panel';
import { ErrorState, LoadingState } from '../components/StateBlocks';
import { StatusBadge } from '../components/StatusBadge';
import { SubmissionVerdict } from '../components/SubmissionVerdict';
import { formatDate } from '../utils/format';

export function SubmissionDetailsPage() {
  const { submissionId = '' } = useParams();
  const summaryQuery = useQuery({
    queryKey: ['submission', submissionId],
    queryFn: () => submissionApi.get(submissionId),
    enabled: Boolean(submissionId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'QUEUED' || status === 'JUDGING' || status === 'CREATED' ? 2000 : false;
    }
  });
  const verdictQuery = useQuery({
    queryKey: ['submission-verdict', submissionId],
    queryFn: () => submissionApi.verdict(submissionId),
    enabled: Boolean(summaryQuery.data?.overallStatus || summaryQuery.data?.status === 'JUDGED'),
    retry: false
  });

  if (summaryQuery.isLoading) {
    return <LoadingState />;
  }
  if (summaryQuery.isError || !summaryQuery.data) {
    return <ErrorState error={summaryQuery.error} />;
  }

  const summary = summaryQuery.data;

  return (
    <div className="page-grid">
      <Panel title={`Submission ${summary.submissionId}`}>
        <div className="meta-grid">
          <div>
            <span className="meta-label">Problem</span>
            <Link to={`/problems/${summary.problemId}`}>{summary.problemId}</Link>
          </div>
          <div>
            <span className="meta-label">Status</span>
            <StatusBadge status={summary.overallStatus ?? summary.status} />
          </div>
          <div>
            <span className="meta-label">Language</span>
            <strong>{summary.language}</strong>
          </div>
          <div>
            <span className="meta-label">Created</span>
            <strong>{formatDate(summary.createdAt)}</strong>
          </div>
        </div>
      </Panel>

      <Panel title="Public verdict">
        {verdictQuery.isLoading ? <LoadingState text="Waiting for judge result..." /> : <SubmissionVerdict verdict={verdictQuery.data} />}
      </Panel>
    </div>
  );
}
