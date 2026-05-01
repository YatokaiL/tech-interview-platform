import { useQuery } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { api } from "@/lib/api";
import type { User } from "@/types/api";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ROLE_LABELS } from "@/lib/roles";

export function ProfilePage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["me"],
    queryFn: async () => (await api.get<User>("/users/me")).data,
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Loader2 className="animate-spin h-6 w-6 text-muted-foreground" />
      </div>
    );
  }

  if (isError || !data) {
    return <p className="text-destructive">Не удалось загрузить профиль.</p>;
  }

  const fullName =
    [data.firstName, data.lastName].filter(Boolean).join(" ").trim() || "—";

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Мой профиль</h1>
        <p className="text-muted-foreground mt-1">Информация о вашей учётной записи.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-xl">{fullName}</CardTitle>
          <CardDescription>{data.email}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Row label="Роль" value={<Badge>{ROLE_LABELS[data.role]}</Badge>} />
          <Row
            label="Компания"
            value={data.companyName ? data.companyName : "—"}
          />
          <Row label="Статус" value={data.blocked ? (
            <Badge variant="destructive">Заблокирован</Badge>
          ) : (
            <Badge variant="secondary">Активен</Badge>
          )} />
          <Row
            label="Зарегистрирован"
            value={new Date(data.createdAt).toLocaleString("ru-RU")}
          />
        </CardContent>
      </Card>
    </div>
  );
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between border-b last:border-0 pb-3 last:pb-0">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className="text-sm">{value}</span>
    </div>
  );
}
