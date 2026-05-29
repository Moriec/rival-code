import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { duelApi } from '../api/endpoints';
import { DataTable } from '../components/DataTable';
import { Panel } from '../components/Panel';
import { ErrorState, LoadingState } from '../components/StateBlocks';
import type { LeaderboardEntryDto } from '../types/contracts';

export function LeaderboardPage() {
  const leaderboardQuery = useQuery({ queryKey: ['leaderboard'], queryFn: () => duelApi.leaderboard() });

  return (
    <Panel title="Leaderboard">
      {leaderboardQuery.isLoading ? (
        <LoadingState />
      ) : leaderboardQuery.isError ? (
        <ErrorState error={leaderboardQuery.error} />
      ) : (
        <DataTable<LeaderboardEntryDto>
          columns={[
            { key: 'rank', header: '#', render: (entry) => entry.rank, align: 'right' },
            { key: 'user', header: 'User', render: (entry) => <Link to={`/users/${entry.userId}`}>{entry.username || entry.userId}</Link> },
            { key: 'rating', header: 'Rating', render: (entry) => entry.rating, align: 'right' },
            { key: 'wins', header: 'W', render: (entry) => entry.wins, align: 'right' },
            { key: 'losses', header: 'L', render: (entry) => entry.losses, align: 'right' },
            { key: 'draws', header: 'D', render: (entry) => entry.draws, align: 'right' }
          ]}
          data={leaderboardQuery.data ?? []}
          getRowKey={(entry) => entry.userId}
          emptyText="Leaderboard is empty"
        />
      )}
    </Panel>
  );
}
