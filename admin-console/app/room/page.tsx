"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import ExitEditor from "../../src/components/ExitEditor";
import RoomForm from "../../src/components/RoomForm";
import TopBar from "../../src/components/TopBar";
import {
  deleteRoom,
  errorMessage,
  getRoom,
  listZones,
  type RoomDetailResponse,
  type RoomSaveRequest,
  setStartRoom,
  updateRoom,
  type ZoneResponse,
} from "../../src/lib/api";

export default function RoomPage() {
  return (
    <Suspense fallback={<p className="muted">불러오는 중…</p>}>
      <AdminGuard>
        <RoomView />
      </AdminGuard>
    </Suspense>
  );
}

function RoomView() {
  const roomId = Number(useSearchParams().get("id"));
  const router = useRouter();
  const [room, setRoom] = useState<RoomDetailResponse | null>(null);
  const [zones, setZones] = useState<ZoneResponse[]>([]);
  const [worldVersion, setWorldVersion] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setRoom(null);
    setError(null);
    Promise.all([getRoom(roomId), listZones()])
      .then(([loadedRoom, loadedZones]) => {
        if (!active) return;
        setRoom(loadedRoom);
        setZones(loadedZones);
      })
      .catch((e) => active && setError(errorMessage(e)));
    return () => {
      active = false;
    };
  }, [roomId]);

  async function handleUpdate(request: RoomSaveRequest) {
    setRoom(await updateRoom(roomId, request));
    setWorldVersion((v) => v + 1);
  }

  async function handleSetStart() {
    setError(null);
    try {
      await setStartRoom(roomId);
      setRoom((current) => current && { ...current, start: true });
      setWorldVersion((v) => v + 1);
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  async function handleDelete(target: RoomDetailResponse) {
    if (!window.confirm(`'${target.name}' 방을 지울까요? 이 방으로 오는 출구도 함께 지워집니다.`)) {
      return;
    }
    setError(null);
    try {
      await deleteRoom(target.id);
      router.push(`/zone/?id=${target.zoneId}`);
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <>
      <TopBar refreshKey={worldVersion} />
      <main>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {room === null ? (
          !error && <p className="muted">불러오는 중…</p>
        ) : (
          <>
            <p>
              <Link href={`/zone/?id=${room.zoneId}`}>{room.zoneName}</Link>
            </p>
            <h1>
              {room.name} {room.start && <span className="badge">시작 방</span>}
            </h1>
            <div className="actions">
              <button type="button" disabled={room.start} onClick={handleSetStart}>
                시작 방으로 지정
              </button>
              <button type="button" onClick={() => handleDelete(room)}>
                삭제
              </button>
            </div>
            <h2>방 정보</h2>
            <RoomForm
              key={room.id}
              zones={zones}
              submitLabel="저장"
              initial={{ zoneId: room.zoneId, name: room.name, description: room.description }}
              onSubmit={handleUpdate}
            />
            <ExitEditor key={`exits-${room.id}`} room={room} onRoomChange={setRoom} />
          </>
        )}
      </main>
    </>
  );
}
