import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ApiError, type ItemTypeResponse, type TechniqueSummaryResponse } from "../lib/api";
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
    bagGrade: null,
    bagGradeName: null,
    bagSlots: 0,
    spiritStone: false,
    buyRatePercent: 25,
    techniqueId: null,
    techniqueName: null,
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
  it("새 아이템은 기준가 0으로 열리고, 저물대가 아니면 bagGrade 는 null 이다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<ItemForm onSubmit={onSubmit} />);

    expect(screen.getByLabelText("기준가 (하품)")).toHaveValue(0);
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
      bagGrade: null,
      techniqueId: null,
    });
  });

  it("종류 저물대·저물탁 을 고르면 저물대 등급 이 보이고 저장 본문의 bagGrade 는 LOW 다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<ItemForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText("이름"), "하급 저물대");
    await user.selectOptions(screen.getByLabelText("종류"), screen.getByRole("option", { name: "저물대·저물탁" }));
    const grade = screen.getByLabelText("저물대 등급");
    expect(within(grade).getAllByRole("option").map((o) => o.textContent)).toEqual([
      "하급 저물대 (20칸)",
      "중급 저물대 (40칸)",
      "상급 저물대 (80칸)",
      "하급 저물탁 (100칸)",
      "중급 저물탁 (200칸)",
      "상급 저물탁 (400칸)",
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
    await user.selectOptions(screen.getByLabelText("종류"), screen.getByRole("option", { name: "저물대·저물탁" }));
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
      "저물대·저물탁",
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
        initial={itemType({ name: "중급 저물대", category: "BAG", categoryName: "저물대·저물탁", bagGrade: "MIDDLE" })}
        onSubmit={vi.fn()}
      />,
    );

    expect(screen.getByLabelText("이름")).toHaveValue("중급 저물대");
    expect(screen.getByLabelText("종류")).toHaveValue("BAG");
    expect(screen.getByLabelText("저물대 등급")).toHaveValue("MIDDLE");
  });

  it("저물탁 등급을 고르면 bagGrade 는 BRACELET_LOW 다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<ItemForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText("이름"), "하급 저물탁");
    await user.selectOptions(screen.getByLabelText("종류"), "BAG");
    await user.selectOptions(
      screen.getByLabelText("저물대 등급"),
      screen.getByRole("option", { name: "하급 저물탁 (100칸)" }),
    );
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ category: "BAG", bagGrade: "BRACELET_LOW" }));
  });

  it("담긴 공법 은 종류가 옥간·서책일 때만 보이고, 첫 항목 없음 다음 공법 이름순이다", async () => {
    const user = userEvent.setup();
    render(<ItemForm techniques={[technique(3, "청목결"), technique(2, "강체결"), technique(1, "장춘공")]} onSubmit={vi.fn()} />);

    expect(screen.queryByLabelText("담긴 공법")).not.toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText("종류"), "SCROLL");

    const select = screen.getByLabelText("담긴 공법");
    expect(within(select).getAllByRole("option").map((o) => o.textContent)).toEqual([
      "없음",
      "강체결",
      "장춘공",
      "청목결",
    ]);
    expect(select).toHaveValue("");
    await user.selectOptions(screen.getByLabelText("종류"), "PILL");
    expect(screen.queryByLabelText("담긴 공법")).not.toBeInTheDocument();
  });

  it("담긴 공법을 고르면 techniqueId 를 보내고, 없음 이면 null 이다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<ItemForm techniques={[technique(1, "장춘공")]} onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText("이름"), "장춘공 옥간");
    await user.selectOptions(screen.getByLabelText("종류"), "SCROLL");
    await user.selectOptions(screen.getByLabelText("담긴 공법"), screen.getByRole("option", { name: "장춘공" }));
    await user.click(screen.getByRole("button", { name: "저장" }));
    expect(onSubmit).toHaveBeenLastCalledWith(expect.objectContaining({ category: "SCROLL", techniqueId: 1 }));

    await user.selectOptions(screen.getByLabelText("담긴 공법"), screen.getByRole("option", { name: "없음" }));
    await user.click(screen.getByRole("button", { name: "저장" }));
    expect(onSubmit).toHaveBeenLastCalledWith(expect.objectContaining({ category: "SCROLL", techniqueId: null }));
  });

  it("고칠 때는 담긴 공법이 골라진 채로 열리고 그대로 저장하면 같은 techniqueId 를 보낸다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(
      <ItemForm
        initial={itemType({
          name: "장춘공 옥간",
          category: "SCROLL",
          categoryName: "옥간·서책",
          techniqueId: 1,
          techniqueName: "장춘공",
        })}
        techniques={[technique(1, "장춘공"), technique(2, "강체결")]}
        onSubmit={onSubmit}
      />,
    );

    expect(screen.getByLabelText("담긴 공법")).toHaveValue("1");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ category: "SCROLL", techniqueId: 1 }));
  });

  it("담긴 공법을 고른 뒤 다른 종류로 바꾸면 techniqueId 는 null 로 보낸다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(
      <ItemForm
        initial={itemType({ category: "SCROLL", categoryName: "옥간·서책", techniqueId: 1, techniqueName: "장춘공" })}
        techniques={[technique(1, "장춘공")]}
        onSubmit={onSubmit}
      />,
    );

    await user.selectOptions(screen.getByLabelText("종류"), "PILL");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ category: "PILL", techniqueId: null }));
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

  it("zeroCountShowsServerDetail: 기준가 0·음수도 브라우저가 막지 않고 서버 detail 을 보인다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockRejectedValue(new ApiError(400, "기준가는 0 이상이어야 합니다."));
    render(<ItemForm initial={itemType()} onSubmit={onSubmit} />);

    await user.clear(screen.getByLabelText("기준가 (하품)"));
    await user.type(screen.getByLabelText("기준가 (하품)"), "-1");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSubmit).toHaveBeenCalled();
    expect(await screen.findByRole("alert")).toHaveTextContent("기준가는 0 이상이어야 합니다.");
  });

  it("blankBaseValueSendsNull: 기준가 칸을 비우면 baseValue: null 을 보내고 서버 400 detail 을 보인다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockRejectedValue(new ApiError(400, "요청 본문을 읽을 수 없습니다"));
    render(<ItemForm initial={itemType()} onSubmit={onSubmit} />);

    await user.clear(screen.getByLabelText("기준가 (하품)"));
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ baseValue: null }));
    expect(await screen.findByRole("alert")).toHaveTextContent("요청 본문을 읽을 수 없습니다");
  });
});

function technique(id: number, name: string): TechniqueSummaryResponse {
  return {
    id,
    name,
    elements: [],
    elementNames: null,
    lineName: "연체술",
    startRealm: "MORTAL",
    startRealmName: "범인",
    stageCount: 9,
    abilityCount: 1,
    learnerCount: 0,
  };
}
