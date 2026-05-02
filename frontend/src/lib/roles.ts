import type { Role } from "@/types/api";

export const ROLE_LABELS: Record<Role, string> = {
  CANDIDATE: "Кандидат",
  INTERVIEWER: "Интервьюер",
  HR_MANAGER: "HR-менеджер",
  ADMIN: "Администратор",
};

export const ROLE_HOMES: Record<Role, string> = {
  CANDIDATE: "/candidate",
  INTERVIEWER: "/interviewer",
  HR_MANAGER: "/hr",
  ADMIN: "/admin",
};

export function initials(firstName?: string | null, lastName?: string | null, email?: string) {
  const f = firstName?.[0] ?? "";
  const l = lastName?.[0] ?? "";
  const combined = (f + l).toUpperCase();
  if (combined) return combined;
  return email?.[0]?.toUpperCase() ?? "?";
}
