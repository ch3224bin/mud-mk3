"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import TopBar from "../../src/components/TopBar";
import { errorMessage, listNpcs, type NpcSummaryResponse } from "../../src/lib/api";

export default function NpcsPage() {
  return (
    <AdminGuard>
      <NpcsView />
    </AdminGuard>
  );
}

function NpcsView() {
  const [npcs, setNpcs] = useState<NpcSummaryResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listNpcs()
      .then(setNpcs)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  return (
    <>
      <TopBar />
      <main>
        <h1>NPC</h1>
        <p>
          <Link className="button" href="/npc/">
            NPC 만들기
          </Link>
        </p>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {npcs === null ? (
          !error && <p className="muted">불러오는 중…</p>
        ) : npcs.length === 0 ? (
          <p className="muted">아직 NPC가 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>이름</th>
                <th>배치</th>
                <th>낱말 수</th>
                <th>LLM</th>
                <th>상점</th>
              </tr>
            </thead>
            <tbody>
              {npcs.map((npc) => (
                <tr key={npc.id}>
                  <td>
                    <Link href={`/npc/?id=${npc.id}`}>{npc.name}</Link>
                  </td>
                  <td>
                    {npc.roomId === null ? (
                      "배치 안 됨"
                    ) : (
                      <Link href={`/room/?id=${npc.roomId}`}>{`${npc.zoneName} / ${npc.roomName}`}</Link>
                    )}
                  </td>
                  <td>{npc.topicCount}</td>
                  <td>{npc.llmEnabled ? "켬" : "끔"}</td>
                  <td>{npc.shopKindName ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </main>
    </>
  );
}
