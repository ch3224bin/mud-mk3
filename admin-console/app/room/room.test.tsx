import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { RoomDetailResponse, ZoneResponse } from "../../src/lib/api";
import RoomPage from "./page";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
  useSearchParams: () => new URLSearchParams("id=3"),
}));

vi.mock("../../src/lib/auth", () => ({
  fetchMe: vi.fn(async () => ({ id: 1, email: "a@example.com", admin: true, characterName: null })),
  logout: vi.fn(),
}));

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

const forest: ZoneResponse = {
  id: 2,
  name: "청석숲",
  description: "",
  roomCount: 1,
  ownerId: null,
  ownerName: null,
  ownerKind: null,
};

let room: RoomDetailResponse;

/** world-server 흉내: 방 읽기·고치기와 방 화면이 함께 읽는 목록들. 방 이름은 겹친 공백을 하나로 줄여 저장한다 */
async function fakeServer(url: string, init: RequestInit = {}): Promise<Response> {
  const path = new URL(url).pathname;
  const method = init.method ?? "GET";
  if (path === "/api/admin/rooms/3" && method === "GET") return json(200, room);
  if (path === "/api/admin/rooms/3" && method === "PUT") {
    const request = JSON.parse(init.body as string);
    room = { ...room, name: request.name.replace(/\s+/g, " "), description: request.description };
    return json(200, room);
  }
  if (path === "/api/admin/zones") return json(200, [forest]);
  if (path === "/api/admin/zones/2/rooms") return json(200, [{ id: 3, name: room.name, exitCount: 0 }]);
  if (path === "/api/admin/item-types" || path === "/api/admin/mobs") return json(200, []);
  if (path === "/api/admin/world") return json(200, { startRoomId: null, startRoomName: null, startZoneName: null });
  return json(404, { detail: `없는 경로: ${method} ${path}` });
}

beforeEach(() => {
  room = {
    id: 3,
    zoneId: 2,
    zoneName: "청석숲",
    name: "숲 입구",
    description: "나무 사이로 길이 난다.",
    start: false,
    exits: [],
    npcs: [],
    floorItems: [],
    mobs: [],
  };
  vi.stubGlobal("fetch", vi.fn(fakeServer));
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("방 화면", () => {
  it("savedNameFromServer: 방 정보를 저장하면 서버가 다듬은 이름으로 폼을 다시 연다", async () => {
    const user = userEvent.setup();
    render(<RoomPage />);

    const heading = await screen.findByRole("heading", { level: 2, name: "방 정보" });
    const form = heading.nextElementSibling as HTMLElement;
    const name = within(form).getByLabelText("이름");
    await user.clear(name);
    await user.type(name, "숲  깊은 곳");
    await user.click(within(form).getByRole("button", { name: "저장" }));

    expect(await screen.findByRole("heading", { level: 1, name: "숲 깊은 곳" })).toBeInTheDocument();
    const savedForm = screen.getByRole("heading", { level: 2, name: "방 정보" }).nextElementSibling as HTMLElement;
    expect(within(savedForm).getByLabelText("이름")).toHaveValue("숲 깊은 곳");
  });
});
