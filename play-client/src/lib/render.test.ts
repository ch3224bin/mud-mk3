import { describe, expect, it } from "vitest";
import type { RoomMessage } from "./protocol";
import { echoLine, renderMessage } from "./render";

function roomMessage(overrides: Partial<RoomMessage> = {}): RoomMessage {
  return {
    type: "room",
    zone: { id: 1, name: "청석숲" },
    zoneEntered: true,
    room: { id: 3, name: "청석숲 오솔길", description: "늙은 소나무 사이로 좁은 길이 굽어 있다." },
    owner: null,
    exits: ["북", "남동"],
    occupants: [{ name: "한립", kind: "player" }],
    items: [],
    ...overrides,
  };
}

describe("renderMessage", () => {
  it("room: 지역에 들어왔으면 지역 줄부터 D8 순서로 그린다", () => {
    expect(renderMessage(roomMessage())).toEqual([
      { text: "◇ 청석숲", style: "zone" },
      { text: "청석숲 오솔길", style: "room-name" },
      { text: "늙은 소나무 사이로 좁은 길이 굽어 있다.", style: "description" },
      { text: "출구: 북 남동", style: "exits" },
      { text: "한립", style: "occupant" },
    ]);
  });

  it("room: 같은 지역 안이면 지역 줄이 없다", () => {
    const lines = renderMessage(roomMessage({ zoneEntered: false, occupants: [] }));

    expect(lines).toEqual([
      { text: "청석숲 오솔길", style: "room-name" },
      { text: "늙은 소나무 사이로 좁은 길이 굽어 있다.", style: "description" },
      { text: "출구: 북 남동", style: "exits" },
    ]);
  });

  it("room: 출구가 없으면 '출구: 없음'", () => {
    const lines = renderMessage(roomMessage({ zoneEntered: false, exits: [], occupants: [] }));

    expect(lines).toContainEqual({ text: "출구: 없음", style: "exits" });
  });

  it("room: 다른 사람은 한 줄에 하나씩, 들어온 순서대로", () => {
    const lines = renderMessage(
      roomMessage({
        zoneEntered: false,
        occupants: [
          { name: "한립", kind: "player" },
          { name: "남궁완", kind: "player" },
        ],
      }),
    );

    expect(lines.slice(-2)).toEqual([
      { text: "한립", style: "occupant" },
      { text: "남궁완", style: "occupant" },
    ]);
  });

  it("room: 소유가 있으면 방 이름 줄 뒤에 두 칸 띄우고 [소유 이름]", () => {
    const lines = renderMessage(
      roomMessage({
        zoneEntered: false,
        room: { id: 7, name: "청석성 남문", description: "돌로 쌓은 성문이 우뚝 서 있다." },
        owner: { kind: "castle", name: "청석성" },
        occupants: [],
      }),
    );

    expect(lines[0]).toEqual({ text: "청석성 남문  [청석성]", style: "room-name" });
  });

  it("room: NPC 줄은 사람 다음, style 은 npc", () => {
    const lines = renderMessage(
      roomMessage({
        zoneEntered: false,
        occupants: [
          { name: "한립", kind: "player" },
          { name: "왕씨", kind: "npc" },
        ],
      }),
    );

    expect(lines.slice(-2)).toEqual([
      { text: "한립", style: "occupant" },
      { text: "왕씨", style: "npc" },
    ]);
  });

  it("room: 바닥 물건 줄은 NPC 다음, 2개 이상이면 'N개', style 은 item", () => {
    const lines = renderMessage(
      roomMessage({
        zoneEntered: false,
        occupants: [{ name: "왕씨", kind: "npc" }],
        items: [
          { name: "하품 영석", count: 3 },
          { name: "비검", count: 1 },
        ],
      }),
    );

    expect(lines.slice(-3)).toEqual([
      { text: "왕씨", style: "npc" },
      { text: "하품 영석 3개", style: "item" },
      { text: "비검", style: "item" },
    ]);
  });

  it("inventory: text 를 줄마다 나누고 style 은 item", () => {
    const lines = renderMessage({
      type: "inventory",
      usedSlots: 2,
      totalSlots: 20,
      stones: [{ name: "하품 영석", count: 30 }],
      items: [
        { name: "회춘단(연기 초)", count: 3, bound: false, quest: false },
        { name: "비검", count: 1, bound: true, quest: false },
      ],
      text: "[소지품 2/20칸]  하품 영석 30\n회춘단(연기 초) x3   비검 (귀속)",
      tone: "item",
    });

    expect(lines).toEqual([
      { text: "[소지품 2/20칸]  하품 영석 30", style: "item" },
      { text: "회춘단(연기 초) x3   비검 (귀속)", style: "item" },
    ]);
  });

  it("system: tone item 은 style item", () => {
    expect(renderMessage({ type: "system", text: "늑대 가죽 2개를 주웠습니다.", tone: "item" })).toEqual([
      { text: "늑대 가죽 2개를 주웠습니다.", style: "item" },
    ]);
  });

  it("notice: tone item 은 style item", () => {
    const lines = renderMessage({
      type: "notice",
      text: "한백이 늑대 가죽 2개를 줍는다.",
      tone: "item",
      actor: "한백",
      event: "pick",
    });

    expect(lines).toEqual([{ text: "한백이 늑대 가죽 2개를 줍는다.", style: "item" }]);
  });

  it("talk: text 를 줄마다 나누고 style 은 talk", () => {
    const lines = renderMessage({
      type: "talk",
      npc: "왕씨",
      word: null,
      text: '왕씨: "어서 오시오."\n물어볼 수 있는 낱말: [단약] [일거리]',
      tone: "talk",
    });

    expect(lines).toEqual([
      { text: '왕씨: "어서 오시오."', style: "talk" },
      { text: "물어볼 수 있는 낱말: [단약] [일거리]", style: "talk" },
    ]);
  });

  it("system: 여러 줄 text(누구 출력)를 줄마다 나눈다", () => {
    const lines = renderMessage({
      type: "system",
      text: "접속한 사람 (2)\n  한립    청석숲\n  한백    청우촌",
      tone: "info",
    });

    expect(lines).toEqual([
      { text: "접속한 사람 (2)", style: "info" },
      { text: "  한립    청석숲", style: "info" },
      { text: "  한백    청우촌", style: "info" },
    ]);
  });

  it("system: tone 이 style 이 된다", () => {
    expect(renderMessage({ type: "system", text: "그쪽으로는 길이 없습니다.", tone: "error" })).toEqual([
      { text: "그쪽으로는 길이 없습니다.", style: "error" },
    ]);
  });

  it("say: text 를 그대로, style 은 speech", () => {
    const lines = renderMessage({
      type: "say",
      speaker: "한백",
      message: "길을 묻겠소.",
      self: false,
      text: '한백이 말한다: "길을 묻겠소."',
      tone: "speech",
    });

    expect(lines).toEqual([{ text: '한백이 말한다: "길을 묻겠소."', style: "speech" }]);
  });

  it("notice: text 를 tone(movement) 으로", () => {
    const lines = renderMessage({
      type: "notice",
      text: "한백이 북쪽으로 떠난다.",
      tone: "movement",
      actor: "한백",
      event: "leave",
    });

    expect(lines).toEqual([{ text: "한백이 북쪽으로 떠난다.", style: "movement" }]);
  });

  it("prompt: 줄마다 prompt style", () => {
    const lines = renderMessage({ type: "prompt", text: "이름 한백\n이대로 만들려면 '확인', 고치려면 '뒤로'." });

    expect(lines).toEqual([
      { text: "이름 한백", style: "prompt" },
      { text: "이대로 만들려면 '확인', 고치려면 '뒤로'.", style: "prompt" },
    ]);
  });

  it("error: error style", () => {
    expect(renderMessage({ type: "error", code: "BAD_MESSAGE", text: "알 수 없는 메시지입니다." })).toEqual([
      { text: "알 수 없는 메시지입니다.", style: "error" },
    ]);
  });

  it("session: 그릴 줄이 없다", () => {
    expect(renderMessage({ type: "session", state: "playing", characterName: "한백" })).toEqual([]);
  });
});

describe("echoLine", () => {
  it("입력 줄 앞에 '> ' 를 붙인다", () => {
    expect(echoLine("북")).toEqual({ text: "> 북", style: "echo" });
  });
});
