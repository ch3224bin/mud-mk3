"use client";

import type { DropTierCode, ItemTypeResponse } from "../lib/api";
import { DROP_TIERS, type DropTableDraft } from "../lib/mobs";

type Props = {
  /** 물건 선택에 보일 아이템 종류(서버 목록 순서) */
  itemTypes: ItemTypeResponse[];
  value: DropTableDraft;
  onChange: (value: DropTableDraft) => void;
};

/**
 * 몹 드롭 표: 고정 재료 표(물건·수·삭제, `고정 재료 추가`)와 계층 드롭 세 칸(고급·희귀·진귀, 물건 목록·삭제, `물건 추가`).
 * 개수·수 검사는 서버에 맡긴다
 */
export default function DropTableEditor({ itemTypes, value, onChange }: Props) {
  const firstItemId = itemTypes[0]?.id;

  function changeFixed(index: number, patch: Partial<DropTableDraft["fixedDrops"][number]>) {
    onChange({ ...value, fixedDrops: value.fixedDrops.map((row, i) => (i === index ? { ...row, ...patch } : row)) });
  }

  function changeTier(tier: DropTierCode, items: number[]) {
    onChange({ ...value, tierDrops: { ...value.tierDrops, [tier]: items } });
  }

  return (
    <section>
      <h2>고정 재료</h2>
      {value.fixedDrops.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>물건</th>
              <th>수</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {value.fixedDrops.map((row, index) => (
              // 행에 따로 id 가 없고 값은 모두 상태로 그리므로 순서 키로 충분하다.
              <tr key={index}>
                <td>
                  <ItemSelect
                    label={`고정 재료 ${index + 1} 물건`}
                    itemTypes={itemTypes}
                    value={row.itemTypeId}
                    onChange={(itemTypeId) => changeFixed(index, { itemTypeId })}
                  />
                </td>
                <td>
                  <input
                    type="number"
                    min={1}
                    aria-label={`고정 재료 ${index + 1} 수`}
                    value={row.count}
                    onChange={(e) => changeFixed(index, { count: e.target.value })}
                  />
                </td>
                <td>
                  <button
                    type="button"
                    onClick={() => onChange({ ...value, fixedDrops: value.fixedDrops.filter((_, i) => i !== index) })}
                  >
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
          disabled={firstItemId === undefined}
          onClick={() =>
            firstItemId !== undefined &&
            onChange({ ...value, fixedDrops: [...value.fixedDrops, { itemTypeId: firstItemId, count: "1" }] })
          }
        >
          고정 재료 추가
        </button>
      </div>
      <h2>계층 드롭</h2>
      {DROP_TIERS.map((tier) => {
        const items = value.tierDrops[tier.code];
        return (
          <fieldset key={tier.code}>
            <legend>{tier.name}</legend>
            {items.map((itemTypeId, index) => (
              <div key={index} className="form inline">
                <ItemSelect
                  label={`${tier.name} ${index + 1} 물건`}
                  itemTypes={itemTypes}
                  value={itemTypeId}
                  onChange={(next) => changeTier(tier.code, items.map((id, i) => (i === index ? next : id)))}
                />
                <button type="button" onClick={() => changeTier(tier.code, items.filter((_, i) => i !== index))}>
                  삭제
                </button>
              </div>
            ))}
            <button
              type="button"
              disabled={firstItemId === undefined}
              onClick={() => firstItemId !== undefined && changeTier(tier.code, [...items, firstItemId])}
            >
              물건 추가
            </button>
          </fieldset>
        );
      })}
    </section>
  );
}

function ItemSelect({
  label,
  itemTypes,
  value,
  onChange,
}: {
  label: string;
  itemTypes: ItemTypeResponse[];
  value: number;
  onChange: (itemTypeId: number) => void;
}) {
  return (
    <select aria-label={label} value={value} onChange={(e) => onChange(Number(e.target.value))}>
      {itemTypes.map((itemType) => (
        <option key={itemType.id} value={itemType.id}>
          {itemType.name}
        </option>
      ))}
    </select>
  );
}
