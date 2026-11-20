import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../lib/api";
import CharacterDeleteForm from "./CharacterDeleteForm";

vi.mock("../lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../lib/api")>()),
  deleteCharacter: vi.fn(),
}));

const character = { id: 3, name: "한백" };

beforeEach(() => {
  vi.clearAllMocks();
});

describe("CharacterDeleteForm", () => {
  it("캐릭터 삭제 를 누르면 확인 칸이 열리고, 이름이 글자 그대로 같을 때만 삭제 가 켜진다", async () => {
    const user = userEvent.setup();
    render(<CharacterDeleteForm character={character} onDeleted={vi.fn()} />);

    expect(screen.getByRole("heading", { name: "캐릭터 삭제" })).toBeInTheDocument();
    expect(
      screen.getByText(
        "캐릭터를 지우면 소지품·익힌 공법·깨달음이 모두 사라지고 되돌릴 수 없습니다. 계정은 남아 다음 접속 때 새 캐릭터를 만듭니다.",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByLabelText("캐릭터 이름")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "캐릭터 삭제" }));

    expect(screen.getByText("지우려면 캐릭터 이름을 그대로 입력하십시오: 한백")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "캐릭터 삭제" })).not.toBeInTheDocument();
    const input = screen.getByLabelText("캐릭터 이름");
    const deleteButton = screen.getByRole("button", { name: "삭제" });
    expect(deleteButton).toBeDisabled();

    await user.type(input, "한");
    expect(deleteButton).toBeDisabled();

    await user.type(input, "백");
    expect(deleteButton).toBeEnabled();

    await user.type(input, " ");
    expect(deleteButton).toBeDisabled();
  });

  it("삭제 하면 그 id 로 지우고 onDeleted 를 부른다", async () => {
    const user = userEvent.setup();
    const onDeleted = vi.fn();
    vi.mocked(api.deleteCharacter).mockResolvedValue(undefined);
    render(<CharacterDeleteForm character={character} onDeleted={onDeleted} />);

    await user.click(screen.getByRole("button", { name: "캐릭터 삭제" }));
    await user.type(screen.getByLabelText("캐릭터 이름"), "한백");
    await user.click(screen.getByRole("button", { name: "삭제" }));

    expect(api.deleteCharacter).toHaveBeenCalledWith(3);
    expect(onDeleted).toHaveBeenCalledTimes(1);
  });

  it("지우는 중에는 취소 가 꺼진다", async () => {
    const user = userEvent.setup();
    vi.mocked(api.deleteCharacter).mockReturnValue(new Promise(() => {}));
    render(<CharacterDeleteForm character={character} onDeleted={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "캐릭터 삭제" }));
    await user.type(screen.getByLabelText("캐릭터 이름"), "한백");
    await user.click(screen.getByRole("button", { name: "삭제" }));

    expect(screen.getByRole("button", { name: "취소" })).toBeDisabled();
  });

  it("서버가 409 를 주면 칸을 연 채 detail 을 보인다", async () => {
    const user = userEvent.setup();
    const onDeleted = vi.fn();
    const detail = "접속 중인 캐릭터는 지울 수 없습니다. 접속을 끊은 뒤 다시 시도하십시오: 한백";
    vi.mocked(api.deleteCharacter).mockRejectedValue(new api.ApiError(409, detail));
    render(<CharacterDeleteForm character={character} onDeleted={onDeleted} />);

    await user.click(screen.getByRole("button", { name: "캐릭터 삭제" }));
    await user.type(screen.getByLabelText("캐릭터 이름"), "한백");
    await user.click(screen.getByRole("button", { name: "삭제" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(detail);
    expect(screen.getByLabelText("캐릭터 이름")).toHaveValue("한백");
    expect(onDeleted).not.toHaveBeenCalled();
  });

  it("취소 하면 칸을 닫고, 다시 열면 입력이 비어 있다", async () => {
    const user = userEvent.setup();
    render(<CharacterDeleteForm character={character} onDeleted={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "캐릭터 삭제" }));
    await user.type(screen.getByLabelText("캐릭터 이름"), "한백");
    await user.click(screen.getByRole("button", { name: "취소" }));

    expect(screen.queryByLabelText("캐릭터 이름")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "캐릭터 삭제" }));
    expect(screen.getByLabelText("캐릭터 이름")).toHaveValue("");
  });
});
