import { ApiError, apiFetch } from "./api";
import { worldServerUrl } from "./config";

/** 로그인 상태는 world-server 세션 쿠키에 있다. 여기서는 토큰을 저장하지 않는다. */
export type AuthMeResponse = { id: number; email: string; admin: boolean; characterName: string | null };

export function googleLoginUrl(): string {
  return `${worldServerUrl}/api/auth/login?client=admin`;
}

export async function fetchMe(): Promise<AuthMeResponse | null> {
  try {
    return await apiFetch<AuthMeResponse>("/api/auth/me");
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }
    throw error;
  }
}

export function loginForDevelopment(email: string): Promise<AuthMeResponse> {
  return apiFetch<AuthMeResponse>("/api/auth/dev", { method: "POST", body: JSON.stringify({ email }) });
}

export function logout(): Promise<void> {
  return apiFetch<void>("/api/auth/logout", { method: "POST" });
}
