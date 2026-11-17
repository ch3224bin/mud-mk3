"use client";

import { useState } from "react";
import {
  type AbilityDraft,
  DEFAULT_FILL,
  type FillDraft,
  fillStagePowers,
  MAX_EFFECT_LINES,
  type StagePowerDraft,
  stageLabel,
} from "../lib/techniques";

type Props = {
  ability: AbilityDraft;
  /** 공법의 단계 수(채우기가 배우는 단계 ~ 단계 수를 채운다) */
  stageCount: number;
  onChange: (patch: Partial<AbilityDraft>) => void;
};

/** `채우기` 칸(입력 순서) */
const FILL_FIELDS: { key: keyof FillDraft; label: string }[] = [
  { key: "basePower", label: "기본 위력" },
  { key: "coefficient", label: "계수" },
  { key: "baseStep", label: "단계마다 기본 +" },
  { key: "coefficientStep", label: "단계마다 계수 +" },
];

const EFFECT_LINE_GUIDE =
  "쓸 때 하나가 무작위로 보입니다. {대상} · {시전자} 를 쓸 수 있고, 바로 뒤 조사는 받침에 맞춰 바뀝니다. (예: {대상}을 → 청모랑을)";

/**
 * 신통 한 줄의 펼침 칸: 이펙트 문장 1~5줄과, 공격이면 채우기·단계별 값 표.
 * 표의 행은 신통 표가 배우는 단계 ~ 단계 수로 맞춰 넘긴다. 값 검사는 서버에 맡긴다
 */
export default function AbilityDetailEditor({ ability, stageCount, onChange }: Props) {
  const [fill, setFill] = useState<FillDraft>(DEFAULT_FILL);
  const lines = ability.effectLines;

  function changeLine(index: number, value: string) {
    onChange({ effectLines: lines.map((line, i) => (i === index ? value : line)) });
  }

  function changePower(index: number, patch: Partial<StagePowerDraft>) {
    onChange({ stagePowers: ability.stagePowers.map((power, i) => (i === index ? { ...power, ...patch } : power)) });
  }

  return (
    <div>
      <h3>이펙트 문장</h3>
      {lines.map((line, index) => (
        // 줄에 따로 id 가 없고 값은 모두 상태로 그리므로 순서 키로 충분하다.
        <div key={index} className="actions">
          <input
            aria-label={`이펙트 문장 ${index + 1}`}
            value={line}
            size={60}
            onChange={(e) => changeLine(index, e.target.value)}
          />
          <button
            type="button"
            disabled={lines.length <= 1}
            aria-label={`이펙트 문장 ${index + 1} 삭제`}
            onClick={() => onChange({ effectLines: lines.filter((_, i) => i !== index) })}
          >
            삭제
          </button>
        </div>
      ))}
      <div className="actions">
        <button
          type="button"
          disabled={lines.length >= MAX_EFFECT_LINES}
          onClick={() => onChange({ effectLines: [...lines, ""] })}
        >
          문장 추가
        </button>
      </div>
      <p className="muted">{EFFECT_LINE_GUIDE}</p>
      {ability.kind === "ATTACK" && (
        <>
          <h3>단계별 값</h3>
          <fieldset>
            <legend>채우기</legend>
            {FILL_FIELDS.map((field) => (
              <label key={field.key}>
                {field.label}
                <input
                  type="number"
                  step="any"
                  className="number"
                  value={fill[field.key]}
                  onChange={(e) => setFill({ ...fill, [field.key]: e.target.value })}
                />
              </label>
            ))}
            <button
              type="button"
              onClick={() => onChange({ stagePowers: fillStagePowers(ability.learnStage, stageCount, fill) })}
            >
              채우기
            </button>
          </fieldset>
          <table>
            <thead>
              <tr>
                <th>단계</th>
                <th>기본 위력</th>
                <th>계수</th>
              </tr>
            </thead>
            <tbody>
              {ability.stagePowers.map((power, index) => (
                <tr key={power.stage}>
                  <td>{stageLabel(power.stage)}</td>
                  <td>
                    <input
                      type="number"
                      step="any"
                      className="number"
                      aria-label={`${stageLabel(power.stage)} 기본 위력`}
                      value={power.basePower}
                      onChange={(e) => changePower(index, { basePower: e.target.value })}
                    />
                  </td>
                  <td>
                    <input
                      type="number"
                      step="any"
                      className="number"
                      aria-label={`${stageLabel(power.stage)} 계수`}
                      value={power.coefficient}
                      onChange={(e) => changePower(index, { coefficient: e.target.value })}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </div>
  );
}
