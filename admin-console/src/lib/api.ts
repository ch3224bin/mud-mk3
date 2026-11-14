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

export type FactionKind = "CASTLE" | "SECT";
export type FactionResponse = {
  id: number;
  kind: FactionKind;
  kindName: string;
  name: string;
  description: string;
  zoneCount: number;
};
export type ZoneResponse = {
  id: number;
  name: string;
  description: string;
  roomCount: number;
  ownerId: number | null;
  ownerName: string | null;
  ownerKind: FactionKind | null;
};
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
  npcs: { id: number; name: string }[];
};
export type WorldResponse = {
  startRoomId: number | null;
  startRoomName: string | null;
  startZoneName: string | null;
};

export type NpcTopic = { word: string; answer: string };
export type NpcSummaryResponse = {
  id: number;
  name: string;
  llmEnabled: boolean;
  topicCount: number;
  roomId: number | null;
  roomName: string | null;
  zoneName: string | null;
};
export type NpcDetailResponse = {
  id: number;
  name: string;
  greeting: string;
  defaultAnswer: string;
  topics: NpcTopic[];
  llmEnabled: boolean;
  roomId: number | null;
  roomName: string | null;
  zoneId: number | null;
  zoneName: string | null;
};

export type ItemCategoryCode =
  | "ARTIFACT"
  | "PILL"
  | "ELIXIR"
  | "TALISMAN"
  | "FORMATION_DISK"
  | "SPIRIT_STONE"
  | "MATERIAL"
  | "SCROLL"
  | "TOOL"
  | "BAG"
  | "BEAST_EGG"
  | "BEAST_FOOD"
  | "PUPPET"
  | "PASS"
  | "QUEST";
export type BagGradeCode = "LOW" | "MIDDLE" | "HIGH";
/** stackSize 는 저물대 0, bagSlots 는 저물대 밖 0, buyRatePercent 는 상점이 사지 않으면 0 */
export type ItemTypeResponse = {
  id: number;
  name: string;
  description: string;
  category: ItemCategoryCode;
  categoryName: string;
  stackSize: number;
  baseValue: number;
  tradable: boolean;
  bagGrade: BagGradeCode | null;
  bagGradeName: string | null;
  bagSlots: number;
  spiritStone: boolean;
  buyRatePercent: number;
};
/** bagGrade 는 종류가 저물대일 때만 보내고 나머지는 null */
export type ItemTypeSaveRequest = {
  name: string;
  description: string;
  category: ItemCategoryCode;
  baseValue: number;
  tradable: boolean;
  bagGrade: BagGradeCode | null;
};
export type StartingItem = { itemTypeId: number; count: number };
export type StartingItemResponse = { itemTypeId: number; itemName: string; count: number };

export type FactionSaveRequest = { kind: FactionKind; name: string; description: string };
export type ZoneSaveRequest = { name: string; description: string; ownerId: number | null };
/** llmEnabled 는 서버에서 필수 값이라 늘 보낸다. roomId 가 null 이면 배치하지 않는다 */
export type NpcSaveRequest = {
  name: string;
  greeting: string;
  defaultAnswer: string;
  topics: NpcTopic[];
  llmEnabled: boolean;
  roomId: number | null;
};
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

export const listFactions = () => apiFetch<FactionResponse[]>("/api/admin/factions");
export const createFaction = (request: FactionSaveRequest) =>
  apiFetch<FactionResponse>("/api/admin/factions", jsonBody("POST", request));
export const updateFaction = (factionId: number, request: FactionSaveRequest) =>
  apiFetch<FactionResponse>(`/api/admin/factions/${factionId}`, jsonBody("PUT", request));
export const deleteFaction = (factionId: number) =>
  apiFetch<void>(`/api/admin/factions/${factionId}`, { method: "DELETE" });

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

export const listNpcs = () => apiFetch<NpcSummaryResponse[]>("/api/admin/npcs");
export const getNpc = (npcId: number) => apiFetch<NpcDetailResponse>(`/api/admin/npcs/${npcId}`);
export const createNpc = (request: NpcSaveRequest) =>
  apiFetch<NpcDetailResponse>("/api/admin/npcs", jsonBody("POST", request));
export const updateNpc = (npcId: number, request: NpcSaveRequest) =>
  apiFetch<NpcDetailResponse>(`/api/admin/npcs/${npcId}`, jsonBody("PUT", request));
export const deleteNpc = (npcId: number) => apiFetch<void>(`/api/admin/npcs/${npcId}`, { method: "DELETE" });

export const getWorld = () => apiFetch<WorldResponse>("/api/admin/world");
export const setStartRoom = (roomId: number) =>
  apiFetch<WorldResponse>("/api/admin/world/start-room", jsonBody("PUT", { roomId }));

export const listItemTypes = () => apiFetch<ItemTypeResponse[]>("/api/admin/item-types");
export const getItemType = (itemTypeId: number) => apiFetch<ItemTypeResponse>(`/api/admin/item-types/${itemTypeId}`);
export const createItemType = (request: ItemTypeSaveRequest) =>
  apiFetch<ItemTypeResponse>("/api/admin/item-types", jsonBody("POST", request));
export const updateItemType = (itemTypeId: number, request: ItemTypeSaveRequest) =>
  apiFetch<ItemTypeResponse>(`/api/admin/item-types/${itemTypeId}`, jsonBody("PUT", request));
export const deleteItemType = (itemTypeId: number) =>
  apiFetch<void>(`/api/admin/item-types/${itemTypeId}`, { method: "DELETE" });

/** 시작 소지품(입력 순서). 저장은 목록을 통째로 바꾼다 */
export const getStartingItems = () => apiFetch<StartingItemResponse[]>("/api/admin/world/starting-items");
export const saveStartingItems = (items: StartingItem[]) =>
  apiFetch<StartingItemResponse[]>("/api/admin/world/starting-items", jsonBody("PUT", { items }));

/** 화면에 보일 오류 문구. 서버가 준 ProblemDetail detail 을 그대로 쓴다. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.detail;
  }
  return "world-server에 연결하지 못했습니다.";
}
