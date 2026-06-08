import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Editor from "@monaco-editor/react";
import {
  Loader2,
  Save,
  Send,
  Trash2,
  CheckCircle2,
  MessageSquarePlus,
  ArrowLeft,
} from "lucide-react";
import { toast } from "sonner";
import { isAxiosError } from "axios";
import { api } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import type {
  ApiError,
  CodeSnapshot,
  CommentRequest,
  Review,
  ReviewComment,
  ReviewRequest,
  ReviewUpdateRequest,
  Session,
  Verdict,
} from "@/types/api";
import { monacoLanguage, VERDICT_LABELS, VERDICT_VARIANT } from "@/lib/roles";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";

const SCORES = [1, 2, 3, 4, 5] as const;
const VERDICTS: Verdict[] = ["HIRE", "MAYBE", "NO_HIRE"];

const CRITERIA: Array<{ key: ScoreKey; label: string }> = [
  { key: "scoreCorrectness", label: "Корректность" },
  { key: "scoreCodeQuality", label: "Чистота кода" },
  { key: "scorePerformance", label: "Производительность" },
  { key: "scoreCommunication", label: "Коммуникация" },
];

type ScoreKey =
  | "scoreCorrectness"
  | "scoreCodeQuality"
  | "scorePerformance"
  | "scoreCommunication";

function errorMessage(err: unknown, fallback: string) {
  return isAxiosError<ApiError>(err) && err.response?.data?.message
    ? err.response.data.message
    : fallback;
}

export function SessionReviewPage() {
  const { id: sessionId } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);

  const { data: session, isLoading: sessionLoading } = useQuery({
    queryKey: ["session", sessionId],
    queryFn: async () => (await api.get<Session>(`/sessions/${sessionId}`)).data,
    enabled: !!sessionId,
  });

  const { data: latestSnapshot } = useQuery({
    queryKey: ["session-snapshot-latest", sessionId],
    queryFn: async () => {
      const res = await api.get<CodeSnapshot | "">(
        `/sessions/${sessionId}/snapshots/latest`
      );
      return res.data && typeof res.data === "object" ? (res.data as CodeSnapshot) : null;
    },
    enabled: !!sessionId,
  });

  const {
    data: review,
    isLoading: reviewLoading,
    refetch: refetchReview,
  } = useQuery({
    queryKey: ["review-by-session", sessionId],
    queryFn: async () => {
      const res = await api.get<Review | "">(`/reviews`, { params: { sessionId } });
      return res.data && typeof res.data === "object" ? (res.data as Review) : null;
    },
    enabled: !!sessionId,
  });

  const isInterviewer =
    !!session && user?.role === "INTERVIEWER" && user.id === session.interviewerId;
  const isAdmin = user?.role === "ADMIN";
  const canEdit = (isInterviewer || isAdmin) && review?.status !== "SUBMITTED";

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!sessionId) throw new Error("no sessionId");
      const body: ReviewRequest = { sessionId };
      return (await api.post<Review>("/reviews", body)).data;
    },
    onSuccess: () => {
      toast.success("Черновик создан");
      queryClient.invalidateQueries({ queryKey: ["review-by-session", sessionId] });
    },
    onError: (err) => toast.error(errorMessage(err, "Не удалось создать ревью")),
  });

  if (sessionLoading || reviewLoading) {
    return (
      <div className="flex justify-center py-12">
        <Loader2 className="animate-spin h-6 w-6 text-muted-foreground" />
      </div>
    );
  }
  if (!session) {
    return <p className="text-destructive">Не удалось загрузить сессию.</p>;
  }
  if (session.status !== "COMPLETED") {
    return (
      <Card>
        <CardContent className="py-6 text-sm text-muted-foreground">
          Ревью доступно только для завершённых сессий.
        </CardContent>
      </Card>
    );
  }

  if (!review) {
    return (
      <div className="space-y-4">
        <Header session={session} onBack={() => navigate(`/sessions/${sessionId}`)} />
        <Card>
          <CardContent className="flex items-center justify-between py-6">
            <div>
              <div className="font-medium">Ревью ещё не создано</div>
              <p className="text-sm text-muted-foreground mt-1">
                {isInterviewer || isAdmin
                  ? "Заполните черновик и отправьте его кандидату."
                  : "Дождитесь, когда интервьюер опубликует ревью."}
              </p>
            </div>
            {(isInterviewer || isAdmin) && (
              <Button
                onClick={() => createMutation.mutate()}
                disabled={createMutation.isPending}
              >
                {createMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                Создать черновик
              </Button>
            )}
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <Header session={session} onBack={() => navigate(`/sessions/${sessionId}`)} />
      <div className="grid gap-4 lg:grid-cols-[1fr_360px]">
        <ReviewEditorPane
          session={session}
          review={review}
          snapshot={latestSnapshot ?? null}
          canEdit={!!canEdit}
          onChanged={refetchReview}
        />
        <ReviewRubricPane review={review} canEdit={!!canEdit} onChanged={refetchReview} />
      </div>
    </div>
  );
}

function Header({ session, onBack }: { session: Session; onBack: () => void }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <div>
        <Button variant="ghost" size="sm" onClick={onBack} className="-ml-2">
          <ArrowLeft className="h-4 w-4" />
          К сессии
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight mt-2">
          Ревью: {session.title}
        </h1>
        <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
          <Badge variant="secondary" className="font-mono">{session.language}</Badge>
          {session.taskTitle && (
            <Badge variant="outline" className="font-normal">Задача: {session.taskTitle}</Badge>
          )}
        </div>
      </div>
    </div>
  );
}

function ReviewEditorPane({
  session,
  review,
  snapshot,
  canEdit,
  onChanged,
}: {
  session: Session;
  review: Review;
  snapshot: CodeSnapshot | null;
  canEdit: boolean;
  onChanged: () => void;
}) {
  const code = snapshot?.code ?? "";
  const [newLine, setNewLine] = useState<string>("");
  const [newText, setNewText] = useState<string>("");

  const addCommentMutation = useMutation({
    mutationFn: async () => {
      const line = newLine.trim() ? Number(newLine) : undefined;
      const body: CommentRequest = { lineNumber: line, text: newText };
      return (await api.post<ReviewComment>(`/reviews/${review.id}/comments`, body)).data;
    },
    onSuccess: () => {
      setNewLine("");
      setNewText("");
      onChanged();
    },
    onError: (err) => toast.error(errorMessage(err, "Не удалось добавить комментарий")),
  });

  const deleteCommentMutation = useMutation({
    mutationFn: async (commentId: string) =>
      api.delete(`/reviews/${review.id}/comments/${commentId}`),
    onSuccess: () => onChanged(),
    onError: (err) => toast.error(errorMessage(err, "Не удалось удалить комментарий")),
  });

  const sortedComments = useMemo(
    () =>
      [...review.comments].sort((a, b) => {
        if (a.lineNumber == null && b.lineNumber == null) return 0;
        if (a.lineNumber == null) return 1;
        if (b.lineNumber == null) return -1;
        return a.lineNumber - b.lineNumber;
      }),
    [review.comments]
  );

  return (
    <div className="space-y-3">
      <Card className="overflow-hidden">
        <CardHeader className="py-3">
          <CardTitle className="text-base">Код сессии</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <Editor
            height="540px"
            language={monacoLanguage(snapshot?.language ?? session.language)}
            value={code}
            theme="vs-dark"
            options={{
              readOnly: true,
              minimap: { enabled: false },
              fontSize: 14,
              automaticLayout: true,
              wordWrap: "on",
              scrollBeyondLastLine: false,
            }}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="py-3">
          <CardTitle className="text-base">
            Комментарии {sortedComments.length > 0 && <>· {sortedComments.length}</>}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {sortedComments.length === 0 ? (
            <p className="text-xs text-muted-foreground">Комментариев пока нет.</p>
          ) : (
            <ul className="space-y-2">
              {sortedComments.map((c) => (
                <li
                  key={c.id}
                  className="flex items-start gap-2 rounded-md border bg-card px-3 py-2 text-sm"
                >
                  <div className="flex-1 min-w-0">
                    <div className="text-[11px] text-muted-foreground">
                      {c.lineNumber != null
                        ? `строка ${c.lineNumber}`
                        : "общий комментарий"}{" "}
                      · {new Date(c.createdAt).toLocaleString("ru-RU")}
                    </div>
                    <div className="break-words mt-0.5">{c.text}</div>
                  </div>
                  {canEdit && (
                    <Button
                      size="icon"
                      variant="ghost"
                      onClick={() => deleteCommentMutation.mutate(c.id)}
                      disabled={deleteCommentMutation.isPending}
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          )}

          {canEdit && (
            <div className="grid grid-cols-[80px_1fr_auto] gap-2 items-start pt-2 border-t">
              <Input
                placeholder="строка"
                value={newLine}
                onChange={(e) => setNewLine(e.target.value.replace(/[^0-9]/g, ""))}
                inputMode="numeric"
              />
              <Textarea
                placeholder="Комментарий"
                rows={2}
                value={newText}
                onChange={(e) => setNewText(e.target.value)}
              />
              <Button
                onClick={() => addCommentMutation.mutate()}
                disabled={addCommentMutation.isPending || !newText.trim()}
              >
                {addCommentMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <MessageSquarePlus className="h-4 w-4" />
                )}
                Добавить
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function ReviewRubricPane({
  review,
  canEdit,
  onChanged,
}: {
  review: Review;
  canEdit: boolean;
  onChanged: () => void;
}) {
  const queryClient = useQueryClient();
  const [scores, setScores] = useState({
    scoreCorrectness: review.scoreCorrectness,
    scoreCodeQuality: review.scoreCodeQuality,
    scorePerformance: review.scorePerformance,
    scoreCommunication: review.scoreCommunication,
  });
  const [verdict, setVerdict] = useState<Verdict | null>(review.verdict);
  const [summary, setSummary] = useState<string>(review.summary ?? "");

  useEffect(() => {
    setScores({
      scoreCorrectness: review.scoreCorrectness,
      scoreCodeQuality: review.scoreCodeQuality,
      scorePerformance: review.scorePerformance,
      scoreCommunication: review.scoreCommunication,
    });
    setVerdict(review.verdict);
    setSummary(review.summary ?? "");
  }, [review.id, review.updatedAt, review.scoreCorrectness, review.scoreCodeQuality, review.scorePerformance, review.scoreCommunication, review.verdict, review.summary]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body: ReviewUpdateRequest = {
        scoreCorrectness: scores.scoreCorrectness,
        scoreCodeQuality: scores.scoreCodeQuality,
        scorePerformance: scores.scorePerformance,
        scoreCommunication: scores.scoreCommunication,
        verdict,
        summary,
      };
      return (await api.patch<Review>(`/reviews/${review.id}`, body)).data;
    },
    onSuccess: () => {
      toast.success("Черновик сохранён");
      onChanged();
    },
    onError: (err) => toast.error(errorMessage(err, "Не удалось сохранить ревью")),
  });

  const submitMutation = useMutation({
    mutationFn: async () => (await api.post<Review>(`/reviews/${review.id}/submit`)).data,
    onSuccess: () => {
      toast.success("Ревью отправлено");
      queryClient.invalidateQueries({ queryKey: ["review-by-session"] });
      onChanged();
    },
    onError: (err) => toast.error(errorMessage(err, "Не удалось отправить ревью")),
  });

  const setScore = useCallback((key: ScoreKey, value: number) => {
    setScores((prev) => ({ ...prev, [key]: value }));
  }, []);

  const submitted = review.status === "SUBMITTED";

  return (
    <Card>
      <CardHeader className="py-3">
        <CardTitle className="flex items-center justify-between text-base">
          <span>Рубрика</span>
          {submitted ? (
            <Badge variant="default" className="gap-1 font-normal">
              <CheckCircle2 className="h-3 w-3" /> Отправлено
            </Badge>
          ) : (
            <Badge variant="outline" className="font-normal">Черновик</Badge>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-3">
          {CRITERIA.map((c) => (
            <div key={c.key} className="space-y-1.5">
              <div className="text-sm font-medium">{c.label}</div>
              <div className="flex gap-1">
                {SCORES.map((s) => (
                  <Button
                    key={s}
                    size="sm"
                    variant={scores[c.key] === s ? "default" : "outline"}
                    onClick={() => canEdit && setScore(c.key, s)}
                    disabled={!canEdit}
                    className="flex-1"
                  >
                    {s}
                  </Button>
                ))}
              </div>
            </div>
          ))}
        </div>

        {review.scoreAverage != null && (
          <div className="rounded-md border bg-muted/40 px-3 py-2 text-sm">
            <span className="text-muted-foreground">Средняя оценка: </span>
            <span className="font-semibold">{review.scoreAverage.toFixed(1)}</span> / 5
          </div>
        )}

        <div className="space-y-1.5">
          <div className="text-sm font-medium">Вердикт</div>
          <div className="grid grid-cols-3 gap-1">
            {VERDICTS.map((v) => (
              <Button
                key={v}
                size="sm"
                variant={verdict === v ? VERDICT_VARIANT[v] : "outline"}
                onClick={() => canEdit && setVerdict(v)}
                disabled={!canEdit}
              >
                {VERDICT_LABELS[v]}
              </Button>
            ))}
          </div>
        </div>

        <div className="space-y-1.5">
          <div className="text-sm font-medium">Итоговый комментарий</div>
          <Textarea
            value={summary}
            onChange={(e) => setSummary(e.target.value)}
            rows={6}
            placeholder="Сильные и слабые стороны, рекомендации..."
            disabled={!canEdit}
          />
        </div>

        {canEdit && (
          <div className="flex gap-2">
            <Button
              variant="outline"
              className="flex-1"
              onClick={() => saveMutation.mutate()}
              disabled={saveMutation.isPending}
            >
              {saveMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
              Сохранить
            </Button>
            <Button
              className="flex-1"
              onClick={() => submitMutation.mutate()}
              disabled={
                submitMutation.isPending
                || !scores.scoreCorrectness
                || !scores.scoreCodeQuality
                || !scores.scorePerformance
                || !scores.scoreCommunication
                || !verdict
              }
            >
              {submitMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
              Отправить
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
