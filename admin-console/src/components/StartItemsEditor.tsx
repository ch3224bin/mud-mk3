"use client";

import { type FormEvent, useState } from "react";
import { errorMessage, type ItemTypeResponse, type StartingItem } from "../lib/api";

/** 시작 소지품에 둘 수 있는 줄 수 */
const MAX_ROWS = 30;

type Row = { itemTypeId: number; count: string };

type Props = {
  /** 물건 선택에 보일 아이템 종류(서버 목록 순서) */
  itemTypes: ItemTypeResponse[];
  initial: StartingItem[];
  onSave: (items: StartingItem[]) => Promise<void>;
};

/** 시작 소지품 표. 행마다 물건·수·삭제, 아래에 물건 추가·저장. 값 검사는 서버에 맡기고 서버의 detail 을 보인다 */
export default function StartItemsEditor({ itemTypes, initial, onSave }: Props) {
  const [rows, setRows] = useState<Row[]>(() =>
    initial.map((item) => ({ itemTypeId: item.itemTypeId, count: String(item.count) })),
  );
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  function update(next: Row[]) {
    setRows(next);
    setSaved(false);
  }

  function change(index: number, patch: Partial<Row>) {
    update(rows.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setSaved(false);
    try {
      await onSave(rows.map((row) => ({ itemTypeId: row.itemTypeId, count: Number(row.count) })));
      setSaved(true);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <p className="muted">새로 만드는 캐릭터부터 받습니다.</p>
      {rows.length === 0 ? (
        <p className="muted">시작 소지품이 없습니다.</p>
      ) : (
        <table>
          <tbody>
            {rows.map((row, index) => (
              // 행에 따로 id 가 없고 값은 모두 상태로 그리므로 순서 키로 충분하다.
              <tr key={index}>
                <td>
                  <label>
                    물건
                    <select
                      value={row.itemTypeId}
                      onChange={(e) => change(index, { itemTypeId: Number(e.target.value) })}
                    >
                      {itemTypes.map((itemType) => (
                        <option key={itemType.id} value={itemType.id}>
                          {itemType.name}
                        </option>
                      ))}
                    </select>
                  </label>
                </td>
                <td>
                  <label>
                    수
                    <input
                      type="number"
                      min={1}
                      value={row.count}
                      onChange={(e) => change(index, { count: e.target.value })}
                    />
                  </label>
                </td>
                <td>
                  <button type="button" onClick={() => update(rows.filter((_, i) => i !== index))}>
                    삭제
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <div className="actions">
        <button
          type="button"
          disabled={rows.length >= MAX_ROWS || itemTypes.length === 0}
          onClick={() => update([...rows, { itemTypeId: itemTypes[0].id, count: "1" }])}
        >
          물건 추가
        </button>
        <button type="submit" disabled={submitting}>
          저장
        </button>
      </div>
      {saved && <p className="muted">저장했습니다.</p>}
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </form>
  );
}
