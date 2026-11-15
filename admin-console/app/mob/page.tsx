"use client";

import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import MobForm from "../../src/components/MobForm";
import TopBar from "../../src/components/TopBar";
import {
  errorMessage,
  getMob,
  type ItemTypeResponse,
  listItemTypes,
  listRealms,
  type MobDetailResponse,
  type RealmResponse,
} from "../../src/lib/api";

export default function MobPage() {
  return (
    <Suspense fallback={<p className="muted">불러오는 중…</p>}>
      <AdminGuard>
        <MobView />
      </AdminGuard>
    </Suspense>
  );
}

/** id 가 없으면 새 몹 만들기, 있으면 그 몹 고치기 */
function MobView() {
  const idParam = useSearchParams().get("id");
  const mobId = idParam === null ? null : Number(idParam);
  const [realms, setRealms] = useState<RealmResponse[] | null>(null);
  const [itemTypes, setItemTypes] = useState<ItemTypeResponse[]>([]);
  const [mob, setMob] = useState<MobDetailResponse | null>(null);
  // 저장하면 서버가 다듬은 값(이름 공백 등)과 새로 계산한 스탯으로 폼을 다시 연다.
  const [savedCount, setSavedCount] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setMob(null);
    setError(null);
    let active = true;
    Promise.all([listRealms(), listItemTypes(), mobId === null ? Promise.resolve(null) : getMob(mobId)])
      .then(([loadedRealms, loadedItemTypes, loaded]) => {
        if (!active) return;
        setRealms(loadedRealms);
        setItemTypes(loadedItemTypes);
        setMob(loaded);
      })
      .catch((e) => active && setError(errorMessage(e)));
    return () => {
      active = false;
    };
  }, [mobId]);

  function handleSaved(saved: MobDetailResponse) {
    setMob(saved);
    setSavedCount((n) => n + 1);
  }

  return (
    <>
      <TopBar />
      <main>
        <h1>몹</h1>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {realms === null || (mobId !== null && mob === null) ? (
          !error && <p className="muted">불러오는 중…</p>
        ) : mob === null ? (
          <MobForm key="new" realms={realms} itemTypes={itemTypes} onSaved={handleSaved} />
        ) : (
          <MobForm
            key={`${mob.id}-${savedCount}`}
            initial={mob}
            realms={realms}
            itemTypes={itemTypes}
            onSaved={handleSaved}
          />
        )}
      </main>
    </>
  );
}
