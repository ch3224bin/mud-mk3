import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fetchMe, googleLoginUrl, loginForDevelopment, logout } from "./auth";

const fetchMock = vi.fn();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

const me = { id: 1, email: "admin@example.com", admin: true, characterName: null };

describe("googleLoginUrl", () => {
  it("world-server 로그인 주소에 client=admin 을 붙인다", () => {
    expect(googleLoginUrl()).toBe("http://localhost:8080/api/auth/login?client=admin");
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

    await expect(loginForDevelopment("admin@example.com")).resolves.toEqual(me);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/auth/dev");
    expect(init.method).toBe("POST");
    expect(init.body).toBe('{"email":"admin@example.com"}');
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
