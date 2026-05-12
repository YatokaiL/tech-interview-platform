import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { isAxiosError } from "axios";
import { api } from "@/lib/api";
import type { ApiError, Difficulty, Task, TaskCategory, TaskRequest } from "@/types/api";
import { LANGUAGE_OPTIONS } from "@/lib/roles";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
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
import { Badge } from "@/components/ui/badge";

const NONE_LANG = "__none__";

interface FormValues {
  title: string;
  description: string;
  difficulty: Difficulty;
  language: string;
  initialCode: string;
  expectedOutput: string;
  timeLimitMinutes: string;
  categoryIds: string[];
}

export function TaskFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isEdit = !!id;

  const { data: categories = [] } = useQuery({
    queryKey: ["task-categories"],
    queryFn: async () => (await api.get<TaskCategory[]>("/task-categories")).data,
    staleTime: 5 * 60_000,
  });

  const { data: task, isLoading: loadingTask } = useQuery({
    enabled: isEdit,
    queryKey: ["task", id],
    queryFn: async () => (await api.get<Task>(`/tasks/${id}`)).data,
  });

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    defaultValues: {
      title: "",
      description: "",
      difficulty: "MEDIUM",
      language: NONE_LANG,
      initialCode: "",
      expectedOutput: "",
      timeLimitMinutes: "",
      categoryIds: [],
    },
  });

  useEffect(() => {
    if (task) {
      reset({
        title: task.title,
        description: task.description,
        difficulty: task.difficulty,
        language: task.language ?? NONE_LANG,
        initialCode: task.initialCode ?? "",
        expectedOutput: task.expectedOutput ?? "",
        timeLimitMinutes: task.timeLimitMinutes?.toString() ?? "",
        categoryIds: task.categories.map((c) => c.id),
      });
    }
  }, [task, reset]);

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const payload: TaskRequest = {
        title: values.title,
        description: values.description,
        difficulty: values.difficulty,
        language: values.language === NONE_LANG ? null : values.language,
        initialCode: values.initialCode || null,
        expectedOutput: values.expectedOutput || null,
        timeLimitMinutes: values.timeLimitMinutes ? Number(values.timeLimitMinutes) : null,
        categoryIds: values.categoryIds,
      };
      if (isEdit) {
        return (await api.patch<Task>(`/tasks/${id}`, payload)).data;
      }
      return (await api.post<Task>("/tasks", payload)).data;
    },
    onSuccess: (saved) => {
      toast.success(isEdit ? "Задача обновлена" : "Задача создана");
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
      navigate(`/tasks/${saved.id}`);
    },
    onError: (err) => {
      const msg =
        isAxiosError<ApiError>(err) && err.response?.data?.message
          ? err.response.data.message
          : "Не удалось сохранить задачу";
      toast.error(msg);
    },
  });

  if (isEdit && loadingTask) {
    return (
      <div className="flex justify-center py-12">
        <Loader2 className="animate-spin h-6 w-6 text-muted-foreground" />
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="max-w-3xl space-y-6">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">
          {isEdit ? "Редактирование задачи" : "Новая задача"}
        </h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Основное</CardTitle>
          <CardDescription>Условие задачи и её параметры.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="title">Название</Label>
            <Input id="title" {...register("title", { required: "Введите название" })} />
            {errors.title && <p className="text-sm text-destructive">{errors.title.message}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Условие</Label>
            <Textarea
              id="description"
              rows={6}
              className="font-sans"
              {...register("description", { required: "Введите условие" })}
            />
            {errors.description && (
              <p className="text-sm text-destructive">{errors.description.message}</p>
            )}
          </div>

          <div className="grid gap-3 md:grid-cols-3">
            <div className="space-y-2">
              <Label>Сложность</Label>
              <Controller
                control={control}
                name="difficulty"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="EASY">Лёгкая</SelectItem>
                      <SelectItem value="MEDIUM">Средняя</SelectItem>
                      <SelectItem value="HARD">Сложная</SelectItem>
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
                      <SelectItem value={NONE_LANG}>Без указания</SelectItem>
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

            <div className="space-y-2">
              <Label htmlFor="timeLimitMinutes">Лимит, мин</Label>
              <Input
                id="timeLimitMinutes"
                type="number"
                min={1}
                {...register("timeLimitMinutes")}
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label>Категории</Label>
            <Controller
              control={control}
              name="categoryIds"
              render={({ field }) => (
                <div className="flex flex-wrap gap-2">
                  {categories.map((c) => {
                    const checked = field.value.includes(c.id);
                    return (
                      <button
                        type="button"
                        key={c.id}
                        onClick={() =>
                          field.onChange(
                            checked
                              ? field.value.filter((v) => v !== c.id)
                              : [...field.value, c.id]
                          )
                        }
                      >
                        <Badge
                          variant={checked ? "default" : "outline"}
                          className="cursor-pointer"
                        >
                          {c.name}
                        </Badge>
                      </button>
                    );
                  })}
                </div>
              )}
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Стартовый код</CardTitle>
          <CardDescription>
            Шаблон, с которого начинает кандидат, и опционально ожидаемый вывод.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="initialCode">Initial code</Label>
            <Textarea id="initialCode" rows={8} {...register("initialCode")} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="expectedOutput">Expected output</Label>
            <Textarea id="expectedOutput" rows={4} {...register("expectedOutput")} />
          </div>
        </CardContent>
      </Card>

      <CardFooter className="flex justify-end gap-2 px-0">
        <Button type="button" variant="outline" onClick={() => navigate(-1)}>
          Отмена
        </Button>
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending && <Loader2 className="animate-spin" />}
          {isEdit ? "Сохранить" : "Создать"}
        </Button>
      </CardFooter>
    </form>
  );
}
