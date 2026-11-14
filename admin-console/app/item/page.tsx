"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import ItemForm from "../../src/components/ItemForm";
import TopBar from "../../src/components/TopBar";
import {
  createItemType,
  deleteItemType,
  errorMessage,
  getItemType,
  type ItemTypeResponse,
  type ItemTypeSaveRequest,
  updateItemType,
} from "../../src/lib/api";

export default function ItemPage() {
  return (
    <Suspense fallback={<p className="muted">불러오는 중…</p>}>
      <AdminGuard>
        <ItemView />
      </AdminGuard>
    </Suspense>
  );
}

/** id 가 없으면 새 아이템 만들기, 있으면 그 아이템 고치기 */
function ItemView() {
  const idParam = useSearchParams().get("id");
  const itemTypeId = idParam === null ? null : Number(idParam);
  const router = useRouter();
  const [itemType, setItemType] = useState<ItemTypeResponse | null>(null);
  // 저장하면 서버가 다듬은 값(이름 공백 등)으로 폼을 다시 연다.
  const [savedCount, setSavedCount] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setItemType(null);
    setError(null);
    if (itemTypeId === null) {
      return;
    }
    let active = true;
    getItemType(itemTypeId)
      .then((loaded) => active && setItemType(loaded))
      .catch((e) => active && setError(errorMessage(e)));
    return () => {
      active = false;
    };
  }, [itemTypeId]);

  async function handleCreate(request: ItemTypeSaveRequest) {
    const created = await createItemType(request);
    router.push(`/item/?id=${created.id}`);
  }

  async function handleUpdate(id: number, request: ItemTypeSaveRequest) {
    setItemType(await updateItemType(id, request));
    setSavedCount((n) => n + 1);
  }

  async function handleDelete(target: ItemTypeResponse) {
    if (!window.confirm(`'${target.name}' 아이템을 지울까요?`)) {
      return;
    }
    setError(null);
    try {
      await deleteItemType(target.id);
      router.push("/items/");
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <>
      <TopBar />
      <main>
        <h1>아이템</h1>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {itemTypeId === null ? (
          <ItemForm key="new" onSubmit={handleCreate} />
        ) : itemType === null ? (
          !error && <p className="muted">불러오는 중…</p>
        ) : (
          <>
            {!itemType.spiritStone && (
              <div className="actions">
                <button type="button" onClick={() => handleDelete(itemType)}>
                  삭제
                </button>
              </div>
            )}
            <ItemForm
              key={`${itemType.id}-${savedCount}`}
              initial={itemType}
              onSubmit={(request) => handleUpdate(itemType.id, request)}
            />
          </>
        )}
      </main>
    </>
  );
}
