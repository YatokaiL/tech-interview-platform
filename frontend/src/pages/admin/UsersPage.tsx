import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Loader2, Search } from "lucide-react";
import { api } from "@/lib/api";
import type { PageResponse, Role, User } from "@/types/api";
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
import { ROLE_LABELS } from "@/lib/roles";
import { EditUserDialog } from "@/pages/admin/EditUserDialog";

type RoleFilter = Role | "ALL";

const PAGE_SIZE = 20;

export function UsersPage() {
  const [page, setPage] = useState(0);
  const [q, setQ] = useState("");
  const [role, setRole] = useState<RoleFilter>("ALL");
  const [editing, setEditing] = useState<User | null>(null);

  const { data, isFetching } = useQuery({
    queryKey: ["users", { page, q, role }],
    queryFn: async () => {
      const params: Record<string, string | number> = { page, size: PAGE_SIZE };
      if (q.trim()) params.q = q.trim();
      if (role !== "ALL") params.role = role;
      const res = await api.get<PageResponse<User>>("/users", { params });
      return res.data;
    },
  });

  const users = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Пользователи</h1>
        <p className="text-muted-foreground mt-1">
          Управление пользователями платформы: поиск, фильтры, смена ролей и блокировка.
        </p>
      </div>

      <div className="flex flex-col gap-3 md:flex-row md:items-end">
        <div className="flex-1 space-y-1.5">
          <label className="text-sm text-muted-foreground">Поиск по email</label>
          <div className="relative">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              className="pl-9"
              placeholder="user@example.com"
              value={q}
              onChange={(e) => {
                setQ(e.target.value);
                setPage(0);
              }}
            />
          </div>
        </div>

        <div className="w-full md:w-56 space-y-1.5">
          <label className="text-sm text-muted-foreground">Роль</label>
          <Select
            value={role}
            onValueChange={(val) => {
              setRole(val as RoleFilter);
              setPage(0);
            }}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Все роли</SelectItem>
              <SelectItem value="CANDIDATE">Кандидат</SelectItem>
              <SelectItem value="INTERVIEWER">Интервьюер</SelectItem>
              <SelectItem value="HR_MANAGER">HR-менеджер</SelectItem>
              <SelectItem value="ADMIN">Администратор</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Email</TableHead>
              <TableHead>Имя</TableHead>
              <TableHead>Роль</TableHead>
              <TableHead>Компания</TableHead>
              <TableHead>Статус</TableHead>
              <TableHead className="text-right">Действия</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isFetching && users.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-8">
                  <Loader2 className="animate-spin h-5 w-5 mx-auto text-muted-foreground" />
                </TableCell>
              </TableRow>
            ) : users.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                  Пользователи не найдены
                </TableCell>
              </TableRow>
            ) : (
              users.map((u) => (
                <TableRow key={u.id}>
                  <TableCell className="font-medium">{u.email}</TableCell>
                  <TableCell>
                    {[u.firstName, u.lastName].filter(Boolean).join(" ") || "—"}
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">{ROLE_LABELS[u.role]}</Badge>
                  </TableCell>
                  <TableCell>{u.companyName ?? "—"}</TableCell>
                  <TableCell>
                    {u.blocked ? (
                      <Badge variant="destructive">Заблокирован</Badge>
                    ) : (
                      <Badge variant="secondary">Активен</Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button variant="outline" size="sm" onClick={() => setEditing(u)}>
                      Изменить
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <div className="flex items-center justify-between text-sm">
        <div className="text-muted-foreground">
          Всего: {data?.totalElements ?? 0}
        </div>
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

      <EditUserDialog user={editing} onClose={() => setEditing(null)} />
    </div>
  );
}
