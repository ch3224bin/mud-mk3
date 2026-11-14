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
  /** 관리자가 둔 바닥 물건(id 순) */
  floorItems: FloorPlacementResponse[];
};
/** respawnMinutes 가 null 이면 한 번뿐, remaining 은 지금 시각으로 계산한 남은 수 */
export type FloorPlacementResponse = {
  id: number;
  roomId: number;
  itemTypeId: number;
  itemName: string;
  count: number;
  respawnMinutes: number | null;
  remaining: number;
};
/** respawnMinutes 가 null 이면 한 번뿐 */
export type FloorPlacementSaveRequest = { itemTypeId: number; count: number; respawnMinutes: number | null };
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
  /** 상점이 아니면 null */
  shopKindName: string | null;
};
export type ShopKindCode =
  | "PILL_SHOP"
  | "ARTIFACT_SHOP"
  | "BOOK_SHOP"
  | "TALISMAN_SHOP"
  | "MATERIAL_SHOP"
  | "GENERAL_STORE"
  | "BEAST_SHOP";
/** price 는 정한 값(없으면 null = 기준가를 따름), effectivePrice 는 실제로 파는 값 */
export type NpcShopItemResponse = { itemTypeId: number; itemName: string; price: number | null; effectivePrice: number };
export type NpcShopResponse = { kind: ShopKindCode; kindName: string; items: NpcShopItemResponse[] };
/** 파는 물건은 입력 순서. price 가 null 이면 기준가로 판다 */
export type ShopSaveRequest = { kind: ShopKindCode; items: { itemTypeId: number; price: number | null }[] };
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
  /** 상점이 아니면 null */
  shop: NpcShopResponse | null;
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
/** LOW·MIDDLE·HIGH 는 저물대, BRACELET_* 는 저물탁 */
export type BagGradeCode = "LOW" | "MIDDLE" | "HIGH" | "BRACELET_LOW" | "BRACELET_MIDDLE" | "BRACELET_HIGH";
/**
 * stackSize 는 저물대·저물탁 0, bagSlots 는 저물대·저물탁 밖 0, buyRatePercent 는 상점이 사지 않으면 0.
 * bagGradeName 은 `하급 저물대`·`하급 저물탁`, techniqueId·techniqueName 은 담긴 공법(없으면 null)
 */
export type ItemTypeResponse = {
  id: number;
  name: string;
  description: string;
  category: ItemCategoryCode;
  categoryName: string;
  stackSize: number;
  baseValue: number;
  bagGrade: BagGradeCode | null;
  bagGradeName: string | null;
  bagSlots: number;
  spiritStone: boolean;
  buyRatePercent: number;
  techniqueId: number | null;
  techniqueName: string | null;
};
/** bagGrade 는 종류가 저물대·저물탁일 때만, techniqueId 는 옥간·서책일 때만 보내고 나머지는 null */
export type ItemTypeSaveRequest = {
  name: string;
  description: string;
  category: ItemCategoryCode;
  baseValue: number;
  bagGrade: BagGradeCode | null;
  techniqueId: number | null;
};
export type StartingItem = { itemTypeId: number; count: number };
export type StartingItemResponse = { itemTypeId: number; itemName: string; count: number };

/** 경지 코드(낮은 경지부터) */
export type RealmCode =
  | "MORTAL"
  | "QI_1"
  | "QI_2"
  | "QI_3"
  | "QI_4"
  | "QI_5"
  | "QI_6"
  | "QI_7"
  | "QI_8"
  | "QI_9"
  | "QI_10"
  | "QI_11"
  | "QI_12"
  | "QI_13"
  | "FOUNDATION_EARLY"
  | "FOUNDATION_MIDDLE"
  | "FOUNDATION_LATE"
  | "FOUNDATION_PEAK";
/** name 은 `범인`·`연기 1층` 같은 화면 이름 */
export type RealmResponse = { code: RealmCode; name: string };

/** 오행(오행 순서) */
export type ElementCode = "METAL" | "WOOD" | "WATER" | "FIRE" | "EARTH";
export type AbilityKindCode =
  | "ATTACK"
  | "AREA_ATTACK"
  | "DEFENSE"
  | "EVASION"
  | "HEALING"
  | "ESCAPE"
  | "DETECTION"
  | "STEALTH"
  | "CONCEALMENT"
  | "BUFF";
export type AbilityTargetCode = "SELF" | "SINGLE" | "ALL_ENEMIES";
export type CostStatCode = "NONE" | "MANA" | "SENSE" | "ESSENCE";
export type UsableWhenCode = "COMBAT_ONLY" | "ANYTIME";

/** 공법 단계 한 줄. 1성의 insightRequired 는 0(읽으면 이름) */
export type TechniqueStage = {
  insightRequired: number;
  aptitudeRequired: number;
  manaBonus: number;
  senseBonus: number;
  spiritBonus: number;
  essenceBonus: number;
  bodyBonus: number;
  strengthBonus: number;
  aptitudeGain: number;
};
/** 신통 저장 값. bodyRefining·costPerTick 은 서버에서 필수라 늘 보낸다. costAmount 는 소모 없음이면 0 */
export type AbilitySaveRequest = {
  name: string;
  learnStage: number;
  kind: AbilityKindCode;
  elements: ElementCode[];
  bodyRefining: boolean;
  target: AbilityTargetCode;
  costStat: CostStatCode;
  costAmount: number;
  costPerTick: boolean;
  castSeconds: number;
  cooldownSeconds: number;
  usableWhen: UsableWhenCode;
};
export type AbilityResponse = AbilitySaveRequest & { kindName: string; targetName: string };
/** stages[0] 이 1성이고 길이가 단계 수다. 단계·신통은 저장할 때 통째로 바뀐다 */
export type TechniqueSaveRequest = {
  name: string;
  description: string;
  elements: ElementCode[];
  startRealm: RealmCode;
  stages: TechniqueStage[];
  abilities: AbilitySaveRequest[];
};
/** elementNames 는 `목`·`화·토`(연체술이면 null), lineName 은 `오행`·`연체술` */
export type TechniqueSummaryResponse = {
  id: number;
  name: string;
  elements: ElementCode[];
  elementNames: string | null;
  lineName: string;
  startRealm: RealmCode;
  startRealmName: string;
  stageCount: number;
  abilityCount: number;
  learnerCount: number;
};
/** scrollItems 는 이 공법이 담긴 아이템 종류(이름순) */
export type TechniqueDetailResponse = {
  id: number;
  name: string;
  description: string;
  elements: ElementCode[];
  elementNames: string | null;
  lineName: string;
  startRealm: RealmCode;
  startRealmName: string;
  stages: TechniqueStage[];
  abilities: AbilityResponse[];
  learnerCount: number;
  scrollItems: { id: number; name: string }[];
};

/** spiritRootName 은 `목·화 진영근`(정하지 않았으면 null), online 은 지금 접속해 세계에 있는지 */
export type CharacterSummaryResponse = {
  id: number;
  name: string;
  realm: RealmCode;
  realmName: string;
  spiritRootName: string | null;
  techniqueCount: number;
  online: boolean;
};
/** aptitude 는 처음 자질 + 공법 증가(소수) */
export type CharacterRootResponse = { element: ElementCode; elementName: string; aptitude: number };
/** 스탯 최대치(정수) */
export type CharacterStatsResponse = {
  qiBlood: number;
  mana: number;
  sense: number;
  essence: number;
  spirit: number;
  body: number;
  strength: number;
};
/** spiritRoot 는 정하지 않았으면 null, techniques 는 익힌 순서 */
export type CharacterDetailResponse = {
  id: number;
  name: string;
  realm: RealmCode;
  realmName: string;
  spiritRoot: { name: string; roots: CharacterRootResponse[] } | null;
  stats: CharacterStatsResponse;
  techniques: { id: number; name: string; stage: number; stageCount: number }[];
};

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

export const createFloorItem = (roomId: number, request: FloorPlacementSaveRequest) =>
  apiFetch<FloorPlacementResponse>(`/api/admin/rooms/${roomId}/floor-items`, jsonBody("POST", request));
/** 고치면 남은 수가 처음 수로 돌아간다 */
export const updateFloorItem = (floorItemId: number, request: FloorPlacementSaveRequest) =>
  apiFetch<FloorPlacementResponse>(`/api/admin/floor-items/${floorItemId}`, jsonBody("PUT", request));
export const deleteFloorItem = (floorItemId: number) =>
  apiFetch<void>(`/api/admin/floor-items/${floorItemId}`, { method: "DELETE" });

export const listNpcs = () => apiFetch<NpcSummaryResponse[]>("/api/admin/npcs");
export const getNpc = (npcId: number) => apiFetch<NpcDetailResponse>(`/api/admin/npcs/${npcId}`);
export const createNpc = (request: NpcSaveRequest) =>
  apiFetch<NpcDetailResponse>("/api/admin/npcs", jsonBody("POST", request));
export const updateNpc = (npcId: number, request: NpcSaveRequest) =>
  apiFetch<NpcDetailResponse>(`/api/admin/npcs/${npcId}`, jsonBody("PUT", request));
export const deleteNpc = (npcId: number) => apiFetch<void>(`/api/admin/npcs/${npcId}`, { method: "DELETE" });
export const saveShop = (npcId: number, request: ShopSaveRequest) =>
  apiFetch<NpcDetailResponse>(`/api/admin/npcs/${npcId}/shop`, jsonBody("PUT", request));
/** 상점이 없어도 204 */
export const deleteShop = (npcId: number) => apiFetch<void>(`/api/admin/npcs/${npcId}/shop`, { method: "DELETE" });

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

/** 경지 목록(낮은 경지부터) */
export const listRealms = () => apiFetch<RealmResponse[]>("/api/admin/realms");

/** 공법 목록(이름순) */
export const listTechniques = () => apiFetch<TechniqueSummaryResponse[]>("/api/admin/techniques");
export const getTechnique = (techniqueId: number) =>
  apiFetch<TechniqueDetailResponse>(`/api/admin/techniques/${techniqueId}`);
export const createTechnique = (request: TechniqueSaveRequest) =>
  apiFetch<TechniqueDetailResponse>("/api/admin/techniques", jsonBody("POST", request));
export const updateTechnique = (techniqueId: number, request: TechniqueSaveRequest) =>
  apiFetch<TechniqueDetailResponse>(`/api/admin/techniques/${techniqueId}`, jsonBody("PUT", request));
export const deleteTechnique = (techniqueId: number) =>
  apiFetch<void>(`/api/admin/techniques/${techniqueId}`, { method: "DELETE" });

/** 캐릭터 목록(이름순) */
export const listCharacters = () => apiFetch<CharacterSummaryResponse[]>("/api/admin/characters");
export const getCharacter = (characterId: number) =>
  apiFetch<CharacterDetailResponse>(`/api/admin/characters/${characterId}`);
/** 수위·돌파 없이 경지를 바로 지정한다. 지정한 뒤의 상세를 돌려준다 */
export const designateRealm = (characterId: number, realm: RealmCode) =>
  apiFetch<CharacterDetailResponse>(`/api/admin/characters/${characterId}/realm`, jsonBody("PUT", { realm }));

/** 화면에 보일 오류 문구. 서버가 준 ProblemDetail detail 을 그대로 쓴다. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.detail;
  }
  return "world-server에 연결하지 못했습니다.";
}
