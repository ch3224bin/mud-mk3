"use client";

import { useCallback, useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import FactionForm from "../../src/components/FactionForm";
import TopBar from "../../src/components/TopBar";
import {
  createFaction,
  deleteFaction,
  errorMessage,
  type FactionResponse,
  type FactionSaveRequest,
  listFactions,
  updateFaction,
} from "../../src/lib/api";

export default function FactionsPage() {
  return (
    <AdminGuard>
      <FactionsView />
    </AdminGuard>
  );
}

function FactionsView() {
  const [factions, setFactions] = useState<FactionResponse[] | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setFactions(await listFactions());
    } catch (e) {
      setError(errorMessage(e));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function handleCreate(request: FactionSaveRequest) {
    await createFaction(request);
    await load();
  }

  async function handleUpdate(factionId: number, request: FactionSaveRequest) {
    await updateFaction(factionId, request);
    setEditingId(null);
    await load();
  }

  async function handleDelete(faction: FactionResponse) {
    if (!window.confirm(`'${faction.name}' ${faction.kindName}을(를) 지울까요?`)) {
      return;
    }
    setError(null);
    try {
      await deleteFaction(faction.id);
      await load();
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <>
      <TopBar />
      <main>
        <h1>성·종문</h1>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {factions === null ? (
          <p className="muted">불러오는 중…</p>
        ) : factions.length === 0 ? (
          <p className="muted">아직 성·종문이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>종류</th>
                <th>이름</th>
                <th>소유 지역 수</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {factions.map((faction) =>
                editingId === faction.id ? (
                  <tr key={faction.id}>
                    <td colSpan={4}>
                      <FactionForm
                        submitLabel="저장"
                        initial={{ kind: faction.kind, name: faction.name, description: faction.description }}
                        onSubmit={(request) => handleUpdate(faction.id, request)}
                      />
                      <button type="button" onClick={() => setEditingId(null)}>
                        취소
                      </button>
                    </td>
                  </tr>
                ) : (
                  <tr key={faction.id}>
                    <td>{faction.kindName}</td>
                    <td>{faction.name}</td>
                    <td>{faction.zoneCount}</td>
                    <td className="actions">
                      <button type="button" onClick={() => setEditingId(faction.id)}>
                        수정
                      </button>
                      <button type="button" onClick={() => handleDelete(faction)}>
                        삭제
                      </button>
                    </td>
                  </tr>
                ),
              )}
            </tbody>
          </table>
        )}
        <h2>성·종문 만들기</h2>
        <FactionForm submitLabel="성·종문 만들기" onSubmit={handleCreate} />
      </main>
    </>
  );
}
