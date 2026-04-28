import { BrowserRouter, Route, Routes } from "react-router-dom";
import { Toaster } from "sonner";
import { Layout } from "@/components/Layout";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { LoginPage } from "@/pages/LoginPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { HomeRedirect } from "@/pages/HomeRedirect";
import { ProfilePage } from "@/pages/ProfilePage";
import { CandidateDashboard } from "@/pages/CandidateDashboard";
import { InterviewerDashboard } from "@/pages/InterviewerDashboard";
import { HrDashboard } from "@/pages/HrDashboard";
import { AdminDashboard } from "@/pages/AdminDashboard";
import { UsersPage } from "@/pages/admin/UsersPage";

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
            <Route element={<ProtectedRoute allowedRoles={["CANDIDATE"]} />}>
              <Route path="/candidate" element={<CandidateDashboard />} />
            </Route>
            <Route element={<ProtectedRoute allowedRoles={["INTERVIEWER"]} />}>
              <Route path="/interviewer" element={<InterviewerDashboard />} />
            </Route>
            <Route element={<ProtectedRoute allowedRoles={["HR_MANAGER"]} />}>
              <Route path="/hr" element={<HrDashboard />} />
            </Route>
            <Route element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              <Route path="/admin" element={<AdminDashboard />} />
              <Route path="/admin/users" element={<UsersPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<HomeRedirect />} />
      </Routes>
      <Toaster position="top-right" richColors />
    </BrowserRouter>
  );
}
