"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import NpcForm from "../../src/components/NpcForm";
import TopBar from "../../src/components/TopBar";
import {
  createNpc,
  deleteNpc,
  errorMessage,
  getNpc,
  type NpcDetailResponse,
  type NpcSaveRequest,
  updateNpc,
} from "../../src/lib/api";

export default function NpcPage() {
  return (
    <Suspense fallback={<p className="muted">불러오는 중…</p>}>
      <AdminGuard>
        <NpcView />
      </AdminGuard>
    </Suspense>
  );
}

/** id 가 없으면 새 NPC 만들기, 있으면 그 NPC 고치기 */
function NpcView() {
  const idParam = useSearchParams().get("id");
  const npcId = idParam === null ? null : Number(idParam);
  const router = useRouter();
  const [npc, setNpc] = useState<NpcDetailResponse | null>(null);
  // 저장하면 서버가 다듬은 값(앞뒤 공백, 낱말의 [ ])으로 폼을 다시 연다.
  const [savedCount, setSavedCount] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setNpc(null);
    setError(null);
    if (npcId === null) {
      return;
    }
    let active = true;
    getNpc(npcId)
      .then((loaded) => active && setNpc(loaded))
      .catch((e) => active && setError(errorMessage(e)));
    return () => {
      active = false;
    };
  }, [npcId]);

  async function handleCreate(request: NpcSaveRequest) {
    const created = await createNpc(request);
    router.push(`/npc/?id=${created.id}`);
  }

  async function handleUpdate(id: number, request: NpcSaveRequest) {
    setNpc(await updateNpc(id, request));
    setSavedCount((n) => n + 1);
  }

  async function handleDelete(target: NpcDetailResponse) {
    if (!window.confirm(`'${target.name}' NPC를 지울까요?`)) {
      return;
    }
    setError(null);
    try {
      await deleteNpc(target.id);
      router.push("/npcs/");
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <>
      <TopBar />
      <main>
        <h1>NPC</h1>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {npcId === null ? (
          <NpcForm key="new" onSubmit={handleCreate} />
        ) : npc === null ? (
          !error && <p className="muted">불러오는 중…</p>
        ) : (
          <>
            <div className="actions">
              <button type="button" onClick={() => handleDelete(npc)}>
                삭제
              </button>
            </div>
            <NpcForm key={`${npc.id}-${savedCount}`} initial={npc} onSubmit={(request) => handleUpdate(npc.id, request)} />
          </>
        )}
      </main>
    </>
  );
}
