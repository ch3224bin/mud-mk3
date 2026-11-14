"use client";

import { type FormEvent, useState } from "react";
import {
  createFloorItem,
  deleteFloorItem,
  errorMessage,
  type FloorPlacementResponse,
  type FloorPlacementSaveRequest,
  type ItemTypeResponse,
  type RoomDetailResponse,
  updateFloorItem,
} from "../lib/api";
import { formatCount } from "../lib/items";

/** 두기 폼의 재등장 기본값(분). 유니크 NPC 재등장 기본 10분(npc.md)과 같다 */
const DEFAULT_RESPAWN_MINUTES = "10";

type Draft = { itemTypeId: number; count: string; respawnMinutes: string };

type Props = {
  room: RoomDetailResponse;
  /** 물건 선택에 보일 아이템 종류(서버 목록 순서) */
  itemTypes: ItemTypeResponse[];
  onRoomChange: (room: RoomDetailResponse) => void;
};

/** 재등장을 비우면 한 번뿐(null) */
function toRequest(draft: Draft): FloorPlacementSaveRequest {
  return {
    itemTypeId: draft.itemTypeId,
    count: Number(draft.count),
    respawnMinutes: draft.respawnMinutes.trim() === "" ? null : Number(draft.respawnMinutes),
  };
}

function toDraft(placement: FloorPlacementResponse): Draft {
  return {
    itemTypeId: placement.itemTypeId,
    count: String(placement.count),
    respawnMinutes: placement.respawnMinutes === null ? "" : String(placement.respawnMinutes),
  };
}

function respawnLabel(respawnMinutes: number | null): string {
  return respawnMinutes === null ? "한 번뿐" : `${formatCount(respawnMinutes)}분`;
}

/** 방의 바닥 물건 표와 두기 폼. 값 검사는 서버에 맡기고 서버의 detail 을 보인다 */
export default function FloorItemsEditor({ room, itemTypes, onRoomChange }: Props) {
  const [draft, setDraft] = useState<Draft>(() => ({
    itemTypeId: itemTypes[0]?.id ?? 0,
    count: "1",
    respawnMinutes: DEFAULT_RESPAWN_MINUTES,
  }));
  const [editing, setEditing] = useState<{ id: number; draft: Draft } | null>(null);
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

  function handleAdd(event: FormEvent) {
    event.preventDefault();
    return run(async () => {
      const created = await createFloorItem(room.id, toRequest(draft));
      onRoomChange({ ...room, floorItems: [...room.floorItems, created] });
    });
  }

  function handleUpdate(event: FormEvent) {
    event.preventDefault();
    if (editing === null) {
      return;
    }
    return run(async () => {
      const updated = await updateFloorItem(editing.id, toRequest(editing.draft));
      onRoomChange({ ...room, floorItems: room.floorItems.map((p) => (p.id === updated.id ? updated : p)) });
      setEditing(null);
    });
  }

  function handleDelete(placement: FloorPlacementResponse) {
    return run(async () => {
      await deleteFloorItem(placement.id);
      onRoomChange({ ...room, floorItems: room.floorItems.filter((p) => p.id !== placement.id) });
      if (editing?.id === placement.id) {
        setEditing(null);
      }
    });
  }

  return (
    <section>
      <h2>바닥 물건</h2>
      {room.floorItems.length === 0 ? (
        <p className="muted">없음</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>물건</th>
              <th>수</th>
              <th>남은 수</th>
              <th>재등장</th>
              <th />
              <th />
            </tr>
          </thead>
          <tbody>
            {room.floorItems.map((placement) =>
              editing?.id === placement.id ? (
                <tr key={placement.id}>
                  <td colSpan={4}>
                    <form id={`floor-item-${placement.id}`} className="form inline" onSubmit={handleUpdate}>
                      <DraftFields
                        itemTypes={itemTypes}
                        draft={editing.draft}
                        onChange={(next) => setEditing({ id: placement.id, draft: next })}
                      />
                    </form>
                  </td>
                  <td>
                    <button type="submit" form={`floor-item-${placement.id}`} disabled={busy}>
                      저장
                    </button>
                  </td>
                  <td>
                    <button type="button" disabled={busy} onClick={() => setEditing(null)}>
                      취소
                    </button>
                  </td>
                </tr>
              ) : (
                <tr key={placement.id}>
                  <td>{placement.itemName}</td>
                  <td>{formatCount(placement.count)}</td>
                  <td>{formatCount(placement.remaining)}</td>
                  <td>{respawnLabel(placement.respawnMinutes)}</td>
                  <td>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => setEditing({ id: placement.id, draft: toDraft(placement) })}
                    >
                      수정
                    </button>
                  </td>
                  <td>
                    <button type="button" disabled={busy} onClick={() => handleDelete(placement)}>
                      삭제
                    </button>
                  </td>
                </tr>
              ),
            )}
          </tbody>
        </table>
      )}

      {editing === null && (
        <form className="form inline" onSubmit={handleAdd}>
          <DraftFields itemTypes={itemTypes} draft={draft} onChange={setDraft} />
          <button type="submit" disabled={busy || itemTypes.length === 0}>
            바닥에 두기
          </button>
        </form>
      )}
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </section>
  );
}

function DraftFields({
  itemTypes,
  draft,
  onChange,
}: {
  itemTypes: ItemTypeResponse[];
  draft: Draft;
  onChange: (draft: Draft) => void;
}) {
  return (
    <>
      <label>
        물건
        <select value={draft.itemTypeId} onChange={(e) => onChange({ ...draft, itemTypeId: Number(e.target.value) })}>
          {itemTypes.map((itemType) => (
            <option key={itemType.id} value={itemType.id}>
              {itemType.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        수
        <input
          type="number"
          min={1}
          value={draft.count}
          onChange={(e) => onChange({ ...draft, count: e.target.value })}
        />
      </label>
      <label>
        재등장(분)
        <input
          type="number"
          min={1}
          value={draft.respawnMinutes}
          onChange={(e) => onChange({ ...draft, respawnMinutes: e.target.value })}
        />
      </label>
    </>
  );
}
