import { describe, expect, it } from "vitest";
import { COMMANDS, Completer, nameCandidates } from "./complete";

describe("Completer", () => {
  it("첫 낱말은 명령어에서 앞부분이 맞는 것으로 채운다", () => {
    expect(new Completer().complete("보", [])).toBe("보기");
  });

  it("초성 약자는 명령어 앞부분이 아니어서 Tab 이 입력을 그대로 둔다", () => {
    expect(new Completer().complete("ㄷ", [])).toBe("ㄷ");
    expect(new Completer().complete("ㅂ", [])).toBe("ㅂ");
    expect(new Completer().complete("ㅅㅌ", [])).toBe("ㅅㅌ");
  });

  it("대화 명령을 채운다", () => {
    expect(new Completer().complete("대", [])).toBe("대화");
  });

  it("대화 뒤 낱말은 이름(NPC 포함)에서 채운다", () => {
    expect(new Completer().complete("대화 왕", ["한립", "왕씨"])).toBe("대화 왕씨");
  });

  it("3차 물건 명령을 채운다", () => {
    expect(new Completer().complete("줍", [])).toBe("줍기");
    expect(new Completer().complete("소", [])).toBe("소지품");
  });

  it("COMMANDS 는 6차 도움말 순서(공격 다음 중지)", () => {
    expect(COMMANDS).toEqual([
      "보기", "말하기", "대화", "공격", "중지", "수습", "상태", "공법", "신통", "읽기", "소지품", "줍기", "버리기", "주기", "목록", "사기",
      "팔기", "값", "확인", "취소", "누구", "도움말", "종료",
      "동", "서", "남", "북", "북동", "북서", "남동", "남서", "위", "아래",
    ]);
  });

  it("4차 명령을 채운다", () => {
    expect(new Completer().complete("읽", [])).toBe("읽기");
    expect(new Completer().complete("상", [])).toBe("상태");
    expect(new Completer().complete("신", [])).toBe("신통");
  });

  it("5차 명령을 채운다: '공' 은 공격이 먼저, 거듭 부르면 공법", () => {
    const completer = new Completer();

    const first = completer.complete("공", []);
    const second = completer.complete(first, []);

    expect([first, second]).toEqual(["공격", "공법"]);
    expect(new Completer().complete("수", [])).toBe("수습");
  });

  it("6차 명령을 채운다: '중' 은 중지", () => {
    expect(new Completer().complete("중", [])).toBe("중지");
  });

  it("배운 신통 이름은 명령 후보에 없다", () => {
    expect(COMMANDS).not.toContain("화탄술");
    expect(new Completer().complete("화", [])).toBe("화");
  });

  it("공격 뒤 낱말은 몹 이름, 수습 뒤 낱말은 시체 이름에서 채운다", () => {
    const names = nameCandidates(["한립", "왕씨", "청모랑", "청모랑의 시체", "늑대 가죽"], ["비검"]);

    expect(new Completer().complete("공격 청", names)).toBe("공격 청모랑");
    const completer = new Completer();
    const first = completer.complete("수습 청", names);
    expect([first, completer.complete(first, names)]).toEqual(["수습 청모랑", "수습 청모랑의 시체"]);
  });

  it("5차 이름 후보 순서는 사람·NPC → 몹 → 시체 → 바닥 물건 → 소지품, 겹치면 앞 것만", () => {
    expect(nameCandidates(["한립", "왕씨", "청모랑", "청모랑의 시체", "늑대 가죽"], ["늑대 가죽", "비검"])).toEqual([
      "한립",
      "왕씨",
      "청모랑",
      "청모랑의 시체",
      "늑대 가죽",
      "비검",
    ]);
  });

  it("읽기 뒤 낱말은 마지막 inventory 의 물건 이름에서 채운다", () => {
    const names = nameCandidates(["한립"], ["장춘공 옥간", "비검"]);

    expect(new Completer().complete("읽기 장", names)).toBe("읽기 장춘공 옥간");
  });

  it("이름 후보 순서는 방 이름(occupants → 바닥 물건) 다음 소지품, 겹치면 앞에 나온 것만", () => {
    expect(nameCandidates(["한립", "왕씨", "늑대 가죽"], ["비검", "늑대 가죽", "장춘공 옥간", "비검"])).toEqual([
      "한립",
      "왕씨",
      "늑대 가죽",
      "비검",
      "장춘공 옥간",
    ]);
  });

  it("물건 이름은 이름 앞부분으로 맞춘다(이름 안 낱말 앞부분은 후보가 아니다)", () => {
    const names = ["늑대 가죽"];

    expect(new Completer().complete("줍기 가", names)).toBe("줍기 가");
    expect(new Completer().complete("줍기 늑", names)).toBe("줍기 늑대 가죽");
  });

  it("거듭 부르면 후보를 COMMANDS 순서로 돈다", () => {
    const completer = new Completer();

    const first = completer.complete("북", []);
    const second = completer.complete(first, []);
    const third = completer.complete(second, []);
    const fourth = completer.complete(third, []);

    expect([first, second, third, fourth]).toEqual(["북", "북동", "북서", "북"]);
  });

  it("뒤 낱말은 이름에서 채우고 거듭 부르면 다음 이름", () => {
    const completer = new Completer();
    const names = ["한립", "한백"];

    const first = completer.complete("말하기 한", names);
    const second = completer.complete(first, names);

    expect(first).toBe("말하기 한립");
    expect(second).toBe("말하기 한백");
  });

  it("입력이 바뀌면 처음 후보부터 다시 찾는다", () => {
    const completer = new Completer();
    completer.complete("북", []);

    expect(completer.complete("남", [])).toBe("남");
    expect(completer.complete("남", [])).toBe("남동");
  });

  it("reset 하면 같은 입력도 처음 후보부터", () => {
    const completer = new Completer();
    completer.complete("북", []);
    completer.reset();

    expect(completer.complete("북", [])).toBe("북");
  });

  it("후보가 없으면 입력 그대로", () => {
    const completer = new Completer();

    expect(completer.complete("뭐", [])).toBe("뭐");
    expect(completer.complete("말하기 진", ["한립"])).toBe("말하기 진");
  });

  it("채울 낱말이 비어 있으면 입력 그대로", () => {
    const completer = new Completer();

    expect(completer.complete("", [])).toBe("");
    expect(completer.complete("말하기 ", ["한립"])).toBe("말하기 ");
  });

  it("여러 낱말 이름은 앞 낱말까지 친 것으로 이름 전체를 채운다", () => {
    expect(new Completer().complete("줍기 늑대 가", ["늑대 가죽"])).toBe("줍기 늑대 가죽");
  });

  it("대상 뒤 여러 낱말 물건도 채운다", () => {
    expect(new Completer().complete("주기 한립 늑대 가", ["한립", "늑대 가죽"])).toBe("주기 한립 늑대 가죽");
  });

  it("가장 긴 꼬리가 먼저다", () => {
    expect(new Completer().complete("보기 청모랑의 시", ["시약", "청모랑의 시체"])).toBe("보기 청모랑의 시체");
  });

  it("공백으로 끝나도 앞 낱말과 이어 맞으면 채운다", () => {
    expect(new Completer().complete("줍기 늑대 ", ["늑대 가죽"])).toBe("줍기 늑대 가죽");
    expect(new Completer().complete("줍기 늑대 ", ["가죽"])).toBe("줍기 늑대 ");
  });

  it("여러 낱말 후보도 거듭 부르면 다음 후보로 돈다", () => {
    const completer = new Completer();
    const names = ["늑대 가죽", "늑대 이빨"];

    expect(completer.complete("줍기 늑대 ", names)).toBe("줍기 늑대 가죽");
    expect(completer.complete("줍기 늑대 가죽", names)).toBe("줍기 늑대 이빨");
    expect(completer.complete("줍기 늑대 이빨", names)).toBe("줍기 늑대 가죽");
  });
});
