import { useCallback, useEffect, useRef, useState } from "react";
import { Play, Loader2, AlertCircle, CheckCircle2, Clock, XCircle } from "lucide-react";
import { toast } from "sonner";
import { isAxiosError } from "axios";
import { api } from "@/lib/api";
import type {
  ApiError,
  CodeExecution,
  ExecutionRequest,
  ExecutionStatus,
} from "@/types/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";

interface ExecutionPanelProps {
  sessionId: string;
  language: string;
  getCode: () => string;
  canRun: boolean;
}

const POLL_INTERVAL_MS = 1500;
const POLL_MAX_ATTEMPTS = 240; // ~6 minutes — enough for the first-run Docker image pull

const STATUS_LABEL: Record<ExecutionStatus, string> = {
  PENDING: "В очереди",
  RUNNING: "Выполняется",
  SUCCESS: "Успех",
  FAILURE: "Ошибка выполнения",
  TIMEOUT: "Таймаут",
  ERROR: "Ошибка системы",
};

const STATUS_VARIANT: Record<ExecutionStatus, "default" | "secondary" | "destructive" | "outline"> = {
  PENDING: "secondary",
  RUNNING: "secondary",
  SUCCESS: "default",
  FAILURE: "destructive",
  TIMEOUT: "destructive",
  ERROR: "destructive",
};

function StatusIcon({ status }: { status: ExecutionStatus }) {
  switch (status) {
    case "SUCCESS":
      return <CheckCircle2 className="h-4 w-4" />;
    case "FAILURE":
    case "ERROR":
      return <XCircle className="h-4 w-4" />;
    case "TIMEOUT":
      return <Clock className="h-4 w-4" />;
    default:
      return <Loader2 className="h-4 w-4 animate-spin" />;
  }
}

export function ExecutionPanel({ sessionId, language, getCode, canRun }: ExecutionPanelProps) {
  const [stdin, setStdin] = useState("");
  const [execution, setExecution] = useState<CodeExecution | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const pollTimer = useRef<number | null>(null);

  const clearPoll = useCallback(() => {
    if (pollTimer.current) {
      window.clearTimeout(pollTimer.current);
      pollTimer.current = null;
    }
  }, []);

  useEffect(() => () => clearPoll(), [clearPoll]);

  const pollOnce = useCallback(
    async (id: string, attempt: number) => {
      try {
        const res = await api.get<CodeExecution>(`/executions/${id}`);
        setExecution(res.data);
        const status = res.data.status;
        if (status === "SUCCESS" || status === "FAILURE" || status === "TIMEOUT" || status === "ERROR") {
          clearPoll();
          return;
        }
        if (attempt >= POLL_MAX_ATTEMPTS) {
          clearPoll();
          toast.error(
            "Превышено время ожидания. Скорее всего тянется образ контейнера для языка — попробуй обновить страницу через минуту."
          );
          return;
        }
        pollTimer.current = window.setTimeout(() => pollOnce(id, attempt + 1), POLL_INTERVAL_MS);
      } catch (err) {
        clearPoll();
        const msg =
          isAxiosError<ApiError>(err) && err.response?.data?.message
            ? err.response.data.message
            : "Не удалось получить статус запуска";
        toast.error(msg);
      }
    },
    [clearPoll]
  );

  const run = useCallback(async () => {
    if (!canRun || submitting) return;
    const code = getCode();
    if (!code.trim()) {
      toast.error("Код пустой");
      return;
    }
    clearPoll();
    setSubmitting(true);
    setExecution(null);
    const body: ExecutionRequest = {
      sessionId,
      language,
      code,
      stdin: stdin || undefined,
    };
    try {
      const res = await api.post<CodeExecution>("/executions", body);
      setExecution(res.data);
      pollTimer.current = window.setTimeout(() => pollOnce(res.data.id, 1), POLL_INTERVAL_MS);
    } catch (err) {
      const msg =
        isAxiosError<ApiError>(err) && err.response?.data?.message
          ? err.response.data.message
          : "Не удалось запустить код";
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  }, [canRun, clearPoll, getCode, language, pollOnce, sessionId, stdin, submitting]);

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0 gap-3 py-3">
        <div className="flex items-center gap-2">
          <CardTitle className="text-base">Запуск</CardTitle>
          {execution && (
            <Badge variant={STATUS_VARIANT[execution.status]} className="gap-1 font-normal">
              <StatusIcon status={execution.status} />
              {STATUS_LABEL[execution.status]}
              {execution.exitCode != null && ` · код ${execution.exitCode}`}
              {execution.durationMs != null && ` · ${execution.durationMs} мс`}
            </Badge>
          )}
        </div>
        <Button onClick={run} disabled={!canRun || submitting} size="sm">
          {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Play className="h-4 w-4" />}
          Запустить
        </Button>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-1">
          <label className="text-xs uppercase tracking-wide text-muted-foreground">stdin</label>
          <Textarea
            value={stdin}
            onChange={(e) => setStdin(e.target.value)}
            placeholder="Опционально — данные для стандартного ввода"
            rows={2}
            className="font-mono text-xs"
            disabled={!canRun || submitting}
          />
        </div>

        {execution?.errorMessage && (
          <div className="flex items-start gap-2 rounded-md border border-destructive/40 bg-destructive/10 p-2 text-xs">
            <AlertCircle className="h-4 w-4 mt-0.5 text-destructive" />
            <span className="font-mono">{execution.errorMessage}</span>
          </div>
        )}

        <StreamBlock label="stdout" value={execution?.stdout} />
        <StreamBlock label="stderr" value={execution?.stderr} tone="warn" />

        {!canRun && (
          <p className="text-xs text-muted-foreground">
            Запуск доступен интервьюеру и кандидату пока сессия в статусе «Идёт».
          </p>
        )}
      </CardContent>
    </Card>
  );
}

function StreamBlock({
  label,
  value,
  tone,
}: {
  label: string;
  value: string | null | undefined;
  tone?: "warn";
}) {
  if (!value) {
    return (
      <div className="space-y-1">
        <div className="text-xs uppercase tracking-wide text-muted-foreground">{label}</div>
        <div className="rounded-md border bg-muted/30 px-3 py-2 text-xs text-muted-foreground">пусто</div>
      </div>
    );
  }
  return (
    <div className="space-y-1">
      <div className="text-xs uppercase tracking-wide text-muted-foreground">{label}</div>
      <pre
        className={`rounded-md border px-3 py-2 text-xs overflow-x-auto max-h-60 whitespace-pre-wrap break-words ${
          tone === "warn" ? "bg-yellow-500/10 border-yellow-500/30" : "bg-muted/30"
        }`}
      >
        {value}
      </pre>
    </div>
  );
}
