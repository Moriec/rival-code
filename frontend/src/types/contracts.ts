export type UserRole = 'USER' | 'ADMIN';

export type ProblemDifficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type ProblemStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type CheckerType = 'STANDARD' | 'CUSTOM';

export type ProgrammingLanguage = 'JAVA' | 'CPP' | 'PYTHON' | 'RUST' | 'RUBY' | 'JAVASCRIPT';
export type SupportedUiLanguage = Exclude<ProgrammingLanguage, 'RUBY'>;
export type SubmissionMode = 'DUEL' | 'PRACTICE';
export type SubmissionStatus = 'CREATED' | 'QUEUED' | 'JUDGING' | 'JUDGED' | 'FAILED' | 'CANCELLED';
export type JudgeStatus =
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'TIME_LIMIT_EXCEEDED'
  | 'MEMORY_LIMIT_EXCEEDED'
  | 'OUTPUT_LIMIT_EXCEEDED'
  | 'RUNTIME_ERROR'
  | 'COMPILATION_ERROR'
  | 'SYSTEM_ERROR';

export type DuelMode = 'RATED' | 'UNRATED' | 'PRACTICE';
export type DuelStatus = 'MATCHMAKING' | 'READY' | 'IN_PROGRESS' | 'FINISHED' | 'CANCELLED' | 'EXPIRED';
export type DuelOutcome = 'WIN' | 'LOSS' | 'DRAW' | 'CANCELLED';
export type MatchmakingTicketStatus = 'WAITING' | 'MATCHED' | 'CANCELLED' | 'EXPIRED';

export type NotificationType =
  | 'MATCH_FOUND'
  | 'DUEL_FINISHED'
  | 'RATING_CHANGED'
  | 'PRACTICE_JUDGED'
  | 'PROFILE_UPDATED'
  | 'SEASON_STARTED'
  | 'SYSTEM';
export type NotificationStatus = 'NEW' | 'READ' | 'ARCHIVED';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first?: boolean;
  last?: boolean;
}

export interface AvatarDto {
  avatarId: string;
  objectKey: string;
  url: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface AvatarUploadResponse extends AvatarDto {
  userId: string;
  active?: boolean;
}

export interface UserProfileDto {
  userId: string;
  email?: string;
  username: string;
  displayName?: string;
  avatar?: AvatarDto;
  roles?: UserRole[];
  enabled?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  displayName?: string;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface AuthTokensResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresInSeconds?: number;
  user?: UserProfileDto;
}

export interface UpdateProfileRequest {
  displayName?: string;
  avatarId?: string;
}

export interface TagDto {
  tagId: string;
  name: string;
  color?: string;
}

export interface ProblemLimitsDto {
  timeLimitMs?: number;
  memoryLimitKb?: number;
  outputLimitBytes?: number;
}

export interface ProblemExampleDto {
  orderNo: number;
  input: string;
  expectedOutput: string;
  explanation?: string;
}

export interface ProblemSummaryDto {
  problemId: string;
  slug: string;
  title: string;
  difficulty: ProblemDifficulty;
  status: ProblemStatus;
  tags?: TagDto[];
  acceptedCount?: number;
  attemptsCount?: number;
  publishedAt?: string;
}

export interface ProblemDetailsDto extends ProblemSummaryDto {
  problemVersionId: string;
  statement: string;
  inputSpec?: string;
  outputSpec?: string;
  checkerType?: CheckerType;
  limits?: ProblemLimitsDto;
  examples?: ProblemExampleDto[];
  updatedAt?: string;
}

export interface ProblemFilterRequest {
  search?: string;
  difficulties?: ProblemDifficulty[];
  statuses?: ProblemStatus[];
  tagIds?: string[];
  solvedByUser?: boolean;
  userId?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export interface UserCode {
  sourceCode: string;
  language: SupportedUiLanguage;
}

export interface CreateSubmissionRequest {
  userId?: string;
  problemId: string;
  problemVersionId?: string;
  duelId?: string;
  mode: SubmissionMode;
  userCode: UserCode;
}

export interface SubmissionCreatedResponse {
  submissionId: string;
  status: SubmissionStatus;
  createdAt: string;
}

export interface SubmissionSummaryDto {
  submissionId: string;
  userId: string;
  problemId: string;
  problemVersionId?: string;
  duelId?: string;
  mode: SubmissionMode;
  language: ProgrammingLanguage;
  status: SubmissionStatus;
  overallStatus?: JudgeStatus;
  accepted?: boolean;
  maxTimeMs?: number;
  maxMemoryKb?: number;
  createdAt?: string;
  judgedAt?: string;
}

export interface PublicTestCaseResult {
  orderNo: number;
  status: JudgeStatus;
  timeMs?: number;
  memoryKb?: number;
  message?: string;
}

export interface PublicSubmissionVerdict {
  submissionId: string;
  overallStatus?: JudgeStatus;
  maxTimeMs?: number;
  maxMemoryKb?: number;
  passedTests?: number;
  totalTests?: number;
  compilationError?: string;
  visibleTests?: PublicTestCaseResult[];
}

export interface CreateMatchmakingTicketRequest {
  userId?: string;
  presetId?: string;
  mode: DuelMode;
  difficulty: ProblemDifficulty;
  currentRating?: number;
}

export interface MatchmakingTicketDto {
  ticketId: string;
  userId: string;
  presetId?: string;
  matchedDuelId?: string;
  mode: DuelMode;
  difficulty?: ProblemDifficulty;
  status: MatchmakingTicketStatus;
  currentRating?: number;
  createdAt?: string;
  expiresAt?: string;
}

export interface DuelParticipantDto {
  userId: string;
  username?: string;
  displayName?: string;
  avatarUrl?: string;
  ratingBefore?: number;
  ratingAfter?: number;
  outcome?: DuelOutcome;
  acceptedSubmissionId?: string;
}

export interface DuelRoomStateDto {
  duelId: string;
  status: DuelStatus;
  mode: DuelMode;
  problemId: string;
  problemVersionId?: string;
  problemTitle?: string;
  problemDifficulty?: ProblemDifficulty;
  startedAt?: string;
  endsAt?: string;
  serverTime?: string;
  participants?: DuelParticipantDto[];
}

export interface CodeSnapshotMessage {
  duelId: string;
  userId: string;
  language: SupportedUiLanguage;
  sourceCode: string;
  version: number;
  sentAt: string;
}

export interface DuelFinishedEvent {
  duelId: string;
  mode: DuelMode;
  winnerUserId?: string;
  problemId: string;
  problemVersionId?: string;
  participants?: DuelParticipantDto[];
  finishedAt?: string;
}

export interface LeaderboardEntryDto {
  userId: string;
  username?: string;
  avatarUrl?: string;
  rating: number;
  rank: number;
  wins: number;
  losses: number;
  draws: number;
}

export interface QueuePresetDto {
  presetId: string;
  name: string;
  mode: DuelMode;
  duelDurationSeconds?: number;
  initialRatingWindow?: number;
  maxRatingWindow?: number;
  active?: boolean;
}

export interface RecentDuelDto {
  duelId: string;
  opponentUserId?: string;
  opponentUsername?: string;
  problemId?: string;
  problemTitle?: string;
  problemDifficulty?: ProblemDifficulty;
  mode?: DuelMode;
  outcome?: DuelOutcome;
  ratingDelta?: number;
  finishedAt?: string;
}

export interface UserDuelProfileDto {
  userId: string;
  username?: string;
  displayName?: string;
  avatarUrl?: string;
  rating?: number;
  rank?: number;
  wins?: number;
  losses?: number;
  draws?: number;
  solvedProblems?: number;
  recentDuels?: RecentDuelDto[];
}

export interface NotificationDto {
  notificationId: string;
  userId: string;
  type: NotificationType;
  status: NotificationStatus;
  title: string;
  body?: string;
  payload?: Record<string, unknown>;
  createdAt?: string;
  readAt?: string;
}

export interface MarkNotificationsReadRequest {
  userId: string;
  notificationIds: string[];
  readAt?: string;
}

export const UI_LANGUAGES: SupportedUiLanguage[] = ['JAVA', 'CPP', 'PYTHON', 'RUST', 'JAVASCRIPT'];
