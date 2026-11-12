import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { GameSocketHandlers } from "../lib/gameSocket";
import type { ServerMessage } from "../lib/protocol";
import Terminal from "./Terminal";

/** 테스트용 가짜 연결. Terminal 이 넘긴 handlers 로 서버 메시지·닫기를 일으킨다. */
class FakeConnection {
  sendCommand = vi.fn<(text: string) => void>();
  close = vi.fn<() => void>();

  constructor(readonly handlers: GameSocketHandlers) {}

  receive(message: ServerMessage) {
    act(() => this.handlers.onMessage(message));
  }

  serverClose(code: number) {
    act(() => this.handlers.onClose(code));
  }
}

let connections: FakeConnection[];
const onUnauthorized = vi.fn();

function renderTerminal() {
  const { unmount } = render(
    <Terminal
      onUnauthorized={onUnauthorized}
      connect={(handlers) => {
        const connection = new FakeConnection(handlers);
        connections.push(connection);
        return connection;
      }}
    />,
  );
  return { connection: connections[0], input: screen.getByRole("textbox", { name: "명령 입력" }), unmount };
}

const room: ServerMessage = {
  type: "room",
  zone: { id: 1, name: "청석숲" },
  zoneEntered: true,
  room: { id: 3, name: "청석숲 오솔길", description: "늙은 소나무 사이로 좁은 길이 굽어 있다." },
  exits: ["북"],
  occupants: [{ name: "한립", kind: "player" }],
};

beforeEach(() => {
  connections = [];
  vi.clearAllMocks();
});

describe("Terminal", () => {
  it("Enter 를 누르면 입력을 메아리로 보이고 서버로 보낸 뒤 입력창을 비운다", async () => {
    const user = userEvent.setup();
    const { connection, input } = renderTerminal();

    await user.type(input, "북{Enter}");

    expect(screen.getByText("> 북")).toHaveClass("line-echo");
    expect(connection.sendCommand).toHaveBeenCalledWith("북");
    expect(input).toHaveValue("");
    expect(input).toHaveFocus();
  });

  it("빈 줄은 보내지 않는다", async () => {
    const user = userEvent.setup();
    const { connection, input } = renderTerminal();

    await user.type(input, "{Enter}");

    expect(connection.sendCommand).not.toHaveBeenCalled();
  });

  it("↑ 로 이전 명령, ↓ 로 되돌아온다", async () => {
    const user = userEvent.setup();
    const { input } = renderTerminal();
    await user.type(input, "북{Enter}");
    await user.type(input, "남{Enter}");

    await user.keyboard("{ArrowUp}");
    expect(input).toHaveValue("남");
    await user.keyboard("{ArrowUp}");
    expect(input).toHaveValue("북");
    await user.keyboard("{ArrowDown}{ArrowDown}");
    expect(input).toHaveValue("");
  });

  it("Tab 으로 명령어를 채우고 포커스를 지킨다", async () => {
    const user = userEvent.setup();
    const { input } = renderTerminal();

    await user.type(input, "누");
    await user.keyboard("{Tab}");

    expect(input).toHaveValue("누구");
    expect(input).toHaveFocus();
  });

  it("Shift+Tab 은 자동완성하지 않고 기본 동작(포커스 이동)으로 둔다", async () => {
    const user = userEvent.setup();
    const { input } = renderTerminal();

    await user.type(input, "누");
    await user.keyboard("{Shift>}{Tab}{/Shift}");

    expect(input).toHaveValue("누");
    expect(input).not.toHaveFocus();
  });

  it("Tab 이름 후보는 room 의 사람에서 notice 로 더하고 뺀다", async () => {
    const user = userEvent.setup();
    const { connection, input } = renderTerminal();
    connection.receive(room);
    connection.receive({ type: "notice", text: "한백이 나타난다.", tone: "movement", actor: "한백", event: "appear" });
    connection.receive({ type: "notice", text: "한립이 북쪽으로 떠난다.", tone: "movement", actor: "한립", event: "leave" });

    await user.type(input, "말하기 한");
    await user.keyboard("{Tab}");

    expect(input).toHaveValue("말하기 한백");
    await user.keyboard("{Tab}");
    expect(input).toHaveValue("말하기 한백");
  });

  it("room 메시지가 오면 방을 그린다", () => {
    const { connection } = renderTerminal();

    connection.receive(room);

    expect(screen.getByText("◇ 청석숲")).toHaveClass("line-zone");
    expect(screen.getByText("청석숲 오솔길")).toHaveClass("line-room-name");
    expect(screen.getByText("출구: 북")).toHaveClass("line-exits");
    expect(screen.getByText("한립")).toHaveClass("line-occupant");
  });

  it("4401 로 닫히면 로그인 화면으로 돌아간다", () => {
    const { connection } = renderTerminal();

    connection.serverClose(4401);

    expect(onUnauthorized).toHaveBeenCalled();
  });

  it("4409 로 닫히면 까닭과 다시 연결 버튼을 보이고, 누르면 새로 연결한다", async () => {
    const user = userEvent.setup();
    const { connection } = renderTerminal();

    connection.receive({ type: "system", text: "다른 곳에서 접속해 이 연결을 끊습니다.", tone: "info" });
    connection.serverClose(4409);

    // 까닭은 서버가 닫기 전에 보낸 문구 한 번만 보인다
    expect(screen.getAllByText(/다른 곳에서 접속해/)).toHaveLength(1);
    expect(screen.queryByText("연결이 끊겼습니다.")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "다시 연결" }));
    expect(connections).toHaveLength(2);
    expect(screen.queryByRole("button", { name: "다시 연결" })).not.toBeInTheDocument();
  });

  it("1000 으로 닫히면 아무 말 없이 다시 연결 버튼만 보인다", () => {
    const { connection } = renderTerminal();

    connection.serverClose(1000);

    expect(screen.getByRole("button", { name: "다시 연결" })).toBeInTheDocument();
    expect(screen.queryByText("연결이 끊겼습니다.")).not.toBeInTheDocument();
  });

  it("그 밖의 코드로 닫히면 '연결이 끊겼습니다.' 와 다시 연결 버튼", () => {
    const { connection } = renderTerminal();

    connection.serverClose(4500);

    expect(screen.getByText("연결이 끊겼습니다.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "다시 연결" })).toBeInTheDocument();
  });

  it("끊긴 뒤에는 입력을 보내지 않는다", async () => {
    const user = userEvent.setup();
    const { connection, input } = renderTerminal();
    connection.serverClose(1000);

    await user.type(input, "북{Enter}");

    expect(connection.sendCommand).not.toHaveBeenCalled();
  });

  it("출력은 최대 2000줄만 남긴다", () => {
    const { connection } = renderTerminal();

    connection.receive({
      type: "system",
      text: Array.from({ length: 2001 }, (_, i) => `줄${i}`).join("\n"),
      tone: "info",
    });

    expect(screen.queryByText("줄0")).not.toBeInTheDocument();
    expect(screen.getByText("줄1")).toBeInTheDocument();
    expect(screen.getByText("줄2000")).toBeInTheDocument();
  });

  it("사라질 때 연결을 닫는다", () => {
    const { connection, unmount } = renderTerminal();

    unmount();

    expect(connection.close).toHaveBeenCalled();
  });
});
