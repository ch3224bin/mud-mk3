import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, apiFetch, deleteExit, listZones, putExit } from "./api";

const fetchMock = vi.fn();

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function lastCall(): { url: string; init: RequestInit } {
  const [url, init] = fetchMock.mock.calls[fetchMock.mock.calls.length - 1];
  return { url, init };
}

function headersOf(init: RequestInit): Headers {
  return new Headers(init.headers);
}

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("apiFetch", () => {
  it("GET 요청은 쿠키를 함께 보내고 X-MUD-Client 헤더를 붙이지 않는다", async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, []));

    await listZones();

    const { url, init } = lastCall();
    expect(url).toBe("http://localhost:8080/api/admin/zones");
    expect(init.credentials).toBe("include");
    expect(headersOf(init).has("X-MUD-Client")).toBe(false);
  });

  it("변경 요청에는 X-MUD-Client: 1 과 JSON Content-Type 을 붙인다", async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { id: 1 }));

    await apiFetch("/api/admin/zones", { method: "POST", body: "{}" });

    const { init } = lastCall();
    expect(init.credentials).toBe("include");
    expect(headersOf(init).get("X-MUD-Client")).toBe("1");
    expect(headersOf(init).get("Content-Type")).toBe("application/json");
  });

  it("204 응답은 undefined 를 돌려준다", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

    await expect(deleteExit(3, "NORTH", true)).resolves.toBeUndefined();

    const { url, init } = lastCall();
    expect(url).toBe("http://localhost:8080/api/admin/rooms/3/exits/NORTH?twoWay=true");
    expect(init.method).toBe("DELETE");
    expect(headersOf(init).get("X-MUD-Client")).toBe("1");
  });

  it("실패 응답의 ProblemDetail detail 을 ApiError 로 던진다", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(409, { title: "Conflict", status: 409, detail: "반대 방향 출구가 이미 다른 방을 가리킵니다." }),
    );

    const error = await putExit(3, "NORTH", { targetRoomId: 4, twoWay: true }).catch((e) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(409);
    expect(error.detail).toBe("반대 방향 출구가 이미 다른 방을 가리킵니다.");
    expect(error.message).toBe("반대 방향 출구가 이미 다른 방을 가리킵니다.");
  });

  it("본문이 ProblemDetail 이 아니면 일반 문구를 detail 로 쓴다", async () => {
    fetchMock.mockResolvedValue(new Response("oops", { status: 500 }));

    const error = await listZones().catch((e) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(500);
    expect(error.detail).toBe("요청을 처리하지 못했습니다. (500)");
  });
});

describe("putExit", () => {
  it("PUT /api/admin/rooms/{id}/exits/{direction} 에 대상 방과 twoWay 를 보낸다", async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { id: 3, exits: [] }));

    await putExit(3, "NORTH", { targetRoomId: 4, twoWay: true });

    const { url, init } = lastCall();
    expect(url).toBe("http://localhost:8080/api/admin/rooms/3/exits/NORTH");
    expect(init.method).toBe("PUT");
    expect(init.body).toBe('{"targetRoomId":4,"twoWay":true}');
    expect(headersOf(init).get("X-MUD-Client")).toBe("1");
  });
});
