import { describe, expect, it } from "vitest";
import { withEunNeun } from "./josa";

describe("withEunNeun", () => {
  it("받침이 있으면 은 을 붙인다", () => {
    expect(withEunNeun("청모랑")).toBe("청모랑은");
  });

  it("받침이 없으면 는 을 붙인다", () => {
    expect(withEunNeun("들토끼")).toBe("들토끼는");
  });

  it("마지막 글자가 한글이 아니면 는 을 붙인다", () => {
    expect(withEunNeun("들토끼123")).toBe("들토끼123는");
  });
});
