import { describe, expect, it } from "vitest";
import { COMMANDS, Completer } from "./complete";

describe("Completer", () => {
  it("첫 낱말은 명령어에서 앞부분이 맞는 것으로 채운다", () => {
    expect(new Completer().complete("보", [])).toBe("보기");
  });

  it("대화 명령을 채운다", () => {
    expect(new Completer().complete("대", [])).toBe("대화");
  });

  it("대화 뒤 낱말은 이름(NPC 포함)에서 채운다", () => {
    expect(new Completer().complete("대화 왕", ["한립", "왕씨"])).toBe("대화 왕씨");
  });

  it("3차 물건 명령을 채운다", () => {
    expect(new Completer().complete("줍", [])).toBe("줍기");
    expect(new Completer().complete("귀", [])).toBe("귀속");
    expect(new Completer().complete("소", [])).toBe("소지품");
  });

  it("COMMANDS 는 D51 순서", () => {
    expect(COMMANDS).toEqual([
      "보기", "말하기", "대화", "소지품", "줍기", "버리기", "주기", "목록", "사기", "팔기", "값", "귀속", "확인", "취소",
      "누구", "도움말", "종료",
      "동", "서", "남", "북", "북동", "북서", "남동", "남서", "위", "아래",
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
});
