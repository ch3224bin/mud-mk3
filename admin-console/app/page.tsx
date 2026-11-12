"use client";

import { useRouter } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { NotAdminNotice } from "../src/components/AdminGuard";
import LoginPanel from "../src/components/LoginPanel";
import { errorMessage } from "../src/lib/api";
import { type AuthMeResponse, fetchMe } from "../src/lib/auth";

type State =
  | { kind: "loading" }
  | { kind: "anonymous" }
  | { kind: "notAdmin" }
  | { kind: "error"; message: string };

export default function HomePage() {
  return (
    <Suspense fallback={<p className="muted">불러오는 중…</p>}>
      <Home />
    </Suspense>
  );
}

function Home() {
  const router = useRouter();
  const [state, setState] = useState<State>({ kind: "loading" });

  function enter(me: AuthMeResponse | null) {
    if (me === null) {
      setState({ kind: "anonymous" });
    } else if (me.admin) {
      router.replace("/zones/");
    } else {
      setState({ kind: "notAdmin" });
    }
  }

  useEffect(() => {
    fetchMe()
      .then(enter)
      .catch((e) => setState({ kind: "error", message: errorMessage(e) }));
  }, []);

  switch (state.kind) {
    case "loading":
      return <p className="muted">불러오는 중…</p>;
    case "anonymous":
      return <LoginPanel onLoggedIn={enter} />;
    case "notAdmin":
      return <NotAdminNotice onLoggedOut={() => setState({ kind: "anonymous" })} />;
    case "error":
      return (
        <p role="alert" className="error">
          {state.message}
        </p>
      );
  }
}
