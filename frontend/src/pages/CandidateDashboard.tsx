import { PlaceholderDashboard } from "@/pages/PlaceholderDashboard";

export function CandidateDashboard() {
  return (
    <PlaceholderDashboard
      title="Кабинет кандидата"
      description="Здесь вы будете видеть свои предстоящие интервью и их результаты."
      upcoming={[
        "Список приглашений на сессии",
        "Подключение к сессии в Monaco-редакторе",
        "Просмотр итогового отчёта после ревью",
      ]}
    />
  );
}
