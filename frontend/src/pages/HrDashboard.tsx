import { PlaceholderDashboard } from "@/pages/PlaceholderDashboard";

export function HrDashboard() {
  return (
    <PlaceholderDashboard
      title="Кабинет HR-менеджера"
      description="Управляйте сессиями компании и подпиской на платформу."
      upcoming={[
        "Управление сессиями компании",
        "Выбор тарифа и оплата (Старт / Команда / Бизнес / Корпорация)",
        "Просмотр отчётов по проведённым собеседованиям",
      ]}
    />
  );
}
