import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { emptyStage, type StageDraft } from "../lib/techniques";
import StageTable from "./StageTable";

function Harness({ initial }: { initial: StageDraft[] }) {
  const [stages, setStages] = useState(initial);
  return <StageTable stages={stages} onChange={setStages} />;
}

function nineStages(): StageDraft[] {
  return Array.from({ length: 9 }, (_, i) => ({ ...emptyStage(), manaBonus: String(i + 1) }));
}

/** 표의 몸통 행(머리 행 뺌) */
function bodyRows(): HTMLElement[] {
  return screen.getAllByRole("row").slice(1);
}

describe("StageTable", () => {
  it("열은 단계·필요 깨달음·자질 조건·법력·의식·영력·정혈·육신·근력·영근 자질 이다", () => {
    render(<Harness initial={nineStages()} />);

    expect(screen.getAllByRole("columnheader").map((h) => h.textContent)).toEqual([
      "단계",
      "필요 깨달음",
      "자질 조건",
      "법력",
      "의식",
      "영력",
      "정혈",
      "육신",
      "근력",
      "영근 자질",
    ]);
  });

  it("1성 필요 깨달음 칸은 입력 없이 읽으면 이고, 2성부터는 입력이 있다", () => {
    render(<Harness initial={nineStages()} />);

    expect(bodyRows()[0]).toHaveTextContent("1성읽으면");
    expect(screen.queryByLabelText("1성 필요 깨달음")).not.toBeInTheDocument();
    expect(screen.getByLabelText("2성 필요 깨달음")).toHaveValue(0);
    expect(screen.getByLabelText("1성 법력")).toHaveValue(1);
  });

  it("단계 수를 9 에서 12 로 늘리면 10~12성 행이 모두 0 으로 더해지고 기존 행은 그대로다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={nineStages()} />);

    expect(screen.getByLabelText("단계 수")).toHaveValue("9");
    await user.selectOptions(screen.getByLabelText("단계 수"), "12");

    expect(bodyRows()).toHaveLength(12);
    expect(screen.getByLabelText("9성 법력")).toHaveValue(9);
    for (const label of ["필요 깨달음", "자질 조건", "법력", "의식", "영력", "정혈", "육신", "근력", "영근 자질"]) {
      expect(screen.getByLabelText(`12성 ${label}`)).toHaveValue(0);
    }
    expect(screen.getByLabelText("10성 필요 깨달음")).toHaveValue(0);
  });

  it("단계 수를 12 에서 3 으로 줄이면 3성까지만 남는다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={Array.from({ length: 12 }, emptyStage)} />);

    await user.selectOptions(screen.getByLabelText("단계 수"), "3");

    expect(bodyRows()).toHaveLength(3);
    expect(screen.getByLabelText("3성 법력")).toBeInTheDocument();
    expect(screen.queryByLabelText("4성 법력")).not.toBeInTheDocument();
  });

  it("단계 수 선택지는 1~12 다", () => {
    render(<Harness initial={nineStages()} />);

    expect(
      Array.from((screen.getByLabelText("단계 수") as HTMLSelectElement).options).map((o) => o.textContent),
    ).toEqual(["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"]);
  });

  it("칸을 고치면 그 단계 값이 바뀐다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={nineStages()} />);

    await user.clear(screen.getByLabelText("2성 필요 깨달음"));
    await user.type(screen.getByLabelText("2성 필요 깨달음"), "120");

    expect(screen.getByLabelText("2성 필요 깨달음")).toHaveValue(120);
  });
});
