"use client";

import { Suspense, useEffect, useState } from "react";
import LoginScreen from "../src/components/LoginScreen";
import Terminal from "../src/components/Terminal";
import { errorMessage } from "../src/lib/api";
import { type AuthMeResponse, fetchMe, logout } from "../src/lib/auth";

type State =
  | { kind: "loading" }
  | { kind: "anonymous" }
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

  function enter(me: AuthMeResponse | null) {
    setState(me === null ? { kind: "anonymous" } : { kind: "playing", me });
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
    try {
      await logout();
      setState({ kind: "anonymous" });
    } catch (e) {
      setLogoutError(errorMessage(e));
    }
  }

  switch (state.kind) {
    case "loading":
      return <p className="muted">불러오는 중…</p>;
    case "anonymous":
      return <LoginScreen onLoggedIn={enter} />;
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
          <Terminal onUnauthorized={() => setState({ kind: "anonymous" })} />
        </div>
      );
  }
}
