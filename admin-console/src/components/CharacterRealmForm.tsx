"use client";

import { type FormEvent, useState } from "react";
import {
  type CharacterDetailResponse,
  designateRealm,
  errorMessage,
  type RealmCode,
  type RealmResponse,
} from "../lib/api";
import { STAT_ROWS } from "../lib/characters";

type Props = {
  character: CharacterDetailResponse;
  /** 경지 선택지(GET /api/admin/realms, 낮은 경지부터) */
  realms: RealmResponse[];
  /** 경지를 지정한 뒤 서버가 돌려준 상세 */
  onSaved: (saved: CharacterDetailResponse) => void;
};

/**
 * 캐릭터 경지 지정: 경지 선택과 `경지 지정`(확인 없이 바로 저장), 스탯 표.
 * 저장하면 받은 상세로 스탯 표를 다시 그린다. 값 검사는 서버에 맡기고 서버의 detail 을 보인다
 */
export default function CharacterRealmForm({ character, realms, onSaved }: Props) {
  const [shown, setShown] = useState(character);
  const [realm, setRealm] = useState<RealmCode>(character.realm);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setMessage(null);
    setError(null);
    try {
      const saved = await designateRealm(shown.id, realm);
      setShown(saved);
      setRealm(saved.realm);
      setMessage("경지를 지정했습니다.");
      onSaved(saved);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <form className="form" onSubmit={handleSubmit}>
        <label>
          경지
          <select value={realm} onChange={(e) => setRealm(e.target.value as RealmCode)}>
            {realms.map((option) => (
              <option key={option.code} value={option.code}>
                {option.name}
              </option>
            ))}
          </select>
        </label>
        <p className="muted">수위·돌파 없이 경지를 바로 바꿉니다. 스탯은 새 경지 값이 되고 바로 다 찹니다.</p>
        <div className="actions">
          <button type="submit" disabled={submitting}>
            경지 지정
          </button>
        </div>
        {message && (
          <p role="status" className="muted">
            {message}
          </p>
        )}
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
      </form>
      <h2>스탯</h2>
      <table aria-label="스탯">
        <tbody>
          {STAT_ROWS.map((row) => (
            <tr key={row.key}>
              <th scope="row">{row.label}</th>
              <td>{shown.stats[row.key]}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}
