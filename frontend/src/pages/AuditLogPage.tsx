import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Loader2, RefreshCw } from "lucide-react";
import { api } from "@/lib/api";
import type { AnalyticsAuditEntry } from "@/types/api";
import { EVENT_LABEL, VERDICT_LABEL, formatPrice, shortId } from "@/lib/analytics";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

const LIMITS = [50, 100, 200];

export function AuditLogPage() {
  const [limit, setLimit] = useState(50);

  const { data = [], isLoading, isFetching, refetch } = useQuery({
    queryKey: ["analytics-audit", limit],
    queryFn: async () =>
      (await api.get<AnalyticsAuditEntry[]>("/analytics/audit", { params: { limit } })).data,
    refetchInterval: 30_000,
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">Журнал событий</h1>
          <p className="text-muted-foreground mt-1">
            Аудит-лог всех событий платформы из общего потока Kafka. Свежие — сверху.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex rounded-md border">
            {LIMITS.map((n) => (
              <button
                key={n}
                onClick={() => setLimit(n)}
                className={`px-3 py-1.5 text-sm transition-colors first:rounded-l-md last:rounded-r-md ${
                  limit === n ? "bg-secondary text-secondary-foreground" : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {n}
              </button>
            ))}
          </div>
          <Button variant="outline" size="icon" onClick={() => refetch()} disabled={isFetching}>
            <RefreshCw className={`h-4 w-4 ${isFetching ? "animate-spin" : ""}`} />
          </Button>
        </div>
      </div>

      <Card>
        <CardContent className="pt-6">
          {isLoading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="animate-spin h-6 w-6 text-muted-foreground" />
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-left text-muted-foreground">
                    <th className="py-2 pr-4 font-medium">Время</th>
                    <th className="py-2 pr-4 font-medium">Событие</th>
                    <th className="py-2 pr-4 font-medium">Компания</th>
                    <th className="py-2 pr-4 font-medium">Пользователь</th>
                    <th className="py-2 pr-4 font-medium">Язык</th>
                    <th className="py-2 pr-4 font-medium">Вердикт</th>
                    <th className="py-2 pr-4 font-medium">Сумма</th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((e) => (
                    <tr key={e.id} className="border-b last:border-0">
                      <td className="py-2 pr-4 whitespace-nowrap text-muted-foreground">
                        {new Date(e.occurredAt).toLocaleString("ru-RU")}
                      </td>
                      <td className="py-2 pr-4">{EVENT_LABEL[e.eventType] ?? e.eventType}</td>
                      <td className="py-2 pr-4 font-mono text-xs text-muted-foreground">{shortId(e.companyId)}</td>
                      <td className="py-2 pr-4 font-mono text-xs text-muted-foreground">{shortId(e.userId)}</td>
                      <td className="py-2 pr-4">{e.language ?? "—"}</td>
                      <td className="py-2 pr-4">
                        {e.verdict ? VERDICT_LABEL[e.verdict] ?? e.verdict : "—"}
                      </td>
                      <td className="py-2 pr-4">
                        {e.valueCents != null ? formatPrice(e.valueCents) : "—"}
                      </td>
                    </tr>
                  ))}
                  {data.length === 0 && (
                    <tr>
                      <td colSpan={7} className="py-6 text-center text-muted-foreground">
                        Событий пока нет
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
