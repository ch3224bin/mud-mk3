"use client";

import { useSearchParams } from "next/navigation";
import { googleLoginUrl, type AuthMeResponse } from "../lib/auth";
import { devLoginEnabled } from "../lib/config";
import DevLoginForm from "./DevLoginForm";

export default function LoginPanel({ onLoggedIn }: { onLoggedIn: (me: AuthMeResponse) => void }) {
  const loginFailed = useSearchParams().get("loginError") === "1";

  return (
    <main className="centered">
      <h1>mud-mk3 관리</h1>
      {loginFailed && (
        <p role="alert" className="error">
          로그인하지 못했습니다. 다시 시도하십시오.
        </p>
      )}
      <a className="button" href={googleLoginUrl()}>
        Google로 로그인
      </a>
      {devLoginEnabled && <DevLoginForm onLoggedIn={onLoggedIn} />}
    </main>
  );
}
