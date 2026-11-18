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
    techniqueId: null,
    techniqueName: null,
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

  it("zeroCountShowsServerDetail: 수 0 도 브라우저가 막지 않고 서버 detail 을 보인다", async () => {
    const user = userEvent.setup();
    const onSave = vi.fn().mockRejectedValue(new ApiError(400, "수는 1 이상이어야 합니다."));
    render(<StartItemsEditor itemTypes={itemTypes} initial={[{ itemTypeId: 5, count: 1 }]} onSave={onSave} />);

    await user.clear(screen.getByLabelText("수"));
    await user.type(screen.getByLabelText("수"), "0");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSave).toHaveBeenCalledWith([{ itemTypeId: 5, count: 0 }]);
    expect(await screen.findByRole("alert")).toHaveTextContent("수는 1 이상이어야 합니다.");
  });

  describe("목록에 없는(지워진) 물건 id", () => {
    const notice = "목록에 없는 물건이 있습니다. 저장하기 전에 고치십시오.";

    it("(지워진 물건 #9) 로 골라 두고 안내 줄을 보인다", () => {
      render(<StartItemsEditor itemTypes={itemTypes} initial={[{ itemTypeId: 9, count: 2 }]} onSave={vi.fn()} />);

      const select = screen.getByLabelText("물건");
      expect(select).toHaveValue("9");
      expect(screen.getByRole("option", { name: "(지워진 물건 #9)", selected: true })).toBeInTheDocument();
      expect(screen.getByText(notice)).toBeInTheDocument();
    });

    it("다른 물건을 고르면 그 선택지와 안내 줄이 사라진다", async () => {
      const user = userEvent.setup();
      render(<StartItemsEditor itemTypes={itemTypes} initial={[{ itemTypeId: 9, count: 2 }]} onSave={vi.fn()} />);

      await user.selectOptions(screen.getByLabelText("물건"), "1");

      expect(screen.getByLabelText("물건")).toHaveValue("1");
      expect(screen.queryByRole("option", { name: "(지워진 물건 #9)" })).not.toBeInTheDocument();
      expect(screen.queryByText(notice)).not.toBeInTheDocument();
    });

    it("그대로 저장하면 그 id 를 보내고 서버 detail 을 보인다", async () => {
      const user = userEvent.setup();
      const onSave = vi.fn().mockRejectedValue(new ApiError(400, "물건 종류를 찾을 수 없습니다. id: 9"));
      render(<StartItemsEditor itemTypes={itemTypes} initial={[{ itemTypeId: 9, count: 2 }]} onSave={onSave} />);

      await user.click(screen.getByRole("button", { name: "저장" }));

      expect(onSave).toHaveBeenCalledWith([{ itemTypeId: 9, count: 2 }]);
      expect(await screen.findByRole("alert")).toHaveTextContent("물건 종류를 찾을 수 없습니다. id: 9");
    });
  });
});
