import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiError,
  apiFetch,
  createItemType,
  createFloorItem,
  createNpc,
  deleteExit,
  deleteFloorItem,
  deleteShop,
  listZones,
  putExit,
  saveShop,
  saveStartingItems,
  updateFloorItem,
  updateZone,
} from "./api";

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

describe("createNpc", () => {
  it("POST /api/admin/npcs 에 낱말 목록과 배치 방(null)을 보낸다", async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, { id: 4 }));

    await createNpc({
      name: "왕씨",
      greeting: "어서 오시오.",
      defaultAnswer: "글쎄, 잘 모르겠구려.",
      topics: [{ word: "단약", answer: "단약은 귀하오." }],
      llmEnabled: false,
      roomId: null,
    });

    const { url, init } = lastCall();
    expect(url).toBe("http://localhost:8080/api/admin/npcs");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual({
      name: "왕씨",
      greeting: "어서 오시오.",
      defaultAnswer: "글쎄, 잘 모르겠구려.",
      topics: [{ word: "단약", answer: "단약은 귀하오." }],
      llmEnabled: false,
      roomId: null,
    });
    expect(headersOf(init).get("X-MUD-Client")).toBe("1");
  });
});

describe("updateZone", () => {
  it("PUT /api/admin/zones/{id} 에 소유 성·종문 id 를 보낸다", async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { id: 2 }));

    await updateZone(2, { name: "청석숲", description: "", ownerId: 7 });

    const { url, init } = lastCall();
    expect(url).toBe("http://localhost:8080/api/admin/zones/2");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(init.body as string)).toEqual({ name: "청석숲", description: "", ownerId: 7 });
  });
});

describe("createItemType", () => {
  it("POST /api/admin/item-types 에 아이템 종류를 보낸다", async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, { id: 7 }));

    await createItemType({
      name: "하급 저물대",
      description: "",
      category: "BAG",
      baseValue: 50,
      bagGrade: "LOW",
    });

    const { url, init } = lastCall();
    expect(url).toBe("http://localhost:8080/api/admin/item-types");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual({
      name: "하급 저물대",
      description: "",
      category: "BAG",
      baseValue: 50,
      bagGrade: "LOW",
    });
    expect(headersOf(init).get("X-MUD-Client")).toBe("1");
  });
});

describe("saveStartingItems", () => {
  it("PUT /api/admin/world/starting-items 에 { items } 를 입력 순서대로 보낸다", async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, []));

    await saveStartingItems([
      { itemTypeId: 5, count: 1 },
      { itemTypeId: 1, count: 30 },
    ]);

    const { url, init } = lastCall();
    expect(url).toBe("http://localhost:8080/api/admin/world/starting-items");
    expect(init.method).toBe("PUT");
    expect(init.body).toBe('{"items":[{"itemTypeId":5,"count":1},{"itemTypeId":1,"count":30}]}');
    expect(headersOf(init).get("X-MUD-Client")).toBe("1");
  });
});

describe("바닥 물건", () => {
  it("createFloorItem 은 POST /api/admin/rooms/{roomId}/floor-items 에 재등장 null 을 그대로 보낸다", async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, { id: 9 }));

    await createFloorItem(3, { itemTypeId: 7, count: 2, respawnMinutes: null });

    const { url, init } = lastCall();
    expect(url).toBe("http://localhost:8080/api/admin/rooms/3/floor-items");
    expect(init.method).toBe("POST");
    expect(init.body).toBe('{"itemTypeId":7,"count":2,"respawnMinutes":null}');
    expect(headersOf(init).get("X-MUD-Client")).toBe("1");
  });

  it("updateFloorItem 은 PUT, deleteFloorItem 은 DELETE /api/admin/floor-items/{id} 로 보낸다", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, { id: 9 }));
    await updateFloorItem(9, { itemTypeId: 7, count: 1, respawnMinutes: 10 });
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/floor-items/9");
    expect(lastCall().init.method).toBe("PUT");
    expect(lastCall().init.body).toBe('{"itemTypeId":7,"count":1,"respawnMinutes":10}');

    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await expect(deleteFloorItem(9)).resolves.toBeUndefined();
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/floor-items/9");
    expect(lastCall().init.method).toBe("DELETE");
  });
});

describe("상점", () => {
  it("saveShop 은 PUT /api/admin/npcs/{id}/shop 에 종류와 물건(값 null 포함)을 입력 순서대로 보낸다", async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { id: 4 }));

    await saveShop(4, {
      kind: "PILL_SHOP",
      items: [
        { itemTypeId: 7, price: null },
        { itemTypeId: 8, price: 12 },
      ],
    });

    const { url, init } = lastCall();
    expect(url).toBe("http://localhost:8080/api/admin/npcs/4/shop");
    expect(init.method).toBe("PUT");
    expect(init.body).toBe('{"kind":"PILL_SHOP","items":[{"itemTypeId":7,"price":null},{"itemTypeId":8,"price":12}]}');
    expect(headersOf(init).get("X-MUD-Client")).toBe("1");
  });

  it("deleteShop 은 DELETE /api/admin/npcs/{id}/shop 를 보낸다", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

    await expect(deleteShop(4)).resolves.toBeUndefined();

    const { url, init } = lastCall();
    expect(url).toBe("http://localhost:8080/api/admin/npcs/4/shop");
    expect(init.method).toBe("DELETE");
    expect(headersOf(init).get("X-MUD-Client")).toBe("1");
  });
});
