import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { errorMessage, updateZone } from "../lib/api";
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

afterEach(() => {
  vi.unstubAllGlobals();
});

/** 저장 버튼 하나뿐인 관리 화면. 실패하면 detail 을 보인다 */
function SaveButton() {
  const [error, setError] = useState<string | null>(null);
  async function save() {
    try {
      await updateZone(2, { name: "청석숲", description: "", ownerId: null });
    } catch (e) {
      setError(errorMessage(e));
    }
  }
  return (
    <>
      <button type="button" onClick={save}>
        저장
      </button>
      {error && <p role="alert">{error}</p>}
    </>
  );
}

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

  it("midSession401ShowsLogin: 로그인된 화면에서 저장 요청이 401 이면 로그인 화면으로 보낸다", async () => {
    vi.mocked(auth.fetchMe).mockResolvedValue({ id: 1, email: "a@example.com", admin: true, characterName: null });
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response(JSON.stringify({ status: 401, detail: "로그인이 필요합니다." }), { status: 401 })),
    );
    const user = userEvent.setup();

    render(
      <AdminGuard>
        <SaveButton />
      </AdminGuard>,
    );
    await user.click(await screen.findByRole("button", { name: "저장" }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/"));
  });

  it("화면을 떠나면 401 처리기를 풀어 로그인 화면으로 보내지 않는다", async () => {
    vi.mocked(auth.fetchMe).mockResolvedValue({ id: 1, email: "a@example.com", admin: true, characterName: null });
    vi.stubGlobal("fetch", vi.fn(async () => new Response(null, { status: 401 })));

    const { unmount } = render(
      <AdminGuard>
        <p>관리 화면</p>
      </AdminGuard>,
    );
    await screen.findByText("관리 화면");
    unmount();
    await updateZone(2, { name: "청석숲", description: "", ownerId: null }).catch(() => undefined);

    expect(replace).not.toHaveBeenCalled();
  });
});
