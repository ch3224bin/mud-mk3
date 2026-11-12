import { describe, expect, it, vi } from "vitest";
import { GameSocket, gameSocketUrl } from "./gameSocket";
import type { ServerMessage } from "./protocol";

/** 테스트용 가짜 WebSocket. 열기·받기·닫기를 손으로 일으킨다. */
class FakeWebSocket {
  static readonly CONNECTING = 0;
  static readonly OPEN = 1;
  static readonly CLOSING = 2;
  static readonly CLOSED = 3;

  readyState = FakeWebSocket.CONNECTING;
  sent: string[] = [];
  closedWith: number | undefined;
  onopen: (() => void) | null = null;
  onmessage: ((event: { data: unknown }) => void) | null = null;
  onclose: ((event: { code: number }) => void) | null = null;

  constructor(readonly url: string) {}

  send(data: string) {
    if (this.readyState !== FakeWebSocket.OPEN) throw new Error("InvalidStateError");
    this.sent.push(data);
  }

  close(code?: number) {
    this.closedWith = code;
    this.readyState = FakeWebSocket.CLOSED;
  }

  open() {
    this.readyState = FakeWebSocket.OPEN;
    this.onopen?.();
  }

  receive(data: unknown) {
    this.onmessage?.({ data });
  }

  serverClose(code: number) {
    this.readyState = FakeWebSocket.CLOSED;
    this.onclose?.({ code });
  }
}

function connect() {
  const sockets: FakeWebSocket[] = [];
  const handlers = { onMessage: vi.fn<(m: ServerMessage) => void>(), onClose: vi.fn<(code: number) => void>() };
  const gameSocket = new GameSocket(gameSocketUrl(), handlers, (url) => {
    const socket = new FakeWebSocket(url);
    sockets.push(socket);
    return socket as unknown as WebSocket;
  });
  return { gameSocket, handlers, socket: sockets[0] };
}

describe("gameSocketUrl", () => {
  it("world-server 주소의 http 를 ws 로 바꾸고 /ws/game 을 붙인다", () => {
    expect(gameSocketUrl()).toBe("ws://localhost:8080/ws/game");
  });

  it("https 는 wss 로, 끝의 / 는 하나로", () => {
    expect(gameSocketUrl("https://mud.example.com/")).toBe("wss://mud.example.com/ws/game");
  });
});

describe("GameSocket", () => {
  it("만들 때 게임 주소로 연결한다", () => {
    const { socket } = connect();

    expect(socket.url).toBe("ws://localhost:8080/ws/game");
  });

  it("sendCommand 는 command 메시지를 보낸다", () => {
    const { gameSocket, socket } = connect();
    socket.open();

    gameSocket.sendCommand("북");

    expect(socket.sent).toEqual(['{"type":"command","text":"북"}']);
  });

  it("열리기 전에 보낸 명령은 열리면 차례로 보낸다", () => {
    const { gameSocket, socket } = connect();

    gameSocket.sendCommand("보기");
    gameSocket.sendCommand("북");
    socket.open();

    expect(socket.sent).toEqual(['{"type":"command","text":"보기"}', '{"type":"command","text":"북"}']);
  });

  it("받은 JSON 을 onMessage 로 넘긴다", () => {
    const { handlers, socket } = connect();
    socket.open();

    socket.receive('{"type":"system","text":"다음에 또 뵙겠습니다.","tone":"info"}');

    expect(handlers.onMessage).toHaveBeenCalledWith({ type: "system", text: "다음에 또 뵙겠습니다.", tone: "info" });
  });

  it("JSON 이 아니거나 type 이 없는 메시지는 버린다", () => {
    const { handlers, socket } = connect();
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    socket.open();

    socket.receive("not json");
    socket.receive('{"text":"no type"}');

    expect(handlers.onMessage).not.toHaveBeenCalled();
    consoleError.mockRestore();
  });

  it("닫기 코드를 onClose 로 넘긴다", () => {
    const { handlers, socket } = connect();
    socket.open();

    socket.serverClose(4409);

    expect(handlers.onClose).toHaveBeenCalledWith(4409);
  });

  it("close 는 1000 으로 닫고 그 뒤 일은 알리지 않는다", () => {
    const { gameSocket, handlers, socket } = connect();
    socket.open();

    gameSocket.close();
    socket.receive('{"type":"system","text":"늦게 온 메시지","tone":"info"}');
    socket.serverClose(1000);

    expect(socket.closedWith).toBe(1000);
    expect(handlers.onMessage).not.toHaveBeenCalled();
    expect(handlers.onClose).not.toHaveBeenCalled();
  });

  it("닫힌 뒤 sendCommand 는 아무것도 하지 않는다", () => {
    const { gameSocket, socket } = connect();
    socket.open();
    socket.serverClose(1000);

    expect(() => gameSocket.sendCommand("북")).not.toThrow();
    expect(socket.sent).toEqual([]);
  });
});
