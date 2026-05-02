import { create } from "zustand";
import { persist } from "zustand/middleware";
import { jwtDecode } from "jwt-decode";
import type { Role, TokenResponse } from "@/types/api";

interface JwtPayload {
  sub: string;
  email: string;
  role: Role;
  companyId?: string;
  exp: number;
  iat: number;
}

interface AuthUser {
  id: string;
  email: string;
  role: Role;
  companyId: string | null;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  setTokens: (tokens: TokenResponse) => void;
  clear: () => void;
  isAuthenticated: () => boolean;
}

function decode(token: string): AuthUser | null {
  try {
    const payload = jwtDecode<JwtPayload>(token);
    return {
      id: payload.sub,
      email: payload.email,
      role: payload.role,
      companyId: payload.companyId ?? null,
    };
  } catch {
    return null;
  }
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setTokens: ({ accessToken, refreshToken }) => {
        set({ accessToken, refreshToken, user: decode(accessToken) });
      },
      clear: () => set({ accessToken: null, refreshToken: null, user: null }),
      isAuthenticated: () => {
        const token = get().accessToken;
        if (!token) return false;
        try {
          const payload = jwtDecode<JwtPayload>(token);
          return payload.exp * 1000 > Date.now();
        } catch {
          return false;
        }
      },
    }),
    {
      name: "interview-platform-auth",
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    }
  )
);
