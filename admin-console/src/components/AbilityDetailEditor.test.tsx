import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { type AbilityDraft, newAbility } from "../lib/techniques";
import AbilityDetailEditor from "./AbilityDetailEditor";

function Harness({ initial, stageCount = 3 }: { initial: AbilityDraft; stageCount?: number }) {
  const [ability, setAbility] = useState(initial);
  return (
    <AbilityDetailEditor
      ability={ability}
      stageCount={stageCount}
      onChange={(patch) => setAbility((current) => ({ ...current, ...patch }))}
    />
  );
}

function lineValues(): string[] {
  return screen.getAllByLabelText(/^이펙트 문장 \d$/).map((input) => (input as HTMLInputElement).value);
}

function columnTexts(): string[] {
  return screen.getAllByRole("columnheader").map((th) => th.textContent ?? "");
}

describe("AbilityDetailEditor", () => {
  it("이펙트 문장 안내 문구를 보인다", () => {
    render(<Harness initial={newAbility(3)} />);

    expect(
      screen.getByText(
        "쓸 때 하나가 무작위로 보입니다. {대상} · {시전자} 를 쓸 수 있고, 바로 뒤 조사는 받침에 맞춰 바뀝니다. (예: {대상}을 → 청모랑을)",
      ),
    ).toBeInTheDocument();
  });

  it("문장이 1개일 때 삭제 는 비활성이고, 문장 추가 로 늘려 5개가 되면 문장 추가 가 비활성이다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={newAbility(3)} />);

    expect(screen.getByRole("button", { name: "삭제" })).toBeDisabled();
    for (let i = 0; i < 4; i++) {
      await user.click(screen.getByRole("button", { name: "문장 추가" }));
    }

    expect(lineValues()).toHaveLength(5);
    expect(screen.getByRole("button", { name: "문장 추가" })).toBeDisabled();
    for (const button of screen.getAllByRole("button", { name: "삭제" })) {
      expect(button).toBeEnabled();
    }
  });

  it("문장을 적고 지우면 그 줄만 빠진다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={{ ...newAbility(3), effectLines: ["불이 인다.", "{대상}을 덮친다!"] }} />);

    await user.type(screen.getByLabelText("이펙트 문장 2"), "!");
    await user.click(screen.getAllByRole("button", { name: "삭제" })[0]);

    expect(lineValues()).toEqual(["{대상}을 덮친다!!"]);
  });

  it("공격이면 단계별 값 표를 보인다(열 단계·기본 위력·계수, 단계 이름 1성)", () => {
    render(<Harness initial={newAbility(3)} />);

    expect(screen.getByText("단계별 값")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "채우기" })).toBeInTheDocument();
    expect(columnTexts()).toEqual(["단계", "기본 위력", "계수"]);
    expect(screen.getByText("1성")).toBeInTheDocument();
    expect(screen.getByLabelText("1성 기본 위력")).toHaveValue(2);
    expect(screen.getByLabelText("3성 계수")).toHaveValue(1.2);
  });

  it("채우기 칸 기본값은 2·1.2·0.2·0 이고 단계별 값 칸은 브라우저가 소수 자리를 막지 않는다", () => {
    render(<Harness initial={newAbility(3)} />);

    expect(screen.getByLabelText("기본 위력")).toHaveValue(2);
    expect(screen.getByLabelText("계수")).toHaveValue(1.2);
    expect(screen.getByLabelText("단계마다 기본 +")).toHaveValue(0.2);
    expect(screen.getByLabelText("단계마다 계수 +")).toHaveValue(0);
    for (const label of ["기본 위력", "계수", "단계마다 기본 +", "단계마다 계수 +", "1성 기본 위력", "1성 계수"]) {
      expect(screen.getByLabelText(label)).toHaveAttribute("step", "any");
    }
  });

  it("채우기 를 누르면 넣은 값으로 표를 채운다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={newAbility(3)} />);

    await user.clear(screen.getByLabelText("기본 위력"));
    await user.type(screen.getByLabelText("기본 위력"), "5");
    await user.clear(screen.getByLabelText("계수"));
    await user.type(screen.getByLabelText("계수"), "2.2");
    await user.clear(screen.getByLabelText("단계마다 기본 +"));
    await user.type(screen.getByLabelText("단계마다 기본 +"), "0.5");
    await user.clear(screen.getByLabelText("단계마다 계수 +"));
    await user.type(screen.getByLabelText("단계마다 계수 +"), "0.044");
    await user.click(screen.getByRole("button", { name: "채우기" }));

    expect(screen.getByLabelText("1성 기본 위력")).toHaveValue(5);
    expect(screen.getByLabelText("2성 기본 위력")).toHaveValue(5.5);
    expect(screen.getByLabelText("3성 기본 위력")).toHaveValue(6);
    expect(screen.getByLabelText("3성 계수")).toHaveValue(2.288);
  });

  it("표의 칸을 고치면 그 단계 값만 바뀐다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={newAbility(3)} />);

    await user.clear(screen.getByLabelText("2성 계수"));
    await user.type(screen.getByLabelText("2성 계수"), "1.25");

    expect(screen.getByLabelText("1성 계수")).toHaveValue(1.2);
    expect(screen.getByLabelText("2성 계수")).toHaveValue(1.25);
  });

  it("공격이 아니면 단계별 값·채우기 를 보이지 않는다", () => {
    render(<Harness initial={{ ...newAbility(3), kind: "ESCAPE" }} />);

    expect(screen.getByText("이펙트 문장")).toBeInTheDocument();
    expect(screen.queryByText("단계별 값")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "채우기" })).not.toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });
});
