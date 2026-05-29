import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Save, Upload } from 'lucide-react';
import { resolveApiAssetUrl } from '../api/client';
import { authApi, duelApi } from '../api/endpoints';
import { DataTable } from '../components/DataTable';
import { DifficultyBadge } from '../components/DifficultyBadge';
import { Panel } from '../components/Panel';
import { ErrorState, FieldError, LoadingState } from '../components/StateBlocks';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../features/auth/AuthProvider';
import type { RecentDuelDto } from '../types/contracts';
import { displayName, formatDate } from '../utils/format';

const MAX_AVATAR_SIZE_BYTES = 10 * 1024 * 1024;

export function ProfilePage() {
  const { user, isLoading, updateProfile } = useAuth();
  const [nextDisplayName, setNextDisplayName] = useState(user?.displayName ?? '');
  const [avatarFile, setAvatarFile] = useState<File>();
  const [avatarError, setAvatarError] = useState<string>();
  const avatarPreviewUrl = useMemo(() => (avatarFile ? URL.createObjectURL(avatarFile) : undefined), [avatarFile]);
  const activeAvatarUrl = resolveApiAssetUrl(user?.avatar?.url);

  const duelProfileQuery = useQuery({
    queryKey: ['duel-profile', user?.userId],
    queryFn: () => duelApi.profile(user?.userId ?? ''),
    enabled: Boolean(user?.userId)
  });

  useEffect(() => {
    setNextDisplayName(user?.displayName ?? '');
  }, [user?.displayName]);

  useEffect(() => {
    return () => {
      if (avatarPreviewUrl) {
        URL.revokeObjectURL(avatarPreviewUrl);
      }
    };
  }, [avatarPreviewUrl]);

  const updateMutation = useMutation({
    mutationFn: async () => {
      const avatar = avatarFile ? await authApi.uploadAvatar(avatarFile) : undefined;
      await updateProfile({
        displayName: nextDisplayName || undefined,
        avatarId: avatar?.avatarId
      });
    },
    onSuccess: () => setAvatarFile(undefined)
  });

  if (isLoading) {
    return <LoadingState />;
  }

  const handleAvatarFile = (file?: File) => {
    if (!file) {
      setAvatarFile(undefined);
      setAvatarError(undefined);
      return;
    }
    if (!file.type.startsWith('image/')) {
      setAvatarFile(undefined);
      setAvatarError('Choose an image file.');
      return;
    }
    if (file.size > MAX_AVATAR_SIZE_BYTES) {
      setAvatarFile(undefined);
      setAvatarError('Avatar file is too large. Maximum size is 10 MB.');
      return;
    }
    setAvatarFile(file);
    setAvatarError(undefined);
  };

  return (
    <div className="two-column">
      <Panel title="Profile">
        <div className="profile-header">
          {avatarPreviewUrl || activeAvatarUrl ? <img className="avatar avatar--large" src={avatarPreviewUrl ?? activeAvatarUrl} alt="" /> : <div className="avatar avatar--large" />}
          <div>
            <h2>{displayName(user)}</h2>
            <div className="muted">{user?.email ?? user?.userId}</div>
            <div className="profile-subline">@{user?.username ?? 'user'}</div>
          </div>
        </div>
        <div className="meta-grid profile-meta">
          <div>
            <span className="meta-label">Username</span>
            <strong>{user?.username ?? '-'}</strong>
          </div>
          <div>
            <span className="meta-label">Rating</span>
            <strong>{duelProfileQuery.data?.rating ?? 1200}</strong>
          </div>
          <div>
            <span className="meta-label">Rank</span>
            <strong>{duelProfileQuery.data?.rank ?? '-'}</strong>
          </div>
          <div>
            <span className="meta-label">Created</span>
            <strong>{formatDate(user?.createdAt)}</strong>
          </div>
        </div>
      </Panel>

      <Panel title="Edit">
        <form
          className="form-grid"
          onSubmit={(event) => {
            event.preventDefault();
            updateMutation.mutate();
          }}
        >
          <label>
            Display name
            <input value={nextDisplayName} onChange={(event) => setNextDisplayName(event.target.value)} />
          </label>
          <label>
            Avatar
            <input
              type="file"
              accept="image/*"
              onChange={(event) => handleAvatarFile(event.target.files?.[0])}
            />
          </label>
          {avatarFile ? (
            <div className="upload-note">
              <Upload size={14} />
              <span>{avatarFile.name}</span>
            </div>
          ) : null}
          <FieldError>{avatarError ?? (updateMutation.error instanceof Error ? updateMutation.error.message : undefined)}</FieldError>
          <button className="button-primary" type="submit" disabled={updateMutation.isPending}>
            <Save size={14} /> {updateMutation.isPending ? 'Saving...' : 'Save'}
          </button>
        </form>
      </Panel>

      <Panel title="Duel summary" className="two-column-span">
        {duelProfileQuery.isLoading ? (
          <LoadingState />
        ) : duelProfileQuery.isError ? (
          <ErrorState error={duelProfileQuery.error} />
        ) : (
          <div className="meta-grid">
            <div>
              <span className="meta-label">Wins</span>
              <strong>{duelProfileQuery.data?.wins ?? 0}</strong>
            </div>
            <div>
              <span className="meta-label">Losses</span>
              <strong>{duelProfileQuery.data?.losses ?? 0}</strong>
            </div>
            <div>
              <span className="meta-label">Draws</span>
              <strong>{duelProfileQuery.data?.draws ?? 0}</strong>
            </div>
            <div>
              <span className="meta-label">Solved</span>
              <strong>{duelProfileQuery.data?.solvedProblems ?? 0}</strong>
            </div>
          </div>
        )}
      </Panel>

      <Panel title="Recent duels" className="two-column-span">
        <DataTable<RecentDuelDto>
          columns={[
            { key: 'finishedAt', header: 'Finished', render: (duel) => formatDate(duel.finishedAt) },
            { key: 'problem', header: 'Problem', render: (duel) => duel.problemTitle ?? duel.problemId ?? '-' },
            { key: 'difficulty', header: 'Difficulty', render: (duel) => <DifficultyBadge difficulty={duel.problemDifficulty} /> },
            { key: 'opponent', header: 'Opponent', render: (duel) => duel.opponentUserId ? <Link to={`/users/${duel.opponentUserId}`}>{duel.opponentUsername ?? duel.opponentUserId}</Link> : '-' },
            { key: 'outcome', header: 'Outcome', render: (duel) => <StatusBadge status={duel.outcome} /> },
            { key: 'delta', header: 'Delta', render: (duel) => ratingDelta(duel.ratingDelta), align: 'right' },
            { key: 'open', header: '', render: (duel) => <Link to={`/duels/${duel.duelId}`}>open</Link>, align: 'right' }
          ]}
          data={duelProfileQuery.data?.recentDuels ?? []}
          getRowKey={(duel) => duel.duelId}
          emptyText="No recent duels"
        />
      </Panel>
    </div>
  );
}

function ratingDelta(delta?: number): string {
  if (!delta) {
    return '0';
  }
  return delta > 0 ? `+${delta}` : String(delta);
}
