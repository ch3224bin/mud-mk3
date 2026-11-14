"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { errorMessage, getWorld, type WorldResponse } from "../lib/api";
import { logout } from "../lib/auth";

/** 위쪽 공통 바: 현재 시작 방, 지역 목록·성·종문·NPC 링크, 로그아웃. refreshKey 가 바뀌면 시작 방을 다시 읽는다. */
export default function TopBar({ refreshKey = 0 }: { refreshKey?: number }) {
  const router = useRouter();
  const [world, setWorld] = useState<WorldResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getWorld()
      .then(setWorld)
      .catch((e) => setError(errorMessage(e)));
  }, [refreshKey]);

  async function handleLogout() {
    try {
      await logout();
      router.replace("/");
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <header className="topbar">
      <span>
        {world === null ? (
          "시작 방: …"
        ) : world.startRoomId === null ? (
          "시작 방: 없음"
        ) : (
          <Link href={`/room/?id=${world.startRoomId}`}>{`시작 방: ${world.startZoneName} / ${world.startRoomName}`}</Link>
        )}
      </span>
      <nav>
        <Link href="/zones/">지역 목록</Link>
        <Link href="/factions/">성·종문</Link>
        <Link href="/npcs/">NPC</Link>
        <button type="button" onClick={handleLogout}>
          로그아웃
        </button>
      </nav>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </header>
  );
}
