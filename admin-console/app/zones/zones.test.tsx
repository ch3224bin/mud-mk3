import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { FactionResponse, ZoneResponse } from "../../src/lib/api";
import ZonesPage from "./page";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
}));

vi.mock("../../src/lib/auth", () => ({
  fetchMe: vi.fn(async () => ({ id: 1, email: "a@example.com", admin: true, characterName: null })),
  logout: vi.fn(),
}));

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

const sect: FactionResponse = { id: 7, kind: "SECT", kindName: "종문", name: "황풍곡", description: "", zoneCount: 1 };

let zones: ZoneResponse[];
let factionsResponse: () => Response;
const fetchMock = vi.fn();

/** world-server 흉내: 지역 목록·고치기, 성·종문 목록, 시작 방. 지역 이름은 겹친 공백을 하나로 줄여 저장한다 */
async function fakeServer(url: string, init: RequestInit = {}): Promise<Response> {
  const path = new URL(url).pathname;
  const method = init.method ?? "GET";
  if (method === "GET" && path === "/api/admin/zones") return json(200, zones);
  if (method === "GET" && path === "/api/admin/factions") return factionsResponse();
  if (method === "GET" && path === "/api/admin/world") {
    return json(200, { startRoomId: null, startRoomName: null, startZoneName: null });
  }
  const update = path.match(/^\/api\/admin\/zones\/(\d+)$/);
  if (method === "PUT" && update) {
    const request = JSON.parse(init.body as string);
    zones = zones.map((z) =>
      z.id === Number(update[1]) ? { ...z, name: request.name.replace(/\s+/g, " "), ownerId: request.ownerId } : z,
    );
    return json(200, zones.find((z) => z.id === Number(update[1])));
  }
  return json(404, { detail: `없는 경로: ${method} ${path}` });
}

beforeEach(() => {
  zones = [
    { id: 2, name: "청석숲", description: "푸른 숲", roomCount: 3, ownerId: 7, ownerName: "황풍곡", ownerKind: "SECT" },
  ];
  factionsResponse = () => json(200, [sect]);
  fetchMock.mockReset();
  fetchMock.mockImplementation(fakeServer);
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("지역 화면", () => {
  it("zoneScreenShowsZonesWhenFactionsFail: 성·종문 목록이 실패해도 지역 목록·폼을 보이고 detail 을 한 줄 보인다", async () => {
    factionsResponse = () => json(500, { detail: "성·종문 목록을 읽지 못했습니다." });

    render(<ZonesPage />);

    // 테스트에는 next.config 의 trailingSlash 가 없어 Link 가 끝 / 를 뗄 수 있다.
    expect(await screen.findByRole("link", { name: "청석숲" })).toHaveAttribute(
      "href",
      expect.stringMatching(/^\/zone\/?\?id=2$/),
    );
    expect(screen.getByRole("alert")).toHaveTextContent("성·종문 목록을 읽지 못했습니다.");
    expect(screen.getByRole("heading", { name: "지역 만들기" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "지역 만들기" })).toBeInTheDocument();
  });

  it("zoneScreenClearsErrorAfterReload: 성·종문 목록이 실패한 뒤 저장으로 다시 읽어 둘 다 성공하면 오류 줄이 사라진다", async () => {
    const user = userEvent.setup();
    factionsResponse = () => json(500, { detail: "성·종문 목록을 읽지 못했습니다." });
    render(<ZonesPage />);

    const row = (await screen.findByRole("link", { name: "청석숲" })).closest("tr") as HTMLElement;
    expect(screen.getByRole("alert")).toHaveTextContent("성·종문 목록을 읽지 못했습니다.");
    factionsResponse = () => json(200, [sect]);
    await user.click(within(row).getByRole("button", { name: "수정" }));
    const form = screen.getAllByRole("button", { name: "저장" })[0].closest("form") as HTMLElement;
    await user.click(within(form).getByRole("button", { name: "저장" }));

    expect(await screen.findByRole("option", { name: "황풍곡 (종문)" })).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("목록 → 수정 → 폼: 저장한 값으로 고치고 서버가 다듬은 이름을 목록에 보인다", async () => {
    const user = userEvent.setup();
    render(<ZonesPage />);

    const row = (await screen.findByRole("link", { name: "청석숲" })).closest("tr") as HTMLElement;
    expect(within(row).getByText("황풍곡 (종문)")).toBeInTheDocument();
    await user.click(within(row).getByRole("button", { name: "수정" }));

    const form = screen.getAllByRole("button", { name: "저장" })[0].closest("form") as HTMLElement;
    expect(within(form).getByLabelText("이름")).toHaveValue("청석숲");
    expect(within(form).getByLabelText("분위기 묘사")).toHaveValue("푸른 숲");
    expect(within(form).getByLabelText("소유")).toHaveValue("7");
    await user.clear(within(form).getByLabelText("이름"));
    await user.type(within(form).getByLabelText("이름"), "깊은  청석숲");
    await user.selectOptions(within(form).getByLabelText("소유"), "없음");
    await user.click(within(form).getByRole("button", { name: "저장" }));

    expect(await screen.findByRole("link", { name: "깊은 청석숲" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "저장" })).not.toBeInTheDocument();
    const put = fetchMock.mock.calls.find(([, init]) => init?.method === "PUT");
    expect(put?.[0]).toBe("http://localhost:8080/api/admin/zones/2");
    expect(JSON.parse(put?.[1].body)).toEqual({ name: "깊은  청석숲", description: "푸른 숲", ownerId: null });
  });
});
