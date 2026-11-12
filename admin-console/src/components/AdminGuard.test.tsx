import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as auth from "../lib/auth";
import AdminGuard from "./AdminGuard";

const replace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, push: vi.fn() }),
}));

vi.mock("../lib/auth", () => ({
  fetchMe: vi.fn(),
  logout: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
});

describe("AdminGuard", () => {
  it("관리자가 아니면 안내와 로그아웃 버튼을 보이고 내용을 숨긴다", async () => {
    vi.mocked(auth.fetchMe).mockResolvedValue({ id: 1, email: "p@example.com", admin: false, characterName: null });
    vi.mocked(auth.logout).mockResolvedValue(undefined);
    const user = userEvent.setup();

    render(
      <AdminGuard>
        <p>관리 화면</p>
      </AdminGuard>,
    );

    expect(await screen.findByText("관리자만 쓸 수 있습니다.")).toBeInTheDocument();
    expect(screen.queryByText("관리 화면")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "로그아웃" }));
    expect(auth.logout).toHaveBeenCalled();
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/"));
  });

  it("로그인하지 않았으면 로그인 화면으로 보낸다", async () => {
    vi.mocked(auth.fetchMe).mockResolvedValue(null);

    render(
      <AdminGuard>
        <p>관리 화면</p>
      </AdminGuard>,
    );

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/"));
    expect(screen.queryByText("관리 화면")).not.toBeInTheDocument();
  });

  it("관리자면 내용을 보인다", async () => {
    vi.mocked(auth.fetchMe).mockResolvedValue({ id: 1, email: "a@example.com", admin: true, characterName: null });

    render(
      <AdminGuard>
        <p>관리 화면</p>
      </AdminGuard>,
    );

    expect(await screen.findByText("관리 화면")).toBeInTheDocument();
    expect(replace).not.toHaveBeenCalled();
  });
});
