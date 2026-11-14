import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ApiError, type ItemTypeResponse } from "../lib/api";
import StartItemsEditor from "./StartItemsEditor";

function itemType(id: number, name: string): ItemTypeResponse {
  return {
    id,
    name,
    description: "",
    category: "PILL",
    categoryName: "단약",
    stackSize: 99,
    baseValue: 1,
    bagGrade: null,
    bagGradeName: null,
    bagSlots: 0,
    spiritStone: false,
    buyRatePercent: 25,
  };
}

const itemTypes = [itemType(1, "하품 영석"), itemType(5, "하급 저물대"), itemType(7, "회춘단(연기 초)")];

describe("StartItemsEditor", () => {
  it("빈 목록이면 안내와 시작 소지품이 없습니다. 를 보이고, 물건 추가 를 누르면 행이 생긴다", async () => {
    const user = userEvent.setup();
    render(<StartItemsEditor itemTypes={itemTypes} initial={[]} onSave={vi.fn()} />);

    expect(screen.getByText("새로 만드는 캐릭터부터 받습니다.")).toBeInTheDocument();
    expect(screen.getByText("시작 소지품이 없습니다.")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "물건 추가" }));

    expect(screen.queryByText("시작 소지품이 없습니다.")).not.toBeInTheDocument();
    expect(screen.getAllByLabelText("물건")).toHaveLength(1);
    expect(screen.getByLabelText("수")).toHaveValue(1);
  });

  it("행의 삭제 를 누르면 그 행만 사라진다", async () => {
    const user = userEvent.setup();
    render(
      <StartItemsEditor
        itemTypes={itemTypes}
        initial={[
          { itemTypeId: 5, count: 1 },
          { itemTypeId: 1, count: 30 },
        ]}
        onSave={vi.fn()}
      />,
    );

    await user.click(screen.getAllByRole("button", { name: "삭제" })[0]);

    expect(screen.getAllByLabelText("물건")).toHaveLength(1);
    expect(screen.getByLabelText("물건")).toHaveValue("1");
    expect(screen.getByLabelText("수")).toHaveValue(30);
  });

  it("저장하면 행 순서대로 보내고 성공하면 저장했습니다. 를 보인다", async () => {
    const user = userEvent.setup();
    const onSave = vi.fn().mockResolvedValue(undefined);
    render(<StartItemsEditor itemTypes={itemTypes} initial={[{ itemTypeId: 5, count: 1 }]} onSave={onSave} />);

    await user.click(screen.getByRole("button", { name: "물건 추가" }));
    await user.selectOptions(screen.getAllByLabelText("물건")[1], "1");
    await user.clear(screen.getAllByLabelText("수")[1]);
    await user.type(screen.getAllByLabelText("수")[1], "30");
    await user.click(screen.getByRole("button", { name: "물건 추가" }));
    await user.selectOptions(screen.getAllByLabelText("물건")[2], "7");
    await user.clear(screen.getAllByLabelText("수")[2]);
    await user.type(screen.getAllByLabelText("수")[2], "3");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSave).toHaveBeenCalledWith([
      { itemTypeId: 5, count: 1 },
      { itemTypeId: 1, count: 30 },
      { itemTypeId: 7, count: 3 },
    ]);
    expect(await screen.findByText("저장했습니다.")).toBeInTheDocument();
  });

  it("서버가 400 을 주면 detail 문구를 보인다", async () => {
    const user = userEvent.setup();
    const onSave = vi.fn().mockRejectedValue(new ApiError(400, "같은 물건이 두 번 있습니다: 하품 영석"));
    render(
      <StartItemsEditor
        itemTypes={itemTypes}
        initial={[
          { itemTypeId: 1, count: 1 },
          { itemTypeId: 1, count: 2 },
        ]}
        onSave={onSave}
      />,
    );

    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("같은 물건이 두 번 있습니다: 하품 영석");
    expect(screen.queryByText("저장했습니다.")).not.toBeInTheDocument();
  });
});
