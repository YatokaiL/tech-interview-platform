import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, Loader2, CreditCard, Ban, Sparkles } from "lucide-react";
import { toast } from "sonner";
import { isAxiosError } from "axios";
import { api } from "@/lib/api";
import type {
  ApiError,
  Payment,
  Subscription,
  SubscriptionRequest,
  Tariff,
} from "@/types/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

function errorMessage(err: unknown, fallback: string) {
  return isAxiosError<ApiError>(err) && err.response?.data?.message
    ? err.response.data.message
    : fallback;
}

function formatPrice(cents: number, currency: string) {
  const value = (cents / 100).toLocaleString("ru-RU");
  const sign = currency === "RUB" ? "₽" : currency;
  return `${value} ${sign}`;
}

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Ожидает оплаты",
  ACTIVE: "Активна",
  EXPIRED: "Истекла",
  CANCELLED: "Отменена",
};

const STATUS_VARIANT: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  PENDING: "secondary",
  ACTIVE: "default",
  EXPIRED: "outline",
  CANCELLED: "destructive",
};

export function SubscriptionPage() {
  const queryClient = useQueryClient();

  const { data: tariffs = [], isLoading: tariffsLoading } = useQuery({
    queryKey: ["tariffs"],
    queryFn: async () => (await api.get<Tariff[]>("/tariffs")).data,
  });

  const { data: current, isLoading: currentLoading } = useQuery({
    queryKey: ["subscription-current"],
    queryFn: async () => {
      const res = await api.get<Subscription | "">("/subscriptions/current");
      return res.data && typeof res.data === "object" ? (res.data as Subscription) : null;
    },
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["subscription-current"] });
  };

  const subscribeMutation = useMutation({
    mutationFn: async (tariffId: string) => {
      const body: SubscriptionRequest = { tariffId, periodMonths: 1 };
      return (await api.post<Subscription>("/subscriptions", body)).data;
    },
    onSuccess: () => {
      toast.success("Подписка создана — осталось оплатить");
      invalidate();
    },
    onError: (err) => toast.error(errorMessage(err, "Не удалось оформить подписку")),
  });

  const payMutation = useMutation({
    mutationFn: async (paymentId: string) =>
      (await api.post<Subscription>(`/payments/${paymentId}/confirm`)).data,
    onSuccess: () => {
      toast.success("Оплата прошла — подписка активирована");
      invalidate();
    },
    onError: (err) => toast.error(errorMessage(err, "Не удалось провести оплату")),
  });

  const cancelMutation = useMutation({
    mutationFn: async (subscriptionId: string) =>
      (await api.post<Subscription>(`/subscriptions/${subscriptionId}/cancel`)).data,
    onSuccess: () => {
      toast.success("Подписка отменена");
      invalidate();
    },
    onError: (err) => toast.error(errorMessage(err, "Не удалось отменить подписку")),
  });

  const pendingPayment: Payment | null = useMemo(() => {
    if (!current) return null;
    return current.payments.find((p) => p.status === "PENDING") ?? null;
  }, [current]);

  const activeTariffId = current?.status === "ACTIVE" ? current.tariffId : null;

  if (tariffsLoading || currentLoading) {
    return (
      <div className="flex justify-center py-12">
        <Loader2 className="animate-spin h-6 w-6 text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">Подписка</h1>
        <p className="text-muted-foreground mt-1">
          Тариф вашей компании и управление оплатой.
        </p>
      </div>

      {current ? (
        <Card>
          <CardHeader className="py-4">
            <CardTitle className="flex items-center justify-between text-lg">
              <span>Текущая подписка</span>
              <Badge variant={STATUS_VARIANT[current.status]}>
                {STATUS_LABEL[current.status] ?? current.status}
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-sm">
              <div>
                <span className="text-muted-foreground">Тариф: </span>
                <span className="font-medium">{current.tariff?.name ?? "—"}</span>
              </div>
              <div>
                <span className="text-muted-foreground">Период: </span>
                {current.periodMonths} мес.
              </div>
              {current.endsAt && (
                <div>
                  <span className="text-muted-foreground">Действует до: </span>
                  {new Date(current.endsAt).toLocaleDateString("ru-RU")}
                </div>
              )}
            </div>

            <div className="flex flex-wrap gap-2 pt-2">
              {pendingPayment && (
                <Button
                  onClick={() => payMutation.mutate(pendingPayment.id)}
                  disabled={payMutation.isPending}
                >
                  {payMutation.isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <CreditCard className="h-4 w-4" />
                  )}
                  Оплатить {formatPrice(pendingPayment.amountCents, pendingPayment.currency)}
                </Button>
              )}
              {(current.status === "ACTIVE" || current.status === "PENDING") && (
                <Button
                  variant="outline"
                  onClick={() => cancelMutation.mutate(current.id)}
                  disabled={cancelMutation.isPending}
                >
                  <Ban className="h-4 w-4" />
                  Отменить
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="py-6 text-sm text-muted-foreground">
            У компании пока нет подписки. Выберите тариф ниже.
          </CardContent>
        </Card>
      )}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {tariffs.map((t) => {
          const isActive = t.id === activeTariffId;
          return (
            <Card key={t.id} className={isActive ? "border-primary ring-1 ring-primary" : ""}>
              <CardHeader className="py-4">
                <CardTitle className="flex items-center justify-between text-lg">
                  {t.name}
                  {isActive && (
                    <Badge variant="default" className="gap-1">
                      <Sparkles className="h-3 w-3" /> ваш
                    </Badge>
                  )}
                </CardTitle>
                <div className="text-2xl font-semibold mt-1">
                  {formatPrice(t.priceCents, t.currency)}
                  <span className="text-sm font-normal text-muted-foreground"> / мес</span>
                </div>
              </CardHeader>
              <CardContent className="space-y-3">
                {t.description && (
                  <p className="text-xs text-muted-foreground">{t.description}</p>
                )}
                <ul className="space-y-1.5">
                  {t.features.map((f, i) => (
                    <li key={i} className="flex items-start gap-2 text-sm">
                      <Check className="h-4 w-4 mt-0.5 text-primary shrink-0" />
                      <span>{f}</span>
                    </li>
                  ))}
                </ul>
                <Button
                  className="w-full mt-2"
                  variant={isActive ? "outline" : "default"}
                  disabled={isActive || subscribeMutation.isPending}
                  onClick={() => subscribeMutation.mutate(t.id)}
                >
                  {subscribeMutation.isPending && subscribeMutation.variables === t.id ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : null}
                  {isActive ? "Текущий тариф" : "Выбрать"}
                </Button>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
