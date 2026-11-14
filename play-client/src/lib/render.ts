import type { RoomMessage, ServerMessage } from "./protocol";

/**
 * 출력 로그 한 줄. style 은 화면 CSS 클래스 `line-<style>` 이 된다.
 * 서버 tone(info, error, movement, speech, talk, item) 을 그대로 쓰고, 방 보기에는 자체 이름(zone, room-name, description,
 * exits, occupant, npc)과 바닥 물건 줄의 item 을, 그 밖에 prompt·echo 를 쓴다.
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

/** D8·D24·D51 방 보기 순서: (지역) · 방 이름(소유가 있으면 "  [소유]") · 묘사 · 출구 · 다른 사람 · NPC · 바닥 물건 */
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
  for (const item of msg.items) {
    lines.push({ text: item.count > 1 ? `${item.name} ${item.count}개` : item.name, style: "item" });
  }
  return lines;
}

function splitLines(text: string, style: string): OutputLine[] {
  return text.split("\n").map((line) => ({ text: line, style }));
}
