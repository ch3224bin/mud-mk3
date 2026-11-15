import type { Corpse, Mob, RoomMessage, ServerMessage } from "./protocol";

/**
 * 출력 로그 한 줄. style 은 화면 CSS 클래스 `line-<style>` 이 된다.
 * 서버 tone(info, error, movement, speech, talk, item, 4차 status·growth, 5차 combat) 을 그대로 쓰고, 방 보기에는 자체 이름(zone, room-name,
 * description, exits, occupant, npc, 5차 몹·시체 줄의 mob)과 바닥 물건 줄의 item 을, 그 밖에 prompt·echo 를 쓴다.
 */
export type OutputLine = { text: string; style: string };

export function renderMessage(msg: ServerMessage): OutputLine[] {
  switch (msg.type) {
    case "room":
      return renderRoom(msg);
    case "system":
    case "notice":
    case "say":
    case "talk":
    case "inventory":
    case "status":
      return splitLines(msg.text, msg.tone);
    case "prompt":
      return splitLines(msg.text, "prompt");
    case "error":
      return splitLines(msg.text, "error");
    case "session":
      return [];
  }
}

/** 내가 친 입력 줄의 메아리 */
export function echoLine(input: string): OutputLine {
  return { text: `> ${input}`, style: "echo" };
}

/** D8·D24·D51·D89 방 보기 순서: (지역) · 방 이름(소유가 있으면 "  [소유]") · 묘사 · 출구 · 다른 사람 · NPC · 몹 · 시체 · 바닥 물건 */
function renderRoom(msg: RoomMessage): OutputLine[] {
  const lines: OutputLine[] = [];
  if (msg.zoneEntered) {
    lines.push({ text: `◇ ${msg.zone.name}`, style: "zone" });
  }
  const roomName = msg.owner ? `${msg.room.name}  [${msg.owner.name}]` : msg.room.name;
  lines.push({ text: roomName, style: "room-name" });
  lines.push({ text: msg.room.description, style: "description" });
  lines.push({ text: `출구: ${msg.exits.join(" ") || "없음"}`, style: "exits" });
  for (const occupant of msg.occupants) {
    lines.push({ text: occupant.name, style: occupant.kind === "npc" ? "npc" : "occupant" });
  }
  for (const mob of msg.mobs) {
    lines.push({ text: mobLine(mob), style: "mob" });
  }
  for (const corpse of msg.corpses) {
    lines.push({ text: corpse.collectible ? `${corpse.name} (수습 가능)` : corpse.name, style: "mob" });
  }
  for (const item of msg.items) {
    lines.push({ text: item.count > 1 ? `${item.name} ${formatCount(item.count)}개` : item.name, style: "item" });
  }
  return lines;
}

/** D90 몹 줄: `이름 (급[, 전투 중])` + 체급 ` [강적]` + 2마리 이상이면 ` ×N` */
function mobLine(mob: Mob): string {
  const tier = mob.tier === null ? "" : ` [${mob.tier}]`;
  const count = mob.count >= 2 ? ` ×${mob.count}` : "";
  return `${mob.name} (${mob.rank}${mob.fighting ? ", 전투 중" : ""})${tier}${count}`;
}

/** 서버 Numbers.count(%,d, Locale.ROOT) 와 같은 천 단위 쉼표. 실행 환경 로캘에 기대지 않는다. */
function formatCount(value: number): string {
  return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

function splitLines(text: string, style: string): OutputLine[] {
  return text.split("\n").map((line) => ({ text: line, style }));
}
