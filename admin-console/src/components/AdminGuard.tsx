"use client";

import { useRouter } from "next/navigation";
import { type ReactNode, useEffect, useState } from "react";
import { errorMessage } from "../lib/api";
import { type AuthMeResponse, fetchMe, logout } from "../lib/auth";

type State = { kind: "loading" } | { kind: "ready"; me: AuthMeResponse } | { kind: "error"; message: string };

/** 관리자로 로그인했을 때만 children 을 보인다. 로그인 안 했으면 로그인 화면(/)으로 보낸다. */
export default function AdminGuard({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    let active = true;
    fetchMe()
      .then((me) => {
        if (!active) return;
        if (me === null) {
          router.replace("/");
        } else {
          setState({ kind: "ready", me });
        }
      })
      .catch((e) => active && setState({ kind: "error", message: errorMessage(e) }));
    return () => {
      active = false;
    };
  }, [router]);

  if (state.kind === "error") {
    return (
      <p role="alert" className="error">
        {state.message}
      </p>
    );
  }
  if (state.kind === "loading") {
    return <p className="muted">불러오는 중…</p>;
  }
  if (!state.me.admin) {
    return <NotAdminNotice />;
  }
  return <>{children}</>;
}

/** 로그인했지만 관리자가 아닌 계정에게 보이는 안내. 로그아웃하면 onLoggedOut(기본: 로그인 화면으로 이동) */
export function NotAdminNotice({ onLoggedOut }: { onLoggedOut?: () => void }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  async function handleLogout() {
    try {
      await logout();
      if (onLoggedOut) {
        onLoggedOut();
      } else {
        router.replace("/");
      }
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <main className="centered">
      <p>관리자만 쓸 수 있습니다.</p>
      <button type="button" onClick={handleLogout}>
        로그아웃
      </button>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </main>
  );
}
