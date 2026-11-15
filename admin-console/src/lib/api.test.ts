import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiError,
  apiFetch,
  createItemType,
  createMob,
  createMobPlacement,
  createFloorItem,
  createNpc,
  createTechnique,
  deleteExit,
  deleteFloorItem,
  deleteMob,
  deleteMobPlacement,
  deleteShop,
  deleteTechnique,
  designateRealm,
  getCharacter,
  getMob,
  getTechnique,
  listCharacters,
  listMobs,
  listRealms,
  listTechniques,
  listZones,
  putExit,
  saveShop,
  saveStartingItems,
  updateFloorItem,
  updateItemType,
  updateMob,
  updateMobPlacement,
  updateTechnique,
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
      techniqueId: null,
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
      techniqueId: null,
    });
    expect(headersOf(init).get("X-MUD-Client")).toBe("1");
  });

  it("updateItemType 은 PUT /api/admin/item-types/{id} 에 담긴 공법 techniqueId 를 보낸다", async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { id: 8 }));

    await updateItemType(8, {
      name: "장춘공 옥간",
      description: "",
      category: "SCROLL",
      baseValue: 30,
      bagGrade: null,
      techniqueId: 2,
    });

    const { url, init } = lastCall();
    expect(url).toBe("http://localhost:8080/api/admin/item-types/8");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(init.body as string)).toEqual(expect.objectContaining({ category: "SCROLL", techniqueId: 2 }));
  });
});

describe("경지·공법", () => {
  const request = {
    name: "장춘공",
    description: "",
    elements: ["WOOD" as const],
    startRealm: "MORTAL" as const,
    stages: [
      {
        insightRequired: 0,
        aptitudeRequired: 0,
        manaBonus: 0,
        senseBonus: 0,
        spiritBonus: 0,
        essenceBonus: 0,
        bodyBonus: 0,
        strengthBonus: 0,
        aptitudeGain: 0,
      },
    ],
    abilities: [
      {
        name: "화탄술",
        learnStage: 1,
        kind: "ATTACK" as const,
        elements: ["FIRE" as const],
        bodyRefining: false,
        target: "SINGLE" as const,
        costStat: "MANA" as const,
        costAmount: 5,
        costPerTick: false,
        castSeconds: 0,
        cooldownSeconds: 3,
        usableWhen: "COMBAT_ONLY" as const,
        effectLines: ["{대상}에게 화탄술을 펼친다!"],
        stagePowers: [{ stage: 1, basePower: 2, coefficient: 1.2 }],
      },
    ],
  };

  it("listRealms·listTechniques·getTechnique 는 GET 이고 X-MUD-Client 를 붙이지 않는다", async () => {
    fetchMock.mockImplementation(async () => jsonResponse(200, []));

    await listRealms();
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/realms");
    expect(lastCall().init.method).toBe("GET");
    await listTechniques();
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/techniques");
    expect(lastCall().init.method).toBe("GET");
    await getTechnique(2);
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/techniques/2");
    expect(lastCall().init.method).toBe("GET");
    expect(headersOf(lastCall().init).has("X-MUD-Client")).toBe(false);
  });

  it("createTechnique 는 POST /api/admin/techniques 에 단계·신통(bodyRefining·costPerTick 포함)을 보낸다", async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, { id: 2 }));

    await createTechnique(request);

    const { url, init } = lastCall();
    expect(url).toBe("http://localhost:8080/api/admin/techniques");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual(request);
    expect(JSON.parse(init.body as string).abilities[0]).toHaveProperty("bodyRefining", false);
    expect(JSON.parse(init.body as string).abilities[0]).toHaveProperty("costPerTick", false);
    expect(headersOf(init).get("X-MUD-Client")).toBe("1");
  });

  it("updateTechnique 는 PUT, deleteTechnique 는 DELETE /api/admin/techniques/{id} 로 보낸다", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, { id: 2 }));
    await updateTechnique(2, request);
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/techniques/2");
    expect(lastCall().init.method).toBe("PUT");
    expect(JSON.parse(lastCall().init.body as string)).toEqual(request);
    expect(headersOf(lastCall().init).get("X-MUD-Client")).toBe("1");

    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await expect(deleteTechnique(2)).resolves.toBeUndefined();
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/techniques/2");
    expect(lastCall().init.method).toBe("DELETE");
    expect(headersOf(lastCall().init).get("X-MUD-Client")).toBe("1");
  });
});

describe("캐릭터", () => {
  it("listCharacters·getCharacter 는 GET 이고 X-MUD-Client 를 붙이지 않는다", async () => {
    fetchMock.mockImplementation(async () => jsonResponse(200, []));

    await listCharacters();
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/characters");
    expect(lastCall().init.method).toBe("GET");
    await getCharacter(3);
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/characters/3");
    expect(lastCall().init.method).toBe("GET");
    expect(headersOf(lastCall().init).has("X-MUD-Client")).toBe(false);
  });

  it("designateRealm 은 PUT /api/admin/characters/{id}/realm 에 { realm } 을 보내고 상세를 돌려준다", async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { id: 3, realm: "QI_7" }));

    await expect(designateRealm(3, "QI_7")).resolves.toEqual({ id: 3, realm: "QI_7" });

    const { url, init } = lastCall();
    expect(url).toBe("http://localhost:8080/api/admin/characters/3/realm");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(init.body as string)).toEqual({ realm: "QI_7" });
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

describe("몹", () => {
  const request = {
    name: "쌍두 흑망",
    description: "",
    kind: "BEAST" as const,
    realm: "QI_12" as const,
    tier: "BOSS" as const,
    attackName: "휘감기",
    attackCoefficient: 0.6,
    respawnSeconds: 2700,
    fixedDrops: [{ itemTypeId: 7, count: 2 }],
    tierDrops: [{ tier: "ADVANCED" as const, itemTypeId: 8 }],
  };

  it("listMobs·getMob 은 GET 이고 X-MUD-Client 를 붙이지 않는다", async () => {
    fetchMock.mockImplementation(async () => jsonResponse(200, []));

    await listMobs();
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/mobs");
    expect(lastCall().init.method).toBe("GET");
    await getMob(2);
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/mobs/2");
    expect(lastCall().init.method).toBe("GET");
    expect(headersOf(lastCall().init).has("X-MUD-Client")).toBe(false);
  });

  it("createMob 은 POST, updateMob 은 PUT, deleteMob 은 DELETE /api/admin/mobs 로 보낸다", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(201, { id: 2 }));
    await createMob(request);
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/mobs");
    expect(lastCall().init.method).toBe("POST");
    expect(JSON.parse(lastCall().init.body as string)).toEqual(request);
    expect(headersOf(lastCall().init).get("X-MUD-Client")).toBe("1");

    fetchMock.mockResolvedValueOnce(jsonResponse(200, { id: 2 }));
    await updateMob(2, { ...request, attackCoefficient: null, respawnSeconds: null });
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/mobs/2");
    expect(lastCall().init.method).toBe("PUT");
    expect(JSON.parse(lastCall().init.body as string)).toMatchObject({ attackCoefficient: null, respawnSeconds: null });

    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await expect(deleteMob(2)).resolves.toBeUndefined();
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/mobs/2");
    expect(lastCall().init.method).toBe("DELETE");
  });

  it("몹 배치는 POST /rooms/{id}/mobs, PUT·DELETE /mob-placements/{id} 로 보낸다", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(201, { id: 21 }));
    await createMobPlacement(3, { mobId: 2, count: 2 });
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/rooms/3/mobs");
    expect(lastCall().init.method).toBe("POST");
    expect(lastCall().init.body).toBe('{"mobId":2,"count":2}');
    expect(headersOf(lastCall().init).get("X-MUD-Client")).toBe("1");

    fetchMock.mockResolvedValueOnce(jsonResponse(200, { id: 21 }));
    await updateMobPlacement(21, { mobId: 2, count: 5 });
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/mob-placements/21");
    expect(lastCall().init.method).toBe("PUT");
    expect(lastCall().init.body).toBe('{"mobId":2,"count":5}');

    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await expect(deleteMobPlacement(21)).resolves.toBeUndefined();
    expect(lastCall().url).toBe("http://localhost:8080/api/admin/mob-placements/21");
    expect(lastCall().init.method).toBe("DELETE");
  });
});
