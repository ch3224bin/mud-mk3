"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import {
  createMob,
  deleteMob,
  errorMessage,
  type ItemTypeResponse,
  type MobDetailResponse,
  type MobKindCode,
  type MobTierCode,
  type MobTypeSaveRequest,
  type RealmCode,
  type RealmResponse,
  updateMob,
} from "../lib/api";
import {
  DEFAULT_BOSS_ATTACK_COEFFICIENT,
  type DropTableDraft,
  dropTableDraftOf,
  emptyDropTable,
  MOB_KINDS,
  MOB_TIERS,
  mobRealms,
  statsText,
  toDropTable,
} from "../lib/mobs";
import DropTableEditor from "./DropTableEditor";

type Props = {
  /** 있으면 고치는 폼, 없으면 새로 만드는 폼 */
  initial?: MobDetailResponse;
  /** 경지 목록(GET /api/admin/realms, 낮은 경지부터). 범인은 폼이 뺀다 */
  realms: RealmResponse[];
  /** 드롭 표의 물건 선택에 보일 아이템 종류(서버 목록 순서) */
  itemTypes: ItemTypeResponse[];
  /** 고친 몹을 저장한 뒤 서버가 돌려준 값. 새로 만들면 대신 `/mob/?id=<새 id>` 로 간다 */
  onSaved: (saved: MobDetailResponse) => void;
};

/** 빈칸이면 null(재등장은 체급 기준, 두목 계수는 서버가 빠짐으로 알림) */
function numberOrNull(text: string): number | null {
  return text.trim() === "" ? null : Number(text);
}

/**
 * 몹 폼: 이름·묘사·종류·기준 경지·체급·공격 이름·공격 계수(두목만)·재등장·드롭 표, 저장과 삭제.
 * 고칠 때는 저장한 값으로 계산한 스탯과 배치된 방을 보인다. 값 검사는 서버에 맡기고 서버의 detail 을 보인다
 */
export default function MobForm({ initial, realms, itemTypes, onSaved }: Props) {
  const router = useRouter();
  const realmOptions = mobRealms(realms);
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [kind, setKind] = useState<MobKindCode>(initial?.kind ?? "BEAST");
  const [realm, setRealm] = useState<RealmCode>(initial?.realm ?? realmOptions[0]?.code ?? "QI_1");
  const [tier, setTier] = useState<MobTierCode>(initial?.tier ?? "NORMAL");
  const [attackName, setAttackName] = useState(initial?.attackName ?? "");
  const [attackCoefficient, setAttackCoefficient] = useState(
    initial?.attackCoefficient == null ? DEFAULT_BOSS_ATTACK_COEFFICIENT : String(initial.attackCoefficient),
  );
  const [respawnSeconds, setRespawnSeconds] = useState(
    initial?.respawnSeconds == null ? "" : String(initial.respawnSeconds),
  );
  const [drops, setDrops] = useState<DropTableDraft>(() =>
    initial ? dropTableDraftOf(initial.fixedDrops, initial.tierDrops) : emptyDropTable(),
  );
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function request(): MobTypeSaveRequest {
    return {
      name,
      description,
      kind,
      realm,
      tier,
      attackName,
      attackCoefficient: tier === "BOSS" ? numberOrNull(attackCoefficient) : null,
      respawnSeconds: numberOrNull(respawnSeconds),
      ...toDropTable(drops),
    };
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      if (initial) {
        onSaved(await updateMob(initial.id, request()));
      } else {
        const created = await createMob(request());
        router.push(`/mob/?id=${created.id}`);
      }
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(target: MobDetailResponse) {
    if (!window.confirm(`'${target.name}' 몹을 지울까요?`)) {
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await deleteMob(target.id);
      router.push("/mobs/");
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
        <input value={name} maxLength={20} onChange={(e) => setName(e.target.value)} />
      </label>
      <label>
        묘사
        <textarea value={description} maxLength={500} rows={3} onChange={(e) => setDescription(e.target.value)} />
      </label>
      <label>
        종류
        <select value={kind} onChange={(e) => setKind(e.target.value as MobKindCode)}>
          {MOB_KINDS.map((option) => (
            <option key={option.code} value={option.code}>
              {option.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        기준 경지
        <select value={realm} onChange={(e) => setRealm(e.target.value as RealmCode)}>
          {realmOptions.map((option) => (
            <option key={option.code} value={option.code}>
              {option.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        체급
        <select value={tier} onChange={(e) => setTier(e.target.value as MobTierCode)}>
          {MOB_TIERS.map((option) => (
            <option key={option.code} value={option.code}>
              {option.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        공격 이름
        <input value={attackName} maxLength={12} onChange={(e) => setAttackName(e.target.value)} />
      </label>
      {tier === "BOSS" && (
        <label>
          공격 계수
          <input
            type="number"
            step={0.1}
            value={attackCoefficient}
            onChange={(e) => setAttackCoefficient(e.target.value)}
          />
        </label>
      )}
      <label>
        재등장(초)
        <input type="number" min={1} value={respawnSeconds} onChange={(e) => setRespawnSeconds(e.target.value)} />
      </label>
      <p className="muted">비우면 체급 기준(일반 90초 · 강적 3분 · 정예 10분). 두목은 30~60분을 꼭 넣습니다.</p>
      <DropTableEditor itemTypes={itemTypes} value={drops} onChange={setDrops} />
      <p className="muted">영석은 고정 재료 값으로 엔진이 정합니다(30%).</p>
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
          <p>{statsText(initial.stats)}</p>
          <h2>배치된 방</h2>
          {initial.placements.length === 0 ? (
            <p className="muted">없음</p>
          ) : (
            <ul>
              {initial.placements.map((placement) => (
                <li key={placement.id}>
                  <Link href={`/room/?id=${placement.roomId}`}>{placement.roomName}</Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </form>
  );
}
