import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../lib/api";
import type { FloorPlacementResponse, ItemTypeResponse, RoomDetailResponse } from "../lib/api";
import FloorItemsEditor from "./FloorItemsEditor";

vi.mock("../lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../lib/api")>()),
  createFloorItem: vi.fn(),
  updateFloorItem: vi.fn(),
  deleteFloorItem: vi.fn(),
}));

function itemType(id: number, name: string): ItemTypeResponse {
  return {
    id,
    name,
    description: "",
    category: "MATERIAL",
    categoryName: "재료",
    stackSize: 99,
    baseValue: 1,
    bagGrade: null,
    bagGradeName: null,
    bagSlots: 0,
    spiritStone: false,
    buyRatePercent: 50,
  };
}

const itemTypes = [itemType(1, "하품 영석"), itemType(8, "늑대 가죽")];

const wolfHide: FloorPlacementResponse = {
  id: 11,
  roomId: 3,
  itemTypeId: 8,
  itemName: "늑대 가죽",
  count: 2,
  respawnMinutes: 10,
  remaining: 1,
};
const stones: FloorPlacementResponse = {
  id: 12,
  roomId: 3,
  itemTypeId: 1,
  itemName: "하품 영석",
  count: 1500,
  respawnMinutes: null,
  remaining: 1500,
};

function room(floorItems: FloorPlacementResponse[]): RoomDetailResponse {
  return {
    id: 3,
    zoneId: 1,
    zoneName: "청우촌",
    name: "마을 어귀",
    description: "",
    start: false,
    exits: [],
    npcs: [],
    floorItems,
  };
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe("FloorItemsEditor", () => {
  it("바닥 물건이 없으면 없음 을 보이고, 두기 폼의 기본값은 수 1·재등장(분) 10 이다", () => {
    render(<FloorItemsEditor room={room([])} itemTypes={itemTypes} onRoomChange={vi.fn()} />);

    expect(screen.getByRole("heading", { name: "바닥 물건" })).toBeInTheDocument();
    expect(screen.getByText("없음")).toBeInTheDocument();
    expect(screen.getByLabelText("물건")).toHaveValue("1");
    expect(screen.getByLabelText("수")).toHaveValue(1);
    expect(screen.getByLabelText("재등장(분)")).toHaveValue(10);
    expect(screen.getByRole("button", { name: "바닥에 두기" })).toBeInTheDocument();
  });

  it("표에 물건·수·남은 수와 재등장(10분 또는 한 번뿐)을 보인다", () => {
    render(<FloorItemsEditor room={room([wolfHide, stones])} itemTypes={itemTypes} onRoomChange={vi.fn()} />);

    const rows = screen.getAllByRole("row").slice(1);
    expect(within(rows[0]).getAllByRole("cell").map((c) => c.textContent)).toEqual([
      "늑대 가죽",
      "2",
      "1",
      "10분",
      "수정",
      "삭제",
    ]);
    expect(within(rows[1]).getAllByRole("cell").map((c) => c.textContent)).toEqual([
      "하품 영석",
      "1,500",
      "1,500",
      "한 번뿐",
      "수정",
      "삭제",
    ]);
  });

  it("재등장을 비우고 두면 respawnMinutes:null 로 보내고 새 행을 방에 더한다", async () => {
    const user = userEvent.setup();
    const onRoomChange = vi.fn();
    vi.mocked(api.createFloorItem).mockResolvedValue(wolfHide);
    render(<FloorItemsEditor room={room([stones])} itemTypes={itemTypes} onRoomChange={onRoomChange} />);

    await user.selectOptions(screen.getByLabelText("물건"), "8");
    await user.clear(screen.getByLabelText("수"));
    await user.type(screen.getByLabelText("수"), "2");
    await user.clear(screen.getByLabelText("재등장(분)"));
    await user.click(screen.getByRole("button", { name: "바닥에 두기" }));

    expect(api.createFloorItem).toHaveBeenCalledWith(3, { itemTypeId: 8, count: 2, respawnMinutes: null });
    expect(onRoomChange).toHaveBeenCalledWith(room([stones, wolfHide]));
  });

  it("수정 을 누르면 그 행을 고쳐 저장하고 응답으로 행을 바꾼다", async () => {
    const user = userEvent.setup();
    const onRoomChange = vi.fn();
    const updated = { ...wolfHide, count: 5, remaining: 5, respawnMinutes: null };
    vi.mocked(api.updateFloorItem).mockResolvedValue(updated);
    render(<FloorItemsEditor room={room([wolfHide, stones])} itemTypes={itemTypes} onRoomChange={onRoomChange} />);

    await user.click(screen.getAllByRole("button", { name: "수정" })[0]);
    const row = screen.getAllByRole("row")[1];
    await user.clear(within(row).getByLabelText("수"));
    await user.type(within(row).getByLabelText("수"), "5");
    await user.clear(within(row).getByLabelText("재등장(분)"));
    await user.click(within(row).getByRole("button", { name: "저장" }));

    expect(api.updateFloorItem).toHaveBeenCalledWith(11, { itemTypeId: 8, count: 5, respawnMinutes: null });
    expect(onRoomChange).toHaveBeenCalledWith(room([updated, stones]));
  });

  it("삭제 를 누르면 그 바닥 물건을 지우고 방에서 뺀다", async () => {
    const user = userEvent.setup();
    const onRoomChange = vi.fn();
    vi.mocked(api.deleteFloorItem).mockResolvedValue(undefined);
    render(<FloorItemsEditor room={room([wolfHide, stones])} itemTypes={itemTypes} onRoomChange={onRoomChange} />);

    await user.click(screen.getAllByRole("button", { name: "삭제" })[0]);

    expect(api.deleteFloorItem).toHaveBeenCalledWith(11);
    expect(onRoomChange).toHaveBeenCalledWith(room([stones]));
  });

  it("서버가 400 을 주면 detail 문구를 보인다", async () => {
    const user = userEvent.setup();
    vi.mocked(api.createFloorItem).mockRejectedValue(new api.ApiError(400, "물건 종류를 찾을 수 없습니다. id: 1"));
    render(<FloorItemsEditor room={room([])} itemTypes={itemTypes} onRoomChange={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "바닥에 두기" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("물건 종류를 찾을 수 없습니다. id: 1");
  });
});

