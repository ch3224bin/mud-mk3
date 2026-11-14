import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ApiError, type FactionResponse } from "../lib/api";
import ZoneForm from "./ZoneForm";

describe("ZoneForm", () => {
  it("이름이 비어 있으면 제출 버튼이 막힌다", async () => {
    const user = userEvent.setup();
    render(<ZoneForm submitLabel="지역 만들기" onSubmit={vi.fn()} />);

    const button = screen.getByRole("button", { name: "지역 만들기" });
    expect(button).toBeDisabled();

    await user.type(screen.getByLabelText("이름"), "   ");
    expect(button).toBeDisabled();

    await user.type(screen.getByLabelText("이름"), "청우촌");
    expect(button).toBeEnabled();
  });

  it("이름은 30자, 묘사는 500자를 넘게 입력할 수 없다", async () => {
    const user = userEvent.setup();
    render(<ZoneForm submitLabel="지역 만들기" onSubmit={vi.fn()} />);

    const name = screen.getByLabelText("이름");
    expect(name).toHaveAttribute("maxLength", "30");
    expect(screen.getByLabelText("분위기 묘사")).toHaveAttribute("maxLength", "500");

    await user.type(name, "가".repeat(35));
    expect(name).toHaveValue("가".repeat(30));
  });

  it("제출하면 이름과 묘사를 넘기고, 새로 만드는 폼은 비운다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<ZoneForm submitLabel="지역 만들기" onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText("이름"), "청우촌");
    await user.type(screen.getByLabelText("분위기 묘사"), "조용한 마을");
    await user.click(screen.getByRole("button", { name: "지역 만들기" }));

    expect(onSubmit).toHaveBeenCalledWith({ name: "청우촌", description: "조용한 마을", ownerId: null });
    expect(screen.getByLabelText("이름")).toHaveValue("");
  });

  it("저장이 실패하면 오류 문구를 보인다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockRejectedValue(new ApiError(409, "이미 있는 지역 이름입니다."));
    render(<ZoneForm submitLabel="저장" initial={{ name: "청우촌", description: "", ownerId: null }} onSubmit={onSubmit} />);

    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("이미 있는 지역 이름입니다.");
    expect(screen.getByLabelText("이름")).toHaveValue("청우촌");
  });

  it("소유를 고르면 그 성·종문의 id 를 넘긴다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const factions: FactionResponse[] = [
      { id: 7, kind: "CASTLE", kindName: "성", name: "청석성", description: "", zoneCount: 0 },
      { id: 8, kind: "SECT", kindName: "종문", name: "황풍곡", description: "", zoneCount: 0 },
    ];
    render(<ZoneForm submitLabel="지역 만들기" factions={factions} onSubmit={onSubmit} />);

    const owner = screen.getByLabelText("소유");
    expect(owner).toHaveValue("");
    expect(screen.getByRole("option", { name: "없음" })).toBeInTheDocument();
    await user.type(screen.getByLabelText("이름"), "청석숲");
    await user.selectOptions(owner, screen.getByRole("option", { name: "청석성 (성)" }));
    await user.click(screen.getByRole("button", { name: "지역 만들기" }));

    expect(onSubmit).toHaveBeenCalledWith({ name: "청석숲", description: "", ownerId: 7 });
  });

  it("고치는 폼은 지금 소유가 골라진 채로 열린다", () => {
    const factions: FactionResponse[] = [
      { id: 8, kind: "SECT", kindName: "종문", name: "황풍곡", description: "", zoneCount: 1 },
    ];
    render(
      <ZoneForm
        submitLabel="저장"
        factions={factions}
        initial={{ name: "청석숲", description: "", ownerId: 8 }}
        onSubmit={vi.fn()}
      />,
    );

    expect(screen.getByLabelText("소유")).toHaveValue("8");
  });
});
