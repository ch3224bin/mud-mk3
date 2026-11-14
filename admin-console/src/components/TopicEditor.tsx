"use client";

import type { NpcTopic } from "../lib/api";

/** NPC 한 명이 가질 수 있는 낱말 대답 수 */
const MAX_TOPICS = 30;

type Props = {
  topics: NpcTopic[];
  onChange: (topics: NpcTopic[]) => void;
};

/** 낱말과 대답이 둘 다 빈 행을 뺀다. 저장할 때 쓴다 */
export function withoutEmptyRows(topics: NpcTopic[]): NpcTopic[] {
  return topics.filter((topic) => topic.word.trim() !== "" || topic.answer.trim() !== "");
}

/** NPC 낱말 대답 표. 행마다 낱말·대답·삭제, 아래에 낱말 추가 */
export default function TopicEditor({ topics, onChange }: Props) {
  function change(index: number, patch: Partial<NpcTopic>) {
    onChange(topics.map((topic, i) => (i === index ? { ...topic, ...patch } : topic)));
  }

  return (
    <section>
      {topics.length > 0 && (
        <table>
          <tbody>
            {topics.map((topic, index) => (
              // 행에 따로 id 가 없고 값은 모두 props 로 그리므로 순서 키로 충분하다.
              <tr key={index}>
                <td>
                  <label>
                    낱말
                    <input value={topic.word} maxLength={22} onChange={(e) => change(index, { word: e.target.value })} />
                  </label>
                </td>
                <td>
                  <label>
                    대답
                    <textarea
                      value={topic.answer}
                      maxLength={1000}
                      rows={2}
                      onChange={(e) => change(index, { answer: e.target.value })}
                    />
                  </label>
                </td>
                <td>
                  <button type="button" onClick={() => onChange(topics.filter((_, i) => i !== index))}>
                    삭제
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <button
        type="button"
        disabled={topics.length >= MAX_TOPICS}
        onClick={() => onChange([...topics, { word: "", answer: "" }])}
      >
        낱말 추가
      </button>
    </section>
  );
}
