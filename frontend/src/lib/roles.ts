import type { Difficulty, Role, SessionStatus } from "@/types/api";

export const ROLE_LABELS: Record<Role, string> = {
  CANDIDATE: "Кандидат",
  INTERVIEWER: "Интервьюер",
  HR_MANAGER: "HR-менеджер",
  ADMIN: "Администратор",
};

export const ROLE_HOMES: Record<Role, string> = {
  CANDIDATE: "/sessions",
  INTERVIEWER: "/sessions",
  HR_MANAGER: "/sessions",
  ADMIN: "/admin",
};

export const DIFFICULTY_LABELS: Record<Difficulty, string> = {
  EASY: "Лёгкая",
  MEDIUM: "Средняя",
  HARD: "Сложная",
};

export const STATUS_LABELS: Record<SessionStatus, string> = {
  SCHEDULED: "Запланирована",
  IN_PROGRESS: "Идёт",
  COMPLETED: "Завершена",
  CANCELLED: "Отменена",
};

export const STATUS_BADGE_VARIANT: Record<
  SessionStatus,
  "default" | "secondary" | "destructive" | "outline"
> = {
  SCHEDULED: "outline",
  IN_PROGRESS: "default",
  COMPLETED: "secondary",
  CANCELLED: "destructive",
};

export const LANGUAGE_OPTIONS = [
  { value: "java", label: "Java" },
  { value: "python", label: "Python" },
  { value: "javascript", label: "JavaScript" },
  { value: "typescript", label: "TypeScript" },
  { value: "go", label: "Go" },
  { value: "cpp", label: "C++" },
];

export function initials(firstName?: string | null, lastName?: string | null, email?: string) {
  const f = firstName?.[0] ?? "";
  const l = lastName?.[0] ?? "";
  const combined = (f + l).toUpperCase();
  if (combined) return combined;
  return email?.[0]?.toUpperCase() ?? "?";
}

export function formatUserName(user: { firstName?: string | null; lastName?: string | null; email: string }) {
  const name = [user.firstName, user.lastName].filter(Boolean).join(" ").trim();
  return name || user.email;
}
