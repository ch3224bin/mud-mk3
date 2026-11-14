"use client";

import { useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import StartItemsEditor from "../../src/components/StartItemsEditor";
import TopBar from "../../src/components/TopBar";
import {
  errorMessage,
  getStartingItems,
  type ItemTypeResponse,
  listItemTypes,
  saveStartingItems,
  type StartingItem,
  type StartingItemResponse,
} from "../../src/lib/api";

export default function StartItemsPage() {
  return (
    <AdminGuard>
      <StartItemsView />
    </AdminGuard>
  );
}

function StartItemsView() {
  const [data, setData] = useState<{ itemTypes: ItemTypeResponse[]; startingItems: StartingItemResponse[] } | null>(
    null,
  );
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([listItemTypes(), getStartingItems()])
      .then(([itemTypes, startingItems]) => setData({ itemTypes, startingItems }))
      .catch((e) => setError(errorMessage(e)));
  }, []);

  async function handleSave(items: StartingItem[]) {
    await saveStartingItems(items);
  }

  return (
    <>
      <TopBar />
      <main>
        <h1>시작 소지품</h1>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {data === null ? (
          !error && <p className="muted">불러오는 중…</p>
        ) : (
          <StartItemsEditor itemTypes={data.itemTypes} initial={data.startingItems} onSave={handleSave} />
        )}
      </main>
    </>
  );
}
