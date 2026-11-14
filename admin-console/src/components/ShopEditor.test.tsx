import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../lib/api";
import type { ItemCategoryCode, ItemTypeResponse, NpcDetailResponse } from "../lib/api";
import ShopEditor from "./ShopEditor";

vi.mock("../lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../lib/api")>()),
  saveShop: vi.fn(),
  deleteShop: vi.fn(),
}));

function itemType(id: number, name: string, category: ItemCategoryCode, baseValue: number): ItemTypeResponse {
  return {
    id,
    name,
    description: "",
    category,
    categoryName: "",
    stackSize: 99,
    baseValue,
    tradable: true,
    bagGrade: null,
    bagGradeName: null,
    bagSlots: 0,
    spiritStone: category === "SPIRIT_STONE",
    buyRatePercent: 25,
  };
}

const itemTypes = [
  itemType(1, "하품 영석", "SPIRIT_STONE", 1),
  itemType(6, "연기기 비검", "ARTIFACT", 30),
  itemType(7, "회춘단(연기 초)", "PILL", 3),
  itemType(8, "늑대 가죽", "MATERIAL", 2),
  itemType(9, "청령단", "PILL", 1500),
];

const npc: NpcDetailResponse = {
  id: 4,
  name: "왕씨",
  greeting: "",
  defaultAnswer: "",
  topics: [],
  llmEnabled: false,
  roomId: null,
  roomName: null,
  zoneId: null,
  zoneName: null,
  shop: null,
};

const pillShop: NpcDetailResponse = {
  ...npc,
  shop: {
    kind: "PILL_SHOP",
    kindName: "단약방",
    items: [{ itemTypeId: 7, itemName: "회춘단(연기 초)", price: 5, effectivePrice: 5 }],
  },
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("ShopEditor", () => {
  it("상점이 아니면 상점 종류가 상점 아님 이고 상점 아님 + 7종을 고를 수 있다", () => {
    render(<ShopEditor npc={npc} itemTypes={itemTypes} onNpcChange={vi.fn()} />);

    expect(screen.getByRole("heading", { name: "상점" })).toBeInTheDocument();
    expect(screen.getByLabelText("상점 종류")).toHaveValue("");
    expect(
      within(screen.getByLabelText("상점 종류"))
        .getAllByRole("option")
        .map((o) => o.textContent),
    ).toEqual(["상점 아님", "단약방", "법기점", "서방", "부적방", "재료상", "잡화점", "영수점"]);
    expect(screen.getByText("값을 비워 두면 기준가로 팝니다. 사 주는 값은 룰북 매입 규칙으로 정해집니다.")).toBeInTheDocument();
  });

  it("종류가 단약방이면 물건 선택에 단약만 보이고, 값 자리 표시는 기준가 {n} 이다", async () => {
    const user = userEvent.setup();
    render(<ShopEditor npc={npc} itemTypes={itemTypes} onNpcChange={vi.fn()} />);

    await user.selectOptions(screen.getByLabelText("상점 종류"), "PILL_SHOP");
    await user.click(screen.getByRole("button", { name: "물건 추가" }));

    const select = screen.getByLabelText("물건");
    expect(within(select).getAllByRole("option").map((o) => o.textContent)).toEqual(["회춘단(연기 초)", "청령단"]);
    expect(screen.getByLabelText("값 (하품)")).toHaveValue(null);
    expect(screen.getByLabelText("값 (하품)")).toHaveAttribute("placeholder", "기준가 3");
    await user.selectOptions(select, "9");
    expect(screen.getByLabelText("값 (하품)")).toHaveAttribute("placeholder", "기준가 1,500");
  });

  it("값을 비우면 price:null, 적으면 그 수로 보내고 응답으로 NPC를 바꾼다", async () => {
    const user = userEvent.setup();
    const onNpcChange = vi.fn();
    vi.mocked(api.saveShop).mockResolvedValue(pillShop);
    render(<ShopEditor npc={pillShop} itemTypes={itemTypes} onNpcChange={onNpcChange} />);

    expect(screen.getByLabelText("상점 종류")).toHaveValue("PILL_SHOP");
    expect(screen.getByLabelText("값 (하품)")).toHaveValue(5);
    await user.click(screen.getByRole("button", { name: "물건 추가" }));
    await user.selectOptions(screen.getAllByLabelText("물건")[1], "9");
    await user.clear(screen.getAllByLabelText("값 (하품)")[0]);
    await user.type(screen.getAllByLabelText("값 (하품)")[1], "1600");
    await user.click(screen.getByRole("button", { name: "상점 저장" }));

    expect(api.saveShop).toHaveBeenCalledWith(4, {
      kind: "PILL_SHOP",
      items: [
        { itemTypeId: 7, price: null },
        { itemTypeId: 9, price: 1600 },
      ],
    });
    expect(onNpcChange).toHaveBeenCalledWith(pillShop);
    expect(await screen.findByText("저장했습니다.")).toBeInTheDocument();
  });

  it("행의 삭제 를 누르면 그 행만 사라진다", async () => {
    const user = userEvent.setup();
    render(<ShopEditor npc={pillShop} itemTypes={itemTypes} onNpcChange={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "삭제" }));

    expect(screen.queryByLabelText("물건")).not.toBeInTheDocument();
  });

  it("상점 아님 으로 저장하면 deleteShop 을 부르고 상점을 없앤 NPC로 바꾼다", async () => {
    const user = userEvent.setup();
    const onNpcChange = vi.fn();
    vi.mocked(api.deleteShop).mockResolvedValue(undefined);
    render(<ShopEditor npc={pillShop} itemTypes={itemTypes} onNpcChange={onNpcChange} />);

    await user.selectOptions(screen.getByLabelText("상점 종류"), "");
    await user.click(screen.getByRole("button", { name: "상점 저장" }));

    expect(api.deleteShop).toHaveBeenCalledWith(4);
    expect(api.saveShop).not.toHaveBeenCalled();
    expect(onNpcChange).toHaveBeenCalledWith({ ...pillShop, shop: null });
  });

  it("서버가 400 을 주면 detail 문구를 보인다", async () => {
    const user = userEvent.setup();
    vi.mocked(api.saveShop).mockRejectedValue(
      new api.ApiError(400, "판매가는 매입가의 2배보다 커야 합니다: 회춘단(연기 초) (매입가 0.75, 판매가 1)"),
    );
    render(<ShopEditor npc={pillShop} itemTypes={itemTypes} onNpcChange={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "상점 저장" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "판매가는 매입가의 2배보다 커야 합니다: 회춘단(연기 초) (매입가 0.75, 판매가 1)",
    );
    expect(screen.queryByText("저장했습니다.")).not.toBeInTheDocument();
  });
});
