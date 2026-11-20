"use client";

import { useState } from "react";
import { deleteCharacter, errorMessage } from "../lib/api";

type Props = {
  character: { id: number; name: string };
  /** 지운 뒤 부른다(캐릭터 화면은 목록으로 간다) */
  onDeleted: () => void;
};

/**
 * 캐릭터 삭제: 안내와 `캐릭터 삭제` 버튼. 누르면 그 자리에 확인 칸이 열리고,
 * 캐릭터 이름을 글자 그대로(앞뒤 공백 포함) 입력해야 `삭제` 가 켜진다. 실패하면 칸을 연 채 서버의 detail 을 보인다
 */
export default function CharacterDeleteForm({ character, onDeleted }: Props) {
  const [open, setOpen] = useState(false);
  const [typed, setTyped] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function close() {
    setOpen(false);
    setTyped("");
    setError(null);
  }

  async function handleDelete() {
    setSubmitting(true);
    setError(null);
    try {
      await deleteCharacter(character.id);
      onDeleted();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section>
      <h2>캐릭터 삭제</h2>
      <p className="muted">
        캐릭터를 지우면 소지품·익힌 공법·깨달음이 모두 사라지고 되돌릴 수 없습니다. 계정은 남아 다음 접속 때 새 캐릭터를
        만듭니다.
      </p>
      {open ? (
        <div className="form">
          <p>지우려면 캐릭터 이름을 그대로 입력하십시오: {character.name}</p>
          <label>
            캐릭터 이름
            <input value={typed} onChange={(e) => setTyped(e.target.value)} />
          </label>
          <div className="actions">
            <button type="button" disabled={typed !== character.name || submitting} onClick={handleDelete}>
              삭제
            </button>
            <button type="button" disabled={submitting} onClick={close}>
              취소
            </button>
          </div>
          {error && (
            <p role="alert" className="error">
              {error}
            </p>
          )}
        </div>
      ) : (
        <div className="actions">
          <button type="button" onClick={() => setOpen(true)}>
            캐릭터 삭제
          </button>
        </div>
      )}
    </section>
  );
}
