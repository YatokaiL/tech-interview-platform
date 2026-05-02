import { Navigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth";
import { ROLE_HOMES } from "@/lib/roles";

export function HomeRedirect() {
  const user = useAuthStore((s) => s.user);
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={ROLE_HOMES[user.role]} replace />;
}
