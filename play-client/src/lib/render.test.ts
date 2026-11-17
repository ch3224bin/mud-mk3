import { describe, expect, it } from "vitest";
import { insightWith, zeroInsight } from "../testFixtures";
import type { NoticeMessage, Occupant, RoomMessage, SystemMessage } from "./protocol";
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
    mobs: [],
    corpses: [],
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

  it("room: 몹 줄은 NPC 다음, 시체 줄은 몹 다음, 바닥 물건은 시체 다음(5차 D89)", () => {
    const lines = renderMessage(
      roomMessage({
        zoneEntered: false,
        occupants: [
          { name: "한립", kind: "player" },
          { name: "왕씨", kind: "npc" },
        ],
        mobs: [{ name: "청모랑", rank: "1급", tier: null, count: 2, fighting: false }],
        corpses: [{ name: "청모랑의 시체", collectible: true }],
        items: [{ name: "늑대 가죽", count: 2 }],
      }),
    );

    expect(lines.slice(-5)).toEqual([
      { text: "한립", style: "occupant" },
      { text: "왕씨", style: "npc" },
      { text: "청모랑 (1급) ×2", style: "mob" },
      { text: "청모랑의 시체 (수습 가능)", style: "mob" },
      { text: "늑대 가죽 2개", style: "item" },
    ]);
  });

  it("room: 몹 줄은 '이름 (급[, 전투 중])' 에 체급 ' [강적]', 2마리 이상이면 ' ×N'(D90)", () => {
    const lines = renderMessage(
      roomMessage({
        zoneEntered: false,
        occupants: [],
        mobs: [
          { name: "적안 독사", rank: "1급", tier: "강적", count: 1, fighting: false },
          { name: "청모랑", rank: "1급", tier: null, count: 1, fighting: true },
          { name: "흑망", rank: "헤아릴 수 없음", tier: "두목", count: 1, fighting: false },
          { name: "떠도는 음혼", rank: "연기 10층", tier: "정예", count: 3, fighting: true },
        ],
      }),
    );

    expect(lines.slice(-4)).toEqual([
      { text: "적안 독사 (1급) [강적]", style: "mob" },
      { text: "청모랑 (1급, 전투 중)", style: "mob" },
      { text: "흑망 (헤아릴 수 없음) [두목]", style: "mob" },
      { text: "떠도는 음혼 (연기 10층, 전투 중) [정예] ×3", style: "mob" },
    ]);
  });

  it("room: 지금 수습할 수 없는 시체는 이름만", () => {
    const lines = renderMessage(
      roomMessage({
        zoneEntered: false,
        occupants: [],
        corpses: [
          { name: "청모랑의 시체", collectible: false },
          { name: "들토끼의 시체", collectible: true },
        ],
      }),
    );

    expect(lines.slice(-2)).toEqual([
      { text: "청모랑의 시체", style: "mob" },
      { text: "들토끼의 시체 (수습 가능)", style: "mob" },
    ]);
  });

  it("room: 바닥 물건 수는 천 단위 쉼표(서버 Numbers.count 와 같게)", () => {
    const lines = renderMessage(
      roomMessage({
        zoneEntered: false,
        items: [
          { name: "하품 영석", count: 50000 },
          { name: "늑대 가죽", count: 999 },
          { name: "중품 영석", count: 1234567 },
        ],
      }),
    );

    expect(lines.slice(-3)).toEqual([
      { text: "하품 영석 50,000개", style: "item" },
      { text: "늑대 가죽 999개", style: "item" },
      { text: "중품 영석 1,234,567개", style: "item" },
    ]);
  });

  it("inventory: text 를 줄마다 나누고 style 은 item", () => {
    const lines = renderMessage({
      type: "inventory",
      usedSlots: 2,
      totalSlots: 20,
      stones: [{ name: "하품 영석", count: 30 }],
      items: [
        { name: "회춘단(연기 초)", count: 3 },
        { name: "비검", count: 1 },
      ],
      text: "[소지품 2/20칸]  하품 영석 30\n회춘단(연기 초) x3   비검",
      tone: "item",
    });

    expect(lines).toEqual([
      { text: "[소지품 2/20칸]  하품 영석 30", style: "item" },
      { text: "회춘단(연기 초) x3   비검", style: "item" },
    ]);
  });

  it("status: text 를 줄마다 나누고 style 은 status", () => {
    const lines = renderMessage({
      type: "status",
      characterName: "한백",
      realm: { code: "QI_1", name: "연기 1층" },
      spiritRoot: {
        name: "목·화 진영근",
        roots: [
          { element: "WOOD", elementName: "목", aptitude: 50 },
          { element: "FIRE", elementName: "화", aptitude: 50 },
        ],
      },
      stats: {
        qiBlood: { current: 60, max: 60 },
        mana: { current: 25, max: 25 },
        sense: { current: 20, max: 20 },
        essence: { current: 20, max: 20 },
        spirit: { current: 5, max: 5 },
        body: 6,
        strength: 3,
      },
      cultivation: { current: 0, required: 190 },
      insight: zeroInsight(),
      text:
        "한백 · 연기 1층 · 목·화 진영근 (목 50, 화 50)\n" +
        "기혈  60 / 60     법력  25 / 25\n" +
        "의식  20 / 20     정혈  20 / 20\n" +
        "육신  6   근력  3   영력  5 / 5\n" +
        "수위  0 / 190\n" +
        "깨달음  금 0 · 목 0 · 수 0 · 화 0 · 토 0 · 연체 0",
      tone: "status",
    });

    expect(lines).toEqual([
      { text: "한백 · 연기 1층 · 목·화 진영근 (목 50, 화 50)", style: "status" },
      { text: "기혈  60 / 60     법력  25 / 25", style: "status" },
      { text: "의식  20 / 20     정혈  20 / 20", style: "status" },
      { text: "육신  6   근력  3   영력  5 / 5", style: "status" },
      { text: "수위  0 / 190", style: "status" },
      { text: "깨달음  금 0 · 목 0 · 수 0 · 화 0 · 토 0 · 연체 0", style: "status" },
    ]);
  });

  it("status: 범인은 cultivation 이 null 이고 수위 줄이 없다", () => {
    const lines = renderMessage({
      type: "status",
      characterName: "한백",
      realm: { code: "MORTAL", name: "범인" },
      spiritRoot: { name: "화 천영근", roots: [{ element: "FIRE", elementName: "화", aptitude: 100 }] },
      stats: {
        qiBlood: { current: 10, max: 10 },
        mana: { current: 0, max: 0 },
        sense: { current: 10, max: 10 },
        essence: { current: 10, max: 10 },
        spirit: { current: 0, max: 0 },
        body: 1,
        strength: 1,
      },
      cultivation: null,
      insight: zeroInsight(),
      text: "한백 · 범인 · 화 천영근 (화 100)\n기혈  10 / 10     법력  0 / 0",
      tone: "status",
    });

    expect(lines).toEqual([
      { text: "한백 · 범인 · 화 천영근 (화 100)", style: "status" },
      { text: "기혈  10 / 10     법력  0 / 0", style: "status" },
    ]);
  });

  it("system: tone growth 는 여러 줄을 줄마다 style growth", () => {
    const lines = renderMessage({
      type: "system",
      text: "옥간을 이마에 대자 구결이 머릿속으로 흘러든다. 장춘공 1성에 이르렀습니다!\n새 신통을 깨쳤습니다: 화탄술",
      tone: "growth",
    });

    expect(lines).toEqual([
      { text: "옥간을 이마에 대자 구결이 머릿속으로 흘러든다. 장춘공 1성에 이르렀습니다!", style: "growth" },
      { text: "새 신통을 깨쳤습니다: 화탄술", style: "growth" },
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

  it("system: tone combat 은 style combat", () => {
    expect(renderMessage({ type: "system", text: "청모랑을 친다. (피해 4)", tone: "combat" })).toEqual([
      { text: "청모랑을 친다. (피해 4)", style: "combat" },
    ]);
  });

  it("notice: tone combat 은 style combat", () => {
    const lines = renderMessage({
      type: "notice",
      text: "한백이 청모랑을 쓰러뜨린다.",
      tone: "combat",
      actor: "한백",
      event: "kill",
    });

    expect(lines).toEqual([{ text: "한백이 청모랑을 쓰러뜨린다.", style: "combat" }]);
  });

  it("status: insight 가 있어도 text 의 줄(쌓인 깨달음 줄 포함)을 그대로 그린다(6차)", () => {
    const lines = renderMessage({
      type: "status",
      characterName: "한백",
      realm: { code: "QI_1", name: "연기 1층" },
      spiritRoot: { name: "화 천영근", roots: [{ element: "FIRE", elementName: "화", aptitude: 100 }] },
      stats: {
        qiBlood: { current: 60, max: 60 },
        mana: { current: 20, max: 25 },
        sense: { current: 20, max: 20 },
        essence: { current: 20, max: 20 },
        spirit: { current: 5, max: 5 },
        body: 6,
        strength: 3,
      },
      cultivation: { current: 0, required: 190 },
      insight: insightWith("FIRE", 1234),
      text: "기혈  60 / 60     법력  20 / 25\n깨달음  금 0 · 목 0 · 수 0 · 화 1,234 · 토 0 · 연체 0",
      tone: "status",
    });

    expect(lines).toEqual([
      { text: "기혈  60 / 60     법력  20 / 25", style: "status" },
      { text: "깨달음  금 0 · 목 0 · 수 0 · 화 1,234 · 토 0 · 연체 0", style: "status" },
    ]);
  });

  it("system: 신통 시전 시작·이펙트 줄·흩어짐·거둠(tone combat)은 style combat(6차)", () => {
    expect(renderMessage({ type: "system", text: "낙뢰술을 펼치기 시작한다… (2초)", tone: "combat" })).toEqual([
      { text: "낙뢰술을 펼치기 시작한다… (2초)", style: "combat" },
    ]);
    expect(
      renderMessage({
        type: "system",
        text: "손끝에서 붉은 불덩이가 피어올라 청모랑을 덮친다! (피해 8, 법력 20/25)",
        tone: "combat",
      }),
    ).toEqual([{ text: "손끝에서 붉은 불덩이가 피어올라 청모랑을 덮친다! (피해 8, 법력 20/25)", style: "combat" }]);
    // 서버 AbilityTexts.SCATTERED 그대로
    expect(renderMessage({ type: "system", text: "펼치던 술법이 갈 곳을 잃고 흩어진다.", tone: "combat" })).toEqual([
      { text: "펼치던 술법이 갈 곳을 잃고 흩어진다.", style: "combat" },
    ]);
    expect(renderMessage({ type: "system", text: "펼치던 술법을 거두었습니다.", tone: "combat" })).toEqual([
      { text: "펼치던 술법을 거두었습니다.", style: "combat" },
    ]);
  });

  it("system: 신통·중지 거절(tone error)은 style error(6차)", () => {
    expect(renderMessage({ type: "system", text: "화탄술은 아직 쓸 수 없습니다. (1.4초)", tone: "error" })).toEqual([
      { text: "화탄술은 아직 쓸 수 없습니다. (1.4초)", style: "error" },
    ]);
    expect(renderMessage({ type: "system", text: "펼치는 술법이 없습니다.", tone: "error" })).toEqual([
      { text: "펼치는 술법이 없습니다.", style: "error" },
    ]);
  });

  it("system: 깨달음 한 상대 상한 줄(tone info)은 style info(6차)", () => {
    expect(renderMessage({ type: "system", text: "같은 상대에게서는 더 깨닫는 것이 없습니다.", tone: "info" })).toEqual([
      { text: "같은 상대에게서는 더 깨닫는 것이 없습니다.", style: "info" },
    ]);
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

  it("prompt: 영근 확인 prompt 네 줄", () => {
    const lines = renderMessage({
      type: "prompt",
      text:
        "목·화 진영근 — 목 50, 화 50\n" +
        "────────\n" +
        "이름 한백 · 영근 목·화 진영근 (목 50, 화 50)\n" +
        "이대로 만들려면 '확인', 고치려면 '뒤로'.",
    });

    expect(lines).toEqual([
      { text: "목·화 진영근 — 목 50, 화 50", style: "prompt" },
      { text: "────────", style: "prompt" },
      { text: "이름 한백 · 영근 목·화 진영근 (목 50, 화 50)", style: "prompt" },
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

describe("서버 값 전수 검사", () => {
  // Record 라서 protocol 에 값이 늘면 여기서 타입 오류가 난다. 서버 GameMessage 의 tone·kind 목록과 같다.
  const occupantStyles = { player: "occupant", npc: "npc" } satisfies Record<Occupant["kind"], string>;
  const systemTones = { info: 1, error: 1, item: 1, growth: 1, combat: 1 } satisfies Record<SystemMessage["tone"], 1>;
  const noticeTones = { movement: 1, item: 1, combat: 1 } satisfies Record<NoticeMessage["tone"], 1>;

  it.each(Object.entries(occupantStyles))("room: occupant kind %s 는 style %s", (kind, style) => {
    const lines = renderMessage(roomMessage({ occupants: [{ name: "누군가", kind: kind as Occupant["kind"] }] }));

    expect(lines.at(-1)).toEqual({ text: "누군가", style });
  });

  it.each(Object.keys(systemTones) as SystemMessage["tone"][])("system tone %s 는 그 이름의 style", (tone) => {
    expect(renderMessage({ type: "system", text: "글", tone })).toEqual([{ text: "글", style: tone }]);
  });

  it.each(Object.keys(noticeTones) as NoticeMessage["tone"][])("notice tone %s 는 그 이름의 style", (tone) => {
    expect(renderMessage({ type: "notice", text: "글", tone, actor: "한립", event: "pick" })).toEqual([
      { text: "글", style: tone },
    ]);
  });
});
