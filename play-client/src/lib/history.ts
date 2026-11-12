/**
 * 입력창 ↑↓ 명령 히스토리(D18). 최대 limit 개, 바로 앞과 같은 입력은 다시 넣지 않고,
 * ↓ 로 가장 최근 것을 지나면 빈 입력이 된다.
 */
export class CommandHistory {
  private readonly limit: number;
  private readonly entries: string[] = [];
  /** 지금 보고 있는 자리. entries.length 면 히스토리 밖(새 입력) */
  private position = 0;

  constructor(limit = 100) {
    this.limit = limit;
  }

  push(line: string): void {
    if (line.trim() !== "" && this.entries.at(-1) !== line) {
      this.entries.push(line);
      if (this.entries.length > this.limit) {
        this.entries.shift();
      }
    }
    this.reset();
  }

  /** ↑: 한 칸 앞 명령. 더 앞이 없으면 가장 오래된 것, 히스토리가 비었으면 current 그대로 */
  previous(current: string): string {
    if (this.entries.length === 0) {
      return current;
    }
    this.position = Math.max(0, this.position - 1);
    return this.entries[this.position];
  }

  /** ↓: 한 칸 뒤 명령. 가장 최근 것을 지나면 빈 입력 */
  next(): string {
    this.position = Math.min(this.entries.length, this.position + 1);
    return this.entries[this.position] ?? "";
  }

  /** 히스토리 밖(새 입력)으로 돌아간다 */
  reset(): void {
    this.position = this.entries.length;
  }
}
