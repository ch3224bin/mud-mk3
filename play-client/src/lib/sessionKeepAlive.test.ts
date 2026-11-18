import { describe, expect, it, vi } from "vitest";
import { createSessionKeepAlive } from "./sessionKeepAlive";

const MINUTE = 60_000;

function clock(start = 0) {
  let current = start;
  return {
    now: () => current,
    advance(ms: number) {
      current += ms;
    },
  };
}

describe("createSessionKeepAlive", () => {
  it("5분이 지나기 전에는 부르지 않는다", () => {
    const time = clock();
    const check = vi.fn(() => Promise.resolve());
    const keepAlive = createSessionKeepAlive(check, time.now);

    keepAlive.onCommand();
    time.advance(5 * MINUTE);
    keepAlive.onCommand();

    expect(check).not.toHaveBeenCalled();
  });

  it("5분이 넘게 지나 명령을 보내면 한 번 부르고 다시 5분을 센다", () => {
    const time = clock();
    const check = vi.fn(() => Promise.resolve());
    const keepAlive = createSessionKeepAlive(check, time.now);

    time.advance(5 * MINUTE + 1);
    keepAlive.onCommand();
    keepAlive.onCommand();
    expect(check).toHaveBeenCalledTimes(1);

    time.advance(5 * MINUTE);
    keepAlive.onCommand();
    expect(check).toHaveBeenCalledTimes(1);

    time.advance(1);
    keepAlive.onCommand();
    expect(check).toHaveBeenCalledTimes(2);
  });

  it("check 가 실패해도 던지지 않는다", async () => {
    const time = clock();
    const check = vi.fn(() => Promise.reject(new Error("네트워크")));
    const keepAlive = createSessionKeepAlive(check, time.now);

    time.advance(6 * MINUTE);
    expect(() => keepAlive.onCommand()).not.toThrow();
    await Promise.resolve();

    expect(check).toHaveBeenCalledTimes(1);
  });
});
