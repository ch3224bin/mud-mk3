"use client";

import { Fragment, useState } from "react";
import type { AbilityKindCode, AbilityTargetCode, CostStatCode, UsableWhenCode } from "../lib/api";
import {
  ABILITY_KINDS,
  ABILITY_TARGETS,
  type AbilityDraft,
  COST_STATS,
  ELEMENTS,
  MAX_ABILITIES,
  newAbility,
  resizeStagePowers,
  stageLabel,
  toggleElement,
  USABLE_WHENS,
} from "../lib/techniques";
import AbilityDetailEditor from "./AbilityDetailEditor";

type Props = {
  /** 정의 순서 = `신통` 목록 순서 */
  abilities: AbilityDraft[];
  /** 배우는 단계 선택지(1성 ~ 단계 수) */
  stageCount: number;
  onChange: (abilities: AbilityDraft[]) => void;
};

/** 표의 열 수(펼친 칸이 한 줄을 다 쓴다) */
const COLUMN_COUNT = 10;

/**
 * 공법의 신통 표. `신통 추가` 의 새 행은 1성·공격·속성 없음·한 대상·소모 없음·시전 0·대기 3·전투 중만,
 * 이펙트 문장 빈 줄 하나, 단계별 값은 채우기 기본값이다. 행마다 `자세히` 로 그 행 아래에 이펙트 문장·단계별 값 칸을 편다.
 * 배우는 단계·종류를 바꾸면 단계별 값 행을 다시 맞춘다(공격이 아니면 숨기기만 하고 값은 남긴다).
 * 단계 수를 줄여 배우는 단계가 넘친 행은 그 값을 남겨 두고 저장하면 서버가 알려 준다
 */
export default function AbilityTable({ abilities, stageCount, onChange }: Props) {
  const [expanded, setExpanded] = useState<ReadonlySet<number>>(new Set());

  function change(index: number, patch: Partial<AbilityDraft>) {
    onChange(
      abilities.map((ability, i) => {
        if (i !== index) {
          return ability;
        }
        const next = { ...ability, ...patch };
        return "learnStage" in patch || "kind" in patch
          ? { ...next, stagePowers: resizeStagePowers(next.stagePowers, next.learnStage, stageCount) }
          : next;
      }),
    );
  }

  function toggle(index: number) {
    const next = new Set(expanded);
    if (!next.delete(index)) {
      next.add(index);
    }
    setExpanded(next);
  }

  function remove(index: number) {
    // 지운 행 뒤의 펼침 표시는 한 칸씩 당긴다.
    setExpanded(new Set([...expanded].filter((i) => i !== index).map((i) => (i > index ? i - 1 : i))));
    onChange(abilities.filter((_, i) => i !== index));
  }

  function stageOptions(ability: AbilityDraft): number[] {
    const stages = Array.from({ length: stageCount }, (_, i) => i + 1);
    return ability.learnStage > stageCount ? [...stages, ability.learnStage] : stages;
  }

  return (
    <>
      {abilities.length === 0 ? (
        <p className="muted">신통이 없습니다.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>이름</th>
                <th>배우는 단계</th>
                <th>종류</th>
                <th>속성</th>
                <th>대상</th>
                <th>소모</th>
                <th>시전(초)</th>
                <th>대기(초)</th>
                <th>쓸 수 있는 때</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {abilities.map((ability, index) => (
                // 행에 따로 id 가 없고 값은 모두 상태로 그리므로 순서 키로 충분하다.
                <Fragment key={index}>
                  <tr>
                    <td>
                      <input
                        aria-label="신통 이름"
                        value={ability.name}
                        maxLength={12}
                        onChange={(e) => change(index, { name: e.target.value })}
                      />
                    </td>
                    <td>
                      <select
                        aria-label="배우는 단계"
                        value={ability.learnStage}
                        onChange={(e) => change(index, { learnStage: Number(e.target.value) })}
                      >
                        {stageOptions(ability).map((stage) => (
                          <option key={stage} value={stage}>
                            {stageLabel(stage)}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <select
                        aria-label="종류"
                        value={ability.kind}
                        onChange={(e) => change(index, { kind: e.target.value as AbilityKindCode })}
                      >
                        {ABILITY_KINDS.map((kind) => (
                          <option key={kind.code} value={kind.code}>
                            {kind.name}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <span className="checks" role="group" aria-label="속성">
                        {ELEMENTS.map((element) => (
                          <label key={element.code} className="checkbox">
                            <input
                              type="checkbox"
                              checked={ability.elements.includes(element.code)}
                              onChange={(e) =>
                                change(index, { elements: toggleElement(ability.elements, element.code, e.target.checked) })
                              }
                            />
                            {element.name}
                          </label>
                        ))}
                        <label className="checkbox">
                          <input
                            type="checkbox"
                            checked={ability.bodyRefining}
                            onChange={(e) => change(index, { bodyRefining: e.target.checked })}
                          />
                          연체
                        </label>
                      </span>
                    </td>
                    <td>
                      <select
                        aria-label="대상"
                        value={ability.target}
                        onChange={(e) => change(index, { target: e.target.value as AbilityTargetCode })}
                      >
                        {ABILITY_TARGETS.map((target) => (
                          <option key={target.code} value={target.code}>
                            {target.name}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <span className="checks">
                        <select
                          aria-label="소모"
                          value={ability.costStat}
                          onChange={(e) => change(index, { costStat: e.target.value as CostStatCode })}
                        >
                          {COST_STATS.map((stat) => (
                            <option key={stat.code} value={stat.code}>
                              {stat.name}
                            </option>
                          ))}
                        </select>
                        <input
                          type="number"
                          step="any"
                          className="number"
                          aria-label="소모 양"
                          value={ability.costAmount}
                          onChange={(e) => change(index, { costAmount: e.target.value })}
                        />
                        <label className="checkbox">
                          <input
                            type="checkbox"
                            checked={ability.costPerTick}
                            onChange={(e) => change(index, { costPerTick: e.target.checked })}
                          />
                          틱마다
                        </label>
                      </span>
                    </td>
                    <td>
                      <input
                        type="number"
                        step="any"
                        className="number"
                        aria-label="시전(초)"
                        value={ability.castSeconds}
                        onChange={(e) => change(index, { castSeconds: e.target.value })}
                      />
                    </td>
                    <td>
                      <input
                        type="number"
                        step="any"
                        className="number"
                        aria-label="대기(초)"
                        value={ability.cooldownSeconds}
                        onChange={(e) => change(index, { cooldownSeconds: e.target.value })}
                      />
                    </td>
                    <td>
                      <select
                        aria-label="쓸 수 있는 때"
                        value={ability.usableWhen}
                        onChange={(e) => change(index, { usableWhen: e.target.value as UsableWhenCode })}
                      >
                        {USABLE_WHENS.map((when) => (
                          <option key={when.code} value={when.code}>
                            {when.name}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <span className="checks">
                        <button type="button" onClick={() => toggle(index)}>
                          {expanded.has(index) ? "접기" : "자세히"}
                        </button>
                        <button
                          type="button"
                          onClick={() => remove(index)}
                          aria-label={ability.name ? `${ability.name} 삭제` : `신통 ${index + 1} 삭제`}
                        >
                          삭제
                        </button>
                        {ability.usableInGame === false && <span className="muted">(게임에서 아직 쓸 수 없음)</span>}
                      </span>
                    </td>
                  </tr>
                  {expanded.has(index) && (
                    <tr>
                      <td colSpan={COLUMN_COUNT}>
                        <AbilityDetailEditor
                          ability={ability}
                          stageCount={stageCount}
                          onChange={(patch) => change(index, patch)}
                        />
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <div className="actions">
        <button
          type="button"
          disabled={abilities.length >= MAX_ABILITIES}
          onClick={() => onChange([...abilities, newAbility(stageCount)])}
        >
          신통 추가
        </button>
      </div>
    </>
  );
}
