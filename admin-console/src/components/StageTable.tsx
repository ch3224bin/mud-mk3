"use client";

import { MAX_STAGES, resizeStages, STAGE_FIELDS, type StageDraft, stageLabel } from "../lib/techniques";

type Props = {
  /** 1성부터 차례로. 길이가 단계 수다 */
  stages: StageDraft[];
  onChange: (stages: StageDraft[]) => void;
};

/**
 * 공법 단계 수와 단계 표. 단계 수를 바꾸면 행이 늘거나 줄고 새 행은 모두 0 이다.
 * 1성은 읽으면 이르므로 필요 깨달음 칸에 입력이 없다. 값 검사는 서버에 맡긴다
 */
export default function StageTable({ stages, onChange }: Props) {
  function change(index: number, key: keyof StageDraft, value: string) {
    onChange(stages.map((stage, i) => (i === index ? { ...stage, [key]: value } : stage)));
  }

  return (
    <>
      <label>
        단계 수
        <select value={stages.length} onChange={(e) => onChange(resizeStages(stages, Number(e.target.value)))}>
          {Array.from({ length: MAX_STAGES }, (_, i) => i + 1).map((count) => (
            <option key={count} value={count}>
              {count}
            </option>
          ))}
        </select>
      </label>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>단계</th>
              {STAGE_FIELDS.map((field) => (
                <th key={field.key}>{field.label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {stages.map((stage, index) => (
              // 행은 단계 순서 그 자체라 순서 키로 충분하다.
              <tr key={index}>
                <td>{stageLabel(index + 1)}</td>
                {STAGE_FIELDS.map((field) => (
                  <td key={field.key}>
                    {index === 0 && field.key === "insightRequired" ? (
                      "읽으면"
                    ) : (
                      <input
                        type="number"
                        step="any"
                        className="number"
                        aria-label={`${stageLabel(index + 1)} ${field.label}`}
                        value={stage[field.key]}
                        onChange={(e) => change(index, field.key, e.target.value)}
                      />
                    )}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
