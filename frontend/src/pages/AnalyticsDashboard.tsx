import { useQuery } from "@tanstack/react-query";
import {
  Activity,
  Users,
  CalendarPlus,
  CheckCircle2,
  Play,
  ClipboardCheck,
  CreditCard,
  Loader2,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { api } from "@/lib/api";
import type {
  AnalyticsDailyPoint,
  AnalyticsOverview,
  AnalyticsSlice,
} from "@/types/api";
import { PIE_COLORS, VERDICT_LABEL, formatPrice } from "@/lib/analytics";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function AnalyticsDashboard() {
  const overview = useQuery({
    queryKey: ["analytics-overview"],
    queryFn: async () => (await api.get<AnalyticsOverview>("/analytics/overview")).data,
    refetchInterval: 30_000,
  });

  const daily = useQuery({
    queryKey: ["analytics-daily"],
    queryFn: async () =>
      (await api.get<AnalyticsDailyPoint[]>("/analytics/daily", { params: { days: 14 } })).data,
    refetchInterval: 30_000,
  });

  const verdicts = useQuery({
    queryKey: ["analytics-verdicts"],
    queryFn: async () => (await api.get<AnalyticsSlice[]>("/analytics/verdicts")).data,
  });

  const languages = useQuery({
    queryKey: ["analytics-languages"],
    queryFn: async () => (await api.get<AnalyticsSlice[]>("/analytics/languages")).data,
  });

  if (overview.isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Loader2 className="animate-spin h-6 w-6 text-muted-foreground" />
      </div>
    );
  }

  const o = overview.data;
  const kpis = o
    ? [
        { icon: Activity, label: "Всего событий", value: o.totalEvents.toLocaleString("ru-RU") },
        { icon: Users, label: "Регистраций", value: o.usersRegistered.toLocaleString("ru-RU") },
        { icon: CalendarPlus, label: "Сессий создано", value: o.sessionsCreated.toLocaleString("ru-RU") },
        { icon: CheckCircle2, label: "Сессий завершено", value: o.sessionsCompleted.toLocaleString("ru-RU") },
        { icon: Play, label: "Запусков кода", value: o.codeExecutions.toLocaleString("ru-RU") },
        { icon: ClipboardCheck, label: "Ревью готово", value: o.reviewsCompleted.toLocaleString("ru-RU") },
        { icon: CreditCard, label: "Оплат", value: o.paymentsReceived.toLocaleString("ru-RU") },
        { icon: CreditCard, label: "Выручка", value: formatPrice(o.revenueCents) },
      ]
    : [];

  const verdictData = (verdicts.data ?? []).map((s) => ({
    name: VERDICT_LABEL[s.label] ?? s.label,
    value: s.count,
  }));
  const languageData = (languages.data ?? []).map((s) => ({
    name: s.label,
    value: s.count,
  }));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Аналитика</h1>
        <p className="text-muted-foreground mt-1">
          KPI платформы, динамика активности и журнал событий. Обновляется автоматически.
        </p>
      </div>

      {/* KPI tiles */}
      <div className="grid gap-4 grid-cols-2 lg:grid-cols-4">
        {kpis.map((k) => {
          const Icon = k.icon;
          return (
            <Card key={k.label}>
              <CardContent className="flex items-center gap-3 py-4">
                <div className="rounded-md bg-secondary p-2">
                  <Icon className="h-5 w-5 text-secondary-foreground" />
                </div>
                <div>
                  <div className="text-2xl font-semibold leading-none">{k.value}</div>
                  <div className="text-xs text-muted-foreground mt-1">{k.label}</div>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Daily activity line chart */}
      <Card>
        <CardHeader className="py-4">
          <CardTitle className="text-lg">Активность за 14 дней</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={daily.data ?? []} margin={{ top: 8, right: 16, left: -16, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis dataKey="day" fontSize={12} tickFormatter={(d: string) => d.slice(5)} />
                <YAxis allowDecimals={false} fontSize={12} />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="sessions" name="Сессии" stroke="#2563eb" strokeWidth={2} />
                <Line type="monotone" dataKey="reviews" name="Ревью" stroke="#16a34a" strokeWidth={2} />
                <Line type="monotone" dataKey="executions" name="Запуски" stroke="#f59e0b" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </CardContent>
      </Card>

      {/* Breakdowns */}
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader className="py-4">
            <CardTitle className="text-lg">Вердикты по ревью</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64">
              {verdictData.length === 0 ? (
                <EmptyChart />
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie data={verdictData} dataKey="value" nameKey="name" outerRadius={90} label>
                      {verdictData.map((_, i) => (
                        <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="py-4">
            <CardTitle className="text-lg">Языки сессий</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64">
              {languageData.length === 0 ? (
                <EmptyChart />
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={languageData} margin={{ top: 8, right: 16, left: -16, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                    <XAxis dataKey="name" fontSize={12} />
                    <YAxis allowDecimals={false} fontSize={12} />
                    <Tooltip />
                    <Bar dataKey="value" name="Сессий" fill="#2563eb" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function EmptyChart() {
  return (
    <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
      Нет данных
    </div>
  );
}
