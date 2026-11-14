import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { type AbilityDraft, newAbility } from "../lib/techniques";
import AbilityTable from "./AbilityTable";

function Harness({ initial, stageCount = 9 }: { initial: AbilityDraft[]; stageCount?: number }) {
  const [abilities, setAbilities] = useState(initial);
  return <AbilityTable abilities={abilities} stageCount={stageCount} onChange={setAbilities} />;
}

function selectedText(element: HTMLElement): string | undefined {
  const select = element as HTMLSelectElement;
  return select.options[select.selectedIndex]?.textContent ?? undefined;
}

describe("AbilityTable", () => {
  it("신통 추가 의 새 행은 1성·공격·속성 없음·한 대상·소모 없음·시전 0·대기 3·전투 중만이다", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<AbilityTable abilities={[]} stageCount={9} onChange={onChange} />);

    expect(screen.getByText("신통이 없습니다.")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "신통 추가" }));

    expect(onChange).toHaveBeenCalledWith([newAbility()]);
  });

  it("새 행은 화면에서도 기본값으로 보인다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={[]} />);

    await user.click(screen.getByRole("button", { name: "신통 추가" }));

    expect(screen.getByLabelText("이름")).toHaveValue("");
    expect(selectedText(screen.getByLabelText("배우는 단계"))).toBe("1성");
    expect(selectedText(screen.getByLabelText("종류"))).toBe("공격");
    const elements = within(screen.getByRole("group", { name: "속성" }));
    for (const name of ["금", "목", "수", "화", "토", "연체"]) {
      expect(elements.getByLabelText(name)).not.toBeChecked();
    }
    expect(selectedText(screen.getByLabelText("대상"))).toBe("한 대상");
    expect(selectedText(screen.getByLabelText("소모"))).toBe("없음");
    expect(screen.getByLabelText("소모 양")).toHaveValue(0);
    expect(screen.getByLabelText("틱마다")).not.toBeChecked();
    expect(screen.getByLabelText("시전(초)")).toHaveValue(0);
    expect(screen.getByLabelText("대기(초)")).toHaveValue(3);
    expect(selectedText(screen.getByLabelText("쓸 수 있는 때"))).toBe("전투 중만");
  });

  it("배우는 단계 선택지는 1성 ~ 단계 수 다", () => {
    render(<Harness initial={[newAbility()]} stageCount={3} />);

    expect(
      Array.from((screen.getByLabelText("배우는 단계") as HTMLSelectElement).options).map((o) => o.textContent),
    ).toEqual(["1성", "2성", "3성"]);
  });

  it("단계 수보다 높은 배우는 단계는 그 값을 선택에 남긴다", () => {
    render(<Harness initial={[{ ...newAbility(), learnStage: 9 }]} stageCount={3} />);

    expect(selectedText(screen.getByLabelText("배우는 단계"))).toBe("9성");
  });

  it("값을 고치면 속성은 오행 순서로, 소모·틱마다·연체가 행에 들어간다", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const { rerender } = render(
      <AbilityTable abilities={[{ ...newAbility(), elements: ["FIRE"] }]} stageCount={9} onChange={onChange} />,
    );

    await user.click(screen.getByLabelText("목"));
    expect(onChange).toHaveBeenLastCalledWith([expect.objectContaining({ elements: ["WOOD", "FIRE"] })]);

    await user.selectOptions(screen.getByLabelText("소모"), "MANA");
    expect(onChange).toHaveBeenLastCalledWith([expect.objectContaining({ costStat: "MANA" })]);

    await user.click(screen.getByLabelText("틱마다"));
    expect(onChange).toHaveBeenLastCalledWith([expect.objectContaining({ costPerTick: true })]);

    rerender(<AbilityTable abilities={[newAbility()]} stageCount={9} onChange={onChange} />);
    await user.click(screen.getByLabelText("연체"));
    expect(onChange).toHaveBeenLastCalledWith([expect.objectContaining({ bodyRefining: true, elements: [] })]);
  });

  it("삭제 를 누르면 그 행만 빠진다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={[{ ...newAbility(), name: "화탄술" }, { ...newAbility(), name: "천안술" }]} />);

    await user.click(screen.getAllByRole("button", { name: "삭제" })[0]);

    expect(screen.getAllByLabelText("이름").map((input) => (input as HTMLInputElement).value)).toEqual(["천안술"]);
  });
});
