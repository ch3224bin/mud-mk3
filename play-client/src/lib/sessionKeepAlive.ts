/**
 * 입력하는 동안 world-server HTTP 세션을 늘린다. 열린 WebSocket 은 세션을 늘리지 않고, 세션이 끝나면 연결도 끊기므로
 * 명령을 보낼 때 마지막 확인에서 intervalMs(5분)가 넘게 지났으면 check(`GET /api/auth/me`)를 한 번 부른다.
 * 만들 때를 마지막 확인으로 본다. 응답은 쓰지 않고 실패도 삼킨다(끊기면 4401 로 알게 된다).
 */
export function createSessionKeepAlive(
  check: () => Promise<unknown>,
  now: () => number = Date.now,
  intervalMs = 5 * 60_000,
): { onCommand(): void } {
  let lastCheck = now();
  return {
    onCommand() {
      const current = now();
      if (current - lastCheck <= intervalMs) return;
      lastCheck = current;
      try {
        check().catch(() => undefined);
      } catch {
        // 응답·실패는 화면에 보이지 않는다
      }
    },
  };
}
