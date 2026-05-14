import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { isAxiosError } from "axios";
import { api } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import type {
  ApiError,
  PageResponse,
  Session,
  SessionRequest,
  Task,
  UserLookup,
} from "@/types/api";
import { LANGUAGE_OPTIONS, formatUserName } from "@/lib/roles";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const NO_TASK = "__none__";
const SELF_INTERVIEWER = "__self__";

interface FormValues {
  title: string;
  candidateId: string;
  interviewerId: string;
  taskId: string;
  language: string;
  scheduledAtLocal: string;
}

export function SessionFormPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const isInterviewer = user?.role === "INTERVIEWER";

  const [candidateQuery, setCandidateQuery] = useState("");
  const [interviewerQuery, setInterviewerQuery] = useState("");

  const { data: candidates = [] } = useQuery({
    queryKey: ["users-lookup", "CANDIDATE", candidateQuery],
    queryFn: async () => {
      const params: Record<string, string | number> = { role: "CANDIDATE", limit: 20 };
      if (candidateQuery.trim()) params.q = candidateQuery.trim();
      return (await api.get<UserLookup[]>("/users/lookup", { params })).data;
    },
    staleTime: 10_000,
  });

  const { data: interviewers = [] } = useQuery({
    enabled: !isInterviewer,
    queryKey: ["users-lookup", "INTERVIEWER", interviewerQuery],
    queryFn: async () => {
      const params: Record<string, string | number> = { role: "INTERVIEWER", limit: 20 };
      if (interviewerQuery.trim()) params.q = interviewerQuery.trim();
      return (await api.get<UserLookup[]>("/users/lookup", { params })).data;
    },
    staleTime: 10_000,
  });

  const { data: tasksPage } = useQuery({
    queryKey: ["tasks-picker"],
    queryFn: async () =>
      (await api.get<PageResponse<Task>>("/tasks", { params: { size: 100 } })).data,
  });
  const tasks = tasksPage?.content ?? [];

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<FormValues>({
    defaultValues: {
      title: "",
      candidateId: "",
      interviewerId: isInterviewer ? "" : SELF_INTERVIEWER,
      taskId: NO_TASK,
      language: "java",
      scheduledAtLocal: defaultDateTimeLocal(),
    },
  });

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      let interviewerId: string | null | undefined;
      if (isInterviewer) {
        interviewerId = undefined;
      } else if (values.interviewerId === SELF_INTERVIEWER) {
        interviewerId = user?.id ?? null;
      } else {
        interviewerId = values.interviewerId;
      }

      const payload: SessionRequest = {
        title: values.title,
        candidateId: values.candidateId,
        interviewerId,
        taskId: values.taskId === NO_TASK ? null : values.taskId,
        language: values.language,
        scheduledAt: new Date(values.scheduledAtLocal).toISOString(),
      };
      return (await api.post<Session>("/sessions", payload)).data;
    },
    onSuccess: (saved) => {
      toast.success("Сессия создана");
      queryClient.invalidateQueries({ queryKey: ["sessions"] });
      navigate(`/sessions/${saved.id}`);
    },
    onError: (err) => {
      const msg =
        isAxiosError<ApiError>(err) && err.response?.data?.message
          ? err.response.data.message
          : "Не удалось создать сессию";
      toast.error(msg);
    },
  });

  return (
    <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="max-w-2xl space-y-6">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Новая сессия</h1>
        <p className="text-muted-foreground mt-1">
          Назначьте кандидата, задачу и время проведения интервью.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Параметры</CardTitle>
          <CardDescription>Эти поля будут видны участникам сессии.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="title">Название</Label>
            <Input
              id="title"
              placeholder="Например, Junior Backend Java"
              {...register("title", { required: "Введите название" })}
            />
            {errors.title && (
              <p className="text-sm text-destructive">{errors.title.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label>Кандидат</Label>
            <Input
              placeholder="Поиск по email"
              value={candidateQuery}
              onChange={(e) => setCandidateQuery(e.target.value)}
            />
            <Controller
              control={control}
              name="candidateId"
              rules={{ required: "Выберите кандидата" }}
              render={({ field }) => (
                <Select value={field.value || undefined} onValueChange={field.onChange}>
                  <SelectTrigger>
                    <SelectValue placeholder="Выберите кандидата" />
                  </SelectTrigger>
                  <SelectContent>
                    {candidates.length === 0 ? (
                      <div className="px-2 py-1.5 text-sm text-muted-foreground">
                        Кандидаты не найдены
                      </div>
                    ) : (
                      candidates.map((c) => (
                        <SelectItem key={c.id} value={c.id}>
                          {formatUserName(c)} — {c.email}
                        </SelectItem>
                      ))
                    )}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.candidateId && (
              <p className="text-sm text-destructive">{errors.candidateId.message}</p>
            )}
          </div>

          {!isInterviewer && (
            <div className="space-y-2">
              <Label>Интервьюер</Label>
              <Input
                placeholder="Поиск по email (или оставьте «Я провожу сам(а)»)"
                value={interviewerQuery}
                onChange={(e) => setInterviewerQuery(e.target.value)}
              />
              <Controller
                control={control}
                name="interviewerId"
                rules={{ required: "Выберите интервьюера" }}
                render={({ field }) => (
                  <Select value={field.value || undefined} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue placeholder="Выберите интервьюера" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={SELF_INTERVIEWER}>
                        Я провожу сам(а) — {user?.email}
                      </SelectItem>
                      {interviewers.map((i) => (
                        <SelectItem key={i.id} value={i.id}>
                          {formatUserName(i)} — {i.email}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
              {errors.interviewerId && (
                <p className="text-sm text-destructive">{errors.interviewerId.message}</p>
              )}
            </div>
          )}

          <div className="grid gap-3 md:grid-cols-2">
            <div className="space-y-2">
              <Label>Задача</Label>
              <Controller
                control={control}
                name="taskId"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NO_TASK}>Без задачи</SelectItem>
                      {tasks.map((t) => (
                        <SelectItem key={t.id} value={t.id}>
                          {t.title}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-2">
              <Label>Язык</Label>
              <Controller
                control={control}
                name="language"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {LANGUAGE_OPTIONS.map((l) => (
                        <SelectItem key={l.value} value={l.value}>
                          {l.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="scheduledAtLocal">Когда начнём</Label>
            <Input
              id="scheduledAtLocal"
              type="datetime-local"
              {...register("scheduledAtLocal", { required: "Укажите время" })}
            />
            {errors.scheduledAtLocal && (
              <p className="text-sm text-destructive">{errors.scheduledAtLocal.message}</p>
            )}
          </div>
        </CardContent>
        <CardFooter className="justify-end gap-2">
          <Button type="button" variant="outline" onClick={() => navigate(-1)}>
            Отмена
          </Button>
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending && <Loader2 className="animate-spin" />}
            Создать
          </Button>
        </CardFooter>
      </Card>
    </form>
  );
}

function defaultDateTimeLocal(): string {
  const d = new Date(Date.now() + 60 * 60 * 1000);
  d.setSeconds(0, 0);
  const pad = (n: number) => n.toString().padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
