"use client";

import { type KeyboardEvent, useEffect, useRef, useState } from "react";
import { Completer } from "../lib/complete";
import { GameSocket, type GameSocketHandlers, gameSocketUrl } from "../lib/gameSocket";
import { CommandHistory } from "../lib/history";
import { COMMAND_MAX_LENGTH, CloseCode, type ServerMessage } from "../lib/protocol";
import { echoLine, type OutputLine, renderMessage } from "../lib/render";

/** 화면이 쓰는 연결의 모양. 기본은 GameSocket, 테스트는 가짜를 넘긴다. */
export type GameConnection = { sendCommand(text: string): void; close(): void };

export type TerminalProps = {
  /** 4401(로그인 안 됨·세션 만료)로 닫혔을 때. 로그인 화면으로 돌아간다. */
  onUnauthorized: () => void;
  connect?: (handlers: GameSocketHandlers) => GameConnection;
};

const MAX_LINES = 2000;

type LogLine = OutputLine & { id: number };

const defaultConnect = (handlers: GameSocketHandlers): GameConnection => new GameSocket(gameSocketUrl(), handlers);

/** 터미널형 플레이 화면: 스크롤되는 출력 로그 + 아래 입력창. */
export default function Terminal({ onUnauthorized, connect = defaultConnect }: TerminalProps) {
  const [lines, setLines] = useState<LogLine[]>([]);
  const [input, setInput] = useState("");
  const [connected, setConnected] = useState(true);
  const [attempt, setAttempt] = useState(0);

  const nextId = useRef(0);
  const connection = useRef<GameConnection | null>(null);
  /** 지금 방 사람·NPC·바닥 물건 이름(자동완성 후보, D18·D29·D51) */
  const names = useRef<string[]>([]);
  const history = useRef(new CommandHistory());
  const completer = useRef(new Completer());
  const logRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const onUnauthorizedRef = useRef(onUnauthorized);
  onUnauthorizedRef.current = onUnauthorized;
  const connectRef = useRef(connect);
  connectRef.current = connect;

  function append(newLines: OutputLine[]) {
    if (newLines.length === 0) return;
    setLines((previous) => {
      const next = previous.concat(newLines.map((line) => ({ ...line, id: nextId.current++ })));
      return next.length > MAX_LINES ? next.slice(-MAX_LINES) : next;
    });
  }

  useEffect(() => {
    const current = connectRef.current({
      onMessage(message) {
        updateNames(names, message);
        append(renderMessage(message));
      },
      onClose(code) {
        connection.current = null;
        setConnected(false);
        if (code === CloseCode.UNAUTHORIZED) {
          onUnauthorizedRef.current();
        } else if (code !== CloseCode.NORMAL && code !== CloseCode.REPLACED) {
          // REPLACED(4409)는 서버가 닫기 전에 까닭을 system 메시지로 보낸다
          append([{ text: "연결이 끊겼습니다.", style: "error" }]);
        }
      },
    });
    connection.current = current;
    return () => {
      connection.current = null;
      current.close();
    };
  }, [attempt]);

  useEffect(() => {
    const log = logRef.current;
    if (log) {
      log.scrollTop = log.scrollHeight;
    }
  }, [lines]);

  function reconnect() {
    names.current = [];
    setConnected(true);
    setAttempt((n) => n + 1);
    inputRef.current?.focus();
  }

  function submit() {
    if (!connected || connection.current === null) return;
    const text = input;
    setInput("");
    completer.current.reset();
    if (text.trim() === "") {
      history.current.reset();
      return;
    }
    history.current.push(text);
    append([echoLine(text)]);
    connection.current.sendCommand(text);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    // 한글 조합 중 Enter·화살표·Tab 은 조합을 끝내는 키다(조합이 끝난 뒤 다시 온다).
    if (event.nativeEvent.isComposing) return;
    switch (event.key) {
      case "Enter":
        event.preventDefault();
        submit();
        break;
      case "ArrowUp":
        event.preventDefault();
        setInput(history.current.previous(input));
        break;
      case "ArrowDown":
        event.preventDefault();
        setInput(history.current.next());
        break;
      case "Tab":
        // Shift+Tab 은 기본 동작(앞 요소로 포커스 이동)으로 둔다. 키보드 사용자가 버튼으로 나갈 수 있게.
        if (event.shiftKey) break;
        event.preventDefault();
        setInput(completer.current.complete(input, names.current));
        break;
    }
  }

  /** 로그를 눌러도 입력창 포커스를 지킨다. 글자를 고르는 중이면 그대로 둔다. */
  function keepFocus() {
    if (window.getSelection()?.toString() === "") {
      inputRef.current?.focus();
    }
  }

  return (
    <div className="terminal">
      <div ref={logRef} className="log" role="log" aria-live="polite" onClick={keepFocus}>
        {lines.map((line) => (
          <div key={line.id} className={`line line-${line.style}`}>
            {line.text}
          </div>
        ))}
      </div>
      {!connected && (
        <div className="reconnect">
          <button type="button" onClick={reconnect}>
            다시 연결
          </button>
        </div>
      )}
      <div className="input-row">
        <span className="input-mark" aria-hidden="true">
          &gt;
        </span>
        <input
          ref={inputRef}
          type="text"
          aria-label="명령 입력"
          value={input}
          maxLength={COMMAND_MAX_LENGTH}
          autoFocus
          autoComplete="off"
          autoCapitalize="off"
          spellCheck={false}
          onChange={(event) => setInput(event.target.value)}
          onKeyDown={handleKeyDown}
        />
      </div>
    </div>
  );
}

/**
 * D18·D29·D51: 이름 후보는 마지막 room 의 occupants 전부(사람 + NPC) 다음 바닥 물건으로 정하고,
 * 이동 notice(arrive·appear·leave·disappear)로 사람만 더하고 뺀다. 물건 notice(pick·drop·give)는 바꾸지 않는다.
 */
function updateNames(names: { current: string[] }, message: ServerMessage) {
  if (message.type === "room") {
    names.current = [
      ...message.occupants.map((occupant) => occupant.name),
      ...message.items.map((item) => item.name),
    ];
  } else if (message.type === "notice") {
    switch (message.event) {
      case "arrive":
      case "appear":
        names.current = [...names.current.filter((name) => name !== message.actor), message.actor];
        break;
      case "leave":
      case "disappear":
        names.current = names.current.filter((name) => name !== message.actor);
        break;
    }
  }
}
