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

export interface SessionEvent {
  id: string;
  sessionId: string;
  eventType: string;
  payload: unknown;
  userId: string | null;
  createdAt: string;
}

// ─── Code execution ────────────────────────────────────────────────
export type ExecutionStatus =
  | "PENDING"
  | "RUNNING"
  | "SUCCESS"
  | "FAILURE"
  | "TIMEOUT"
  | "ERROR";

export interface CodeExecution {
  id: string;
  sessionId: string;
  userId: string;
  language: string;
  status: ExecutionStatus;
  stdout: string | null;
  stderr: string | null;
  exitCode: number | null;
  durationMs: number | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ExecutionRequest {
  sessionId: string;
  language: string;
  code: string;
  stdin?: string;
}

// ─── Code analysis ─────────────────────────────────────────────────
export type AnalysisStatus = "PENDING" | "RUNNING" | "COMPLETED" | "ERROR";

export interface AnalysisIssue {
  tool: string;
  severity: string;
  line: number | null;
  rule: string | null;
  message: string;
}

export interface CodeAnalysis {
  id: string;
  sessionId: string;
  userId: string;
  language: string;
  status: AnalysisStatus;
  linesOfCode: number | null;
  cyclomaticComplexity: number | null;
  cognitiveComplexity: number | null;
  methodCount: number | null;
  timeComplexity: string | null;
  issues: AnalysisIssue[] | null;
  durationMs: number | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AnalysisRequest {
  sessionId: string;
  language: string;
  code: string;
}

// ─── Review ────────────────────────────────────────────────────────
export type ReviewStatus = "DRAFT" | "SUBMITTED";
export type Verdict = "HIRE" | "NO_HIRE" | "MAYBE";

export interface ReviewComment {
  id: string;
  reviewId: string;
  authorId: string;
  lineNumber: number | null;
  text: string;
  createdAt: string;
}

export interface Review {
  id: string;
  sessionId: string;
  reviewerId: string;
  candidateId: string | null;
  companyId: string;
  scoreCorrectness: number | null;
  scoreCodeQuality: number | null;
  scorePerformance: number | null;
  scoreCommunication: number | null;
  scoreAverage: number | null;
  verdict: Verdict | null;
  summary: string | null;
  status: ReviewStatus;
  submittedAt: string | null;
  createdAt: string;
  updatedAt: string;
  comments: ReviewComment[];
}

export interface ReviewRequest {
  sessionId: string;
  scoreCorrectness?: number | null;
  scoreCodeQuality?: number | null;
  scorePerformance?: number | null;
  scoreCommunication?: number | null;
  verdict?: Verdict | null;
  summary?: string | null;
}

export interface ReviewUpdateRequest {
  scoreCorrectness?: number | null;
  scoreCodeQuality?: number | null;
  scorePerformance?: number | null;
  scoreCommunication?: number | null;
  verdict?: Verdict | null;
  summary?: string | null;
}

export interface CommentRequest {
  lineNumber?: number | null;
  text: string;
}

// ─── Reports ───────────────────────────────────────────────────────
export type ReportFormat = "PDF" | "JSON";

export interface Report {
  id: string;
  sessionId: string;
  reviewId: string | null;
  format: ReportFormat;
  sizeBytes: number | null;
  generatedBy: string;
  createdAt: string;
}

export interface ReportRequest {
  reviewId: string;
  format: ReportFormat;
}

// ─── Subscriptions ─────────────────────────────────────────────────
export type SubscriptionStatus = "PENDING" | "ACTIVE" | "EXPIRED" | "CANCELLED";
export type PaymentStatus = "PENDING" | "SUCCEEDED" | "FAILED" | "REFUNDED";

export interface Tariff {
  id: string;
  code: string;
  name: string;
  description: string | null;
  priceCents: number;
  currency: string;
  maxSessionsPerMonth: number | null;
  features: string[];
  sortOrder: number;
  active: boolean;
}

export interface Payment {
  id: string;
  subscriptionId: string;
  amountCents: number;
  currency: string;
  status: PaymentStatus;
  gatewayRef: string | null;
  paidAt: string | null;
  createdAt: string;
}

export interface Subscription {
  id: string;
  companyId: string;
  tariffId: string;
  tariff: Tariff | null;
  status: SubscriptionStatus;
  periodMonths: number;
  startedAt: string | null;
  endsAt: string | null;
  autoRenew: boolean;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  payments: Payment[];
}

export interface SubscriptionRequest {
  tariffId: string;
  periodMonths?: number;
  autoRenew?: boolean;
}

// ─── Notifications ─────────────────────────────────────────────────
export interface AppNotification {
  id: string;
  type: string;
  title: string;
  message: string;
  link: string | null;
  read: boolean;
  createdAt: string;
}

// ─── User lookup (for selecting participants) ──────────────────────
export interface UserLookup {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  role: Role;
}
