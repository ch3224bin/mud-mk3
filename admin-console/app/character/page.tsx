"use client";

import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import CharacterDetail from "../../src/components/CharacterDetail";
import TopBar from "../../src/components/TopBar";
import {
  type CharacterDetailResponse,
  errorMessage,
  getCharacter,
  listRealms,
  type RealmResponse,
} from "../../src/lib/api";

export default function CharacterPage() {
  return (
    <Suspense fallback={<p className="muted">불러오는 중…</p>}>
      <AdminGuard>
        <CharacterView />
      </AdminGuard>
    </Suspense>
  );
}

/** `/character/?id=3`: 캐릭터 상세와 경지 지정 */
function CharacterView() {
  const idParam = useSearchParams().get("id");
  const characterId = idParam === null ? null : Number(idParam);
  const [realms, setRealms] = useState<RealmResponse[] | null>(null);
  const [character, setCharacter] = useState<CharacterDetailResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setCharacter(null);
    setError(null);
    if (characterId === null) {
      setError("캐릭터를 찾을 수 없습니다.");
      return;
    }
    let active = true;
    Promise.all([listRealms(), getCharacter(characterId)])
      .then(([loadedRealms, loaded]) => {
        if (!active) return;
        setRealms(loadedRealms);
        setCharacter(loaded);
      })
      .catch((e) => active && setError(errorMessage(e)));
    return () => {
      active = false;
    };
  }, [characterId]);

  return (
    <>
      <TopBar />
      <main>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {realms === null || character === null ? (
          !error && <p className="muted">불러오는 중…</p>
        ) : (
          <CharacterDetail key={character.id} initial={character} realms={realms} />
        )}
      </main>
    </>
  );
}
