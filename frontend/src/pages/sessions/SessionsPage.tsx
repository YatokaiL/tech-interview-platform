import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Loader2, Plus } from "lucide-react";
import { api } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import type { PageResponse, Session, SessionStatus } from "@/types/api";
import { STATUS_BADGE_VARIANT, STATUS_LABELS } from "@/lib/roles";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

type StatusFilter = SessionStatus | "ALL";

const PAGE_SIZE = 20;

export function SessionsPage() {
  const navigate = useNavigate();
  const role = useAuthStore((s) => s.user?.role);
  const canCreate = role === "INTERVIEWER" || role === "HR_MANAGER" || role === "ADMIN";

  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<StatusFilter>("ALL");

  const { data, isFetching } = useQuery({
    queryKey: ["sessions", { page, status }],
    queryFn: async () => {
      const params: Record<string, string | number> = { page, size: PAGE_SIZE };
      if (status !== "ALL") params.status = status;
      return (await api.get<PageResponse<Session>>("/sessions", { params })).data;
    },
  });

  const sessions = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">Сессии</h1>
          <p className="text-muted-foreground mt-1">
            {role === "CANDIDATE"
              ? "Ваши предстоящие и прошедшие интервью."
              : role === "HR_MANAGER"
              ? "Все сессии вашей компании."
              : role === "INTERVIEWER"
              ? "Сессии, в которых вы интервьюер."
              : "Все сессии на платформе."}
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/sessions/new")}>
            <Plus className="h-4 w-4" />
            Создать сессию
          </Button>
        )}
      </div>

      <div className="flex items-end gap-3">
        <div className="w-56 space-y-1.5">
          <label className="text-sm text-muted-foreground">Статус</label>
          <Select
            value={status}
            onValueChange={(v) => {
              setStatus(v as StatusFilter);
              setPage(0);
            }}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Все</SelectItem>
              <SelectItem value="SCHEDULED">Запланированные</SelectItem>
              <SelectItem value="IN_PROGRESS">Идут</SelectItem>
              <SelectItem value="COMPLETED">Завершённые</SelectItem>
              <SelectItem value="CANCELLED">Отменённые</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Название</TableHead>
              <TableHead>Задача</TableHead>
              <TableHead>Язык</TableHead>
              <TableHead>Когда</TableHead>
              <TableHead>Статус</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isFetching && sessions.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8">
                  <Loader2 className="animate-spin h-5 w-5 mx-auto text-muted-foreground" />
                </TableCell>
              </TableRow>
            ) : sessions.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                  Сессий нет
                </TableCell>
              </TableRow>
            ) : (
              sessions.map((s) => (
                <TableRow
                  key={s.id}
                  className="cursor-pointer"
                  onClick={() => navigate(`/sessions/${s.id}`)}
                >
                  <TableCell className="font-medium">
                    <Link to={`/sessions/${s.id}`} className="hover:underline">
                      {s.title}
                    </Link>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {s.taskTitle ?? "—"}
                  </TableCell>
                  <TableCell>
                    <Badge variant="secondary" className="font-normal">{s.language}</Badge>
                  </TableCell>
                  <TableCell>{new Date(s.scheduledAt).toLocaleString("ru-RU")}</TableCell>
                  <TableCell>
                    <Badge variant={STATUS_BADGE_VARIANT[s.status]}>
                      {STATUS_LABELS[s.status]}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <div className="flex items-center justify-between text-sm">
        <div className="text-muted-foreground">Всего: {data?.totalElements ?? 0}</div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Назад
          </Button>
          <span className="px-2">
            {page + 1} / {Math.max(totalPages, 1)}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={data?.last ?? true}
            onClick={() => setPage((p) => p + 1)}
          >
            Вперёд
          </Button>
        </div>
      </div>
    </div>
  );
}
