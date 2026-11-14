"use client";

import { useEffect, useState } from "react";
import { errorMessage, listRooms, listZones, type RoomSummaryResponse, type ZoneResponse } from "../lib/api";

type Props = {
  /** 고른 방 id. 방을 고르지 않았으면 null */
  value: number | null;
  onChange: (roomId: number | null) => void;
  /** 처음 열 때 골라 둘 지역(value 방이 있는 지역) */
  initialZoneId?: number | null;
  zoneLabel: string;
  roomLabel: string;
  /** 지역 선택의 첫 항목 문구 */
  zonePlaceholder: string;
  /** 방 선택의 첫 항목 문구 */
  roomPlaceholder: string;
  /** 방 목록에서 뺄 방(출구 편집에서 지금 방) */
  excludeRoomId?: number;
  onError: (message: string) => void;
};

/** 지역을 고른 뒤 그 지역의 방을 고르는 선택 상자 둘. 지역을 바꾸면 고른 방은 비운다 */
export default function RoomPicker({
  value,
  onChange,
  initialZoneId = null,
  zoneLabel,
  roomLabel,
  zonePlaceholder,
  roomPlaceholder,
  excludeRoomId,
  onError,
}: Props) {
  const [zones, setZones] = useState<ZoneResponse[]>([]);
  const [zoneId, setZoneId] = useState<number | null>(initialZoneId);
  const [rooms, setRooms] = useState<RoomSummaryResponse[]>([]);

  useEffect(() => {
    listZones()
      .then(setZones)
      .catch((e) => onError(errorMessage(e)));
    // onError 는 부모가 그릴 때마다 새로 만들어질 수 있으므로 의존 목록에 넣지 않는다.
  }, []);

  useEffect(() => {
    setRooms([]);
    if (zoneId === null) {
      return;
    }
    let active = true;
    listRooms(zoneId)
      .then((loaded) => active && setRooms(loaded.filter((r) => r.id !== excludeRoomId)))
      .catch((e) => active && onError(errorMessage(e)));
    return () => {
      active = false;
    };
  }, [zoneId, excludeRoomId]);

  function handleZoneChange(next: number | null) {
    setZoneId(next);
    onChange(null);
  }

  return (
    <>
      <label>
        {zoneLabel}
        <select value={zoneId ?? ""} onChange={(e) => handleZoneChange(e.target.value ? Number(e.target.value) : null)}>
          <option value="">{zonePlaceholder}</option>
          {zones.map((zone) => (
            <option key={zone.id} value={zone.id}>
              {zone.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        {roomLabel}
        <select value={value ?? ""} onChange={(e) => onChange(e.target.value ? Number(e.target.value) : null)}>
          <option value="">{roomPlaceholder}</option>
          {rooms.map((room) => (
            <option key={room.id} value={room.id}>
              {room.name}
            </option>
          ))}
        </select>
      </label>
    </>
  );
}
