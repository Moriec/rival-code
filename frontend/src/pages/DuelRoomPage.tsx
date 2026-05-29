import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Send } from 'lucide-react';
import { duelApi, problemApi, submissionApi } from '../api/endpoints';
import { CodeEditor } from '../components/CodeEditor';
import { DifficultyBadge } from '../components/DifficultyBadge';
import { DuelTimer } from '../components/DuelTimer';
import { FormattedStatement } from '../components/FormattedStatement';
import { Panel } from '../components/Panel';
import { ErrorState, FieldError, LoadingState } from '../components/StateBlocks';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../features/auth/AuthProvider';
import { useDebouncedEffect } from '../hooks/useDebouncedEffect';
import { useDuelSocket } from '../hooks/useDuelSocket';
import type { DuelFinishedEvent, DuelRoomStateDto, SupportedUiLanguage } from '../types/contracts';
import { UI_LANGUAGES } from '../types/contracts';
import { defaultCode } from '../utils/languageTemplates';

export function DuelRoomPage() {
  const { duelId = '' } = useParams();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [roomState, setRoomState] = useState<DuelRoomStateDto>();
  const [finishedEvent, setFinishedEvent] = useState<DuelFinishedEvent>();
  const [language, setLanguage] = useState<SupportedUiLanguage>('JAVA');
  const [opponentCode, setOpponentCode] = useState('');
  const snapshotVersionRef = useRef(0);
  const draftKey = useMemo(() => `rivalcode.draft.duel.${duelId}.${language}`, [duelId, language]);
  const [sourceCode, setSourceCode] = useState(() => window.localStorage.getItem(draftKey) ?? defaultCode(language));

  const duelQuery = useQuery({
    queryKey: ['duel', duelId],
    queryFn: () => duelApi.get(duelId),
    enabled: Boolean(duelId),
    refetchInterval: (query) => (isFinished(query.state.data?.status) ? false : 3000)
  });

  useEffect(() => {
    if (duelQuery.data) {
      setRoomState(duelQuery.data);
    }
  }, [duelQuery.data]);

  useEffect(() => {
    setSourceCode(window.localStorage.getItem(draftKey) ?? defaultCode(language));
  }, [draftKey, language]);

  useEffect(() => {
    window.localStorage.setItem(draftKey, sourceCode);
  }, [draftKey, sourceCode]);

  const state = roomState ?? duelQuery.data;
  const participants = state?.participants ?? [];
  const me = participants.find((participant) => participant.userId === user?.userId);
  const opponent = participants.find((participant) => participant.userId !== user?.userId);

  const problemQuery = useQuery({
    queryKey: ['problem', state?.problemId],
    queryFn: () => problemApi.get(state?.problemId ?? ''),
    enabled: Boolean(state?.problemId)
  });

  const handleState = useCallback((nextState: DuelRoomStateDto) => {
    setRoomState(nextState);
    void queryClient.invalidateQueries({ queryKey: ['duel', nextState.duelId] });
  }, [queryClient]);

  const handleFinished = useCallback((event: DuelFinishedEvent) => {
    setFinishedEvent(event);
    void queryClient.invalidateQueries({ queryKey: ['duel', event.duelId] });
  }, [queryClient]);

  const { connected, sendCodeSnapshot } = useDuelSocket({
    duelId,
    userId: user?.userId,
    onState: handleState,
    onFinished: handleFinished,
    onOpponentCode: (message) => setOpponentCode(message.sourceCode)
  });

  useDebouncedEffect(
    () => {
      if (!user?.userId || !connected || isFinished(state?.status)) {
        return;
      }
      const nextVersion = snapshotVersionRef.current + 1;
      snapshotVersionRef.current = nextVersion;
      sendCodeSnapshot({
        duelId,
        userId: user.userId,
        language,
        sourceCode,
        version: nextVersion,
        sentAt: new Date().toISOString()
      });
    },
    [duelId, sourceCode, language, connected, user?.userId, state?.status, sendCodeSnapshot],
    750
  );

  const submitMutation = useMutation({
    mutationFn: () =>
      submissionApi.create({
        problemId: state?.problemId ?? '',
        problemVersionId: state?.problemVersionId,
        duelId,
        mode: 'DUEL',
        userCode: { sourceCode, language }
      }),
    onSuccess: (response) => {
      void queryClient.invalidateQueries({ queryKey: ['duel', duelId] });
      void queryClient.invalidateQueries({ queryKey: ['submission', response.submissionId] });
    }
  });

  if (duelQuery.isLoading) {
    return <LoadingState />;
  }
  if (duelQuery.isError || !state) {
    return <ErrorState error={duelQuery.error} />;
  }

  const submitBlocked = isFinished(state.status) || submitMutation.isPending;

  return (
    <div className="page-grid">
      <Panel
        title={state.problemTitle ?? 'Duel room'}
        actions={
          <div className="duel-toolbar">
            <DifficultyBadge difficulty={state.problemDifficulty} />
            <StatusBadge status={state.status} />
            <DuelTimer endsAt={state.endsAt} stopped={isFinished(state.status)} />
            <span className={connected ? 'badge badge--success' : 'badge badge--warning'}>{connected ? 'WS connected' : 'WS reconnecting'}</span>
          </div>
        }
      >
        <div className="meta-grid">
          <div>
            <span className="meta-label">Mode</span>
            <strong>{state.mode}</strong>
          </div>
          <div>
            <span className="meta-label">You</span>
            <strong>{me?.displayName || me?.username || user?.username || user?.userId}</strong>
          </div>
          <div>
            <span className="meta-label">Opponent</span>
            <strong>{opponent?.displayName || opponent?.username || opponent?.userId || 'waiting'}</strong>
          </div>
          <div>
            <span className="meta-label">Outcome</span>
            <StatusBadge status={me?.outcome ?? (finishedEvent?.winnerUserId === user?.userId ? 'WIN' : undefined)} />
          </div>
        </div>
      </Panel>

      <Panel title="Participants">
        <div className="participant-list">
          {participants.map((participant) => (
            <div className="participant-card" key={participant.userId}>
              <strong>{participant.displayName || participant.username || participant.userId}</strong>
              <div className="muted participant-rating">
                {formatRating(participant.ratingBefore)} -&gt; {formatRating(participant.ratingAfter)}
              </div>
              <StatusBadge status={participant.outcome} />
            </div>
          ))}
        </div>
      </Panel>

      <Panel title="Statement" actions={state.problemId ? <Link to={`/problems/${state.problemId}`}>open problem</Link> : undefined}>
        {problemQuery.isLoading ? <LoadingState /> : <FormattedStatement text={problemQuery.data?.statement} fallback="Statement is not loaded yet." />}
      </Panel>

      <Panel title="Examples">
        <div className="page-grid">
          {(problemQuery.data?.examples ?? []).map((example) => (
            <div className="example-pair" key={example.orderNo}>
              <strong className="example-label">Input</strong>
              <pre className="example-block">{example.input}</pre>
              <strong className="example-label">Output</strong>
              <pre className="example-block">{example.expectedOutput}</pre>
            </div>
          ))}
        </div>
      </Panel>

      <Panel title="Live code">
        <form
          className="form-grid"
          onSubmit={(event) => {
            event.preventDefault();
            submitMutation.mutate();
          }}
        >
          <div className="editor-toolbar">
            <label>
              Language
              <select value={language} onChange={(event) => setLanguage(event.target.value as SupportedUiLanguage)} disabled={submitBlocked}>
                {UI_LANGUAGES.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>
            </label>
            <button className="button-primary" type="submit" disabled={submitBlocked}>
              <Send size={14} /> {submitMutation.isPending ? 'Submitting...' : 'Submit duel solution'}
            </button>
            {submitMutation.data && <Link to={`/submissions/${submitMutation.data.submissionId}`}>submission {submitMutation.data.submissionId}</Link>}
          </div>
          <FieldError>{submitMutation.error instanceof Error ? submitMutation.error.message : undefined}</FieldError>
          <div className="split-editors">
            <div>
              <span className="meta-label">Your code</span>
              <CodeEditor
                value={sourceCode}
                language={language}
                readOnly={submitBlocked}
                height="clamp(380px, 58vh, 640px)"
                onChange={setSourceCode}
              />
            </div>
            <div>
              <span className="meta-label">Opponent code</span>
              <CodeEditor
                value={opponentCode || '// Waiting for opponent snapshot'}
                language={language}
                height="clamp(380px, 58vh, 640px)"
                readOnly
              />
            </div>
          </div>
        </form>
      </Panel>
    </div>
  );
}

function isFinished(status?: string): boolean {
  return status === 'FINISHED' || status === 'CANCELLED' || status === 'EXPIRED';
}

function formatRating(value?: number | null): string {
  return typeof value === 'number' ? String(value) : '?';
}
