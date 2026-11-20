const HANGUL_FIRST = 0xac00;
const HANGUL_LAST = 0xd7a3;
/** 한글 음절 하나에 딸린 종성 경우의 수(종성 없음 포함) */
const FINAL_CONSONANT_COUNT = 28;

/** 마지막 글자가 받침 있는 한글 음절인가. 한글이 아니면(숫자 등) 받침 없음으로 본다 */
function endsWithFinalConsonant(word: string): boolean {
  const code = word.codePointAt(word.length - 1);
  if (code === undefined || code < HANGUL_FIRST || code > HANGUL_LAST) {
    return false;
  }
  return (code - HANGUL_FIRST) % FINAL_CONSONANT_COUNT !== 0;
}

/** 단어 뒤에 받침에 맞춘 `은`/`는` 을 붙인다. 예: 청모랑은, 들토끼는, 들토끼123는 */
export function withEunNeun(word: string): string {
  return word + (endsWithFinalConsonant(word) ? "은" : "는");
}
