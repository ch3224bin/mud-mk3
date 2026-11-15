import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../lib/api";
import type { MobPlacementResponse, MobSummaryResponse, RoomDetailResponse } from "../lib/api";
import MobPlacementsEditor from "./MobPlacementsEditor";

vi.mock("../lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../lib/api")>()),
  createMobPlacement: vi.fn(),
  updateMobPlacement: vi.fn(),
  deleteMobPlacement: vi.fn(),
}));

function mob(id: number, name: string): MobSummaryResponse {
  return {
    id,
    name,
    kind: "BEAST",
    kindName: "요수",
    rankName: "1급",
    tier: "NORMAL",
    tierName: "일반",
    stats: { qiBlood: 49, body: 8, power: 8 },
    effectiveRespawnSeconds: 90,
    placementCount: 0,
  };
}

// 서버 순서와 상관없이 선택은 이름순이다.
const mobs = [mob(2, "청모랑"), mob(1, "들토끼"), mob(3, "쌍두 흑망")];

const wolves: MobPlacementResponse = { id: 21, roomId: 3, mobId: 2, mobName: "청모랑", count: 2 };
const rabbits: MobPlacementResponse = { id: 22, roomId: 3, mobId: 1, mobName: "들토끼", count: 3 };

function room(placements: MobPlacementResponse[]): RoomDetailResponse {
  return {
    id: 3,
    zoneId: 1,
    zoneName: "청우촌",
    name: "마을 어귀",
    description: "",
    start: false,
    exits: [],
    npcs: [],
    floorItems: [],
    mobs: placements,
  };
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe("MobPlacementsEditor", () => {
  it("배치가 없으면 없음 을 보이고, 추가 폼은 몹을 이름순으로 고르고 수는 1 이다", () => {
    render(<MobPlacementsEditor room={room([])} mobs={mobs} onRoomChange={vi.fn()} />);

    expect(screen.getByRole("heading", { name: "몹 배치" })).toBeInTheDocument();
    expect(screen.getByText("없음")).toBeInTheDocument();
    const select = screen.getByLabelText("몹") as HTMLSelectElement;
    expect(Array.from(select.options).map((o) => o.textContent)).toEqual(["들토끼", "쌍두 흑망", "청모랑"]);
    expect(select).toHaveValue("1");
    expect(screen.getByLabelText("수")).toHaveValue(1);
  });

  it("행마다 몹 이름 링크와 수를 보인다", () => {
    render(<MobPlacementsEditor room={room([wolves, rabbits])} mobs={mobs} onRoomChange={vi.fn()} />);

    expect(screen.getByRole("link", { name: "청모랑" })).toHaveAttribute("href", expect.stringMatching(/^\/mob\/?\?id=2$/));
    expect(screen.getByRole("link", { name: "들토끼" })).toHaveAttribute("href", expect.stringMatching(/^\/mob\/?\?id=1$/));
    expect(screen.getByLabelText("청모랑 수")).toHaveValue(2);
    expect(screen.getByLabelText("들토끼 수")).toHaveValue(3);
  });

  it("추가 는 고른 몹과 수를 POST 하고 새 행을 방에 더한다", async () => {
    const user = userEvent.setup();
    const onRoomChange = vi.fn();
    vi.mocked(api.createMobPlacement).mockResolvedValue(wolves);
    render(<MobPlacementsEditor room={room([rabbits])} mobs={mobs} onRoomChange={onRoomChange} />);

    await user.selectOptions(screen.getByLabelText("몹"), "2");
    await user.clear(screen.getByLabelText("수"));
    await user.type(screen.getByLabelText("수"), "2");
    await user.click(screen.getByRole("button", { name: "추가" }));

    expect(api.createMobPlacement).toHaveBeenCalledWith(3, { mobId: 2, count: 2 });
    expect(onRoomChange).toHaveBeenCalledWith(room([rabbits, wolves]));
  });

  it("추가 폼의 수 0 도 브라우저 검사 없이 보내 서버 detail 을 보인다", async () => {
    const user = userEvent.setup();
    vi.mocked(api.createMobPlacement).mockRejectedValue(new api.ApiError(400, "몹 수는 1~10으로 정하십시오."));
    render(<MobPlacementsEditor room={room([])} mobs={mobs} onRoomChange={vi.fn()} />);

    await user.clear(screen.getByLabelText("수"));
    await user.type(screen.getByLabelText("수"), "0");
    await user.click(screen.getByRole("button", { name: "추가" }));

    expect(api.createMobPlacement).toHaveBeenCalledWith(3, { mobId: 1, count: 0 });
    expect(await screen.findByRole("alert")).toHaveTextContent("몹 수는 1~10으로 정하십시오.");
  });

  it("행의 수를 고쳐 저장 하면 같은 몹으로 PUT 하고 응답으로 행을 바꾼다", async () => {
    const user = userEvent.setup();
    const onRoomChange = vi.fn();
    const updated = { ...wolves, count: 5 };
    vi.mocked(api.updateMobPlacement).mockResolvedValue(updated);
    render(<MobPlacementsEditor room={room([wolves, rabbits])} mobs={mobs} onRoomChange={onRoomChange} />);

    await user.clear(screen.getByLabelText("청모랑 수"));
    await user.type(screen.getByLabelText("청모랑 수"), "5");
    const row = screen.getByLabelText("청모랑 수").closest("tr")!;
    await user.click(within(row).getByRole("button", { name: "저장" }));

    expect(api.updateMobPlacement).toHaveBeenCalledWith(21, { mobId: 2, count: 5 });
    expect(onRoomChange).toHaveBeenCalledWith(room([updated, rabbits]));
  });

  it("삭제 를 누르면 그 배치를 지우고 방에서 뺀다", async () => {
    const user = userEvent.setup();
    const onRoomChange = vi.fn();
    vi.mocked(api.deleteMobPlacement).mockResolvedValue(undefined);
    render(<MobPlacementsEditor room={room([wolves, rabbits])} mobs={mobs} onRoomChange={onRoomChange} />);

    const row = screen.getByLabelText("청모랑 수").closest("tr")!;
    await user.click(within(row).getByRole("button", { name: "삭제" }));

    expect(api.deleteMobPlacement).toHaveBeenCalledWith(21);
    expect(onRoomChange).toHaveBeenCalledWith(room([rabbits]));
  });

  it("서버가 400 을 주면 detail 문구를 보인다", async () => {
    const user = userEvent.setup();
    vi.mocked(api.updateMobPlacement).mockRejectedValue(new api.ApiError(400, "몹 수는 1~10으로 정하십시오."));
    render(<MobPlacementsEditor room={room([wolves])} mobs={mobs} onRoomChange={vi.fn()} />);

    await user.clear(screen.getByLabelText("청모랑 수"));
    await user.type(screen.getByLabelText("청모랑 수"), "11");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("몹 수는 1~10으로 정하십시오.");
  });
});
