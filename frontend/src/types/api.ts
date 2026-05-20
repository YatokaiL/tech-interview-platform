export type Role = "CANDIDATE" | "INTERVIEWER" | "HR_MANAGER" | "ADMIN";

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

export interface User {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  role: Role;
  companyId: string | null;
  companyName?: string | null;
  blocked: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  role: "CANDIDATE" | "HR_MANAGER";
  companyName?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  role?: Role;
  blocked?: boolean;
}

export interface ApiError {
  error: string;
  message: string;
  violations?: { field: string; message: string }[];
  timestamp: string;
}

// ─── Tasks ─────────────────────────────────────────────────────────
export type Difficulty = "EASY" | "MEDIUM" | "HARD";

export interface TaskCategory {
  id: string;
  name: string;
  description?: string | null;
}

export interface Task {
  id: string;
  title: string;
  description: string;
  difficulty: Difficulty;
  language: string | null;
  initialCode: string | null;
  expectedOutput: string | null;
  timeLimitMinutes: number | null;
  companyId: string | null;
  createdBy: string;
  categories: TaskCategory[];
  createdAt: string;
  updatedAt: string;
}

export interface TaskRequest {
  title: string;
  description: string;
  difficulty: Difficulty;
  language?: string | null;
  initialCode?: string | null;
  expectedOutput?: string | null;
  timeLimitMinutes?: number | null;
  categoryIds?: string[];
}

// ─── Sessions ──────────────────────────────────────────────────────
export type SessionStatus = "SCHEDULED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export interface Session {
  id: string;
  title: string;
  companyId: string;
  interviewerId: string;
  candidateId: string | null;
  taskId: string | null;
  taskTitle: string | null;
  language: string;
  status: SessionStatus;
  scheduledAt: string;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SessionRequest {
  title: string;
  candidateId: string;
  interviewerId?: string | null;
  taskId?: string | null;
  language: string;
  scheduledAt: string;
}

// ─── Code snapshots ────────────────────────────────────────────────
export type SnapshotType = "AUTO" | "MANUAL" | "FINAL";

export interface CodeSnapshot {
  id: string;
  sessionId: string;
  language: string;
  code: string;
  snapshotType: SnapshotType;
  createdAt: string;
}

export interface CodeSnapshotRequest {
  language: string;
  code: string;
  snapshotType?: SnapshotType;
}

// ─── User lookup (for selecting participants) ──────────────────────
export interface UserLookup {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  role: Role;
}
