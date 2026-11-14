"use client";

import { type FormEvent, useState } from "react";
import {
  deleteShop,
  errorMessage,
  type ItemTypeResponse,
  type NpcDetailResponse,
  saveShop,
  type ShopKindCode,
} from "../lib/api";
import { formatCount, SHOP_KINDS } from "../lib/items";

/** 상점에 둘 수 있는 물건 수 */
const MAX_ROWS = 50;

/** price 가 빈칸이면 기준가로 판다 */
type Row = { itemTypeId: number; price: string };

type Props = {
  npc: NpcDetailResponse;
  /** 아이템 종류 전체(서버 목록 순서). 물건 선택에는 상점 종류가 파는 종류만 보인다 */
  itemTypes: ItemTypeResponse[];
  onNpcChange: (npc: NpcDetailResponse) => void;
};

/** NPC의 상점 역할. `상점 아님` 으로 저장하면 상점을 없앤다. 값 검사는 서버에 맡기고 서버의 detail 을 보인다 */
export default function ShopEditor({ npc, itemTypes, onNpcChange }: Props) {
  const [kind, setKind] = useState<ShopKindCode | "">(npc.shop?.kind ?? "");
  const [rows, setRows] = useState<Row[]>(() =>
    (npc.shop?.items ?? []).map((item) => ({
      itemTypeId: item.itemTypeId,
      price: item.price === null ? "" : String(item.price),
    })),
  );
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const sells = SHOP_KINDS.find((k) => k.code === kind)?.sells ?? [];
  const sellable = itemTypes.filter((itemType) => sells.includes(itemType.category));

  function update(next: Row[]) {
    setRows(next);
    setSaved(false);
  }

  function change(index: number, patch: Partial<Row>) {
    update(rows.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  }

  /** 종류를 바꿔 이제 팔 수 없게 된 물건도 그 행에서는 보이게 둔다(저장하면 서버가 알려 준다) */
  function optionsFor(row: Row): ItemTypeResponse[] {
    if (sellable.some((itemType) => itemType.id === row.itemTypeId)) {
      return sellable;
    }
    const current = itemTypes.find((itemType) => itemType.id === row.itemTypeId);
    return current ? [current, ...sellable] : sellable;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setSaved(false);
    try {
      if (kind === "") {
        await deleteShop(npc.id);
        onNpcChange({ ...npc, shop: null });
        setRows([]);
      } else {
        onNpcChange(
          await saveShop(npc.id, {
            kind,
            items: rows.map((row) => ({
              itemTypeId: row.itemTypeId,
              price: row.price.trim() === "" ? null : Number(row.price),
            })),
          }),
        );
      }
      setSaved(true);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section>
      <h2>상점</h2>
      <form className="form" onSubmit={handleSubmit}>
        <label>
          상점 종류
          <select
            value={kind}
            onChange={(e) => {
              setKind(e.target.value as ShopKindCode | "");
              setSaved(false);
            }}
          >
            <option value="">상점 아님</option>
            {SHOP_KINDS.map((shopKind) => (
              <option key={shopKind.code} value={shopKind.code}>
                {shopKind.name}
              </option>
            ))}
          </select>
        </label>
        {kind !== "" && (
          <>
            {rows.length > 0 && (
              <table>
                <tbody>
                  {rows.map((row, index) => {
                    const itemType = itemTypes.find((t) => t.id === row.itemTypeId);
                    return (
                      // 행에 따로 id 가 없고 값은 모두 상태로 그리므로 순서 키로 충분하다.
                      <tr key={index}>
                        <td>
                          <label>
                            물건
                            <select
                              value={row.itemTypeId}
                              onChange={(e) => change(index, { itemTypeId: Number(e.target.value) })}
                            >
                              {optionsFor(row).map((option) => (
                                <option key={option.id} value={option.id}>
                                  {option.name}
                                </option>
                              ))}
                            </select>
                          </label>
                        </td>
                        <td>
                          <label>
                            값 (하품)
                            <input
                              type="number"
                              min={1}
                              value={row.price}
                              placeholder={itemType ? `기준가 ${formatCount(itemType.baseValue)}` : undefined}
                              onChange={(e) => change(index, { price: e.target.value })}
                            />
                          </label>
                        </td>
                        <td>
                          <button type="button" onClick={() => update(rows.filter((_, i) => i !== index))}>
                            삭제
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
            <div className="actions">
              <button
                type="button"
                disabled={rows.length >= MAX_ROWS || sellable.length === 0}
                onClick={() => update([...rows, { itemTypeId: sellable[0].id, price: "" }])}
              >
                물건 추가
              </button>
            </div>
          </>
        )}
        <p className="muted">값을 비워 두면 기준가로 팝니다. 사 주는 값은 룰북 매입 규칙으로 정해집니다.</p>
        <div className="actions">
          <button type="submit" disabled={submitting}>
            상점 저장
          </button>
        </div>
        {saved && <p className="muted">저장했습니다.</p>}
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
      </form>
    </section>
  );
}
