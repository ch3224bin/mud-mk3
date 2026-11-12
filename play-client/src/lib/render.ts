import type { RoomMessage, ServerMessage } from "./protocol";

/**
 * 출력 로그 한 줄. style 은 화면 CSS 클래스 `line-<style>` 이 된다.
 * 서버 tone(info, error, movement, speech) 을 그대로 쓰고, 방 보기에는 자체 이름(zone, room-name, description,
 * exits, occupant)을, 그 밖에 prompt·echo 를 쓴다.
 */
export type OutputLine = { text: string; style: string };

export function renderMessage(msg: ServerMessage): OutputLine[] {
  switch (msg.type) {
    case "room":
      return renderRoom(msg);
    case "system":
    case "notice":
    case "say":
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

/** D8 방 보기 순서: (지역) · 방 이름 · 묘사 · 출구 · 다른 사람 */
function renderRoom(msg: RoomMessage): OutputLine[] {
  const lines: OutputLine[] = [];
  if (msg.zoneEntered) {
    lines.push({ text: `◇ ${msg.zone.name}`, style: "zone" });
  }
  lines.push({ text: msg.room.name, style: "room-name" });
  lines.push({ text: msg.room.description, style: "description" });
  lines.push({ text: `출구: ${msg.exits.join(" ") || "없음"}`, style: "exits" });
  for (const occupant of msg.occupants) {
    lines.push({ text: occupant.name, style: "occupant" });
  }
  return lines;
}

function splitLines(text: string, style: string): OutputLine[] {
  return text.split("\n").map((line) => ({ text: line, style }));
}
