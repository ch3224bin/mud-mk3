"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useCallback, useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import RoomForm from "../../src/components/RoomForm";
import TopBar from "../../src/components/TopBar";
import {
  createRoom,
  errorMessage,
  getWorld,
  listRooms,
  listZones,
  type RoomSaveRequest,
  type RoomSummaryResponse,
  type ZoneResponse,
} from "../../src/lib/api";

export default function ZonePage() {
  return (
    <Suspense fallback={<p className="muted">불러오는 중…</p>}>
      <AdminGuard>
        <ZoneView />
      </AdminGuard>
    </Suspense>
  );
}

type Loaded = { zones: ZoneResponse[]; rooms: RoomSummaryResponse[]; startRoomId: number | null };

function ZoneView() {
  const zoneId = Number(useSearchParams().get("id"));
  const [data, setData] = useState<Loaded | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [zones, rooms, world] = await Promise.all([listZones(), listRooms(zoneId), getWorld()]);
      setData({ zones, rooms, startRoomId: world.startRoomId });
    } catch (e) {
      setError(errorMessage(e));
    }
  }, [zoneId]);

  useEffect(() => {
    setData(null);
    setError(null);
    void load();
  }, [load]);

  async function handleCreate(request: RoomSaveRequest) {
    await createRoom(request);
    await load();
  }

  const zone = data?.zones.find((z) => z.id === zoneId);

  return (
    <>
      <TopBar />
      <main>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {data === null ? (
          !error && <p className="muted">불러오는 중…</p>
        ) : zone === undefined ? (
          <p className="error">지역을 찾을 수 없습니다.</p>
        ) : (
          <>
            <h1>{zone.name}</h1>
            {zone.description && <p className="description">{zone.description}</p>}
            <h2>방</h2>
            {data.rooms.length === 0 ? (
              <p className="muted">아직 방이 없습니다.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>이름</th>
                    <th>출구 수</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data.rooms.map((room) => (
                    <tr key={room.id}>
                      <td>
                        <Link href={`/room/?id=${room.id}`}>{room.name}</Link>
                      </td>
                      <td>{room.exitCount}</td>
                      <td>{room.id === data.startRoomId && <span className="badge">시작 방</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            <h2>방 만들기</h2>
            <RoomForm zones={data.zones} defaultZoneId={zoneId} submitLabel="방 만들기" onSubmit={handleCreate} />
          </>
        )}
      </main>
    </>
  );
}
