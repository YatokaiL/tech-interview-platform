export type Role = "CANDIDATE" | "INTERVIEWER" | "HR_MANAGER" | "ADMIN";

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

export interface User {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  role: Role;
  companyId: string | null;
  companyName?: string | null;
  blocked: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  role: "CANDIDATE" | "HR_MANAGER";
  companyName?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  role?: Role;
  blocked?: boolean;
}

export interface ApiError {
  error: string;
  message: string;
  violations?: { field: string; message: string }[];
  timestamp: string;
}
