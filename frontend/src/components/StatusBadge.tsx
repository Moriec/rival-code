import type { DuelOutcome, DuelStatus, JudgeStatus, MatchmakingTicketStatus, NotificationStatus, SubmissionStatus } from '../types/contracts';

type Status = JudgeStatus | SubmissionStatus | DuelStatus | DuelOutcome | MatchmakingTicketStatus | NotificationStatus | string | undefined;

const SUCCESS = new Set(['ACCEPTED', 'JUDGED', 'WIN', 'MATCHED', 'READ']);
const DANGER = new Set(['WRONG_ANSWER', 'RUNTIME_ERROR', 'SYSTEM_ERROR', 'FAILED', 'LOSS', 'CANCELLED', 'EXPIRED']);
const WARNING = new Set(['TIME_LIMIT_EXCEEDED', 'MEMORY_LIMIT_EXCEEDED', 'OUTPUT_LIMIT_EXCEEDED', 'COMPILATION_ERROR', 'WAITING']);
const INFO = new Set(['QUEUED', 'JUDGING', 'CREATED', 'IN_PROGRESS', 'READY', 'MATCHMAKING', 'DRAW', 'NEW']);

export function StatusBadge({ status }: { status: Status }) {
  const label = status ?? 'UNKNOWN';
  let tone = 'muted';
  if (SUCCESS.has(label)) tone = 'success';
  if (DANGER.has(label)) tone = 'danger';
  if (WARNING.has(label)) tone = 'warning';
  if (INFO.has(label)) tone = 'info';
  return <span className={`badge badge--${tone}`}>{label}</span>;
}
