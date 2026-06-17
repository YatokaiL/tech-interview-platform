import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { UserLookup } from "@/types/api";

export function useUserProfile(userId: string | null | undefined) {
  return useQuery({
    queryKey: ["user-profile", userId],
    queryFn: async () => {
      if (!userId) return null;
      const res = await api.get<UserLookup>(`/users/${userId}/profile`);
      return res.data;
    },
    enabled: !!userId,
    staleTime: 60_000,
  });
}

export function userDisplay(profile: UserLookup | null | undefined, fallbackId?: string | null) {
  if (!profile) return fallbackId ?? "—";
  const name = [profile.firstName, profile.lastName].filter(Boolean).join(" ").trim();
  if (name && profile.email) return `${name} (${profile.email})`;
  return name || profile.email || fallbackId || "—";
}
