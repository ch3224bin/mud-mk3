"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import {
  createTechnique,
  deleteTechnique,
  type ElementCode,
  errorMessage,
  type RealmCode,
  type RealmResponse,
  type TechniqueDetailResponse,
  type TechniqueSaveRequest,
  updateTechnique,
} from "../lib/api";
import {
  abilityDraftOf,
  type AbilityDraft,
  DEFAULT_STAGE_COUNT,
  ELEMENTS,
  emptyStage,
  inElementOrder,
  resizeStagePowers,
  type StageDraft,
  stageDraftOf,
  toAbility,
  toggleElement,
  toStages,
} from "../lib/techniques";
import AbilityTable from "./AbilityTable";
import StageTable from "./StageTable";

type Props = {
  /** 있으면 고치는 폼, 없으면 새로 만드는 폼 */
  initial?: TechniqueDetailResponse;
  /** 시작 경지 선택지(GET /api/admin/realms, 낮은 경지부터) */
  realms: RealmResponse[];
  /** 고친 공법을 저장한 뒤 서버가 돌려준 값. 새로 만들면 대신 `/technique/?id=<새 id>` 로 간다 */
  onSaved: (saved: TechniqueDetailResponse) => void;
};

/**
 * 공법 폼: 이름·묘사·속성·시작 경지·단계 표·신통 표, 저장과 삭제.
 * 값 검사는 서버에 맡기고 서버의 detail 을 보인다
 */
export default function TechniqueForm({ initial, realms, onSaved }: Props) {
  const router = useRouter();
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [elements, setElements] = useState<ElementCode[]>(inElementOrder(initial?.elements ?? []));
  const [startRealm, setStartRealm] = useState<RealmCode>(initial?.startRealm ?? realms[0]?.code ?? "MORTAL");
  const [stages, setStages] = useState<StageDraft[]>(
    () => initial?.stages.map(stageDraftOf) ?? Array.from({ length: DEFAULT_STAGE_COUNT }, emptyStage),
  );
  const [abilities, setAbilities] = useState<AbilityDraft[]>(() => initial?.abilities.map(abilityDraftOf) ?? []);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  /** 단계 수가 바뀌면 신통마다 단계별 값 행도 배우는 단계 ~ 새 단계 수로 맞춘다 */
  function changeStages(next: StageDraft[]) {
    if (next.length !== stages.length) {
      setAbilities(
        abilities.map((ability) => ({
          ...ability,
          stagePowers: resizeStagePowers(ability.stagePowers, ability.learnStage, next.length),
        })),
      );
    }
    setStages(next);
  }

  function request(): TechniqueSaveRequest {
    return {
      name,
      description,
      elements: inElementOrder(elements),
      startRealm,
      stages: toStages(stages),
      abilities: abilities.map(toAbility),
    };
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      if (initial) {
        onSaved(await updateTechnique(initial.id, request()));
      } else {
        const created = await createTechnique(request());
        router.push(`/technique/?id=${created.id}`);
      }
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(target: TechniqueDetailResponse) {
    if (!window.confirm(`'${target.name}' 공법을 지울까요?`)) {
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await deleteTechnique(target.id);
      router.push("/techniques/");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="form wide" onSubmit={handleSubmit}>
      <label>
        이름
        <input value={name} maxLength={12} onChange={(e) => setName(e.target.value)} />
      </label>
      <label>
        묘사
        <textarea value={description} maxLength={500} rows={3} onChange={(e) => setDescription(e.target.value)} />
      </label>
      <fieldset>
        <legend>속성</legend>
        {ELEMENTS.map((element) => (
          <label key={element.code} className="checkbox">
            <input
              type="checkbox"
              checked={elements.includes(element.code)}
              onChange={(e) => setElements(toggleElement(elements, element.code, e.target.checked))}
            />
            {element.name}
          </label>
        ))}
      </fieldset>
      <p className="muted">속성을 고르지 않으면 연체술 공법입니다.</p>
      <label>
        시작 경지
        <select value={startRealm} onChange={(e) => setStartRealm(e.target.value as RealmCode)}>
          {realms.map((realm) => (
            <option key={realm.code} value={realm.code}>
              {realm.name}
            </option>
          ))}
        </select>
      </label>
      <h2>단계</h2>
      <StageTable stages={stages} onChange={changeStages} />
      <h2>신통</h2>
      <AbilityTable abilities={abilities} stageCount={stages.length} onChange={setAbilities} />
      <div className="actions">
        <button type="submit" disabled={submitting}>
          저장
        </button>
        {initial && (
          <button type="button" disabled={submitting} onClick={() => handleDelete(initial)}>
            삭제
          </button>
        )}
      </div>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      {initial && (
        <section>
          <h2>이 공법이 담긴 물건</h2>
          {initial.scrollItems.length === 0 ? (
            <p className="muted">없음</p>
          ) : (
            <ul>
              {initial.scrollItems.map((item) => (
                <li key={item.id}>
                  <Link href={`/item/?id=${item.id}`}>{item.name}</Link>
                </li>
              ))}
            </ul>
          )}
          <p>{`익힌 캐릭터 ${initial.learnerCount}명`}</p>
        </section>
      )}
    </form>
  );
}
