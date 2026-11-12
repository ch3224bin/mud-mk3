import { worldServerUrl } from "./config";

export class ApiError extends Error {
  readonly status: number;
  readonly detail: string;

  constructor(status: number, detail: string) {
    super(detail);
    this.name = "ApiError";
    this.status = status;
    this.detail = detail;
  }
}

/** world-server REST 호출. 세션 쿠키를 보내고, 변경 요청에는 CSRF 대신 쓰는 X-MUD-Client 헤더를 붙인다. */
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  if (method !== "GET") {
    headers.set("X-MUD-Client", "1");
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(`${worldServerUrl}${path}`, { ...init, method, headers, credentials: "include" });
  if (!response.ok) {
    throw new ApiError(response.status, await readDetail(response));
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function readDetail(response: Response): Promise<string> {
  try {
    const body: unknown = await response.json();
    if (body && typeof body === "object" && typeof (body as { detail?: unknown }).detail === "string") {
      return (body as { detail: string }).detail;
    }
  } catch {
    // 본문이 JSON 이 아니면 아래 일반 문구를 쓴다.
  }
  return `요청을 처리하지 못했습니다. (${response.status})`;
}

/**
 * 화면에 보일 오류 문구. 서버가 준 ProblemDetail detail 을 그대로 쓴다.
 * fetch 자체가 실패한 것(TypeError)만 연결 실패로 보고, 그 밖의 예외는 원인을 콘솔에 남긴다.
 */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.detail;
  }
  if (error instanceof TypeError) {
    return "world-server에 연결하지 못했습니다.";
  }
  console.error(error);
  return "요청을 처리하지 못했습니다.";
}
