"use client";

import { useSearchParams } from "next/navigation";
import { type AuthMeResponse, googleLoginUrl } from "../lib/auth";
import { devLoginEnabled } from "../lib/config";
import DevLoginForm from "./DevLoginForm";

export type LoginScreenProps = {
  onLoggedIn: (me: AuthMeResponse) => void;
  /** 게임 연결이 세션 끝으로 닫혔을 때 서버가 보낸 안내. 버튼 위에 한 줄로 보인다. */
  notice?: string | null;
};

export default function LoginScreen({ onLoggedIn, notice }: LoginScreenProps) {
  const loginFailed = useSearchParams().get("loginError") === "1";

  return (
    <main className="centered">
      <h1>mud-mk3</h1>
      {loginFailed && (
        <p role="alert" className="error">
          로그인하지 못했습니다. 다시 시도하십시오.
        </p>
      )}
      {notice && (
        <p role="status" className="muted">
          {notice}
        </p>
      )}
      <a className="button" href={googleLoginUrl()}>
        Google로 로그인
      </a>
      {devLoginEnabled && <DevLoginForm onLoggedIn={onLoggedIn} />}
    </main>
  );
}
