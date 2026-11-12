import { worldServerUrl } from "./config";
import type { CommandMessage, ServerMessage } from "./protocol";

/** WebSocket readyState 값 */
const CONNECTING = 0;
const OPEN = 1;

export type GameSocketHandlers = {
  onMessage(m: ServerMessage): void;
  onClose(code: number): void;
};

/** world-server 주소에서 게임 WebSocket 주소를 만든다: http→ws, https→wss, + /ws/game */
export function gameSocketUrl(base: string = worldServerUrl): string {
  return `${base.replace(/^http/, "ws").replace(/\/+$/, "")}/ws/game`;
}

/**
 * 게임 WebSocket 연결 하나. 인증은 브라우저가 핸드셰이크에 세션 쿠키를 싣는 것으로 끝난다(D4).
 * 다시 연결하지 않는다 — 끊기면 새로 만든다(D18).
 */
export class GameSocket {
  private readonly socket: WebSocket;
  private readonly handlers: GameSocketHandlers;
  /** 열리기 전에 보낸 명령. 열리면 차례로 보낸다. */
  private readonly waiting: string[] = [];
  private closedByClient = false;

  constructor(
    url: string,
    handlers: GameSocketHandlers,
    socketFactory: (url: string) => WebSocket = (u) => new WebSocket(u),
  ) {
    this.handlers = handlers;
    this.socket = socketFactory(url);
    this.socket.onopen = () => {
      for (const data of this.waiting.splice(0)) {
        this.socket.send(data);
      }
    };
    this.socket.onmessage = (event: MessageEvent) => {
      if (this.closedByClient) return;
      const message = parse(event.data);
      if (message !== null) {
        this.handlers.onMessage(message);
      }
    };
    this.socket.onclose = (event: CloseEvent) => {
      if (this.closedByClient) return;
      this.handlers.onClose(event.code);
    };
  }

  /** 입력 한 줄을 보낸다. 열리는 중이면 열린 뒤 보내고, 닫혔으면 버린다. */
  sendCommand(text: string): void {
    const message: CommandMessage = { type: "command", text };
    const data = JSON.stringify(message);
    if (this.socket.readyState === OPEN) {
      this.socket.send(data);
    } else if (this.socket.readyState === CONNECTING) {
      this.waiting.push(data);
    }
  }

  /** 이쪽에서 닫는다(화면을 떠날 때). 그 뒤 오는 메시지·닫힘은 알리지 않는다. */
  close(): void {
    this.closedByClient = true;
    this.waiting.length = 0;
    this.socket.close(1000);
  }
}

function parse(data: unknown): ServerMessage | null {
  if (typeof data !== "string") {
    console.error("게임 서버가 텍스트가 아닌 메시지를 보냈습니다.", data);
    return null;
  }
  try {
    const value: unknown = JSON.parse(data);
    if (value && typeof value === "object" && typeof (value as { type?: unknown }).type === "string") {
      return value as ServerMessage;
    }
  } catch (error) {
    console.error("게임 서버 메시지를 읽지 못했습니다.", error);
    return null;
  }
  console.error("게임 서버 메시지에 type 이 없습니다.", data);
  return null;
}
