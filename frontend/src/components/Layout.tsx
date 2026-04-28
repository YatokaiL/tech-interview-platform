import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { LogOut, User as UserIcon } from "lucide-react";
import { useAuthStore } from "@/stores/auth";
import { api } from "@/lib/api";
import { ROLE_LABELS, initials } from "@/lib/roles";
import {
  Avatar,
  AvatarFallback,
} from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const NAV_BY_ROLE: Record<string, { to: string; label: string }[]> = {
  ADMIN: [
    { to: "/admin", label: "Главная" },
    { to: "/admin/users", label: "Пользователи" },
  ],
  HR_MANAGER: [{ to: "/hr", label: "Сессии" }],
  INTERVIEWER: [{ to: "/interviewer", label: "Сессии" }],
  CANDIDATE: [{ to: "/candidate", label: "Мои интервью" }],
};

export function Layout() {
  const user = useAuthStore((s) => s.user);
  const clear = useAuthStore((s) => s.clear);
  const refreshToken = useAuthStore((s) => s.refreshToken);
  const navigate = useNavigate();

  const nav = user ? NAV_BY_ROLE[user.role] ?? [] : [];

  async function handleLogout() {
    if (refreshToken) {
      try {
        await api.post("/auth/logout", { refreshToken });
      } catch {
        // ignore — clear locally anyway
      }
    }
    clear();
    navigate("/login", { replace: true });
  }

  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b bg-card">
        <div className="container flex h-14 items-center justify-between">
          <div className="flex items-center gap-6">
            <Link to="/" className="font-semibold tracking-tight">
              Interview Platform
            </Link>
            <nav className="hidden md:flex items-center gap-1">
              {nav.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    cn(
                      "px-3 py-1.5 rounded-md text-sm font-medium transition-colors",
                      isActive
                        ? "bg-secondary text-secondary-foreground"
                        : "text-muted-foreground hover:text-foreground"
                    )
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>

          {user && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="px-2 gap-2">
                  <Avatar className="h-8 w-8">
                    <AvatarFallback>{initials(undefined, undefined, user.email)}</AvatarFallback>
                  </Avatar>
                  <span className="hidden sm:inline text-sm">{user.email}</span>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>
                  <div className="font-normal">
                    <div className="text-sm truncate">{user.email}</div>
                    <div className="text-xs text-muted-foreground">{ROLE_LABELS[user.role]}</div>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem onSelect={() => navigate("/profile")}>
                  <UserIcon className="h-4 w-4" />
                  Профиль
                </DropdownMenuItem>
                <DropdownMenuItem onSelect={handleLogout}>
                  <LogOut className="h-4 w-4" />
                  Выйти
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>
      </header>

      <main className="flex-1 container py-8">
        <Outlet />
      </main>
    </div>
  );
}
