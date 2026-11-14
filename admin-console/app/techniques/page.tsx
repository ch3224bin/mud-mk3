"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import TopBar from "../../src/components/TopBar";
import { errorMessage, listTechniques, type TechniqueSummaryResponse } from "../../src/lib/api";
import { stageLabel } from "../../src/lib/techniques";

export default function TechniquesPage() {
  return (
    <AdminGuard>
      <TechniquesView />
    </AdminGuard>
  );
}

function TechniquesView() {
  const [techniques, setTechniques] = useState<TechniqueSummaryResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listTechniques()
      .then(setTechniques)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  return (
    <>
      <TopBar />
      <main>
        <h1>공법</h1>
        <p>
          <Link className="button" href="/technique/">
            공법 만들기
          </Link>
        </p>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {techniques === null ? (
          !error && <p className="muted">불러오는 중…</p>
        ) : techniques.length === 0 ? (
          <p className="muted">아직 공법이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>이름</th>
                <th>계열</th>
                <th>속성</th>
                <th>시작 경지</th>
                <th>단계 수</th>
                <th>신통 수</th>
                <th>익힌 캐릭터 수</th>
              </tr>
            </thead>
            <tbody>
              {techniques.map((technique) => (
                <tr key={technique.id}>
                  <td>
                    <Link href={`/technique/?id=${technique.id}`}>{technique.name}</Link>
                  </td>
                  <td>{technique.lineName}</td>
                  <td>{technique.elementNames ?? "-"}</td>
                  <td>{technique.startRealmName}</td>
                  <td>{stageLabel(technique.stageCount)}</td>
                  <td>{technique.abilityCount}</td>
                  <td>{technique.learnerCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </main>
    </>
  );
}
