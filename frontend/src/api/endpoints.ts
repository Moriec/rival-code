import { apiRequest } from './client';
import type {
  AvatarUploadResponse,
  AuthTokensResponse,
  CreateMatchmakingTicketRequest,
  CreateSubmissionRequest,
  LeaderboardEntryDto,
  LoginRequest,
  MarkNotificationsReadRequest,
  MatchmakingTicketDto,
  NotificationDto,
  Page,
  ProblemDetailsDto,
  ProblemFilterRequest,
  ProblemSummaryDto,
  PublicSubmissionVerdict,
  QueuePresetDto,
  RegisterRequest,
  SubmissionCreatedResponse,
  SubmissionSummaryDto,
  TagDto,
  UpdateProfileRequest,
  UserDuelProfileDto,
  UserProfileDto,
  DuelRoomStateDto
} from '../types/contracts';

export const authApi = {
  login: (request: LoginRequest) =>
    apiRequest<AuthTokensResponse>('/api/auth/login', { method: 'POST', body: request, auth: false }),
  register: (request: RegisterRequest) =>
    apiRequest<AuthTokensResponse>('/api/auth/register', { method: 'POST', body: request, auth: false }),
  me: () => apiRequest<UserProfileDto>('/api/users/me'),
  updateMe: (request: UpdateProfileRequest) =>
    apiRequest<UserProfileDto>('/api/users/me', { method: 'PATCH', body: request }),
  uploadAvatar: (file: File) => {
    const body = new FormData();
    body.append('file', file);
    return apiRequest<AvatarUploadResponse>('/api/users/me/avatar', { method: 'POST', body });
  },
  getUser: (userId: string) => apiRequest<UserProfileDto>(`/api/users/${userId}/profile`, { auth: false })
};

export const problemApi = {
  list: (filter: ProblemFilterRequest) =>
    apiRequest<Page<ProblemSummaryDto> | ProblemSummaryDto[]>('/api/problems', { query: filter, auth: false }),
  get: (problemId: string) => apiRequest<ProblemDetailsDto>(`/api/problems/${problemId}`, { auth: false }),
  tags: () => apiRequest<TagDto[]>('/api/problems/tags', { auth: false })
};

export const submissionApi = {
  create: (request: CreateSubmissionRequest) =>
    apiRequest<SubmissionCreatedResponse>('/api/submissions', { method: 'POST', body: request }),
  get: (submissionId: string) => apiRequest<SubmissionSummaryDto>(`/api/submissions/${submissionId}`),
  verdict: (submissionId: string) =>
    apiRequest<PublicSubmissionVerdict>(`/api/submissions/${submissionId}/verdict`),
  byUser: (userId: string) => apiRequest<SubmissionSummaryDto[]>(`/api/users/${userId}/submissions`)
};

export const duelApi = {
  createTicket: (request: CreateMatchmakingTicketRequest) =>
    apiRequest<MatchmakingTicketDto>('/api/duels/matchmaking/tickets', { method: 'POST', body: request }),
  getTicket: (ticketId: string) =>
    apiRequest<MatchmakingTicketDto>(`/api/duels/matchmaking/tickets/${ticketId}`),
  cancelTicket: (ticketId: string) =>
    apiRequest<MatchmakingTicketDto>(`/api/duels/matchmaking/tickets/${ticketId}`, { method: 'DELETE' }),
  get: (duelId: string) => apiRequest<DuelRoomStateDto>(`/api/duels/${duelId}`),
  profile: (userId: string) => apiRequest<UserDuelProfileDto>(`/api/duels/profile/${userId}`),
  leaderboard: (seasonId?: string) =>
    apiRequest<LeaderboardEntryDto[]>('/api/duels/leaderboard', { query: { seasonId }, auth: false }),
  queuePresets: () => apiRequest<QueuePresetDto[]>('/api/duels/queue-presets')
};

export const notificationApi = {
  list: (userId?: string) => apiRequest<NotificationDto[]>('/api/notifications', { query: { userId } }),
  markRead: (request: MarkNotificationsReadRequest) =>
    apiRequest<NotificationDto[]>('/api/notifications/read', { method: 'POST', body: request })
};

export function pageContent<T>(response: Page<T> | T[] | undefined): T[] {
  if (!response) {
    return [];
  }
  return Array.isArray(response) ? response : response.content ?? [];
}
