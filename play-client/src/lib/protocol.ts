/**
 * world-server 게임 WebSocket(/ws/game) 메시지(D5). 모든 메시지에 type 이 있다.
 * 서버 쪽 정의: world-server GameMessage(record 필드 = JSON 필드), GameWebSocketHandler(error).
 */

/** 클라이언트 → 서버: 입력 한 줄(최대 500자) */
export type CommandMessage = { type: "command"; text: string };

/**
 * 클라이언트 → 서버 인증 메시지. D4 에 따라 따로 보내는 인증 메시지는 없고,
 * 브라우저가 핸드셰이크에 세션 쿠키를 싣는 것으로 인증이 끝난다. 그래서 이 유니온은 비어 있다.
 */
export type AuthMessage = never;

export type ClientMessage = CommandMessage | AuthMessage;

export const COMMAND_MAX_LENGTH = 500;

export type SessionMessage = { type: "session"; state: "creating" | "playing"; characterName: string | null };

export type PromptMessage = { type: "prompt"; text: string };

export type Occupant = { name: string; kind: "player" | "npc" };

/** 방이 든 지역을 소유한 성(castle)·종문(sect) */
export type RoomOwner = { kind: "castle" | "sect"; name: string };

/** 방 바닥의 물건 한 묶음(3차 D40·D48) */
export type FloorItem = { name: string; count: number };

/**
 * 방의 몹 한 묶음(5차 D89). rank 는 `1급`·`연기 10층`·`헤아릴 수 없음`, tier 는 체급 표시(일반이면 null),
 * count 는 묶인 수, fighting 은 교전 중인지
 */
export type Mob = { name: string; rank: string; tier: "강적" | "정예" | "두목" | null; count: number; fighting: boolean };

/** 방의 시체(5차 D89). collectible 은 보는 사람이 지금 수습할 수 있는지 */
export type Corpse = { name: string; collectible: boolean };

export type RoomMessage = {
  type: "room";
  zone: { id: number; name: string };
  zoneEntered: boolean;
  room: { id: number; name: string; description: string };
  /** 지역 소유, 없으면 null */
  owner: RoomOwner | null;
  /** 방위 한글 이름, 순서 동 서 남 북 북동 북서 남동 남서 위 아래 */
  exits: string[];
  /** 나를 뺀 사람(들어온 순서) 다음 그 방의 NPC(만든 순서) */
  occupants: Occupant[];
  /** 살아 있는 몹(같은 유형·같은 교전 상태를 묶은 한 줄씩, 묶음이 처음 나온 순서), 없으면 [] */
  mobs: Mob[];
  /** 시체(쓰러진 순), 없으면 [] */
  corpses: Corpse[];
  /** 바닥 물건(D40 순서), 없으면 [] */
  items: FloorItem[];
};

/**
 * tone growth 는 읽기 성공·관리 툴 경지 지정 알림(4차 D71), combat 은 교전 시작·침·맞음·처치·질 때(5차 D90)와
 * 6차 신통 시전 시작·발동(이펙트 줄)·흩어짐·`중지` 로 거둠. 신통·`중지` 거절은 error, 깨달음 한 상대 상한은 info
 */
export type SystemMessage = { type: "system"; text: string; tone: "info" | "error" | "item" | "growth" | "combat" };

/**
 * 이동 이벤트(arrive·leave·appear·disappear), 물건 이벤트(pick·drop·give, 3차 D47, 5차 collect),
 * 교전 이벤트(engage·kill·defeated·respawn, 5차 D90)
 */
export type NoticeEvent =
  | "arrive"
  | "leave"
  | "appear"
  | "disappear"
  | "pick"
  | "drop"
  | "give"
  | "collect"
  | "engage"
  | "kill"
  | "defeated"
  | "respawn";

export type NoticeMessage = {
  type: "notice";
  text: string;
  tone: "movement" | "item" | "combat";
  actor: string;
  event: NoticeEvent;
};

/** 소지품(묻는 사람에게만, 3차 D48). stones 는 가진 등급만 하품부터, items 는 영석 밖 표시 순서, text 는 그린 줄들(\n) */
export type InventoryMessage = {
  type: "inventory";
  usedSlots: number;
  totalSlots: number;
  stones: { name: string; count: number }[];
  items: { name: string; count: number }[];
  text: string;
  tone: "item";
};

/** 현재·최대 수치 한 쌍 */
export type Gauge = { current: number; max: number };

/** 깨달음 한 갈래(6차). name 은 금·목·수·화·토·연체, amount 는 소수점 버린 정수 */
export type InsightEntry = { kind: "METAL" | "WOOD" | "WATER" | "FIRE" | "EARTH" | "BODY"; name: string; amount: number };

/**
 * 상태(묻는 사람에게만, 4차 D71). 필드 순서는 서버 StatusMessage 와 같다.
 * roots 는 오행 순, aptitude 는 소수점 버린 정수. cultivation 은 범인이면 null. text 는 그린 줄들(\n).
 * stats.mana.current 는 저장된 현재 법력(6차). insight 는 여섯 갈래가 늘 금·목·수·화·토·연체 순(6차) —
 * 6차 화면은 쓰지 않고 text 를 그린다.
 */
export type StatusMessage = {
  type: "status";
  characterName: string;
  realm: { code: string; name: string };
  spiritRoot: { name: string; roots: { element: string; elementName: string; aptitude: number }[] };
  stats: {
    qiBlood: Gauge;
    mana: Gauge;
    sense: Gauge;
    essence: Gauge;
    spirit: Gauge;
    body: number;
    strength: number;
  };
  cultivation: { current: number; required: number } | null;
  insight: InsightEntry[];
  text: string;
  tone: "status";
};

export type SayMessage = {
  type: "say";
  speaker: string;
  message: string;
  self: boolean;
  text: string;
  tone: "speech";
};

/** NPC 대답(묻는 사람에게만). word 는 물은 낱말(인사면 null), text 는 그린 줄들(\n 으로 나뉨) */
export type TalkMessage = { type: "talk"; npc: string; word: string | null; text: string; tone: "talk" };

export type ErrorMessage = { type: "error"; code: "AUTH_REQUIRED" | "BAD_MESSAGE"; text: string };

export type ServerMessage =
  | SessionMessage
  | PromptMessage
  | RoomMessage
  | SystemMessage
  | NoticeMessage
  | SayMessage
  | TalkMessage
  | InventoryMessage
  | StatusMessage
  | ErrorMessage;

/** 닫기 코드(D5·Task 7). */
export const CloseCode = {
  /** 종료 명령 */
  NORMAL: 1000,
  /** 시작 방이 없어 세계가 열리지 않음 */
  WORLD_NOT_READY: 1013,
  /** 로그인 안 됨(세션 없음·만료) */
  UNAUTHORIZED: 4401,
  /** 같은 계정이 다른 곳에서 접속해 밀려남 */
  REPLACED: 4409,
  /** 보내기가 밀리거나 실패해 서버가 끊음 */
  NOT_RELIABLE: 4500,
} as const;
