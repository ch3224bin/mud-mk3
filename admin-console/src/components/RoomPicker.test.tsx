import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../lib/api";
import type { ZoneResponse } from "../lib/api";
import RoomPicker from "./RoomPicker";

vi.mock("../lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../lib/api")>();
  return { ...actual, listZones: vi.fn(), listRooms: vi.fn() };
});

function zone(id: number, name: string): ZoneResponse {
  return { id, name, description: "", roomCount: 0, ownerId: null, ownerName: null, ownerKind: null };
}

const onChange = vi.fn();
const onError = vi.fn();

/** 고른 방을 들고 있는 부모. 고른 값을 RoomPicker 에 다시 넘긴다 */
function Picker({ excludeRoomId, initialZoneId }: { excludeRoomId?: number; initialZoneId?: number }) {
  const [value, setValue] = useState<number | null>(null);
  return (
    <RoomPicker
      value={value}
      onChange={(roomId) => {
        setValue(roomId);
        onChange(roomId);
      }}
      initialZoneId={initialZoneId}
      zoneLabel="대상 지역"
      roomLabel="대상 방"
      zonePlaceholder="지역 선택"
      roomPlaceholder="방 선택"
      excludeRoomId={excludeRoomId}
      onError={onError}
    />
  );
}

function optionNames(label: string): string[] {
  return within(screen.getByLabelText(label))
    .getAllByRole("option")
    .map((option) => option.textContent ?? "");
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe("RoomPicker", () => {
  it("지역 목록을 보이고, 지역을 고르면 그 지역의 방(빼는 방 제외)을 보인다", async () => {
    vi.mocked(api.listZones).mockResolvedValue([zone(1, "청석숲"), zone(2, "황풍곡")]);
    vi.mocked(api.listRooms).mockResolvedValue([
      { id: 10, name: "숲 입구", exitCount: 1 },
      { id: 11, name: "숲 안쪽", exitCount: 2 },
    ]);
    const user = userEvent.setup();
    render(<Picker excludeRoomId={10} />);

    await screen.findByRole("option", { name: "청석숲" });
    expect(optionNames("대상 지역")).toEqual(["지역 선택", "청석숲", "황풍곡"]);
    expect(optionNames("대상 방")).toEqual(["방 선택"]);

    await user.selectOptions(screen.getByLabelText("대상 지역"), "청석숲");

    await screen.findByRole("option", { name: "숲 안쪽" });
    expect(api.listRooms).toHaveBeenCalledWith(1);
    expect(optionNames("대상 방")).toEqual(["방 선택", "숲 안쪽"]);
  });

  it("방을 고르면 onChange 에 방 id 를, 지역을 바꾸면 null 을 넘긴다", async () => {
    vi.mocked(api.listZones).mockResolvedValue([zone(1, "청석숲"), zone(2, "황풍곡")]);
    vi.mocked(api.listRooms).mockImplementation(async (zoneId) =>
      zoneId === 1 ? [{ id: 11, name: "숲 안쪽", exitCount: 2 }] : [{ id: 20, name: "골짜기", exitCount: 0 }],
    );
    const user = userEvent.setup();
    render(<Picker initialZoneId={1} />);

    await screen.findByRole("option", { name: "숲 안쪽" });
    await user.selectOptions(screen.getByLabelText("대상 방"), "숲 안쪽");
    expect(onChange).toHaveBeenLastCalledWith(11);
    expect(screen.getByLabelText("대상 방")).toHaveValue("11");

    await user.selectOptions(screen.getByLabelText("대상 지역"), "황풍곡");
    expect(onChange).toHaveBeenLastCalledWith(null);
    await screen.findByRole("option", { name: "골짜기" });
    expect(screen.getByLabelText("대상 방")).toHaveValue("");
  });

  it("지역·방이 없으면 첫 항목만 보인다", async () => {
    vi.mocked(api.listZones).mockResolvedValue([zone(1, "청석숲")]);
    vi.mocked(api.listRooms).mockResolvedValue([]);
    render(<Picker initialZoneId={1} />);

    await screen.findByRole("option", { name: "청석숲" });
    await vi.waitFor(() => expect(api.listRooms).toHaveBeenCalledWith(1));
    expect(optionNames("대상 방")).toEqual(["방 선택"]);
  });

  it("지역 목록이 없으면 지역 선택도 첫 항목만 보인다", async () => {
    vi.mocked(api.listZones).mockResolvedValue([]);
    render(<Picker />);

    await vi.waitFor(() => expect(api.listZones).toHaveBeenCalled());
    expect(optionNames("대상 지역")).toEqual(["지역 선택"]);
    expect(optionNames("대상 방")).toEqual(["방 선택"]);
    expect(api.listRooms).not.toHaveBeenCalled();
  });

  it("목록을 읽지 못하면 onError 에 detail 을 넘긴다", async () => {
    vi.mocked(api.listZones).mockRejectedValue(new api.ApiError(500, "요청을 처리하지 못했습니다. (500)"));
    render(<Picker />);

    await vi.waitFor(() => expect(onError).toHaveBeenCalledWith("요청을 처리하지 못했습니다. (500)"));
  });
});
