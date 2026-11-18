/**
 * 명령어(1차 D6 + 2차 대화 + 3차 물건 + 4차 상태·공법·신통·읽기 + 5차 공격·수습 D92 + 6차 중지 D115).
 * 자동완성 후보 순서이기도 하다. 배운 신통 이름은 넣지 않는다(배운 신통을 구조로 보내는 메시지가 없음).
 */
export const COMMANDS = [
  "보기",
  "말하기",
  "대화",
  "공격",
  "중지",
  "수습",
  "상태",
  "공법",
  "신통",
  "읽기",
  "소지품",
  "줍기",
  "버리기",
  "주기",
  "목록",
  "사기",
  "팔기",
  "값",
  "확인",
  "취소",
  "누구",
  "도움말",
  "종료",
  "동",
  "서",
  "남",
  "북",
  "북동",
  "북서",
  "남동",
  "남서",
  "위",
  "아래",
];

/**
 * 이름 후보(D74·D92): 방 이름(occupants → 몹 → 시체 → 바닥 물건) 다음 마지막 inventory 의 물건 이름.
 * 겹치면 앞에 나온 것만 남긴다.
 */
export function nameCandidates(roomNames: string[], inventoryNames: string[]): string[] {
  return [...new Set([...roomNames, ...inventoryNames])];
}

/** 명령어 다음 낱말부터 마지막 낱말까지 각 낱말의 시작 위치(앞에서부터). */
function nameStarts(input: string): number[] {
  const starts: number[] = [];
  for (let i = 1; i < input.length; i++) {
    if (input[i - 1] === " " && input[i] !== " ") {
      starts.push(i);
    }
  }
  return starts;
}

/**
 * 탭 자동완성(D18). 첫 낱말이면 명령어에서 앞부분이 맞는 후보로 그 낱말을 바꾼다.
 * 그 뒤에서는 명령어 다음 낱말들의 꼬리(앞에서부터, 가장 긴 것 먼저) 가운데 이름의 앞부분과 맞는 첫 꼬리를 이름 전체로 바꾼다(여러 낱말 이름).
 * 방금 돌려준 값으로 다시 부르면 다음 후보로 돈다. 입력이 바뀌면 처음 후보부터. 후보가 없으면 입력 그대로.
 */
export class Completer {
  private cycle: { head: string; candidates: string[]; index: number; last: string } | null = null;

  complete(input: string, names: string[]): string {
    if (this.cycle !== null && input === this.cycle.last) {
      const cycle = this.cycle;
      cycle.index = (cycle.index + 1) % cycle.candidates.length;
      cycle.last = cycle.head + cycle.candidates[cycle.index];
      return cycle.last;
    }

    this.cycle = null;
    const split = input.lastIndexOf(" ") + 1;
    if (input.slice(0, split).trim() === "") {
      return this.start(input, "", input.slice(split), COMMANDS);
    }
    for (const pos of nameStarts(input)) {
      const tail = input.slice(pos);
      if (tail !== "" && names.some((name) => name.startsWith(tail))) {
        return this.start(input, input.slice(0, pos), tail, names);
      }
    }
    return input;
  }

  private start(input: string, head: string, word: string, pool: string[]): string {
    if (word === "") {
      return input;
    }
    const candidates = pool.filter((candidate) => candidate.startsWith(word));
    if (candidates.length === 0) {
      return input;
    }
    this.cycle = { head, candidates, index: 0, last: head + candidates[0] };
    return this.cycle.last;
  }

  reset(): void {
    this.cycle = null;
  }
}
