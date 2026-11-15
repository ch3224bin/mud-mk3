"use client";

import Link from "next/link";
import { type FormEvent, useState } from "react";
import {
  createMobPlacement,
  deleteMobPlacement,
  errorMessage,
  type MobPlacementResponse,
  type MobSummaryResponse,
  type RoomDetailResponse,
  updateMobPlacement,
} from "../lib/api";

type Props = {
  room: RoomDetailResponse;
  /** 몹 선택에 보일 몹 목록(화면에서 이름순으로 다시 늘어놓는다) */
  mobs: MobSummaryResponse[];
  onRoomChange: (room: RoomDetailResponse) => void;
};

/** 방의 몹 배치 표(몹 이름 링크·수 고치기·삭제)와 추가 폼(몹 선택·수). 값 검사는 서버에 맡기고 서버의 detail 을 보인다 */
export default function MobPlacementsEditor({ room, mobs, onRoomChange }: Props) {
  const options = [...mobs].sort((a, b) => a.name.localeCompare(b.name, "ko"));
  const [mobId, setMobId] = useState<number>(() => options[0]?.id ?? 0);
  const [count, setCount] = useState("1");
  /** 배치 id → 고치는 중인 수(입력 그대로의 글자) */
  const [counts, setCounts] = useState<Record<number, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function run(action: () => Promise<void>) {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  function countOf(placement: MobPlacementResponse): string {
    return counts[placement.id] ?? String(placement.count);
  }

  function forget(placementId: number) {
    setCounts(({ [placementId]: _, ...rest }) => rest);
  }

  function handleAdd(event: FormEvent) {
    event.preventDefault();
    return run(async () => {
      const created = await createMobPlacement(room.id, { mobId, count: Number(count) });
      onRoomChange({ ...room, mobs: [...room.mobs, created] });
    });
  }

  function handleUpdate(placement: MobPlacementResponse) {
    return run(async () => {
      const updated = await updateMobPlacement(placement.id, {
        mobId: placement.mobId,
        count: Number(countOf(placement)),
      });
      onRoomChange({ ...room, mobs: room.mobs.map((p) => (p.id === updated.id ? updated : p)) });
      forget(placement.id);
    });
  }

  function handleDelete(placement: MobPlacementResponse) {
    return run(async () => {
      await deleteMobPlacement(placement.id);
      onRoomChange({ ...room, mobs: room.mobs.filter((p) => p.id !== placement.id) });
      forget(placement.id);
    });
  }

  return (
    <section>
      <h2>몹 배치</h2>
      {room.mobs.length === 0 ? (
        <p className="muted">없음</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>몹</th>
              <th>수</th>
              <th />
              <th />
            </tr>
          </thead>
          <tbody>
            {room.mobs.map((placement) => (
              <tr key={placement.id}>
                <td>
                  <Link href={`/mob/?id=${placement.mobId}`}>{placement.mobName}</Link>
                </td>
                <td>
                  <input
                    type="number"
                    step="any"
                    aria-label={`${placement.mobName} 수`}
                    value={countOf(placement)}
                    onChange={(e) => {
                      const value = e.target.value;
                      setCounts((current) => ({ ...current, [placement.id]: value }));
                    }}
                  />
                </td>
                <td>
                  <button type="button" disabled={busy} onClick={() => handleUpdate(placement)}>
                    저장
                  </button>
                </td>
                <td>
                  <button type="button" disabled={busy} onClick={() => handleDelete(placement)}>
                    삭제
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <form className="form inline" onSubmit={handleAdd}>
        <label>
          몹
          <select value={mobId} onChange={(e) => setMobId(Number(e.target.value))}>
            {options.map((mob) => (
              <option key={mob.id} value={mob.id}>
                {mob.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          수
          <input type="number" step="any" value={count} onChange={(e) => setCount(e.target.value)} />
        </label>
        <button type="submit" disabled={busy || options.length === 0}>
          추가
        </button>
      </form>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </section>
  );
}
