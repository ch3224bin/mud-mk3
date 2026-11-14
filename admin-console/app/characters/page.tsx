"use client";

import { useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import CharacterTable from "../../src/components/CharacterTable";
import TopBar from "../../src/components/TopBar";
import { type CharacterSummaryResponse, errorMessage, listCharacters } from "../../src/lib/api";

export default function CharactersPage() {
  return (
    <AdminGuard>
      <CharactersView />
    </AdminGuard>
  );
}

function CharactersView() {
  const [characters, setCharacters] = useState<CharacterSummaryResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listCharacters()
      .then(setCharacters)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  return (
    <>
      <TopBar />
      <main>
        <h1>캐릭터</h1>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {characters === null ? !error && <p className="muted">불러오는 중…</p> : <CharacterTable characters={characters} />}
      </main>
    </>
  );
}
