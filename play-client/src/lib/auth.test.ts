import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { errorMessage } from "./api";
import { fetchMe, googleLoginUrl, loginForDevelopment, logout } from "./auth";

const fetchMock = vi.fn();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

const me = { id: 1, email: "player@example.com", admin: false, characterName: null };

describe("googleLoginUrl", () => {
  it("world-server 로그인 주소에 client=play 을 붙인다", () => {
    expect(googleLoginUrl()).toBe("http://localhost:8080/api/auth/login?client=play");
  });
});

describe("fetchMe", () => {
  it("401 이면 null 을 돌려준다", async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ status: 401, detail: "로그인이 필요합니다." }), { status: 401 }),
    );

    await expect(fetchMe()).resolves.toBeNull();
  });

  it("200 이면 본문을 돌려준다", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify(me), { status: 200 }));

    await expect(fetchMe()).resolves.toEqual(me);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/auth/me");
    expect(init.credentials).toBe("include");
  });
});

describe("loginForDevelopment", () => {
  it("이메일을 POST /api/auth/dev 로 보낸다", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify(me), { status: 200 }));

    await expect(loginForDevelopment("player@example.com")).resolves.toEqual(me);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/auth/dev");
    expect(init.method).toBe("POST");
    expect(init.body).toBe('{"email":"player@example.com"}');
    expect(new Headers(init.headers).get("X-MUD-Client")).toBe("1");
  });
});

describe("logout", () => {
  it("POST /api/auth/logout 을 부른다", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

    await logout();

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/auth/logout");
    expect(init.method).toBe("POST");
    expect(new Headers(init.headers).get("X-MUD-Client")).toBe("1");
  });
});

describe("errorMessage", () => {
  it("ApiError 면 서버가 준 detail", async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ status: 404, detail: "찾을 수 없습니다." }), { status: 404 }),
    );

    const error = await loginForDevelopment("player@example.com").catch((e: unknown) => e);

    expect(errorMessage(error)).toBe("찾을 수 없습니다.");
  });

  it("fetch 자체가 실패하면(TypeError) 연결하지 못했다고 한다", async () => {
    fetchMock.mockRejectedValue(new TypeError("Failed to fetch"));

    const error = await fetchMe().catch((e: unknown) => e);

    expect(errorMessage(error)).toBe("world-server에 연결하지 못했습니다.");
  });

  it("그 밖의 예외는 원인을 콘솔에 남기고 일반 문구를 보인다", () => {
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    const cause = new SyntaxError("Unexpected token");

    expect(errorMessage(cause)).toBe("요청을 처리하지 못했습니다.");
    expect(consoleError).toHaveBeenCalledWith(cause);
    consoleError.mockRestore();
  });
});
