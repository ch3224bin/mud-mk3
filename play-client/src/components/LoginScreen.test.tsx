import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import LoginScreen from "./LoginScreen";

vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams(),
}));

describe("LoginScreen", () => {
  it("notice 가 있으면 role=status 로 보인다", () => {
    render(<LoginScreen onLoggedIn={() => {}} notice="로그인이 끝났습니다. 다시 로그인하십시오." />);

    const status = screen.getByRole("status");
    expect(status).toHaveTextContent("로그인이 끝났습니다. 다시 로그인하십시오.");
    expect(status).toHaveClass("muted");
    // 버튼 위
    expect(
      status.compareDocumentPosition(screen.getByRole("link", { name: "Google로 로그인" })) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
  });

  it("없으면 보이지 않는다", () => {
    render(<LoginScreen onLoggedIn={() => {}} />);

    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });
});
