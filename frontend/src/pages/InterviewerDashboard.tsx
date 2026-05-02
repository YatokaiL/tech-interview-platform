import { PlaceholderDashboard } from "@/pages/PlaceholderDashboard";

export function InterviewerDashboard() {
  return (
    <PlaceholderDashboard
      title="Кабинет интервьюера"
      description="Создавайте сессии, ведите собеседования и оставляйте код-ревью."
      upcoming={[
        "Управление библиотекой задач",
        "Создание и проведение сессий собеседования",
        "Код-ревью с комментариями к строкам и оценками",
      ]}
    />
  );
}
