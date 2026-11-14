"use client";

import Link from "next/link";
import { type FormEvent, useState } from "react";
import { deleteExit, errorMessage, type ExitResponse, getRoom, putExit, type RoomDetailResponse } from "../lib/api";
import { DIRECTIONS, type DirectionCode } from "../lib/directions";
import RoomPicker from "./RoomPicker";

type Props = {
  room: RoomDetailResponse;
  onRoomChange: (room: RoomDetailResponse) => void;
};

/** 방의 출구 표와 출구 추가 폼. "반대 방향도" 체크는 추가와 삭제 모두에 쓴다. */
export default function ExitEditor({ room, onRoomChange }: Props) {
  const [direction, setDirection] = useState<DirectionCode | "">("");
  const [targetRoomId, setTargetRoomId] = useState<number | null>(null);
  const [twoWay, setTwoWay] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleAdd(event: FormEvent) {
    event.preventDefault();
    if (direction === "" || targetRoomId === null) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      onRoomChange(await putExit(room.id, direction, { targetRoomId, twoWay }));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete(exit: ExitResponse) {
    const question = `${exit.directionName} 출구를 지울까요?${twoWay ? " 반대 방향 출구도 함께 지웁니다." : ""}`;
    if (!window.confirm(question)) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await deleteExit(room.id, exit.direction, twoWay);
      onRoomChange(await getRoom(room.id));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section>
      <h2>출구</h2>
      {room.exits.length === 0 ? (
        <p className="muted">출구: 없음</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>방위</th>
              <th>대상 방 (지역)</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {room.exits.map((exit) => (
              <tr key={exit.direction}>
                <td>{exit.directionName}</td>
                <td>
                  <Link href={`/room/?id=${exit.targetRoomId}`}>{`${exit.targetRoomName} (${exit.targetZoneName})`}</Link>
                </td>
                <td>
                  <button type="button" disabled={busy} onClick={() => handleDelete(exit)}>
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
          방위
          <select value={direction} onChange={(e) => setDirection(e.target.value as DirectionCode | "")}>
            <option value="">방위 선택</option>
            {DIRECTIONS.map((d) => (
              <option key={d.code} value={d.code}>
                {d.name}
              </option>
            ))}
          </select>
        </label>
        <RoomPicker
          value={targetRoomId}
          onChange={setTargetRoomId}
          zoneLabel="대상 지역"
          roomLabel="대상 방"
          zonePlaceholder="지역 선택"
          roomPlaceholder="방 선택"
          excludeRoomId={room.id}
          onError={setError}
        />
        <label className="checkbox">
          <input type="checkbox" checked={twoWay} onChange={(e) => setTwoWay(e.target.checked)} />
          반대 방향도
        </label>
        <button type="submit" disabled={busy || direction === "" || targetRoomId === null}>
          출구 추가
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
