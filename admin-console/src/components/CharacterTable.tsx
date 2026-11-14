import Link from "next/link";
import type { CharacterSummaryResponse } from "../lib/api";

/** 캐릭터 목록 표(서버가 준 이름순). 이름은 `/character/?id=` 링크 */
export default function CharacterTable({ characters }: { characters: CharacterSummaryResponse[] }) {
  if (characters.length === 0) {
    return <p className="muted">아직 캐릭터가 없습니다.</p>;
  }
  return (
    <table>
      <thead>
        <tr>
          <th>이름</th>
          <th>경지</th>
          <th>영근</th>
          <th>익힌 공법 수</th>
          <th>접속</th>
        </tr>
      </thead>
      <tbody>
        {characters.map((character) => (
          <tr key={character.id}>
            <td>
              <Link href={`/character/?id=${character.id}`}>{character.name}</Link>
            </td>
            <td>{character.realmName}</td>
            <td>{character.spiritRootName ?? "미정"}</td>
            <td>{character.techniqueCount}</td>
            <td>{character.online ? "접속 중" : "-"}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
