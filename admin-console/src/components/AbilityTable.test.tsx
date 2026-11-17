import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import type { AbilityResponse } from "../lib/api";
import { type AbilityDraft, abilityDraftOf, newAbility } from "../lib/techniques";
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

    expect(onChange).toHaveBeenCalledWith([newAbility(9)]);
  });

  it("새 행은 화면에서도 기본값으로 보인다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={[]} />);

    await user.click(screen.getByRole("button", { name: "신통 추가" }));

    expect(screen.getByLabelText("신통 이름")).toHaveValue("");
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

  it("소모 양·시전·대기 칸은 브라우저가 소수 자리를 막지 않는다(서버 400 을 보인다)", () => {
    render(<Harness initial={[newAbility(9)]} stageCount={9} />);

    for (const label of ["소모 양", "시전(초)", "대기(초)"]) {
      expect(screen.getByLabelText(label)).toHaveAttribute("step", "any");
    }
  });

  it("배우는 단계 선택지는 1성 ~ 단계 수 다", () => {
    render(<Harness initial={[newAbility(3)]} stageCount={3} />);

    expect(
      Array.from((screen.getByLabelText("배우는 단계") as HTMLSelectElement).options).map((o) => o.textContent),
    ).toEqual(["1성", "2성", "3성"]);
  });

  it("단계 수보다 높은 배우는 단계는 그 값을 선택에 남긴다", () => {
    render(<Harness initial={[{ ...newAbility(9), learnStage: 9 }]} stageCount={3} />);

    expect(selectedText(screen.getByLabelText("배우는 단계"))).toBe("9성");
  });

  it("값을 고치면 속성은 오행 순서로, 소모·틱마다·연체가 행에 들어간다", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const { rerender } = render(
      <AbilityTable abilities={[{ ...newAbility(9), elements: ["FIRE"] }]} stageCount={9} onChange={onChange} />,
    );

    await user.click(screen.getByLabelText("목"));
    expect(onChange).toHaveBeenLastCalledWith([expect.objectContaining({ elements: ["WOOD", "FIRE"] })]);

    await user.selectOptions(screen.getByLabelText("소모"), "MANA");
    expect(onChange).toHaveBeenLastCalledWith([expect.objectContaining({ costStat: "MANA" })]);

    await user.click(screen.getByLabelText("틱마다"));
    expect(onChange).toHaveBeenLastCalledWith([expect.objectContaining({ costPerTick: true })]);

    rerender(<AbilityTable abilities={[newAbility(9)]} stageCount={9} onChange={onChange} />);
    await user.click(screen.getByLabelText("연체"));
    expect(onChange).toHaveBeenLastCalledWith([expect.objectContaining({ bodyRefining: true, elements: [] })]);
  });

  it("삭제 를 누르면 그 행만 빠진다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={[{ ...newAbility(9), name: "화탄술" }, { ...newAbility(9), name: "천안술" }]} />);

    await user.click(screen.getByRole("button", { name: "화탄술 삭제" }));

    expect(screen.getAllByLabelText("신통 이름").map((input) => (input as HTMLInputElement).value)).toEqual(["천안술"]);
  });

  it("자세히 를 누르면 그 행 아래 칸이 펼쳐지고 접기 로 접힌다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={[{ ...newAbility(9), name: "화탄술" }, { ...newAbility(9), name: "천안술" }]} />);

    expect(screen.queryByText("이펙트 문장")).not.toBeInTheDocument();
    await user.click(screen.getAllByRole("button", { name: "자세히" })[1]);

    expect(screen.getByText("이펙트 문장")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "자세히" })).toHaveLength(1);
    const rows = screen.getAllByRole("row");
    const detailRow = rows.findIndex((row) => within(row).queryByText("이펙트 문장") !== null);
    expect(within(rows[detailRow - 1]).getByLabelText("신통 이름")).toHaveValue("천안술");

    await user.click(screen.getByRole("button", { name: "접기" }));
    expect(screen.queryByText("이펙트 문장")).not.toBeInTheDocument();
  });

  it("펼친 칸에서 적은 이펙트 문장이 행에 들어간다", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<AbilityTable abilities={[newAbility(9)]} stageCount={9} onChange={onChange} />);

    await user.click(screen.getByRole("button", { name: "자세히" }));
    await user.type(screen.getByLabelText("이펙트 문장 1"), "불");

    expect(onChange).toHaveBeenLastCalledWith([expect.objectContaining({ effectLines: ["불"] })]);
  });

  it("저장된 응답이 게임에서 쓸 수 없는 꼴이면 행 끝에 알리고, 새 행은 알리지 않는다", async () => {
    const user = userEvent.setup();
    const response: AbilityResponse = {
      name: "어풍결",
      learnStage: 1,
      kind: "ESCAPE",
      kindName: "둔술",
      elements: ["WOOD"],
      bodyRefining: false,
      target: "SELF",
      targetName: "자신",
      costStat: "MANA",
      costAmount: 1,
      costPerTick: true,
      castSeconds: 0,
      cooldownSeconds: 3,
      usableWhen: "ANYTIME",
      effectLines: ["몸이 바람을 탄다."],
      stagePowers: [],
      usableInGame: false,
    };
    render(<Harness initial={[abilityDraftOf(response), abilityDraftOf({ ...response, name: "화탄술", usableInGame: true })]} />);

    const rows = screen.getAllByRole("row").slice(1);
    expect(rows[0].lastElementChild).toHaveTextContent("(게임에서 아직 쓸 수 없음)");
    expect(within(rows[1]).queryByText("(게임에서 아직 쓸 수 없음)")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "신통 추가" }));
    expect(screen.getAllByText("(게임에서 아직 쓸 수 없음)")).toHaveLength(1);
  });

  it("종류를 공격에서 바꾸면 단계별 값을 숨기고, 다시 공격으로 돌리면 숨기기 전 값이 보인다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={[newAbility(9)]} />);

    await user.click(screen.getByRole("button", { name: "자세히" }));
    await user.clear(screen.getByLabelText("3성 기본 위력"));
    await user.type(screen.getByLabelText("3성 기본 위력"), "7.5");
    await user.selectOptions(screen.getByLabelText("종류"), "ESCAPE");

    expect(screen.queryByText("단계별 값")).not.toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText("종류"), "ATTACK");

    expect(screen.getByText("단계별 값")).toBeInTheDocument();
    expect(screen.getByLabelText("3성 기본 위력")).toHaveValue(7.5);
  });

  it("공격이 아니던 신통을 공격으로 바꾸면 배우는 단계 ~ 단계 수 행을 채우기 기본값으로 만든다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={[{ ...newAbility(3), kind: "ESCAPE", stagePowers: [] }]} stageCount={3} />);

    await user.click(screen.getByRole("button", { name: "자세히" }));
    await user.selectOptions(screen.getByLabelText("종류"), "ATTACK");

    expect(screen.getByLabelText("1성 기본 위력")).toHaveValue(2);
    expect(screen.getByLabelText("3성 계수")).toHaveValue(1.2);
  });

  it("배우는 단계를 바꾸면 단계별 값 행을 배우는 단계부터로 맞추고 남는 단계 값은 그대로다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={[newAbility(9)]} />);

    await user.click(screen.getByRole("button", { name: "자세히" }));
    await user.clear(screen.getByLabelText("5성 기본 위력"));
    await user.type(screen.getByLabelText("5성 기본 위력"), "9");
    await user.selectOptions(screen.getByLabelText("배우는 단계"), "3");

    expect(screen.queryByLabelText("2성 기본 위력")).not.toBeInTheDocument();
    expect(screen.getByLabelText("3성 기본 위력")).toHaveValue(2.4);
    expect(screen.getByLabelText("5성 기본 위력")).toHaveValue(9);
    expect(screen.getByLabelText("9성 기본 위력")).toHaveValue(3.6);
  });
});
