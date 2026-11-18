"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import LoginScreen from "../src/components/LoginScreen";
import Terminal from "../src/components/Terminal";
import { errorMessage } from "../src/lib/api";
import { type AuthMeResponse, fetchMe, logout } from "../src/lib/auth";
import { createSessionKeepAlive } from "../src/lib/sessionKeepAlive";

type State =
  | { kind: "loading" }
  /** notice: 게임 연결이 세션 끝으로 닫혔을 때 서버 안내(로그아웃 버튼·처음 화면은 null) */
  | { kind: "anonymous"; notice: string | null }
  | { kind: "playing"; me: AuthMeResponse }
  | { kind: "error"; message: string };

export default function HomePage() {
  return (
    <Suspense fallback={<p className="muted">불러오는 중…</p>}>
      <Home />
    </Suspense>
  );
}

function Home() {
  const [state, setState] = useState<State>({ kind: "loading" });
  const [logoutError, setLogoutError] = useState<string | null>(null);
  /** 입력하는 동안 세션 늘리기. 들어갈 때(방금 세션을 확인함) 새로 만든다. */
  const keepAlive = useRef<{ onCommand(): void } | null>(null);
  /**
   * 로그아웃 버튼으로 나가는 중(성공하면 다음에 들어갈 때까지). 서버가 이 연결을 4401 로 닫아도 안내 없이 나간다.
   * 화면이 바뀌어 연결이 닫히기 전에 4401 이 먼저 올 수 있어서다.
   */
  const loggingOut = useRef(false);
  /** 로그아웃 중에 4401 로 닫혔다(세션이 이미 끝남) */
  const closedWhileLoggingOut = useRef(false);

  function enter(me: AuthMeResponse | null) {
    if (me === null) {
      setState({ kind: "anonymous", notice: null });
      return;
    }
    keepAlive.current = createSessionKeepAlive(fetchMe);
    loggingOut.current = false;
    setState({ kind: "playing", me });
  }

  function handleUnauthorized(notice: string | null) {
    if (loggingOut.current) {
      closedWhileLoggingOut.current = true;
      return;
    }
    setState({ kind: "anonymous", notice });
  }

  useEffect(() => {
    let active = true;
    fetchMe()
      .then((me) => active && enter(me))
      .catch((e) => active && setState({ kind: "error", message: errorMessage(e) }));
    return () => {
      active = false;
    };
  }, []);

  async function handleLogout() {
    setLogoutError(null);
    loggingOut.current = true;
    closedWhileLoggingOut.current = false;
    try {
      await logout();
      setState({ kind: "anonymous", notice: null });
    } catch (e) {
      if (closedWhileLoggingOut.current) {
        // 응답은 못 받았어도 세션은 끝났다
        setState({ kind: "anonymous", notice: null });
      } else {
        loggingOut.current = false;
        setLogoutError(errorMessage(e));
      }
    }
  }

  switch (state.kind) {
    case "loading":
      return <p className="muted">불러오는 중…</p>;
    case "anonymous":
      return <LoginScreen onLoggedIn={enter} notice={state.notice} />;
    case "error":
      return (
        <p role="alert" className="error">
          {state.message}
        </p>
      );
    case "playing":
      return (
        <div className="play">
          <header className="topbar">
            {logoutError && (
              <span role="alert" className="error">
                {logoutError}
              </span>
            )}
            <button type="button" className="small" onClick={handleLogout}>
              로그아웃
            </button>
          </header>
          <Terminal onUnauthorized={handleUnauthorized} onCommandSent={() => keepAlive.current?.onCommand()} />
        </div>
      );
  }
}
