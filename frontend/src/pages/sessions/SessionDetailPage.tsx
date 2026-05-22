import { useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Ban, CheckCircle2, Loader2, Play, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { isAxiosError } from "axios";
import { api } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import type { ApiError, Session } from "@/types/api";
import { STATUS_BADGE_VARIANT, STATUS_LABELS } from "@/lib/roles";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { CollabRoom, type CollabRoomHandle } from "@/components/CollabRoom";

function errorMessage(err: unknown, fallback: string) {
  return isAxiosError<ApiError>(err) && err.response?.data?.message
    ? err.response.data.message
    : fallback;
}

export function SessionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const collabRef = useRef<CollabRoomHandle | null>(null);

  const { data: session, isLoading, isError } = useQuery({
    queryKey: ["session", id],
    queryFn: async () => (await api.get<Session>(`/sessions/${id}`)).data,
    enabled: !!id,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["session", id] });
    queryClient.invalidateQueries({ queryKey: ["sessions"] });
  };

  const startMutation = useMutation({
    mutationFn: async () => (await api.post<Session>(`/sessions/${id}/start`)).data,
    onSuccess: () => {
      toast.success("Сессия начата");
      invalidate();
    },
    onError: (err) => toast.error(errorMessage(err, "Не удалось начать сессию")),
  });

  const completeMutation = useMutation({
    mutationFn: async () => {
      try {
        await collabRef.current?.flushSnapshot("FINAL");
      } catch {
        // best-effort; complete anyway
      }
      return (await api.post<Session>(`/sessions/${id}/complete`)).data;
    },
    onSuccess: () => {
      toast.success("Сессия завершена");
      invalidate();
    },
    onError: (err) => toast.error(errorMessage(err, "Не удалось завершить сессию")),
  });

  const cancelMutation = useMutation({
    mutationFn: async () => (await api.post<Session>(`/sessions/${id}/cancel`)).data,
    onSuccess: () => {
      toast.success("Сессия отменена");
      invalidate();
    },
    onError: (err) => toast.error(errorMessage(err, "Не удалось отменить сессию")),
  });

  const deleteMutation = useMutation({
    mutationFn: async () => api.delete(`/sessions/${id}`),
    onSuccess: () => {
      toast.success("Сессия удалена");
      queryClient.invalidateQueries({ queryKey: ["sessions"] });
      navigate("/sessions", { replace: true });
    },
    onError: (err) => toast.error(errorMessage(err, "Не удалось удалить сессию")),
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Loader2 className="animate-spin h-6 w-6 text-muted-foreground" />
      </div>
    );
  }
  if (isError || !session) {
    return <p className="text-destructive">Не удалось загрузить сессию.</p>;
  }

  const isInterviewer =
    user?.role === "INTERVIEWER" && user.id === session.interviewerId;
  const isAdmin = user?.role === "ADMIN";
  const isHr = user?.role === "HR_MANAGER";
  const canManage = isInterviewer || isAdmin || isHr;

  const showCollab = session.status !== "CANCELLED";

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">{session.title}</h1>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <Badge variant={STATUS_BADGE_VARIANT[session.status]}>
              {STATUS_LABELS[session.status]}
            </Badge>
            <Badge variant="secondary" className="font-normal">{session.language}</Badge>
            {session.taskTitle && (
              <Badge variant="outline" className="font-normal">
                Задача: {session.taskTitle}
              </Badge>
            )}
          </div>
        </div>
        {canManage && (
          <div className="flex flex-wrap gap-2">
            {(isInterviewer || isAdmin) && session.status === "SCHEDULED" && (
              <Button onClick={() => startMutation.mutate()} disabled={startMutation.isPending}>
                <Play className="h-4 w-4" />
                Начать
              </Button>
            )}
            {(isInterviewer || isAdmin) && session.status === "IN_PROGRESS" && (
              <Button
                onClick={() => completeMutation.mutate()}
                disabled={completeMutation.isPending}
              >
                <CheckCircle2 className="h-4 w-4" />
                Завершить
              </Button>
            )}
            {session.status !== "COMPLETED" && session.status !== "CANCELLED" && (
              <Button
                variant="outline"
                onClick={() => cancelMutation.mutate()}
                disabled={cancelMutation.isPending}
              >
                <Ban className="h-4 w-4" />
                Отменить
              </Button>
            )}
            <Button
              variant="destructive"
              onClick={() => {
                if (confirm("Удалить сессию?")) deleteMutation.mutate();
              }}
              disabled={deleteMutation.isPending}
            >
              <Trash2 className="h-4 w-4" />
              Удалить
            </Button>
          </div>
        )}
      </div>

      {showCollab && <CollabRoom ref={collabRef} session={session} />}

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Параметры сессии</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <Row label="Начало" value={new Date(session.scheduledAt).toLocaleString("ru-RU")} />
          {session.startedAt && (
            <Row label="Начата" value={new Date(session.startedAt).toLocaleString("ru-RU")} />
          )}
          {session.completedAt && (
            <Row label="Завершена" value={new Date(session.completedAt).toLocaleString("ru-RU")} />
          )}
          <Row label="Интервьюер" value={session.interviewerId} mono />
          <Row label="Кандидат" value={session.candidateId ?? "—"} mono />
          {session.taskId && <Row label="Задача" value={session.taskId} mono />}
        </CardContent>
      </Card>
    </div>
  );
}

function Row({ label, value, mono }: { label: string; value: React.ReactNode; mono?: boolean }) {
  return (
    <div className="flex items-center justify-between border-b last:border-0 pb-2 last:pb-0">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className={mono ? "text-xs font-mono" : "text-sm"}>{value}</span>
    </div>
  );
}
