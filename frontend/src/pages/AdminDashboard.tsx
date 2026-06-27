import { Link } from "react-router-dom";
import { Users, BarChart3, ShieldCheck } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

const TILES = [
  {
    to: "/admin/users",
    icon: Users,
    title: "Пользователи",
    description: "Список всех пользователей, фильтры, смена ролей и блокировка.",
    enabled: true,
  },
  {
    to: "/admin/analytics",
    icon: BarChart3,
    title: "Аналитика",
    description: "KPI платформы, динамика активности и распределения по вердиктам и языкам.",
    enabled: true,
  },
  {
    to: "/admin/audit",
    icon: ShieldCheck,
    title: "Журнал событий",
    description: "Аудит-лог всех событий платформы из общего потока Kafka.",
    enabled: true,
  },
];

export function AdminDashboard() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Администрирование</h1>
        <p className="text-muted-foreground mt-1">
          Управление платформой технических интервью.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {TILES.map((tile) => {
          const Icon = tile.icon;
          const card = (
            <Card
              className={`h-full transition-colors ${
                tile.enabled ? "hover:bg-accent" : "opacity-60"
              }`}
            >
              <CardHeader>
                <Icon className="h-8 w-8 mb-2 text-muted-foreground" />
                <CardTitle className="text-xl">{tile.title}</CardTitle>
                <CardDescription>{tile.description}</CardDescription>
              </CardHeader>
              <CardContent>
                {!tile.enabled && (
                  <span className="text-xs text-muted-foreground">скоро</span>
                )}
              </CardContent>
            </Card>
          );
          return tile.enabled ? (
            <Link key={tile.title} to={tile.to}>
              {card}
            </Link>
          ) : (
            <div key={tile.title}>{card}</div>
          );
        })}
      </div>
    </div>
  );
}
