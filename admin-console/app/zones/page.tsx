"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import TopBar from "../../src/components/TopBar";
import ZoneForm from "../../src/components/ZoneForm";
import {
  createZone,
  deleteZone,
  errorMessage,
  type FactionResponse,
  listFactions,
  listZones,
  updateZone,
  type ZoneResponse,
  type ZoneSaveRequest,
} from "../../src/lib/api";
import { factionLabel } from "../../src/lib/factions";

export default function ZonesPage() {
  return (
    <AdminGuard>
      <ZonesView />
    </AdminGuard>
  );
}

function ZonesView() {
  const [zones, setZones] = useState<ZoneResponse[] | null>(null);
  const [factions, setFactions] = useState<FactionResponse[]>([]);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [loadedZones, loadedFactions] = await Promise.all([listZones(), listFactions()]);
      setZones(loadedZones);
      setFactions(loadedFactions);
    } catch (e) {
      setError(errorMessage(e));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function handleCreate(request: ZoneSaveRequest) {
    await createZone(request);
    await load();
  }

  async function handleUpdate(zoneId: number, request: ZoneSaveRequest) {
    await updateZone(zoneId, request);
    setEditingId(null);
    await load();
  }

  async function handleDelete(zone: ZoneResponse) {
    if (!window.confirm(`'${zone.name}' 지역을 지울까요?`)) {
      return;
    }
    setError(null);
    try {
      await deleteZone(zone.id);
      await load();
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <>
      <TopBar />
      <main>
        <h1>지역</h1>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {zones === null ? (
          <p className="muted">불러오는 중…</p>
        ) : zones.length === 0 ? (
          <p className="muted">아직 지역이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>이름</th>
                <th>방 수</th>
                <th>소유</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {zones.map((zone) =>
                editingId === zone.id ? (
                  <tr key={zone.id}>
                    <td colSpan={4}>
                      <ZoneForm
                        submitLabel="저장"
                        factions={factions}
                        initial={{ name: zone.name, description: zone.description, ownerId: zone.ownerId }}
                        onSubmit={(request) => handleUpdate(zone.id, request)}
                      />
                      <button type="button" onClick={() => setEditingId(null)}>
                        취소
                      </button>
                    </td>
                  </tr>
                ) : (
                  <tr key={zone.id}>
                    <td>
                      <Link href={`/zone/?id=${zone.id}`}>{zone.name}</Link>
                    </td>
                    <td>{zone.roomCount}</td>
                    <td>
                      {zone.ownerName !== null && zone.ownerKind !== null
                        ? factionLabel(zone.ownerName, zone.ownerKind)
                        : "-"}
                    </td>
                    <td className="actions">
                      <button type="button" onClick={() => setEditingId(zone.id)}>
                        수정
                      </button>
                      <button type="button" onClick={() => handleDelete(zone)}>
                        삭제
                      </button>
                    </td>
                  </tr>
                ),
              )}
            </tbody>
          </table>
        )}
        <h2>지역 만들기</h2>
        <ZoneForm submitLabel="지역 만들기" factions={factions} onSubmit={handleCreate} />
      </main>
    </>
  );
}
