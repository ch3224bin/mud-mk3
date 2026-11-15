import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import type { ItemTypeResponse } from "../lib/api";
import { type DropTableDraft, emptyDropTable, toDropTable } from "../lib/mobs";
import DropTableEditor from "./DropTableEditor";

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
    techniqueId: null,
    techniqueName: null,
  };
}

const itemTypes = [itemType(7, "늑대 가죽"), itemType(8, "늑대 힘줄"), itemType(9, "청랑 송곳니")];

/** 폼처럼 초안을 들고, 마지막 초안을 요청 모양으로 화면에 적어 둔다 */
function Harness({ initial = emptyDropTable() }: { initial?: DropTableDraft }) {
  const [draft, setDraft] = useState(initial);
  return (
    <>
      <DropTableEditor itemTypes={itemTypes} value={draft} onChange={setDraft} />
      <output data-testid="request">{JSON.stringify(toDropTable(draft))}</output>
    </>
  );
}

function request() {
  return JSON.parse(screen.getByTestId("request").textContent ?? "");
}

function tierGroup(name: string) {
  return within(screen.getByRole("group", { name }));
}

describe("DropTableEditor", () => {
  it("비어 있으면 고정 재료 표와 계층 셋 칸(고급·희귀·진귀)이 빈 채로 보인다", () => {
    render(<Harness />);

    expect(screen.getByRole("heading", { name: "고정 재료" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "계층 드롭" })).toBeInTheDocument();
    expect(screen.getAllByRole("group").map((g) => g.querySelector("legend")?.textContent)).toEqual([
      "고급",
      "희귀",
      "진귀",
    ]);
    expect(request()).toEqual({ fixedDrops: [], tierDrops: [] });
  });

  it("고정 재료를 추가하면 첫 물건·수 1 로 더하고, 물건·수를 고치고 지울 수 있다", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.click(screen.getByRole("button", { name: "고정 재료 추가" }));
    await user.click(screen.getByRole("button", { name: "고정 재료 추가" }));
    expect(screen.getByLabelText("고정 재료 1 물건")).toHaveValue("7");
    expect(screen.getByLabelText("고정 재료 1 수")).toHaveValue(1);

    await user.selectOptions(screen.getByLabelText("고정 재료 2 물건"), "8");
    await user.clear(screen.getByLabelText("고정 재료 2 수"));
    await user.type(screen.getByLabelText("고정 재료 2 수"), "3");
    expect(request().fixedDrops).toEqual([
      { itemTypeId: 7, count: 1 },
      { itemTypeId: 8, count: 3 },
    ]);

    const firstRow = screen.getByLabelText("고정 재료 1 물건").closest("tr")!;
    await user.click(within(firstRow).getByRole("button", { name: "삭제" }));
    expect(request().fixedDrops).toEqual([{ itemTypeId: 8, count: 3 }]);
  });

  it("계층마다 물건을 추가·고치기·삭제하고, 요청은 고급·희귀·진귀 순서의 tierDrops 다", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.click(tierGroup("진귀").getByRole("button", { name: "물건 추가" }));
    await user.click(tierGroup("고급").getByRole("button", { name: "물건 추가" }));
    await user.click(tierGroup("고급").getByRole("button", { name: "물건 추가" }));
    await user.selectOptions(tierGroup("고급").getByLabelText("고급 2 물건"), "8");
    await user.selectOptions(tierGroup("진귀").getByLabelText("진귀 1 물건"), "9");

    expect(request()).toEqual({
      fixedDrops: [],
      tierDrops: [
        { tier: "ADVANCED", itemTypeId: 7 },
        { tier: "ADVANCED", itemTypeId: 8 },
        { tier: "TREASURE", itemTypeId: 9 },
      ],
    });

    await user.click(tierGroup("고급").getAllByRole("button", { name: "삭제" })[0]);
    expect(request().tierDrops).toEqual([
      { tier: "ADVANCED", itemTypeId: 8 },
      { tier: "TREASURE", itemTypeId: 9 },
    ]);
    expect(tierGroup("희귀").queryAllByRole("combobox")).toHaveLength(0);
  });

  it("저장된 드롭 표로 열린다", () => {
    render(
      <Harness
        initial={{ fixedDrops: [{ itemTypeId: 7, count: "2" }], tierDrops: { ADVANCED: [], RARE: [9], TREASURE: [] } }}
      />,
    );

    expect(screen.getByLabelText("고정 재료 1 물건")).toHaveValue("7");
    expect(screen.getByLabelText("고정 재료 1 수")).toHaveValue(2);
    expect(tierGroup("희귀").getByLabelText("희귀 1 물건")).toHaveValue("9");
    expect(request()).toEqual({ fixedDrops: [{ itemTypeId: 7, count: 2 }], tierDrops: [{ tier: "RARE", itemTypeId: 9 }] });
  });
});
