import { worldServerUrl } from "./config";
import type { DirectionCode } from "./directions";

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

export type ZoneResponse = { id: number; name: string; description: string; roomCount: number };
export type RoomSummaryResponse = { id: number; name: string; exitCount: number };
export type ExitResponse = {
  direction: DirectionCode;
  directionName: string;
  targetRoomId: number;
  targetRoomName: string;
  targetZoneName: string;
};
export type RoomDetailResponse = {
  id: number;
  zoneId: number;
  zoneName: string;
  name: string;
  description: string;
  start: boolean;
  exits: ExitResponse[];
};
export type WorldResponse = {
  startRoomId: number | null;
  startRoomName: string | null;
  startZoneName: string | null;
};

export type ZoneSaveRequest = { name: string; description: string };
export type RoomSaveRequest = { zoneId: number; name: string; description: string };
export type ExitSaveRequest = { targetRoomId: number; twoWay: boolean };

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

function jsonBody(method: string, body: unknown): RequestInit {
  return { method, body: JSON.stringify(body) };
}

export const listZones = () => apiFetch<ZoneResponse[]>("/api/admin/zones");
export const createZone = (request: ZoneSaveRequest) =>
  apiFetch<ZoneResponse>("/api/admin/zones", jsonBody("POST", request));
export const updateZone = (zoneId: number, request: ZoneSaveRequest) =>
  apiFetch<ZoneResponse>(`/api/admin/zones/${zoneId}`, jsonBody("PUT", request));
export const deleteZone = (zoneId: number) => apiFetch<void>(`/api/admin/zones/${zoneId}`, { method: "DELETE" });

export const listRooms = (zoneId: number) => apiFetch<RoomSummaryResponse[]>(`/api/admin/zones/${zoneId}/rooms`);
export const getRoom = (roomId: number) => apiFetch<RoomDetailResponse>(`/api/admin/rooms/${roomId}`);
export const createRoom = (request: RoomSaveRequest) =>
  apiFetch<RoomDetailResponse>("/api/admin/rooms", jsonBody("POST", request));
export const updateRoom = (roomId: number, request: RoomSaveRequest) =>
  apiFetch<RoomDetailResponse>(`/api/admin/rooms/${roomId}`, jsonBody("PUT", request));
export const deleteRoom = (roomId: number) => apiFetch<void>(`/api/admin/rooms/${roomId}`, { method: "DELETE" });

export const putExit = (roomId: number, direction: DirectionCode, request: ExitSaveRequest) =>
  apiFetch<RoomDetailResponse>(`/api/admin/rooms/${roomId}/exits/${direction}`, jsonBody("PUT", request));
export const deleteExit = (roomId: number, direction: DirectionCode, twoWay: boolean) =>
  apiFetch<void>(`/api/admin/rooms/${roomId}/exits/${direction}?twoWay=${twoWay}`, { method: "DELETE" });

export const getWorld = () => apiFetch<WorldResponse>("/api/admin/world");
export const setStartRoom = (roomId: number) =>
  apiFetch<WorldResponse>("/api/admin/world/start-room", jsonBody("PUT", { roomId }));

/** 화면에 보일 오류 문구. 서버가 준 ProblemDetail detail 을 그대로 쓴다. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.detail;
  }
  return "world-server에 연결하지 못했습니다.";
}
