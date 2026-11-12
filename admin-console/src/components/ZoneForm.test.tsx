import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "../lib/api";
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

    expect(onSubmit).toHaveBeenCalledWith({ name: "청우촌", description: "조용한 마을" });
    expect(screen.getByLabelText("이름")).toHaveValue("");
  });

  it("저장이 실패하면 오류 문구를 보인다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockRejectedValue(new ApiError(409, "이미 있는 지역 이름입니다."));
    render(<ZoneForm submitLabel="저장" initial={{ name: "청우촌", description: "" }} onSubmit={onSubmit} />);

    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("이미 있는 지역 이름입니다.");
    expect(screen.getByLabelText("이름")).toHaveValue("청우촌");
  });
});
