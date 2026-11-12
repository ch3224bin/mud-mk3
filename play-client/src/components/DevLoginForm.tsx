"use client";

import { type FormEvent, useState } from "react";
import { errorMessage } from "../lib/api";
import { type AuthMeResponse, loginForDevelopment } from "../lib/auth";

/** Google 없이 이메일로 로그인한다. NEXT_PUBLIC_DEV_LOGIN=true 빌드에서만 보인다(서버도 local 프로필에서만 받는다). */
export default function DevLoginForm({ onLoggedIn }: { onLoggedIn: (me: AuthMeResponse) => void }) {
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      onLoggedIn(await loginForDevelopment(email.trim()));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <label>
        개발용 로그인 이메일
        <input type="email" value={email} required onChange={(e) => setEmail(e.target.value)} />
      </label>
      <button type="submit" disabled={submitting || email.trim() === ""}>
        개발용 로그인
      </button>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </form>
  );
}
