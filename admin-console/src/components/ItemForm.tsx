"use client";

import { type FormEvent, useState } from "react";
import {
  type BagGradeCode,
  errorMessage,
  type ItemCategoryCode,
  type ItemTypeResponse,
  type ItemTypeSaveRequest,
} from "../lib/api";
import { BAG_GRADES, bagGradeLabel, ITEM_CATEGORIES } from "../lib/items";

type Props = {
  /** 있으면 고치는 폼, 없으면 새로 만드는 폼 */
  initial?: ItemTypeResponse;
  onSubmit: (request: ItemTypeSaveRequest) => Promise<void>;
};

/**
 * 아이템 종류 폼. 종류 선택에 영석은 없고, 저물대 등급은 종류가 저물대일 때만 보인다.
 * 영석이면 모든 칸이 읽기 전용이다. 값 검사는 서버에 맡기고 서버의 detail 을 보인다
 */
export default function ItemForm({ initial, onSubmit }: Props) {
  const readOnly = initial?.spiritStone ?? false;
  const [name, setName] = useState(initial?.name ?? "");
  const [category, setCategory] = useState<ItemCategoryCode>(initial?.category ?? ITEM_CATEGORIES[0].code);
  const [bagGrade, setBagGrade] = useState<BagGradeCode>(initial?.bagGrade ?? BAG_GRADES[0].code);
  const [description, setDescription] = useState(initial?.description ?? "");
  const [baseValue, setBaseValue] = useState(String(initial?.baseValue ?? 0));
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // 관리자가 만들 수 있는 종류만 고르게 한다. 영석을 열었을 때만 지금 종류를 함께 보인다.
  const categories = ITEM_CATEGORIES.filter((c) => c.creatable || c.code === initial?.category);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit({
        name,
        description,
        category,
        baseValue: Number(baseValue),
        bagGrade: category === "BAG" ? bagGrade : null,
      });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      {readOnly && <p className="muted">영석은 엔진이 정한 물건이라 고칠 수 없습니다.</p>}
      <label>
        이름
        <input value={name} maxLength={20} disabled={readOnly} onChange={(e) => setName(e.target.value)} />
      </label>
      <label>
        종류
        <select
          value={category}
          disabled={readOnly}
          onChange={(e) => setCategory(e.target.value as ItemCategoryCode)}
        >
          {categories.map((c) => (
            <option key={c.code} value={c.code}>
              {c.name}
            </option>
          ))}
        </select>
      </label>
      {category === "BAG" && (
        <label>
          저물대 등급
          <select value={bagGrade} disabled={readOnly} onChange={(e) => setBagGrade(e.target.value as BagGradeCode)}>
            {BAG_GRADES.map((grade) => (
              <option key={grade.code} value={grade.code}>
                {bagGradeLabel(grade.code)}
              </option>
            ))}
          </select>
        </label>
      )}
      <label>
        묘사
        <textarea
          value={description}
          maxLength={500}
          rows={3}
          disabled={readOnly}
          onChange={(e) => setDescription(e.target.value)}
        />
      </label>
      <label>
        기준가 (하품)
        <input
          type="number"
          min={0}
          value={baseValue}
          disabled={readOnly}
          onChange={(e) => setBaseValue(e.target.value)}
        />
      </label>
      {!readOnly && (
        <button type="submit" disabled={submitting}>
          저장
        </button>
      )}
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </form>
  );
}
