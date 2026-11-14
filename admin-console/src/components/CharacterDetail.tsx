"use client";

import Link from "next/link";
import { useState } from "react";
import type { CharacterDetailResponse, RealmResponse } from "../lib/api";
import { spiritRootText } from "../lib/characters";
import { stageLabel } from "../lib/techniques";
import CharacterRealmForm from "./CharacterRealmForm";

/** 캐릭터 상세: 이름·영근(읽기 전용)·경지 지정과 스탯 표·익힌 공법 표. 경지 말고는 고칠 수 없다 */
export default function CharacterDetail({
  initial,
  realms,
}: {
  initial: CharacterDetailResponse;
  realms: RealmResponse[];
}) {
  const [character, setCharacter] = useState(initial);

  return (
    <>
      <h1>{character.name}</h1>
      <p>{`영근: ${spiritRootText(character.spiritRoot)}`}</p>
      <CharacterRealmForm character={character} realms={realms} onSaved={setCharacter} />
      <h2>익힌 공법</h2>
      {character.techniques.length === 0 ? (
        <p className="muted">없음</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>이름</th>
              <th>단계</th>
            </tr>
          </thead>
          <tbody>
            {character.techniques.map((technique) => (
              <tr key={technique.id}>
                <td>
                  <Link href={`/technique/?id=${technique.id}`}>{technique.name}</Link>
                </td>
                <td>{`${technique.stage}/${stageLabel(technique.stageCount)}`}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
