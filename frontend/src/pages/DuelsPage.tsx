import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Swords, X } from 'lucide-react';
import { duelApi } from '../api/endpoints';
import { DataTable } from '../components/DataTable';
import { DifficultyBadge } from '../components/DifficultyBadge';
import { Panel } from '../components/Panel';
import { ErrorState, FieldError, LoadingState } from '../components/StateBlocks';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../features/auth/AuthProvider';
import type { DuelMode, ProblemDifficulty, QueuePresetDto, RecentDuelDto } from '../types/contracts';
import { formatDate } from '../utils/format';

const DUEL_DIFFICULTIES: ProblemDifficulty[] = ['EASY', 'MEDIUM', 'HARD'];

export function DuelsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [ticketId, setTicketId] = useState<string>();
  const [ticketError, setTicketError] = useState<string>();
  const [waitingDifficulty, setWaitingDifficulty] = useState<ProblemDifficulty>();

  const profileQuery = useQuery({
    queryKey: ['duel-profile', user?.userId],
    queryFn: () => duelApi.profile(user?.userId ?? ''),
    enabled: Boolean(user?.userId)
  });
  const presetsQuery = useQuery({ queryKey: ['duel-presets'], queryFn: duelApi.queuePresets });
  const ticketQuery = useQuery({
    queryKey: ['matchmaking-ticket', ticketId],
    queryFn: () => duelApi.getTicket(ticketId ?? ''),
    enabled: Boolean(ticketId),
    refetchInterval: (query) => (query.state.data?.status === 'WAITING' ? 1500 : false)
  });

  const createTicket = useMutation({
    mutationFn: (difficulty: ProblemDifficulty) => {
      const preset = selectPreset(presetsQuery.data ?? [], 'RATED');
      return duelApi.createTicket({
        mode: 'RATED',
        difficulty,
        presetId: preset?.presetId,
        currentRating: profileQuery.data?.rating
      });
    },
    onMutate: (difficulty) => setWaitingDifficulty(difficulty),
    onSuccess: (ticket) => {
      setTicketError(undefined);
      setWaitingDifficulty(ticket.difficulty);
      if (ticket.status === 'MATCHED' && ticket.matchedDuelId) {
        navigate(`/duels/${ticket.matchedDuelId}`);
        return;
      }
      setTicketId(ticket.ticketId);
    },
    onError: (error) => {
      setWaitingDifficulty(undefined);
      setTicketError(error instanceof Error ? error.message : 'Matchmaking failed');
    }
  });

  useEffect(() => {
    const ticket = ticketQuery.data;
    if (!ticket) {
      return;
    }
    if (ticket.status === 'MATCHED' && ticket.matchedDuelId) {
      setTicketId(undefined);
      setWaitingDifficulty(undefined);
      navigate(`/duels/${ticket.matchedDuelId}`);
      return;
    }
    if (ticket.status === 'CANCELLED' || ticket.status === 'EXPIRED') {
      setTicketId(undefined);
      setWaitingDifficulty(undefined);
    }
  }, [navigate, ticketQuery.data]);

  const cancelTicket = useMutation({
    mutationFn: () => duelApi.cancelTicket(ticketId ?? ''),
    onSuccess: () => {
      setTicketId(undefined);
      setWaitingDifficulty(undefined);
      void queryClient.invalidateQueries({ queryKey: ['duel-profile', user?.userId] });
    }
  });

  if (profileQuery.isLoading) {
    return <LoadingState />;
  }

  return (
    <div className="two-column">
      <div className="page-grid">
        <Panel
          title="Duel queue"
          actions={
            <div className="inline-actions">
              {DUEL_DIFFICULTIES.map((difficulty) => (
                <button
                  className={difficulty === 'MEDIUM' ? 'button-primary' : undefined}
                  type="button"
                  onClick={() => createTicket.mutate(difficulty)}
                  disabled={createTicket.isPending || Boolean(ticketId)}
                  key={difficulty}
                >
                  <Swords size={14} /> {difficultyLabel(difficulty)}
                </button>
              ))}
            </div>
          }
        >
          <FieldError>{ticketError}</FieldError>
          {ticketId ? (
            <div className="state-block">
              <strong>
                {ticketQuery.data?.status === 'MATCHED'
                  ? 'Opening duel'
                  : `Waiting for ${difficultyLabel(ticketQuery.data?.difficulty ?? waitingDifficulty)} opponent`}
              </strong>
              <span className="muted"> Ticket {ticketId}</span>
              <button type="button" className="button-danger" onClick={() => cancelTicket.mutate()} disabled={cancelTicket.isPending}>
                <X size={14} /> Cancel
              </button>
            </div>
          ) : (
            <p className="muted">Choose a difficulty to enter rated matchmaking. Opponent and problem will use the same difficulty.</p>
          )}
        </Panel>

        <Panel title="Recent duels">
          <DataTable<RecentDuelDto>
            columns={[
              { key: 'finishedAt', header: 'Finished', render: (duel) => formatDate(duel.finishedAt) },
              { key: 'problem', header: 'Problem', render: (duel) => duel.problemTitle ?? duel.problemId ?? '-' },
              { key: 'difficulty', header: 'Difficulty', render: (duel) => <DifficultyBadge difficulty={duel.problemDifficulty} /> },
              { key: 'opponent', header: 'Opponent', render: (duel) => duel.opponentUsername ?? duel.opponentUserId ?? '-' },
              { key: 'outcome', header: 'Outcome', render: (duel) => <StatusBadge status={duel.outcome} /> },
              { key: 'delta', header: 'Delta', render: (duel) => duel.ratingDelta ?? 0, align: 'right' },
              { key: 'open', header: '', render: (duel) => <Link to={`/duels/${duel.duelId}`}>open</Link>, align: 'right' }
            ]}
            data={profileQuery.data?.recentDuels ?? []}
            getRowKey={(duel) => duel.duelId}
            emptyText="No recent duels"
          />
        </Panel>
      </div>

      <aside className="page-grid">
        <Panel title="My duel profile">
          {profileQuery.isError ? (
            <ErrorState error={profileQuery.error} />
          ) : (
            <div className="meta-grid">
              <div>
                <span className="meta-label">Rating</span>
                <strong>{profileQuery.data?.rating ?? 1200}</strong>
              </div>
              <div>
                <span className="meta-label">Rank</span>
                <strong>{profileQuery.data?.rank ?? '-'}</strong>
              </div>
              <div>
                <span className="meta-label">Wins</span>
                <strong>{profileQuery.data?.wins ?? 0}</strong>
              </div>
              <div>
                <span className="meta-label">Losses</span>
                <strong>{profileQuery.data?.losses ?? 0}</strong>
              </div>
            </div>
          )}
        </Panel>

        <Panel title="Queue presets">
          <DataTable<QueuePresetDto>
            columns={[
              { key: 'name', header: 'Name', render: (preset) => preset.name },
              { key: 'mode', header: 'Mode', render: (preset) => preset.mode },
              { key: 'duration', header: 'Time', render: (preset) => `${preset.duelDurationSeconds ?? 0}s`, align: 'right' }
            ]}
            data={(presetsQuery.data ?? []).filter((preset) => preset.active !== false)}
            getRowKey={(preset) => preset.presetId}
            emptyText="No active presets"
          />
        </Panel>
      </aside>
    </div>
  );
}

function selectPreset(presets: QueuePresetDto[], mode: DuelMode): QueuePresetDto | undefined {
  return presets.find((preset) => preset.mode === mode && preset.active !== false);
}

function difficultyLabel(difficulty?: ProblemDifficulty): string {
  if (!difficulty) {
    return 'selected';
  }
  return difficulty[0] + difficulty.slice(1).toLowerCase();
}
