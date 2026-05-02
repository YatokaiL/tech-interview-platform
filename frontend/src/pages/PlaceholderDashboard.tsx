import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

interface Props {
  title: string;
  description: string;
  upcoming: string[];
}

export function PlaceholderDashboard({ title, description, upcoming }: Props) {
  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">{title}</h1>
        <p className="text-muted-foreground mt-1">{description}</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Скоро здесь появится</CardTitle>
          <CardDescription>
            Эти возможности добавятся по мере готовности соответствующих сервисов.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-2">
          {upcoming.map((item) => (
            <div key={item} className="flex items-center gap-3">
              <Badge variant="outline">скоро</Badge>
              <span className="text-sm">{item}</span>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
