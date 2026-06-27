// Shared labels and formatters for the admin analytics + audit-log pages.

export const PIE_COLORS = [
  "#2563eb",
  "#16a34a",
  "#f59e0b",
  "#dc2626",
  "#7c3aed",
  "#0891b2",
  "#db2777",
];

export const VERDICT_LABEL: Record<string, string> = {
  HIRE: "Нанимать",
  NO_HIRE: "Отказать",
  MAYBE: "Под вопросом",
};

export const EVENT_LABEL: Record<string, string> = {
  "user-registered": "Регистрация",
  "session-created": "Сессия создана",
  "session-completed": "Сессия завершена",
  "code-executed": "Запуск кода",
  "analysis-completed": "Анализ кода",
  "review-completed": "Ревью готово",
  "payment-received": "Оплата",
  "notification-requested": "Уведомление",
  "analytics-event": "Событие",
};

export function formatPrice(cents: number) {
  return `${(cents / 100).toLocaleString("ru-RU")} ₽`;
}

export function shortId(id: string | null) {
  return id ? id.slice(0, 8) : "—";
}
