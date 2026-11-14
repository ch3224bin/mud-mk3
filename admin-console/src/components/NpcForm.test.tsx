import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../lib/api";
import type { NpcDetailResponse } from "../lib/api";
import NpcForm from "./NpcForm";

vi.mock("../lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../lib/api")>()),
  listZones: vi.fn(),
  listRooms: vi.fn(),
}));

const npc: NpcDetailResponse = {
  id: 4,
  name: "왕씨",
  greeting: "어서 오시오.",
  defaultAnswer: "모르겠소.",
  topics: [{ word: "단약", answer: "단약은 귀하오." }],
  llmEnabled: false,
  roomId: 4,
  roomName: "청석숲 오솔길",
  zoneId: 2,
  zoneName: "청석숲",
};

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(api.listZones).mockResolvedValue([
    { id: 1, name: "청우촌", description: "", roomCount: 1, ownerId: null, ownerName: null, ownerKind: null },
    { id: 2, name: "청석숲", description: "", roomCount: 1, ownerId: null, ownerName: null, ownerKind: null },
  ]);
  vi.mocked(api.listRooms).mockImplementation(async (zoneId) =>
    zoneId === 2 ? [{ id: 4, name: "청석숲 오솔길", exitCount: 0 }] : [{ id: 3, name: "마을 어귀", exitCount: 1 }],
  );
});

describe("NpcForm", () => {
  it("새 NPC 의 기본 대답은 '글쎄, 잘 모르겠구려.' 이고 배치·LLM 은 꺼진 채로 보낸다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<NpcForm onSubmit={onSubmit} />);

    expect(screen.getByLabelText("기본 대답")).toHaveValue("글쎄, 잘 모르겠구려.");
    expect(screen.getByLabelText("LLM 대화")).not.toBeChecked();
    expect(screen.getByText("켜 두어도 지금은 정해진 대사로 답합니다.")).toBeInTheDocument();
    await user.type(screen.getByLabelText("이름"), "왕씨");
    await user.type(screen.getByLabelText("인사말"), "어서 오시오.");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSubmit).toHaveBeenCalledWith({
      name: "왕씨",
      greeting: "어서 오시오.",
      defaultAnswer: "글쎄, 잘 모르겠구려.",
      topics: [],
      llmEnabled: false,
      roomId: null,
    });
  });

  it("LLM 대화 를 켜고 지역·방을 고르면 llmEnabled 와 roomId 를 보내고, 빈 낱말 행은 뺀다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<NpcForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText("이름"), "왕씨");
    await user.type(screen.getByLabelText("인사말"), "어서 오시오.");
    await user.click(screen.getByRole("button", { name: "낱말 추가" }));
    await user.click(screen.getByRole("button", { name: "낱말 추가" }));
    await user.type(screen.getAllByLabelText("낱말")[0], "단약");
    await user.type(screen.getAllByLabelText("대답")[0], "단약은 귀하오.");
    await user.click(screen.getByLabelText("LLM 대화"));
    await screen.findByRole("option", { name: "청우촌" });
    await user.selectOptions(screen.getByLabelText("지역"), "1");
    await screen.findByRole("option", { name: "마을 어귀" });
    await user.selectOptions(screen.getByLabelText("방"), "3");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        topics: [{ word: "단약", answer: "단약은 귀하오." }],
        llmEnabled: true,
        roomId: 3,
      }),
    );
  });

  it("고칠 때는 지금 배치된 지역·방이 골라진 채로 열리고, 배치 안 함 을 고르면 roomId 는 null 이다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<NpcForm initial={npc} onSubmit={onSubmit} />);

    expect(screen.getByLabelText("이름")).toHaveValue("왕씨");
    expect(screen.getByLabelText("기본 대답")).toHaveValue("모르겠소.");
    expect(screen.getByLabelText("낱말")).toHaveValue("단약");
    await screen.findByRole("option", { name: "청석숲 오솔길" });
    expect(screen.getByLabelText("지역")).toHaveValue("2");
    expect(screen.getByLabelText("방")).toHaveValue("4");

    await user.selectOptions(screen.getByLabelText("지역"), screen.getByRole("option", { name: "배치 안 함" }));
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ name: "왕씨", llmEnabled: false, roomId: null }));
  });

  it("서버가 409 를 주면 detail 문구를 보인다", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockRejectedValue(new api.ApiError(409, "이미 있는 NPC 이름입니다: 왕씨"));
    render(<NpcForm initial={npc} onSubmit={onSubmit} />);

    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("이미 있는 NPC 이름입니다: 왕씨");
  });
});
