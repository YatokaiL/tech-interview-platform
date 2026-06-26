import { BrowserRouter, Route, Routes } from "react-router-dom";
import { Toaster } from "sonner";
import { Layout } from "@/components/Layout";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { LoginPage } from "@/pages/LoginPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { HomeRedirect } from "@/pages/HomeRedirect";
import { ProfilePage } from "@/pages/ProfilePage";
import { AdminDashboard } from "@/pages/AdminDashboard";
import { UsersPage } from "@/pages/admin/UsersPage";
import { TasksPage } from "@/pages/tasks/TasksPage";
import { TaskDetailPage } from "@/pages/tasks/TaskDetailPage";
import { TaskFormPage } from "@/pages/tasks/TaskFormPage";
import { SessionsPage } from "@/pages/sessions/SessionsPage";
import { SessionFormPage } from "@/pages/sessions/SessionFormPage";
import { SessionDetailPage } from "@/pages/sessions/SessionDetailPage";
import { SessionReviewPage } from "@/pages/sessions/SessionReviewPage";
import { SubscriptionPage } from "@/pages/SubscriptionPage";
import { AnalyticsDashboard } from "@/pages/AnalyticsDashboard";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            <Route path="/" element={<HomeRedirect />} />
            <Route path="/profile" element={<ProfilePage />} />

            <Route path="/sessions" element={<SessionsPage />} />
            <Route path="/sessions/:id" element={<SessionDetailPage />} />
            <Route path="/sessions/:id/review" element={<SessionReviewPage />} />
            <Route
              element={<ProtectedRoute allowedRoles={["INTERVIEWER", "HR_MANAGER", "ADMIN"]} />}
            >
              <Route path="/sessions/new" element={<SessionFormPage />} />
            </Route>

            <Route
              element={<ProtectedRoute allowedRoles={["INTERVIEWER", "HR_MANAGER", "ADMIN"]} />}
            >
              <Route path="/tasks" element={<TasksPage />} />
              <Route path="/tasks/new" element={<TaskFormPage />} />
              <Route path="/tasks/:id/edit" element={<TaskFormPage />} />
            </Route>
            <Route path="/tasks/:id" element={<TaskDetailPage />} />

            <Route element={<ProtectedRoute allowedRoles={["HR_MANAGER", "ADMIN"]} />}>
              <Route path="/subscription" element={<SubscriptionPage />} />
            </Route>

            <Route element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route path="/admin" element={<AdminDashboard />} />
              <Route path="/admin/users" element={<UsersPage />} />
              <Route path="/admin/analytics" element={<AnalyticsDashboard />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<HomeRedirect />} />
      </Routes>
      <Toaster position="top-right" richColors />
    </BrowserRouter>
  );
}
