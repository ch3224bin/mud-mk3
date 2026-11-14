import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "../lib/api";
import FactionForm from "./FactionForm";

describe("FactionForm", () => {
  it("종류를 종문으로 고르고 저장하면 kind 는 SECT 로 넘긴다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<FactionForm submitLabel="저장" onSubmit={onSubmit} />);

    expect(screen.getByLabelText("종류")).toHaveValue("CASTLE");
    await user.selectOptions(screen.getByLabelText("종류"), screen.getByRole("option", { name: "종문" }));
    await user.type(screen.getByLabelText("이름"), "황풍곡");
    await user.type(screen.getByLabelText("묘사"), "바람이 센 골짜기");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSubmit).toHaveBeenCalledWith({ kind: "SECT", name: "황풍곡", description: "바람이 센 골짜기" });
  });

  it("새로 만드는 폼은 성공하면 비운다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<FactionForm submitLabel="성·종문 만들기" onSubmit={onSubmit} />);

    const button = screen.getByRole("button", { name: "성·종문 만들기" });
    expect(button).toBeDisabled();
    await user.type(screen.getByLabelText("이름"), "청석성");
    await user.click(button);

    expect(onSubmit).toHaveBeenCalledWith({ kind: "CASTLE", name: "청석성", description: "" });
    expect(screen.getByLabelText("이름")).toHaveValue("");
  });

  it("저장이 실패하면 서버의 detail 문구를 보인다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockRejectedValue(new ApiError(409, "이미 있는 성·종문 이름입니다: 청석성"));
    render(
      <FactionForm
        submitLabel="저장"
        initial={{ kind: "SECT", name: "청석성", description: "" }}
        onSubmit={onSubmit}
      />,
    );

    expect(screen.getByLabelText("종류")).toHaveValue("SECT");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("이미 있는 성·종문 이름입니다: 청석성");
  });
});
