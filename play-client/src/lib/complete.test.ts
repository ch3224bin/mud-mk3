import { describe, expect, it } from "vitest";
import { Completer } from "./complete";

describe("Completer", () => {
  it("첫 낱말은 명령어에서 앞부분이 맞는 것으로 채운다", () => {
    expect(new Completer().complete("보", [])).toBe("보기");
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
