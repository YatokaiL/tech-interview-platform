import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Loader2, Pencil, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { isAxiosError } from "axios";
import { api } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import type { ApiError, Task } from "@/types/api";
import { DIFFICULTY_LABELS } from "@/lib/roles";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

export function TaskDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);

  const { data: task, isLoading, isError } = useQuery({
    queryKey: ["task", id],
    queryFn: async () => (await api.get<Task>(`/tasks/${id}`)).data,
    enabled: !!id,
  });

  const deleteMutation = useMutation({
    mutationFn: async () => api.delete(`/tasks/${id}`),
    onSuccess: () => {
      toast.success("Задача удалена");
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      navigate("/tasks", { replace: true });
    },
    onError: (err) => {
      const msg =
        isAxiosError<ApiError>(err) && err.response?.data?.message
          ? err.response.data.message
          : "Не удалось удалить задачу";
      toast.error(msg);
    },
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Loader2 className="animate-spin h-6 w-6 text-muted-foreground" />
      </div>
    );
  }

  if (isError || !task) {
    return <p className="text-destructive">Не удалось загрузить задачу.</p>;
  }

  const canEdit =
    user?.role === "ADMIN" || (user && task.createdBy === user.id);

  return (
    <div className="max-w-3xl space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">{task.title}</h1>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <Badge variant="outline">{DIFFICULTY_LABELS[task.difficulty]}</Badge>
            {task.language && <Badge variant="secondary">{task.language}</Badge>}
            {task.timeLimitMinutes && (
              <Badge variant="outline">{task.timeLimitMinutes} мин</Badge>
            )}
            {task.companyId ? (
              <Badge variant="outline" className="font-normal">
                Видна только в вашей компании
              </Badge>
            ) : (
              <Badge variant="secondary" className="font-normal">
                Общая задача
              </Badge>
            )}
          </div>
        </div>
        {canEdit && (
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => navigate(`/tasks/${task.id}/edit`)}>
              <Pencil className="h-4 w-4" />
              Изменить
            </Button>
            <Button
              variant="destructive"
              onClick={() => {
                if (confirm("Удалить задачу?")) deleteMutation.mutate();
              }}
              disabled={deleteMutation.isPending}
            >
              <Trash2 className="h-4 w-4" />
              Удалить
            </Button>
          </div>
        )}
      </div>

      {task.categories.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {task.categories.map((c) => (
            <Badge key={c.id} variant="secondary" className="font-normal">
              {c.name}
            </Badge>
          ))}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Условие</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="whitespace-pre-wrap text-sm leading-6">{task.description}</p>
        </CardContent>
      </Card>

      {task.initialCode && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Начальный код</CardTitle>
          </CardHeader>
          <CardContent>
            <pre className="rounded-md bg-muted p-4 overflow-x-auto text-sm font-mono">
              {task.initialCode}
            </pre>
          </CardContent>
        </Card>
      )}

      {task.expectedOutput && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Ожидаемый вывод</CardTitle>
          </CardHeader>
          <CardContent>
            <pre className="rounded-md bg-muted p-4 overflow-x-auto text-sm font-mono">
              {task.expectedOutput}
            </pre>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
