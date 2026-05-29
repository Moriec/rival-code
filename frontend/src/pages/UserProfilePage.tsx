import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { resolveApiAssetUrl } from '../api/client';
import { authApi, duelApi } from '../api/endpoints';
import { DataTable } from '../components/DataTable';
import { Panel } from '../components/Panel';
import { ErrorState, LoadingState } from '../components/StateBlocks';
import { StatusBadge } from '../components/StatusBadge';
import type { RecentDuelDto } from '../types/contracts';
import { displayName, formatDate } from '../utils/format';

export function UserProfilePage() {
  const { userId = '' } = useParams();
  const userQuery = useQuery({ queryKey: ['user', userId], queryFn: () => authApi.getUser(userId), enabled: Boolean(userId) });
  const duelProfileQuery = useQuery({ queryKey: ['duel-profile-public', userId], queryFn: () => duelApi.profile(userId), enabled: Boolean(userId) });
  const avatarUrl = resolveApiAssetUrl(userQuery.data?.avatar?.url);

  if (userQuery.isLoading) {
    return <LoadingState />;
  }
  if (userQuery.isError || !userQuery.data) {
    return <ErrorState error={userQuery.error} />;
  }

  return (
    <div className="two-column">
      <Panel title="User">
        <div className="profile-header">
          {avatarUrl ? <img className="avatar" src={avatarUrl} alt="" /> : <div className="avatar" />}
          <div>
            <h2>{displayName(userQuery.data)}</h2>
            <div className="muted">@{userQuery.data.username}</div>
          </div>
        </div>
      </Panel>

      <Panel title="Duel rating">
        {duelProfileQuery.isLoading ? (
          <LoadingState />
        ) : (
          <div className="meta-grid">
            <div>
              <span className="meta-label">Rating</span>
              <strong>{duelProfileQuery.data?.rating ?? 1200}</strong>
            </div>
            <div>
              <span className="meta-label">Rank</span>
              <strong>{duelProfileQuery.data?.rank ?? '-'}</strong>
            </div>
            <div>
              <span className="meta-label">Wins</span>
              <strong>{duelProfileQuery.data?.wins ?? 0}</strong>
            </div>
            <div>
              <span className="meta-label">Losses</span>
              <strong>{duelProfileQuery.data?.losses ?? 0}</strong>
            </div>
          </div>
        )}
      </Panel>

      <Panel title="Recent duels" className="two-column-span">
        <DataTable<RecentDuelDto>
          columns={[
            { key: 'finishedAt', header: 'Finished', render: (duel) => formatDate(duel.finishedAt) },
            { key: 'problem', header: 'Problem', render: (duel) => duel.problemTitle ?? duel.problemId ?? '-' },
            { key: 'opponent', header: 'Opponent', render: (duel) => duel.opponentUsername ?? duel.opponentUserId ?? '-' },
            { key: 'outcome', header: 'Outcome', render: (duel) => <StatusBadge status={duel.outcome} /> }
          ]}
          data={duelProfileQuery.data?.recentDuels ?? []}
          getRowKey={(duel) => duel.duelId}
          emptyText="No recent duels"
        />
      </Panel>
    </div>
  );
}
