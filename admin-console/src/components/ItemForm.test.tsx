import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ApiError, type ItemTypeResponse } from "../lib/api";
import ItemForm from "./ItemForm";

function itemType(patch: Partial<ItemTypeResponse> = {}): ItemTypeResponse {
  return {
    id: 7,
    name: "회춘단",
    description: "상처를 아물게 하는 단약.",
    category: "PILL",
    categoryName: "단약",
    stackSize: 99,
    baseValue: 3,
    tradable: true,
    bagGrade: null,
    bagGradeName: null,
    bagSlots: 0,
    spiritStone: false,
    buyRatePercent: 25,
    ...patch,
  };
}

const spiritStone = itemType({
  id: 1,
  name: "하품 영석",
  description: "영기가 옅게 서린 돌. 수선자들이 값을 치르는 데 쓴다.",
  category: "SPIRIT_STONE",
  categoryName: "영석",
  stackSize: 10000,
  baseValue: 1,
  spiritStone: true,
  buyRatePercent: 0,
});

describe("ItemForm", () => {
  it("새 아이템은 기준가 0·거래 가능 켬으로 열리고, 저물대가 아니면 bagGrade 는 null 이다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<ItemForm onSubmit={onSubmit} />);

    expect(screen.getByLabelText("기준가 (하품)")).toHaveValue(0);
    expect(screen.getByLabelText("거래 가능")).toBeChecked();
    expect(screen.queryByLabelText("저물대 등급")).not.toBeInTheDocument();
    await user.type(screen.getByLabelText("이름"), "회춘단");
    await user.selectOptions(screen.getByLabelText("종류"), screen.getByRole("option", { name: "단약" }));
    await user.type(screen.getByLabelText("묘사"), "상처를 아물게 하는 단약.");
    await user.clear(screen.getByLabelText("기준가 (하품)"));
    await user.type(screen.getByLabelText("기준가 (하품)"), "3");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSubmit).toHaveBeenCalledWith({
      name: "회춘단",
      description: "상처를 아물게 하는 단약.",
      category: "PILL",
      baseValue: 3,
      tradable: true,
      bagGrade: null,
    });
  });

  it("종류 저물대 를 고르면 저물대 등급 이 보이고 저장 본문의 bagGrade 는 LOW 다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<ItemForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText("이름"), "하급 저물대");
    await user.selectOptions(screen.getByLabelText("종류"), screen.getByRole("option", { name: "저물대" }));
    const grade = screen.getByLabelText("저물대 등급");
    expect(within(grade).getAllByRole("option").map((o) => o.textContent)).toEqual([
      "하급 (20칸)",
      "중급 (40칸)",
      "상급 (80칸)",
    ]);
    expect(grade).toHaveValue("LOW");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ category: "BAG", bagGrade: "LOW" }));
  });

  it("저물대 등급을 고른 뒤 다른 종류로 바꾸면 bagGrade 는 null 로 보낸다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<ItemForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText("이름"), "비검");
    await user.selectOptions(screen.getByLabelText("종류"), screen.getByRole("option", { name: "저물대" }));
    await user.selectOptions(screen.getByLabelText("저물대 등급"), "HIGH");
    await user.selectOptions(screen.getByLabelText("종류"), screen.getByRole("option", { name: "법기" }));
    expect(screen.queryByLabelText("저물대 등급")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ category: "ARTIFACT", bagGrade: null }));
  });

  it("종류 선택에 영석 은 없고 나머지 14종이 룰북 순서로 있다", () => {
    render(<ItemForm onSubmit={vi.fn()} />);

    const names = within(screen.getByLabelText("종류"))
      .getAllByRole("option")
      .map((o) => o.textContent);
    expect(names).not.toContain("영석");
    expect(names).toEqual([
      "법기",
      "단약",
      "영약",
      "부적",
      "진법원반",
      "재료",
      "옥간·서책",
      "제작 도구",
      "저물대",
      "영수 알·새끼",
      "영수 먹이",
      "괴뢰",
      "출입권",
      "퀘스트 물건",
    ]);
  });

  it("고칠 때는 저장된 값으로 열린다", () => {
    render(
      <ItemForm
        initial={itemType({ name: "중급 저물대", category: "BAG", categoryName: "저물대", bagGrade: "MIDDLE", tradable: false })}
        onSubmit={vi.fn()}
      />,
    );

    expect(screen.getByLabelText("이름")).toHaveValue("중급 저물대");
    expect(screen.getByLabelText("종류")).toHaveValue("BAG");
    expect(screen.getByLabelText("저물대 등급")).toHaveValue("MIDDLE");
    expect(screen.getByLabelText("거래 가능")).not.toBeChecked();
  });

  it("영석이면 모든 칸이 읽기 전용이고 안내 문구를 보이며 저장 버튼이 없다", () => {
    render(<ItemForm initial={spiritStone} onSubmit={vi.fn()} />);

    expect(screen.getByText("영석은 엔진이 정한 물건이라 고칠 수 없습니다.")).toBeInTheDocument();
    expect(screen.getByLabelText("이름")).toHaveValue("하품 영석");
    expect(screen.getByLabelText("이름")).toBeDisabled();
    expect(screen.getByLabelText("종류")).toBeDisabled();
    expect(screen.getByLabelText("종류")).toHaveValue("SPIRIT_STONE");
    expect(screen.getByLabelText("묘사")).toBeDisabled();
    expect(screen.getByLabelText("기준가 (하품)")).toBeDisabled();
    expect(screen.getByLabelText("거래 가능")).toBeDisabled();
    expect(screen.queryByRole("button", { name: "저장" })).not.toBeInTheDocument();
  });

  it("서버가 409 를 주면 detail 문구를 보인다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi
      .fn()
      .mockRejectedValue(new ApiError(409, "쓰이는 물건은 종류·저물대 등급을 바꿀 수 없습니다: 회춘단"));
    render(<ItemForm initial={itemType()} onSubmit={onSubmit} />);

    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "쓰이는 물건은 종류·저물대 등급을 바꿀 수 없습니다: 회춘단",
    );
  });
});
