import { describe, expect, it } from "vitest";
import { CommandHistory } from "./history";

describe("CommandHistory", () => {
  it("↑ 는 최근 것부터 거슬러 올라가고 ↓ 는 되돌아오며 끝에서 빈 입력이 된다", () => {
    const history = new CommandHistory();
    history.push("북");
    history.push("남");

    expect(history.previous("")).toBe("남");
    expect(history.previous("남")).toBe("북");
    expect(history.next()).toBe("남");
    expect(history.next()).toBe("");
  });

  it("가장 오래된 것에서 ↑ 를 더 눌러도 그대로다", () => {
    const history = new CommandHistory();
    history.push("북");

    expect(history.previous("")).toBe("북");
    expect(history.previous("북")).toBe("북");
  });

  it("비어 있으면 ↑ 는 지금 입력을 그대로 돌려준다", () => {
    expect(new CommandHistory().previous("보")).toBe("보");
  });

  it("바로 앞과 같은 입력은 하나로 친다", () => {
    const history = new CommandHistory();
    history.push("북");
    history.push("북");

    expect(history.previous("")).toBe("북");
    expect(history.previous("북")).toBe("북");
    expect(history.next()).toBe("");
  });

  it("넣고 나면 처음(최근 것)부터 다시 거슬러 올라간다", () => {
    const history = new CommandHistory();
    history.push("북");
    history.push("남");
    history.previous("");
    history.previous("남");

    history.push("동");

    expect(history.previous("")).toBe("동");
  });

  it("한도(100)를 넘으면 가장 오래된 것이 빠진다", () => {
    const history = new CommandHistory();
    for (let i = 1; i <= 101; i++) {
      history.push(`명령${i}`);
    }

    let oldest = "";
    for (let i = 0; i < 101; i++) {
      oldest = history.previous(oldest);
    }

    expect(oldest).toBe("명령2");
  });

  it("빈 입력은 넣지 않는다", () => {
    const history = new CommandHistory();
    history.push("북");
    history.push("   ");

    expect(history.previous("")).toBe("북");
  });
});
