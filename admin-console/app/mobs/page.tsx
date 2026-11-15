"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import TopBar from "../../src/components/TopBar";
import { errorMessage, listMobs, type MobSummaryResponse } from "../../src/lib/api";
import { formatCount } from "../../src/lib/items";
import { formatRespawn } from "../../src/lib/mobs";

export default function MobsPage() {
  return (
    <AdminGuard>
      <MobsView />
    </AdminGuard>
  );
}

function MobsView() {
  const [mobs, setMobs] = useState<MobSummaryResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listMobs()
      .then(setMobs)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  return (
    <>
      <TopBar />
      <main>
        <h1>몹</h1>
        <p>
          <Link className="button" href="/mob/">
            몹 만들기
          </Link>
        </p>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {mobs === null ? (
          !error && <p className="muted">불러오는 중…</p>
        ) : mobs.length === 0 ? (
          <p className="muted">아직 몹이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>이름</th>
                <th>종류</th>
                <th>급/경지</th>
                <th>체급</th>
                <th>기혈</th>
                <th>육신</th>
                <th>위력</th>
                <th>재등장</th>
                <th>배치 수</th>
              </tr>
            </thead>
            <tbody>
              {mobs.map((mob) => (
                <tr key={mob.id}>
                  <td>
                    <Link href={`/mob/?id=${mob.id}`}>{mob.name}</Link>
                  </td>
                  <td>{mob.kindName}</td>
                  <td>{mob.rankName}</td>
                  <td>{mob.tierName}</td>
                  <td>{formatCount(mob.stats.qiBlood)}</td>
                  <td>{formatCount(mob.stats.body)}</td>
                  <td>{formatCount(mob.stats.power)}</td>
                  <td>{formatRespawn(mob.effectiveRespawnSeconds)}</td>
                  <td>{mob.placementCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </main>
    </>
  );
}
