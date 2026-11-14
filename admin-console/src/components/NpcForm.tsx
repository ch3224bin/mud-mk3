"use client";

import { type FormEvent, useState } from "react";
import { errorMessage, type NpcDetailResponse, type NpcSaveRequest, type NpcTopic } from "../lib/api";
import RoomPicker from "./RoomPicker";
import TopicEditor, { withoutEmptyRows } from "./TopicEditor";

/** 새 NPC 폼의 기본 대답(룰북 예) */
const DEFAULT_ANSWER = "글쎄, 잘 모르겠구려.";

type Props = {
  /** 있으면 고치는 폼, 없으면 새로 만드는 폼 */
  initial?: NpcDetailResponse;
  onSubmit: (request: NpcSaveRequest) => Promise<void>;
};

/** NPC 이름·대사·낱말 대답·LLM 대화·배치 폼. 값 검사는 서버에 맡기고 서버의 detail 을 보인다 */
export default function NpcForm({ initial, onSubmit }: Props) {
  const [name, setName] = useState(initial?.name ?? "");
  const [greeting, setGreeting] = useState(initial?.greeting ?? "");
  const [defaultAnswer, setDefaultAnswer] = useState(initial?.defaultAnswer ?? DEFAULT_ANSWER);
  const [topics, setTopics] = useState<NpcTopic[]>(initial?.topics ?? []);
  const [llmEnabled, setLlmEnabled] = useState(initial?.llmEnabled ?? false);
  const [roomId, setRoomId] = useState<number | null>(initial?.roomId ?? null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit({ name, greeting, defaultAnswer, topics: withoutEmptyRows(topics), llmEnabled, roomId });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <label>
        이름
        <input value={name} maxLength={12} onChange={(e) => setName(e.target.value)} />
      </label>
      <label>
        인사말
        <textarea value={greeting} maxLength={500} rows={3} onChange={(e) => setGreeting(e.target.value)} />
      </label>
      <label>
        기본 대답
        <textarea value={defaultAnswer} maxLength={500} rows={2} onChange={(e) => setDefaultAnswer(e.target.value)} />
      </label>
      <h2>낱말</h2>
      <TopicEditor topics={topics} onChange={setTopics} />
      <label className="checkbox">
        <input type="checkbox" checked={llmEnabled} onChange={(e) => setLlmEnabled(e.target.checked)} />
        LLM 대화
      </label>
      <p className="muted">켜 두어도 지금은 정해진 대사로 답합니다.</p>
      <fieldset>
        <legend>배치</legend>
        <RoomPicker
          value={roomId}
          onChange={setRoomId}
          initialZoneId={initial?.zoneId ?? null}
          zoneLabel="지역"
          roomLabel="방"
          zonePlaceholder="배치 안 함"
          roomPlaceholder="방 선택"
          onError={setError}
        />
      </fieldset>
      <button type="submit" disabled={submitting}>
        저장
      </button>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </form>
  );
}
