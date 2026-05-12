import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Loader2, Plus, Search } from "lucide-react";
import { api } from "@/lib/api";
import type { Difficulty, PageResponse, Task, TaskCategory } from "@/types/api";
import { DIFFICULTY_LABELS, LANGUAGE_OPTIONS } from "@/lib/roles";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAuthStore } from "@/stores/auth";

type DiffFilter = Difficulty | "ALL";
const ANY_LANG = "ALL";
const ANY_CAT = "ALL";

const PAGE_SIZE = 20;

export function TasksPage() {
  const navigate = useNavigate();
  const role = useAuthStore((s) => s.user?.role);
  const canCreate = role === "INTERVIEWER" || role === "HR_MANAGER" || role === "ADMIN";

  const [page, setPage] = useState(0);
  const [q, setQ] = useState("");
  const [difficulty, setDifficulty] = useState<DiffFilter>("ALL");
  const [language, setLanguage] = useState<string>(ANY_LANG);
  const [categoryId, setCategoryId] = useState<string>(ANY_CAT);

  const { data: categories = [] } = useQuery({
    queryKey: ["task-categories"],
    queryFn: async () => (await api.get<TaskCategory[]>("/task-categories")).data,
    staleTime: 5 * 60_000,
  });

  const { data, isFetching } = useQuery({
    queryKey: ["tasks", { page, q, difficulty, language, categoryId }],
    queryFn: async () => {
      const params: Record<string, string | number> = { page, size: PAGE_SIZE };
      if (q.trim()) params.q = q.trim();
      if (difficulty !== "ALL") params.difficulty = difficulty;
      if (language !== ANY_LANG) params.language = language;
      if (categoryId !== ANY_CAT) params.categoryId = categoryId;
      return (await api.get<PageResponse<Task>>("/tasks", { params })).data;
    },
  });

  const tasks = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">Библиотека задач</h1>
          <p className="text-muted-foreground mt-1">
            Задачи для собеседований: фильтры по категории, языку и сложности.
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/tasks/new")}>
            <Plus className="h-4 w-4" />
            Создать задачу
          </Button>
        )}
      </div>

      <div className="grid gap-3 md:grid-cols-4">
        <div className="space-y-1.5 md:col-span-2">
          <label className="text-sm text-muted-foreground">Поиск</label>
          <div className="relative">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              className="pl-9"
              placeholder="Заголовок или описание"
              value={q}
              onChange={(e) => {
                setQ(e.target.value);
                setPage(0);
              }}
            />
          </div>
        </div>

        <div className="space-y-1.5">
          <label className="text-sm text-muted-foreground">Сложность</label>
          <Select
            value={difficulty}
            onValueChange={(v) => {
              setDifficulty(v as DiffFilter);
              setPage(0);
            }}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Все</SelectItem>
              <SelectItem value="EASY">Лёгкая</SelectItem>
              <SelectItem value="MEDIUM">Средняя</SelectItem>
              <SelectItem value="HARD">Сложная</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1.5">
          <label className="text-sm text-muted-foreground">Язык</label>
          <Select
            value={language}
            onValueChange={(v) => {
              setLanguage(v);
              setPage(0);
            }}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ANY_LANG}>Все</SelectItem>
              {LANGUAGE_OPTIONS.map((l) => (
                <SelectItem key={l.value} value={l.value}>
                  {l.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1.5 md:col-span-4">
          <label className="text-sm text-muted-foreground">Категория</label>
          <Select
            value={categoryId}
            onValueChange={(v) => {
              setCategoryId(v);
              setPage(0);
            }}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ANY_CAT}>Все категории</SelectItem>
              {categories.map((c) => (
                <SelectItem key={c.id} value={c.id}>
                  {c.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Название</TableHead>
              <TableHead>Сложность</TableHead>
              <TableHead>Язык</TableHead>
              <TableHead>Категории</TableHead>
              <TableHead>Видимость</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isFetching && tasks.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8">
                  <Loader2 className="animate-spin h-5 w-5 mx-auto text-muted-foreground" />
                </TableCell>
              </TableRow>
            ) : tasks.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                  Задач не найдено
                </TableCell>
              </TableRow>
            ) : (
              tasks.map((t) => (
                <TableRow
                  key={t.id}
                  className="cursor-pointer"
                  onClick={() => navigate(`/tasks/${t.id}`)}
                >
                  <TableCell className="font-medium">
                    <Link to={`/tasks/${t.id}`} className="hover:underline">
                      {t.title}
                    </Link>
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">{DIFFICULTY_LABELS[t.difficulty]}</Badge>
                  </TableCell>
                  <TableCell>{t.language ?? "—"}</TableCell>
                  <TableCell className="space-x-1">
                    {t.categories.length === 0 ? (
                      <span className="text-muted-foreground text-xs">—</span>
                    ) : (
                      t.categories.map((c) => (
                        <Badge key={c.id} variant="secondary" className="font-normal">
                          {c.name}
                        </Badge>
                      ))
                    )}
                  </TableCell>
                  <TableCell>
                    {t.companyId ? (
                      <Badge variant="outline" className="font-normal">
                        Компания
                      </Badge>
                    ) : (
                      <Badge variant="secondary" className="font-normal">
                        Платформа
                      </Badge>
                    )}
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
