import { useCallback, useEffect, useRef, useState } from "react";
import { Loader2, ScanLine, AlertTriangle, CheckCircle2, XCircle, Info } from "lucide-react";
import { toast } from "sonner";
import { isAxiosError } from "axios";
import { api } from "@/lib/api";
import type {
  AnalysisIssue,
  AnalysisRequest,
  AnalysisStatus,
  ApiError,
  CodeAnalysis,
} from "@/types/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

interface AnalysisPanelProps {
  sessionId: string;
  language: string;
  getCode: () => string;
  canAnalyze: boolean;
}

const POLL_INTERVAL_MS = 1200;
const POLL_MAX_ATTEMPTS = 60;

const STATUS_LABEL: Record<AnalysisStatus, string> = {
  PENDING: "В очереди",
  RUNNING: "Анализ",
  COMPLETED: "Готово",
  ERROR: "Ошибка",
};

const STATUS_VARIANT: Record<AnalysisStatus, "default" | "secondary" | "destructive" | "outline"> = {
  PENDING: "secondary",
  RUNNING: "secondary",
  COMPLETED: "default",
  ERROR: "destructive",
};

function StatusIcon({ status }: { status: AnalysisStatus }) {
  switch (status) {
    case "COMPLETED": return <CheckCircle2 className="h-4 w-4" />;
    case "ERROR":     return <XCircle className="h-4 w-4" />;
    default:          return <Loader2 className="h-4 w-4 animate-spin" />;
  }
}

function severityIcon(severity: string) {
  const s = severity.toUpperCase();
  if (s === "ERROR" || s === "HIGH") return <XCircle className="h-3.5 w-3.5 text-destructive" />;
  if (s === "WARNING" || s === "MEDIUM") return <AlertTriangle className="h-3.5 w-3.5 text-yellow-500" />;
  return <Info className="h-3.5 w-3.5 text-muted-foreground" />;
}

export function AnalysisPanel({ sessionId, language, getCode, canAnalyze }: AnalysisPanelProps) {
  const [analysis, setAnalysis] = useState<CodeAnalysis | null>(null);
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
        const res = await api.get<CodeAnalysis>(`/analyses/${id}`);
        setAnalysis(res.data);
        if (res.data.status === "COMPLETED" || res.data.status === "ERROR") {
          clearPoll();
          return;
        }
        if (attempt >= POLL_MAX_ATTEMPTS) {
          clearPoll();
          toast.error("Анализ занимает дольше обычного — попробуй обновить страницу.");
          return;
        }
        pollTimer.current = window.setTimeout(() => pollOnce(id, attempt + 1), POLL_INTERVAL_MS);
      } catch (err) {
        clearPoll();
        const msg =
          isAxiosError<ApiError>(err) && err.response?.data?.message
            ? err.response.data.message
            : "Не удалось получить статус анализа";
        toast.error(msg);
      }
    },
    [clearPoll]
  );

  const run = useCallback(async () => {
    if (!canAnalyze || submitting) return;
    const code = getCode();
    if (!code.trim()) {
      toast.error("Код пустой");
      return;
    }
    clearPoll();
    setSubmitting(true);
    setAnalysis(null);
    const body: AnalysisRequest = { sessionId, language, code };
    try {
      const res = await api.post<CodeAnalysis>("/analyses", body);
      setAnalysis(res.data);
      pollTimer.current = window.setTimeout(() => pollOnce(res.data.id, 1), POLL_INTERVAL_MS);
    } catch (err) {
      const msg =
        isAxiosError<ApiError>(err) && err.response?.data?.message
          ? err.response.data.message
          : "Не удалось запустить анализ";
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  }, [canAnalyze, clearPoll, getCode, language, pollOnce, sessionId, submitting]);

  const issues = analysis?.issues ?? [];
  const isJava = language.toLowerCase() === "java";

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0 gap-3 py-3">
        <div className="flex items-center gap-2">
          <CardTitle className="text-base">Статический анализ</CardTitle>
          {analysis && (
            <Badge variant={STATUS_VARIANT[analysis.status]} className="gap-1 font-normal">
              <StatusIcon status={analysis.status} />
              {STATUS_LABEL[analysis.status]}
              {analysis.durationMs != null && ` · ${analysis.durationMs} мс`}
            </Badge>
          )}
        </div>
        <Button onClick={run} disabled={!canAnalyze || submitting} size="sm" variant="outline">
          {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <ScanLine className="h-4 w-4" />}
          Анализировать
        </Button>
      </CardHeader>
      <CardContent className="space-y-4">
        {!isJava && (
          <p className="text-xs text-muted-foreground">
            Для языка <span className="font-mono">{language}</span> доступен только подсчёт строк —
            JavaParser/Checkstyle/PMD работают только с Java.
          </p>
        )}

        {analysis?.errorMessage && (
          <div className="flex items-start gap-2 rounded-md border border-destructive/40 bg-destructive/10 p-2 text-xs">
            <XCircle className="h-4 w-4 mt-0.5 text-destructive" />
            <span className="font-mono">{analysis.errorMessage}</span>
          </div>
        )}

        <div className="grid gap-3 sm:grid-cols-4">
          <Metric label="Строк кода" value={analysis?.linesOfCode} />
          <Metric label="Методов" value={analysis?.methodCount} />
          <Metric label="Цикл. сложность" value={analysis?.cyclomaticComplexity} />
          <Metric label="Когн. сложность" value={analysis?.cognitiveComplexity} />
        </div>

        <div>
          <div className="mb-2 text-xs uppercase tracking-wide text-muted-foreground">
            Проблемы {issues.length > 0 && <>· {issues.length}</>}
          </div>
          {issues.length === 0 ? (
            <div className="rounded-md border bg-muted/30 px-3 py-2 text-xs text-muted-foreground">
              {analysis?.status === "COMPLETED" ? "Проблем не найдено." : "Нет данных."}
            </div>
          ) : (
            <ul className="space-y-1 max-h-60 overflow-y-auto pr-1">
              {issues.map((issue, idx) => (
                <IssueRow key={idx} issue={issue} />
              ))}
            </ul>
          )}
        </div>

        {!canAnalyze && (
          <p className="text-xs text-muted-foreground">
            Анализ доступен интервьюеру и кандидату пока сессия в статусе «Идёт».
          </p>
        )}
      </CardContent>
    </Card>
  );
}

function Metric({ label, value }: { label: string; value: number | null | undefined }) {
  return (
    <div className="rounded-md border p-2">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="text-lg font-semibold tabular-nums">
        {value ?? "—"}
      </div>
    </div>
  );
}

function IssueRow({ issue }: { issue: AnalysisIssue }) {
  return (
    <li className="flex items-start gap-2 rounded-md border bg-card px-2 py-1.5 text-xs">
      <div className="pt-0.5">{severityIcon(issue.severity)}</div>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2 text-[11px] uppercase tracking-wide text-muted-foreground">
          <span>{issue.tool}</span>
          {issue.rule && <span className="font-mono normal-case">{issue.rule}</span>}
          {issue.line != null && <span>· строка {issue.line}</span>}
        </div>
        <div className="break-words">{issue.message}</div>
      </div>
    </li>
  );
}
