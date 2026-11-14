"use client";

import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import TechniqueForm from "../../src/components/TechniqueForm";
import TopBar from "../../src/components/TopBar";
import {
  errorMessage,
  getTechnique,
  listRealms,
  type RealmResponse,
  type TechniqueDetailResponse,
} from "../../src/lib/api";

export default function TechniquePage() {
  return (
    <Suspense fallback={<p className="muted">불러오는 중…</p>}>
      <AdminGuard>
        <TechniqueView />
      </AdminGuard>
    </Suspense>
  );
}

/** id 가 없으면 새 공법 만들기, 있으면 그 공법 고치기 */
function TechniqueView() {
  const idParam = useSearchParams().get("id");
  const techniqueId = idParam === null ? null : Number(idParam);
  const [realms, setRealms] = useState<RealmResponse[] | null>(null);
  const [technique, setTechnique] = useState<TechniqueDetailResponse | null>(null);
  // 저장하면 서버가 다듬은 값(이름 공백 등)으로 폼을 다시 연다.
  const [savedCount, setSavedCount] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setTechnique(null);
    setError(null);
    let active = true;
    Promise.all([listRealms(), techniqueId === null ? Promise.resolve(null) : getTechnique(techniqueId)])
      .then(([loadedRealms, loaded]) => {
        if (!active) return;
        setRealms(loadedRealms);
        setTechnique(loaded);
      })
      .catch((e) => active && setError(errorMessage(e)));
    return () => {
      active = false;
    };
  }, [techniqueId]);

  function handleSaved(saved: TechniqueDetailResponse) {
    setTechnique(saved);
    setSavedCount((n) => n + 1);
  }

  return (
    <>
      <TopBar />
      <main>
        <h1>공법</h1>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {realms === null || (techniqueId !== null && technique === null) ? (
          !error && <p className="muted">불러오는 중…</p>
        ) : technique === null ? (
          <TechniqueForm key="new" realms={realms} onSaved={handleSaved} />
        ) : (
          <TechniqueForm
            key={`${technique.id}-${savedCount}`}
            initial={technique}
            realms={realms}
            onSaved={handleSaved}
          />
        )}
      </main>
    </>
  );
}
