import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../lib/api";
import type { RoomDetailResponse } from "../lib/api";
import ExitEditor from "./ExitEditor";

vi.mock("../lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../lib/api")>()),
  listZones: vi.fn(),
  listRooms: vi.fn(),
  putExit: vi.fn(),
  deleteExit: vi.fn(),
  getRoom: vi.fn(),
}));

const room: RoomDetailResponse = {
  id: 3,
  zoneId: 1,
  zoneName: "청우촌",
  name: "마을 어귀",
  description: "마을로 들어가는 길목이다.",
  start: false,
  exits: [
    {
      direction: "EAST",
      directionName: "동",
      targetRoomId: 5,
      targetRoomName: "우물가",
      targetZoneName: "청우촌",
    },
  ],
  npcs: [],
  floorItems: [],
};

const updatedRoom: RoomDetailResponse = { ...room, exits: [] };

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(api.listZones).mockResolvedValue([
    { id: 1, name: "청우촌", description: "", roomCount: 2, ownerId: null, ownerName: null, ownerKind: null },
    { id: 2, name: "청석숲", description: "", roomCount: 1, ownerId: null, ownerName: null, ownerKind: null },
  ]);
  vi.mocked(api.listRooms).mockImplementation(async (zoneId) =>
    zoneId === 2 ? [{ id: 4, name: "청석숲 오솔길", exitCount: 0 }] : [{ id: 3, name: "마을 어귀", exitCount: 1 }],
  );
  vi.mocked(api.putExit).mockResolvedValue(updatedRoom);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

async function fillExitForm(user: ReturnType<typeof userEvent.setup>) {
  await user.selectOptions(screen.getByLabelText("방위"), "NORTH");
  await screen.findByRole("option", { name: "청석숲" });
  await user.selectOptions(screen.getByLabelText("대상 지역"), "2");
  await screen.findByRole("option", { name: "청석숲 오솔길" });
  await user.selectOptions(screen.getByLabelText("대상 방"), "4");
}

describe("ExitEditor", () => {
  it("출구 표에 방위와 대상 방(지역)을 보인다", () => {
    render(<ExitEditor room={room} onRoomChange={vi.fn()} />);

    const row = screen.getByRole("row", { name: /동/ });
    expect(within(row).getByText("우물가 (청우촌)")).toBeInTheDocument();
  });

  it("방위·대상 지역·대상 방을 고르고 추가하면 반대 방향도 함께 만든다", async () => {
    const user = userEvent.setup();
    const onRoomChange = vi.fn();
    render(<ExitEditor room={room} onRoomChange={onRoomChange} />);

    expect(screen.getByRole("button", { name: "출구 추가" })).toBeDisabled();
    expect(screen.getByLabelText("반대 방향도")).toBeChecked();

    await fillExitForm(user);
    await user.click(screen.getByRole("button", { name: "출구 추가" }));

    expect(api.putExit).toHaveBeenCalledWith(3, "NORTH", { targetRoomId: 4, twoWay: true });
    expect(onRoomChange).toHaveBeenCalledWith(updatedRoom);
  });

  it("반대 방향도 체크를 끄면 twoWay=false 로 보낸다", async () => {
    const user = userEvent.setup();
    render(<ExitEditor room={room} onRoomChange={vi.fn()} />);

    await fillExitForm(user);
    await user.click(screen.getByLabelText("반대 방향도"));
    await user.click(screen.getByRole("button", { name: "출구 추가" }));

    expect(api.putExit).toHaveBeenCalledWith(3, "NORTH", { targetRoomId: 4, twoWay: false });
  });

  it("409 이면 서버의 detail 문구를 보인다", async () => {
    vi.mocked(api.putExit).mockRejectedValue(
      new api.ApiError(409, "대상 방의 반대 방향 출구가 이미 다른 방으로 이어져 있습니다."),
    );
    const user = userEvent.setup();
    const onRoomChange = vi.fn();
    render(<ExitEditor room={room} onRoomChange={onRoomChange} />);

    await fillExitForm(user);
    await user.click(screen.getByRole("button", { name: "출구 추가" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "대상 방의 반대 방향 출구가 이미 다른 방으로 이어져 있습니다.",
    );
    expect(onRoomChange).not.toHaveBeenCalled();
  });

  it("대상 방 목록에서 지금 방은 뺀다", async () => {
    const user = userEvent.setup();
    render(<ExitEditor room={room} onRoomChange={vi.fn()} />);

    await screen.findByRole("option", { name: "청우촌" });
    await user.selectOptions(screen.getByLabelText("대상 지역"), "1");

    await vi.waitFor(() => expect(api.listRooms).toHaveBeenCalledWith(1));
    const targetRoom = screen.getByLabelText("대상 방");
    expect(within(targetRoom).queryByRole("option", { name: "마을 어귀" })).not.toBeInTheDocument();
  });

  it("삭제는 확인을 받은 뒤 지우고 방을 다시 읽는다", async () => {
    vi.stubGlobal("confirm", vi.fn().mockReturnValue(true));
    vi.mocked(api.deleteExit).mockResolvedValue(undefined);
    vi.mocked(api.getRoom).mockResolvedValue(updatedRoom);
    const user = userEvent.setup();
    const onRoomChange = vi.fn();
    render(<ExitEditor room={room} onRoomChange={onRoomChange} />);

    await user.click(screen.getByRole("button", { name: "삭제" }));

    expect(window.confirm).toHaveBeenCalled();
    expect(api.deleteExit).toHaveBeenCalledWith(3, "EAST", true);
    await vi.waitFor(() => expect(onRoomChange).toHaveBeenCalledWith(updatedRoom));
  });

  it("삭제 확인을 취소하면 지우지 않는다", async () => {
    vi.stubGlobal("confirm", vi.fn().mockReturnValue(false));
    const user = userEvent.setup();
    render(<ExitEditor room={room} onRoomChange={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "삭제" }));

    expect(api.deleteExit).not.toHaveBeenCalled();
  });
});
