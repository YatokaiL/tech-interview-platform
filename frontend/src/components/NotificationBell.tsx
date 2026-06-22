import { useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, CheckCheck } from "lucide-react";
import { api } from "@/lib/api";
import type { AppNotification } from "@/types/api";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export function NotificationBell() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: unread } = useQuery({
    queryKey: ["notifications-unread"],
    queryFn: async () =>
      (await api.get<{ count: number }>("/notifications/unread-count")).data.count,
    refetchInterval: 20_000,
  });

  const { data: items = [], refetch } = useQuery({
    queryKey: ["notifications-list"],
    queryFn: async () =>
      (await api.get<AppNotification[]>("/notifications", { params: { limit: 20 } })).data,
    enabled: false,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["notifications-unread"] });
    refetch();
  };

  const markRead = useMutation({
    mutationFn: async (id: string) => api.post(`/notifications/${id}/read`),
    onSuccess: invalidate,
  });

  const markAll = useMutation({
    mutationFn: async () => api.post("/notifications/read-all"),
    onSuccess: invalidate,
  });

  const count = unread ?? 0;

  function openItem(n: AppNotification) {
    if (!n.read) markRead.mutate(n.id);
    if (n.link) navigate(n.link);
  }

  return (
    <DropdownMenu onOpenChange={(open) => { if (open) refetch(); }}>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" className="relative">
          <Bell className="h-5 w-5" />
          {count > 0 && (
            <span className="absolute -top-0.5 -right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-medium text-destructive-foreground">
              {count > 9 ? "9+" : count}
            </span>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80">
        <DropdownMenuLabel className="flex items-center justify-between">
          <span>Уведомления</span>
          {count > 0 && (
            <Button
              variant="ghost"
              size="sm"
              className="h-7 px-2 text-xs"
              onClick={(e) => {
                e.preventDefault();
                markAll.mutate();
              }}
            >
              <CheckCheck className="h-3.5 w-3.5" />
              Прочитать все
            </Button>
          )}
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <div className="max-h-96 overflow-y-auto">
          {items.length === 0 ? (
            <div className="px-3 py-6 text-center text-sm text-muted-foreground">
              Уведомлений нет
            </div>
          ) : (
            items.map((n) => (
              <button
                key={n.id}
                onClick={() => openItem(n)}
                className={`block w-full text-left px-3 py-2 hover:bg-accent transition-colors ${
                  n.read ? "" : "bg-accent/40"
                }`}
              >
                <div className="flex items-start gap-2">
                  {!n.read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary" />}
                  <div className={`min-w-0 flex-1 ${n.read ? "pl-4" : ""}`}>
                    <div className="text-sm font-medium truncate">{n.title}</div>
                    <div className="text-xs text-muted-foreground line-clamp-2">{n.message}</div>
                    <div className="text-[10px] text-muted-foreground mt-0.5">
                      {new Date(n.createdAt).toLocaleString("ru-RU")}
                    </div>
                  </div>
                </div>
              </button>
            ))
          )}
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
