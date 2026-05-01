import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { isAxiosError } from "axios";
import { api } from "@/lib/api";
import type { ApiError, Role, UpdateUserRequest, User } from "@/types/api";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface Props {
  user: User | null;
  onClose: () => void;
}

export function EditUserDialog({ user, onClose }: Props) {
  const queryClient = useQueryClient();
  const [role, setRole] = useState<Role>("CANDIDATE");
  const [blocked, setBlocked] = useState(false);

  useEffect(() => {
    if (user) {
      setRole(user.role);
      setBlocked(user.blocked);
    }
  }, [user]);

  const mutation = useMutation({
    mutationFn: async (payload: UpdateUserRequest) => {
      if (!user) return null;
      return (await api.patch<User>(`/users/${user.id}`, payload)).data;
    },
    onSuccess: () => {
      toast.success("Пользователь обновлён");
      queryClient.invalidateQueries({ queryKey: ["users"] });
      onClose();
    },
    onError: (err) => {
      const msg =
        isAxiosError<ApiError>(err) && err.response?.data?.message
          ? err.response.data.message
          : "Не удалось обновить пользователя";
      toast.error(msg);
    },
  });

  function handleSubmit() {
    if (!user) return;
    const payload: UpdateUserRequest = {};
    if (role !== user.role) payload.role = role;
    if (blocked !== user.blocked) payload.blocked = blocked;
    if (Object.keys(payload).length === 0) {
      onClose();
      return;
    }
    mutation.mutate(payload);
  }

  return (
    <Dialog open={!!user} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Изменить пользователя</DialogTitle>
          <DialogDescription>{user?.email}</DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-2">
            <Label>Роль</Label>
            <Select value={role} onValueChange={(v) => setRole(v as Role)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="CANDIDATE">Кандидат</SelectItem>
                <SelectItem value="INTERVIEWER">Интервьюер</SelectItem>
                <SelectItem value="HR_MANAGER">HR-менеджер</SelectItem>
                <SelectItem value="ADMIN">Администратор</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="flex items-center justify-between rounded-lg border p-3">
            <div>
              <Label className="text-base">Блокировка</Label>
              <p className="text-sm text-muted-foreground">
                Заблокированный пользователь не сможет войти.
              </p>
            </div>
            <Button
              type="button"
              variant={blocked ? "destructive" : "outline"}
              onClick={() => setBlocked((b) => !b)}
            >
              {blocked ? "Заблокирован" : "Активен"}
            </Button>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Отмена
          </Button>
          <Button onClick={handleSubmit} disabled={mutation.isPending}>
            {mutation.isPending && <Loader2 className="animate-spin" />}
            Сохранить
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
