#!/usr/bin/env node
// compose 로 띄운 세 서비스(world-server)에 대고 1차 시나리오를 자동으로 확인한다.
// 사용: node scripts/smoke-test.mjs [baseUrl=http://localhost:8080]
// 전제: world-server 가 SPRING_PROFILES_ACTIVE=local, MUD_DEV_LOGIN_ENABLED=true 로 떠 있고
//       MUD_ADMIN_EMAILS 에 관리자 이메일(기본 smoke-admin@example.com, 바꾸려면 SMOKE_ADMIN_EMAIL)이 들어 있다.
// Node 26 내장 fetch·WebSocket 만 쓴다(의존성 없음). 실패하면 무엇이 기대와 달랐는지 출력하고 종료 코드 1.
// 실행마다 지역·플레이어 이메일·캐릭터 이름에 실행 번호를 섞는다. 만든 데이터는 지우지 않는다.

const baseUrl = (process.argv[2] ?? "http://localhost:8080").replace(/\/$/, "");
const wsUrl = baseUrl.replace(/^http/, "ws") + "/ws/game";
const adminEmail = process.env.SMOKE_ADMIN_EMAIL ?? "smoke-admin@example.com";
const ADMIN_ORIGIN = "http://localhost:3001";
const PLAY_ORIGIN = "http://localhost:3000";
const TIMEOUT_MS = 5000;

const runId = Date.now().toString(36);
const randomHangul = () => String.fromCodePoint(0xac00 + Math.floor(Math.random() * (0xd7a3 - 0xac00 + 1)));
const nameA = "갑사" + randomHangul() + randomHangul();
const nameB = "을사" + randomHangul() + randomHangul();

class SmokeFailure extends Error {}

function fail(message) {
  throw new SmokeFailure(message);
}

function step(text) {
  console.log(`- ${text}`);
}

// ---- REST ----

async function devLogin(email) {
  const res = await fetch(`${baseUrl}/api/auth/dev`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-MUD-Client": "1", Origin: ADMIN_ORIGIN },
    body: JSON.stringify({ email }),
  });
  if (res.status !== 200) fail(`개발용 로그인(${email}) 기대 200, 실제 ${res.status}: ${await res.text()}`);
  const setCookie = res.headers.getSetCookie().find((c) => c.startsWith("JSESSIONID="));
  if (!setCookie) fail(`개발용 로그인(${email}) 응답에 JSESSIONID Set-Cookie 가 없음`);
  const me = await res.json();
  return { email, me, cookie: setCookie.split(";")[0], setCookie };
}

async function api(session, method, path, body, { expect = 200, clientHeader = true } = {}) {
  const headers = { Origin: ADMIN_ORIGIN };
  if (session) headers.Cookie = session.cookie;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (clientHeader && method !== "GET") headers["X-MUD-Client"] = "1";
  const res = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  if (res.status !== expect) fail(`${method} ${path} 기대 ${expect}, 실제 ${res.status}: ${text}`);
  return text ? JSON.parse(text) : null;
}

// ---- WebSocket ----

class GameClient {
  constructor(label, cookie, origin = PLAY_ORIGIN) {
    this.label = label;
    this.messages = [];
    this.waiters = [];
    this.closeEvent = null;
    const headers = { Origin: origin };
    if (cookie) headers.Cookie = cookie;
    this.ws = new WebSocket(wsUrl, { headers });
    this.opened = new Promise((resolve, reject) => {
      this.ws.addEventListener("open", () => resolve());
      this.ws.addEventListener("error", () => reject(new SmokeFailure(`${label}: WebSocket 연결 실패`)));
    });
    this.opened.catch(() => {});
    this.closed = new Promise((resolve) => {
      this.ws.addEventListener("close", (event) => {
        this.closeEvent = { code: event.code, reason: event.reason };
        resolve(this.closeEvent);
      });
    });
    this.ws.addEventListener("message", (event) => {
      this.messages.push(JSON.parse(event.data));
      this.waiters.forEach((check) => check());
    });
  }

  mark() {
    return this.messages.length;
  }

  send(text) {
    const mark = this.mark();
    this.ws.send(JSON.stringify({ type: "command", text }));
    return mark;
  }

  /** since 이후에 받은 메시지 중 조건에 맞는 첫 메시지를 기다린다 */
  waitFor(predicate, description, since = 0) {
    return new Promise((resolve, reject) => {
      const check = () => {
        const found = this.messages.slice(since).find(predicate);
        if (found) {
          done();
          resolve(found);
        }
      };
      const timer = setTimeout(() => {
        done();
        const received = this.messages.slice(since).map((m) => JSON.stringify(m)).join("\n    ");
        reject(new SmokeFailure(`${this.label}: ${description} 를 ${TIMEOUT_MS}ms 안에 받지 못함. 받은 것:\n    ${received || "(없음)"}`));
      }, TIMEOUT_MS);
      const done = () => {
        clearTimeout(timer);
        this.waiters = this.waiters.filter((w) => w !== check);
      };
      this.waiters.push(check);
      check();
    });
  }

  async waitClose(expectedCode) {
    const timeout = new Promise((_, reject) =>
      setTimeout(() => reject(new SmokeFailure(`${this.label}: ${TIMEOUT_MS}ms 안에 닫히지 않음`)), TIMEOUT_MS));
    const event = await Promise.race([this.closed, timeout]);
    if (event.code !== expectedCode) fail(`${this.label}: 닫기 코드 기대 ${expectedCode}, 실제 ${event.code}`);
  }

  close() {
    if (this.ws.readyState <= 1) this.ws.close();
  }
}

const isRoom = (roomName) => (m) => m.type === "room" && m.room?.name === roomName;
const isNotice = (event, actor) => (m) => m.type === "notice" && m.event === event && m.actor === actor;

async function createCharacter(client, name) {
  await client.opened;
  await client.waitFor((m) => m.type === "session" && m.state === "creating", "session(creating)");
  await client.waitFor((m) => m.type === "prompt" && m.text.startsWith("이름을 정하십시오"), "이름 prompt");
  let mark = client.send(name);
  await client.waitFor((m) => m.type === "prompt" && m.text.startsWith(`이름 ${name}`), "이름 확인 prompt", mark);
  mark = client.send("확인");
  await client.waitFor((m) => m.type === "session" && m.state === "playing" && m.characterName === name,
    `session(playing, ${name})`, mark);
  await client.waitFor(isRoom("광장"), "시작 방 광장 room", mark);
}

// ---- 시나리오 ----

async function run() {
  console.log(`스모크 테스트 ${baseUrl} (실행 번호 ${runId}, 관리자 ${adminEmail}, 캐릭터 ${nameA}·${nameB})`);
  const clients = [];
  const connect = (...args) => {
    const client = new GameClient(...args);
    clients.push(client);
    return client;
  };

  try {
    step("1. 개발용 로그인 — 관리자·플레이어 A·B 세션");
    const admin = await devLogin(adminEmail);
    if (!admin.me.admin) fail(`${adminEmail} 이 관리자가 아님 — MUD_ADMIN_EMAILS 를 확인하십시오`);
    const playerA = await devLogin(`smoke-a-${runId}@example.com`);
    const playerB = await devLogin(`smoke-b-${runId}@example.com`);
    if (playerA.me.admin || playerB.me.admin) fail("플레이어가 관리자로 로그인됨");
    if (!/HttpOnly/i.test(admin.setCookie) || !/SameSite=Lax/i.test(admin.setCookie)) {
      fail(`세션 쿠키 속성이 HttpOnly·SameSite=Lax 가 아님: ${admin.setCookie}`);
    }
    const me = await api(playerA, "GET", "/api/auth/me");
    if (me.email !== playerA.email || me.characterName !== null) fail(`/api/auth/me 응답이 다름: ${JSON.stringify(me)}`);

    step("2. 관리자 — 지역·방·양방향 출구·시작 방. 플레이어는 관리 API 403, 미로그인 401, X-MUD-Client 없으면 403");
    const zone = await api(admin, "POST", "/api/admin/zones",
      { name: `스모크 ${runId}`, description: "스모크 테스트가 만든 지역" }, { expect: 201 });
    const square = await api(admin, "POST", "/api/admin/rooms",
      { zoneId: zone.id, name: "광장", description: "넓은 돌바닥 광장이다." }, { expect: 201 });
    const northRoad = await api(admin, "POST", "/api/admin/rooms",
      { zoneId: zone.id, name: "북쪽 길", description: "북으로 뻗은 흙길이다." }, { expect: 201 });
    const withExit = await api(admin, "PUT", `/api/admin/rooms/${square.id}/exits/NORTH`,
      { targetRoomId: northRoad.id, twoWay: true });
    if (!withExit.exits.some((e) => e.direction === "NORTH" && e.targetRoomId === northRoad.id)) {
      fail(`광장 북 출구가 없음: ${JSON.stringify(withExit.exits)}`);
    }
    const back = await api(admin, "GET", `/api/admin/rooms/${northRoad.id}`);
    if (!back.exits.some((e) => e.direction === "SOUTH" && e.targetRoomId === square.id)) {
      fail(`twoWay 인데 북쪽 길 남 출구가 없음: ${JSON.stringify(back.exits)}`);
    }
    const world = await api(admin, "PUT", "/api/admin/world/start-room", { roomId: square.id });
    if (world.startRoomId !== square.id) fail(`시작 방 지정이 반영되지 않음: ${JSON.stringify(world)}`);
    await api(playerA, "GET", "/api/admin/zones", undefined, { expect: 403 });
    await api(playerA, "POST", "/api/admin/zones", { name: `막힘 ${runId}`, description: "" }, { expect: 403 });
    await api(null, "GET", "/api/admin/zones", undefined, { expect: 401 });
    await api(admin, "POST", "/api/admin/zones", { name: `헤더없음 ${runId}`, description: "" },
      { expect: 403, clientHeader: false });

    step("3. 미로그인 WebSocket → AUTH_REQUIRED 와 4401, 허용 밖 Origin → 핸드셰이크 거절");
    const anonymous = connect("미로그인");
    await anonymous.opened;
    await anonymous.waitFor((m) => m.type === "error" && m.code === "AUTH_REQUIRED", "error(AUTH_REQUIRED)");
    await anonymous.waitClose(4401);
    const foreign = connect("다른 오리진", playerA.cookie, "http://evil.example.com");
    const foreignResult = await foreign.opened.then(() => "open", () => "rejected");
    if (foreignResult !== "rejected") fail("허용 목록 밖 Origin 의 WebSocket 이 열림");

    step(`4. A·B 접속 → 캐릭터 생성(${nameA}, ${nameB}) → 시작 방 광장`);
    const a = connect("A", playerA.cookie);
    await createCharacter(a, nameA);
    const aMarkBeforeB = a.mark();
    const b = connect("B", playerB.cookie);
    await createCharacter(b, nameB);

    step("5. A가 B의 appear 를 받음, A 보기 → occupants 에 B");
    const appear = await a.waitFor(isNotice("appear", nameB), `${nameB} appear`, aMarkBeforeB);
    if (!appear.text.includes("나타난다")) fail(`appear 문구가 다름: ${appear.text}`);
    let mark = a.send("보기");
    const look = await a.waitFor(isRoom("광장"), "보기 room", mark);
    if (!look.occupants.some((o) => o.name === nameB && o.kind === "player")) {
      fail(`A 보기 occupants 에 ${nameB} 가 없음: ${JSON.stringify(look.occupants)}`);
    }
    if (!look.exits.includes("북")) fail(`광장 출구에 북이 없음: ${JSON.stringify(look.exits)}`);

    step("6. A 북 → B가 leave, A는 북쪽 길(출구 남)");
    let bMark = b.mark();
    mark = a.send("북");
    const moved = await a.waitFor(isRoom("북쪽 길"), "북쪽 길 room", mark);
    if (!moved.exits.includes("남")) fail(`북쪽 길 출구에 남이 없음: ${JSON.stringify(moved.exits)}`);
    const leave = await b.waitFor(isNotice("leave", nameA), `${nameA} leave`, bMark);
    if (!leave.text.includes("북쪽으로 떠난다")) fail(`leave 문구가 다름: ${leave.text}`);

    step("7. B n → A가 arrive, B '안녕하시오 → A가 say(self=false)");
    let aMark = a.mark();
    bMark = b.send("n");
    await b.waitFor(isRoom("북쪽 길"), "B 북쪽 길 room", bMark);
    const arrive = await a.waitFor(isNotice("arrive", nameB), `${nameB} arrive`, aMark);
    if (!arrive.text.includes("남쪽에서 다가온다")) fail(`arrive 문구가 다름: ${arrive.text}`);
    aMark = a.mark();
    bMark = b.send("'안녕하시오");
    const heard = await a.waitFor((m) => m.type === "say" && m.speaker === nameB, "say", aMark);
    if (heard.self !== false || heard.message !== "안녕하시오") fail(`A가 받은 say 가 다름: ${JSON.stringify(heard)}`);
    const said = await b.waitFor((m) => m.type === "say" && m.speaker === nameB, "자기 say", bMark);
    if (said.self !== true) fail(`B가 받은 자기 say 의 self 가 true 가 아님: ${JSON.stringify(said)}`);

    step("8. A 누구 → 두 이름, A 종료 → 1000, B가 disappear");
    mark = a.send("누구");
    const who = await a.waitFor((m) => m.type === "system" && m.text.startsWith("접속한 사람"), "누구 결과", mark);
    if (!who.text.includes(nameA) || !who.text.includes(nameB)) fail(`누구 결과에 두 이름이 없음: ${who.text}`);
    bMark = b.mark();
    mark = a.send("종료");
    await a.waitFor((m) => m.type === "system" && m.text === "다음에 또 뵙겠습니다.", "종료 인사", mark);
    await a.waitClose(1000);
    await b.waitFor(isNotice("disappear", nameA), `${nameA} disappear`, bMark);

    step("9. 관리자가 북쪽 길 삭제 → B가 시작 장소로 옮겨짐(광장)");
    bMark = b.mark();
    await api(admin, "DELETE", `/api/admin/rooms/${northRoad.id}`, undefined, { expect: 204 });
    await b.waitFor((m) => m.type === "system" && m.text === "서 있던 곳이 사라져 시작 장소로 옮겨졌습니다.",
      "옮겨짐 안내", bMark);
    await b.waitFor(isRoom("광장"), "광장 room", bMark);
    mark = b.send("보기");
    await b.waitFor(isRoom("광장"), "옮겨진 뒤 보기", mark);

    step("10. 같은 계정 재접속 → 앞 연결 4409, 새 연결이 광장을 이어받음");
    bMark = b.mark();
    const b2 = connect("B(재접속)", playerB.cookie);
    await b2.opened;
    await b.waitFor((m) => m.type === "system" && m.text === "다른 곳에서 접속해 이 연결을 끊습니다.", "밀려남 안내", bMark);
    await b.waitClose(4409);
    await b2.waitFor(isRoom("광장"), "재접속 room");
    const b2Who = b2.send("누구");
    const who2 = await b2.waitFor((m) => m.type === "system" && m.text.startsWith("접속한 사람"), "재접속 누구", b2Who);
    if (who2.text.split(nameB).length !== 2) fail(`재접속 뒤 누구에 ${nameB} 가 한 번만 있어야 함: ${who2.text}`);
    b2.send("종료");
    await b2.waitClose(1000);

    console.log(`SMOKE OK (지역 "스모크 ${runId}" id=${zone.id}, 시작 방 광장 id=${square.id})`);
  } finally {
    clients.forEach((c) => c.close());
  }
}

run().then(
  () => process.exit(0),
  (error) => {
    console.error(`SMOKE FAIL: ${error instanceof SmokeFailure ? error.message : error.stack ?? error}`);
    process.exit(1);
  },
);
