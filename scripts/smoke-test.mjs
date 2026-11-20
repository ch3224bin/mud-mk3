#!/usr/bin/env node
// compose 로 띄운 세 서비스(world-server)에 대고 1~6차 시나리오를 자동으로 확인한다.
// 사용: node scripts/smoke-test.mjs [baseUrl=http://localhost:8080]
// 전제: world-server 가 SPRING_PROFILES_ACTIVE=local, MUD_DEV_LOGIN_ENABLED=true 로 떠 있고
//       MUD_ADMIN_EMAILS 에 관리자 이메일(기본 smoke-admin@example.com, 바꾸려면 SMOKE_ADMIN_EMAIL)이 들어 있다.
// Node 26 내장 fetch·WebSocket 만 쓴다(의존성 없음). 실패하면 무엇이 기대와 달랐는지 출력하고 종료 코드 1.
// 실행마다 지역·플레이어 이메일·캐릭터 이름·성·종문·NPC·아이템 종류 이름에 실행 번호를 섞는다. 만든 데이터는 지우지 않는다.
// 시작 소지품은 3차 단계에서 바꾸고 끝에(실패해도) 처음 값으로 되돌린다. 시작 방은 그 실행의 광장으로 남는다.
// 4차: 새 캐릭터는 범인이라 상점 단계 전에 관리 API로 C를 연기 1층으로 지정한다. 공법·옥간·저물탁 이름에도 실행 번호를 섞는다.
// 5차: 몹·사냥터 이름에도 실행 번호를 섞는다. 서버 틱(1초)이 실제로 돌므로 몹 행동은 고정 대기 없이 제한 시간 안에
//      메시지가 오거나(waitFor) 보기를 되풀이해 조건이 맞을 때까지(lookUntil) 기다린다. 이 실행의 몹 배치는 끝에 지운다.
//      시작 소지품은 단계 1 뒤에 읽어 두고 [] 로 정한 뒤 끝에(실패해도) 읽어 둔 값으로 되돌린다.
// 6차: 공법·신통·옥간·몹 이름에도 실행 번호를 섞는다. 신통 대기시간·법력 회복도 실제 시간이라 되풀이로 기다린다.
//      5차 단계 42 의 정리(몹 배치 지움·시작 소지품 되돌림)는 단계 51 로 옮겨 한 번만 한다.
// 부채 정리 2차: 조용한 방 상태(quiet room)는 isQuietRoom 으로 따로 본다(단계 21·38·49). 단계 52 는 로그아웃 뒤 게임 연결 4401.
// 관리 툴 삭제: 단계 36 의 배치된 몹 삭제 409 는 없어짐. 단계 53 몹 삭제, 54 캐릭터 삭제.
// 방위 초성 약자: 도움말 문구(HELP_6)가 바뀜. 단계 55 는 새 플레이어 H 가 초성 약자로 오가고 봐·보·ㅅㅌ 을 쓴다.

const baseUrl = (process.argv[2] ?? "http://localhost:8080").replace(/\/$/, "");
const wsUrl = baseUrl.replace(/^http/, "ws") + "/ws/game";
const adminEmail = process.env.SMOKE_ADMIN_EMAIL ?? "smoke-admin@example.com";
const ADMIN_ORIGIN = "http://localhost:3001";
const PLAY_ORIGIN = "http://localhost:3000";
const TIMEOUT_MS = 5000;

class SmokeFailure extends Error {}

const runId = Date.now().toString(36);
// NPC 이름은 띄어쓰기 없이 12자까지라 숫자 6자리만 붙인다(성·종문·NPC 이름).
// 기존 pgdata 에 같은 번호의 이름이 남아 있으면 409 가 나므로 그 번호의 성·종문이 없을 때까지 1씩 더한다
const runNo = await pickRunNo().catch((error) => {
  console.error(`SMOKE FAIL: 실행 번호 고르기 — ${error instanceof SmokeFailure ? error.message : error.stack ?? error}`);
  process.exit(1);
});
const castleName = `청석성${runNo}`;
const sectName = `황풍곡${runNo}`;
const npcName = `왕씨${runNo}`;
const innNpcName = `점소이${runNo}`;
const randomHangul = () => String.fromCodePoint(0xac00 + Math.floor(Math.random() * (0xd7a3 - 0xac00 + 1)));
const nameA = "갑사" + randomHangul() + randomHangul();
const nameB = "을사" + randomHangul() + randomHangul();
const nameC = "병사" + randomHangul() + randomHangul();
const nameD = "정사" + randomHangul() + randomHangul();
const nameE = "무사" + randomHangul() + randomHangul();
const nameH = "기사" + randomHangul() + randomHangul();
// 3차 아이템 종류·상점 NPC. 숫자로 끝나는 이름은 조사 규칙상 받침 없음(회춘단123456는·를·와·가)
const bagName = `하급 저물대${runNo}`;
const pillName = `회춘단${runNo}`;
const hideName = `늑대 가죽${runNo}`;
const swordName = `비검${runNo}`;
const merchantName = `상인${runNo}`;
// 4차 공법·옥간·저물탁(공법·신통 이름은 띄어쓰기 없이 12자까지)
const springName = `장춘공${runNo}`;
const greenName = `청목결${runNo}`;
const springScrollName = `${springName} 옥간`;
const greenScrollName = `${greenName} 옥간`;
const braceletName = `하급 저물탁${runNo}`;
// 5차 재료·몹·사냥터(숫자로 끝나 받침 없음: 들토끼123456와·가·를)
const rabbitHideName = `토끼 가죽${runNo}`;
const rabbitName = `들토끼${runNo}`;
const bossName = `흑망${runNo}`;
const huntName = `사냥터${runNo}`;
// 6차 공법·신통·옥간·몹(숫자로 끝나 받침 없음: 음혼123456를·와, 염탄123456는)
const blazeName = `열화공${runNo}`;
const emberName = `염탄${runNo}`;
const blazeScrollName = `${blazeName} 옥간`;
const ghostName = `음혼${runNo}`;
// 관리 툴 삭제 몹(숫자로 끝나 받침 없음: 들쥐123456와)
const ratName = `들쥐${runNo}`;

function fail(message) {
  throw new SmokeFailure(message);
}

function step(text) {
  console.log(`- ${text}`);
}

/** Date.now() 끝 6자리에서 시작해, 관리 API 성·종문 목록에 `청석성<n>`·`황풍곡<n>` 이 없는 번호를 고른다(6자리 그대로) */
async function pickRunNo() {
  const session = await devLogin(adminEmail);
  const taken = new Set((await api(session, "GET", "/api/admin/factions")).map((f) => f.name));
  const pad = (n) => String(n).padStart(6, "0");
  let n = Date.now() % 1_000_000;
  while (taken.has(`청석성${pad(n)}`) || taken.has(`황풍곡${pad(n)}`)) n = (n + 1) % 1_000_000;
  return pad(n);
}

/** 마감까지 intervalMs 간격으로 attempt 를 되풀이해 predicate 에 맞는 결과를 돌려준다. 못 받으면 마지막 결과와 함께 실패 */
async function repeatUntil(attempt, predicate, { intervalMs, timeoutMs, description }) {
  const deadline = Date.now() + timeoutMs;
  let last = null;
  while (Date.now() < deadline) {
    last = await attempt();
    if (predicate(last)) return last;
    await new Promise((resolve) => setTimeout(resolve, intervalMs));
  }
  fail(`${description} 를 ${timeoutMs}ms 안에 받지 못함. 마지막: ${JSON.stringify(last)}`);
}

/** 끝 글자 받침에 맞춘 이/가. 한글이 아니면 받침 없음으로 본다 */
function withIGa(word) {
  const code = word.codePointAt(word.length - 1) - 0xac00;
  return word + (code >= 0 && code <= 0xd7a3 - 0xac00 && code % 28 !== 0 ? "이" : "가");
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
  waitFor(predicate, description, since = 0, timeoutMs = TIMEOUT_MS) {
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
        reject(new SmokeFailure(`${this.label}: ${description} 를 ${timeoutMs}ms 안에 받지 못함. 받은 것:\n    ${received || "(없음)"}`));
      }, timeoutMs);
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

// command.md 6차 도움말(D115), 방위 초성 약자 도움말(direction-abbrev)
const HELP_6 = "명령: 보기(봐 보) · 동 서 남 북 북동 북서 남동 남서 위 아래 (ㄷ ㅅ ㄴ ㅂ ㅂㄷ ㅂㅅ ㄴㄷ ㄴㅅ ㅇ ㅇㄹ / e w s n ne nw se sw u d)"
  + " · 말하기(') · 대화 <NPC> [낱말]"
  + " · 공격 <대상> · <신통> [대상] · 중지 · 수습 <시체> · 상태(ㅅㅌ) · 공법 · 신통 · 읽기 <물건>"
  + " · 소지품(ㅈ) · 보기 <물건> · 줍기 <물건> [수량] · 버리기 <물건> [수량] · 주기 <대상> <물건> [수량]"
  + " · 목록 · 사기 <물건> [수량] · 팔기 <물건> [수량] · 값 <물건>"
  + " · 확인 · 취소 · 누구 · 도움말 · 종료";

/** 방 보기 room. 조용한 방 상태(quiet: true, 부채 정리 2차)는 건너뛴다(L-6) */
const isRoom = (roomName) => (m) => m.type === "room" && m.quiet !== true && m.room?.name === roomName;
const isNotice = (event, actor) => (m) => m.type === "notice" && m.event === event && m.actor === actor;
/** 조용한 방 상태(quiet: true, 부채 정리 2차 #13) — 화면 줄 대신 오는 그 방의 바뀐 상태 */
const isQuietRoom = (roomName, predicate = () => true) => (m) =>
  m.type === "room" && m.quiet === true && m.room?.name === roomName && predicate(m);

const ROOT_PROMPT = "영근을 고르십시오.";

/** 이름 → 영근 → 확인. 4차부터 이름 확인 단계 없이 영근을 고른다 */
async function createCharacter(client, name, roots = "목 화") {
  await client.opened;
  await client.waitFor((m) => m.type === "session" && m.state === "creating", "session(creating)");
  await client.waitFor((m) => m.type === "prompt" && m.text.startsWith("이름을 정하십시오"), "이름 prompt");
  let mark = client.send(name);
  await client.waitFor((m) => m.type === "prompt" && m.text.startsWith(ROOT_PROMPT), "영근 prompt", mark);
  mark = client.send(roots);
  await client.waitFor((m) => m.type === "prompt" && m.text.startsWith("목·화 진영근 — 목 50, 화 50"), "영근 확인 prompt", mark);
  mark = client.send("확인");
  await client.waitFor((m) => m.type === "session" && m.state === "playing" && m.characterName === name,
    `session(playing, ${name})`, mark);
  await client.waitFor(isRoom("광장"), "시작 방 광장 room", mark);
}

// ---- 시나리오 ----

async function run() {
  console.log(`스모크 테스트 ${baseUrl} (실행 번호 ${runId}, 관리자 ${adminEmail}, 캐릭터 ${nameA}·${nameB}·${nameC}·${nameD}·${nameE})`);
  const clients = [];
  let admin = null;
  let originalStartingItems = null;
  // 스모크 시작 때의 시작 소지품(단계 1 뒤에 읽고 [] 로 정함, 끝에 되돌림)과 이 실행의 몹 배치 id
  let initialStartingItems = null;
  const mobPlacementIds = [];
  const connect = (...args) => {
    const client = new GameClient(...args);
    clients.push(client);
    return client;
  };

  try {
    step("1. 개발용 로그인 — 관리자·플레이어 A·B 세션");
    admin = await devLogin(adminEmail);
    if (!admin.me.admin) fail(`${adminEmail} 이 관리자가 아님 — MUD_ADMIN_EMAILS 를 확인하십시오`);
    const playerA = await devLogin(`smoke-a-${runId}@example.com`);
    const playerB = await devLogin(`smoke-b-${runId}@example.com`);
    if (playerA.me.admin || playerB.me.admin) fail("플레이어가 관리자로 로그인됨");
    if (!/HttpOnly/i.test(admin.setCookie) || !/SameSite=Lax/i.test(admin.setCookie)) {
      fail(`세션 쿠키 속성이 HttpOnly·SameSite=Lax 가 아님: ${admin.setCookie}`);
    }
    const me = await api(playerA, "GET", "/api/auth/me");
    if (me.email !== playerA.email || me.characterName !== null) fail(`/api/auth/me 응답이 다름: ${JSON.stringify(me)}`);
    // 기존 pgdata 의 시작 소지품이 있으면 E 의 칸이 모자랄 수 있어 [] 로 정하고 끝에 되돌린다(단계 18 의 처음 값도 [])
    initialStartingItems = (await api(admin, "GET", "/api/admin/world/starting-items"))
      .map(({ itemTypeId, count }) => ({ itemTypeId, count }));
    await api(admin, "PUT", "/api/admin/world/starting-items", { items: [] });

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

    step(`11. 관리자 — 성 ${castleName}·종문 ${sectName} 만들기, 지역 소유를 ${castleName} 으로`);
    const castle = await api(admin, "POST", "/api/admin/factions",
      { kind: "CASTLE", name: castleName, description: "스모크 테스트가 만든 성" }, { expect: 201 });
    if (castle.kindName !== "성" || castle.zoneCount !== 0) fail(`성 응답이 다름: ${JSON.stringify(castle)}`);
    const sect = await api(admin, "POST", "/api/admin/factions",
      { kind: "SECT", name: sectName, description: "" }, { expect: 201 });
    if (sect.kindName !== "종문") fail(`종문 응답이 다름: ${JSON.stringify(sect)}`);
    await api(admin, "PUT", `/api/admin/zones/${zone.id}`,
      { name: zone.name, description: zone.description, ownerId: castle.id });
    const zones = await api(admin, "GET", "/api/admin/zones");
    const ownedZone = zones.find((z) => z.id === zone.id);
    if (ownedZone?.ownerName !== castleName || ownedZone.ownerKind !== "CASTLE") {
      fail(`지역 목록 소유가 ${castleName}(CASTLE) 이 아님: ${JSON.stringify(ownedZone)}`);
    }
    const factions = await api(admin, "GET", "/api/admin/factions");
    if (factions.find((f) => f.id === castle.id)?.zoneCount !== 1) {
      fail(`성 목록 zoneCount 가 1 이 아님: ${JSON.stringify(factions.find((f) => f.id === castle.id))}`);
    }

    step(`12. 관리자 — NPC ${npcName}(인사말·낱말 둘·기본 대답·LLM 켬)을 광장에 배치`);
    const topics = [
      { word: "단약", answer: "회춘단이 좋소." },
      { word: "일거리", answer: "늑대가 말썽이오." },
    ];
    const created = await api(admin, "POST", "/api/admin/npcs", {
      name: npcName, greeting: "어서 오시오.", defaultAnswer: "글쎄, 잘 모르겠구려.",
      topics, llmEnabled: true, roomId: square.id,
    }, { expect: 201 });
    const npc = await api(admin, "GET", `/api/admin/npcs/${created.id}`);
    if (npc.name !== npcName || npc.greeting !== "어서 오시오." || npc.defaultAnswer !== "글쎄, 잘 모르겠구려."
      || npc.llmEnabled !== true || npc.roomId !== square.id || npc.zoneId !== zone.id
      || JSON.stringify(npc.topics) !== JSON.stringify(topics)) {
      fail(`NPC 상세가 다름: ${JSON.stringify(npc)}`);
    }

    step(`13. B 재접속 → 광장 보기: 방 머리 소유 ${castleName}, occupants 에 NPC ${npcName}`);
    const b3 = connect("B(2차)", playerB.cookie);
    await b3.opened;
    await b3.waitFor(isRoom("광장"), "재접속 room");
    mark = b3.send("보기");
    const ownedLook = await b3.waitFor(isRoom("광장"), "2차 보기 room", mark);
    if (ownedLook.owner?.name !== castleName || ownedLook.owner?.kind !== "castle") {
      fail(`방 머리 소유가 ${castleName}(castle) 이 아님: ${JSON.stringify(ownedLook.owner)}`);
    }
    if (!ownedLook.occupants.some((o) => o.name === npcName && o.kind === "npc")) {
      fail(`occupants 에 NPC ${npcName} 가 없음: ${JSON.stringify(ownedLook.occupants)}`);
    }

    step(`14. B 대화 ${npcName} → 인사말·낱말, 낱말 대답, 모르는 낱말 → 기본 대답, 도움말에 대화`);
    const talk = async (input, expected) => {
      const m = b3.send(input);
      const answer = await b3.waitFor((msg) => msg.type === "talk" && msg.npc === npcName, `${input} talk`, m);
      if (answer.text !== expected) fail(`${input} 대답이 다름. 기대 ${JSON.stringify(expected)}, 실제 ${JSON.stringify(answer.text)}`);
    };
    await talk(`대화 ${npcName}`, `${npcName}: "어서 오시오."\n물어볼 수 있는 낱말: [단약] [일거리]`);
    await talk(`대화 ${npcName} 일거리`, `${npcName}: "늑대가 말썽이오."`);
    await talk(`대화 ${npcName} 비석`, `${npcName}: "글쎄, 잘 모르겠구려."`);
    mark = b3.send("도움말");
    const help = await b3.waitFor((m) => m.type === "system" && m.text.startsWith("명령:"), "도움말", mark);
    if (!help.text.includes("대화 <NPC> [낱말]")) fail(`도움말에 대화 <NPC> [낱말] 이 없음: ${help.text}`);
    b3.send("종료");
    await b3.waitClose(1000);

    step(`15. 소유 지역이 있는 성 삭제 → 409. NPC ${innNpcName} 가 있는 방을 지우면 배치가 풀림`);
    await api(admin, "DELETE", `/api/admin/factions/${castle.id}`, undefined, { expect: 409 });
    const inn = await api(admin, "POST", "/api/admin/rooms",
      { zoneId: zone.id, name: "객잔", description: "낡은 객잔이다." }, { expect: 201 });
    const innNpc = await api(admin, "POST", "/api/admin/npcs", {
      name: innNpcName, greeting: "들어오시오.", defaultAnswer: "모르오.", topics: [], llmEnabled: false, roomId: inn.id,
    }, { expect: 201 });
    if (innNpc.roomId !== inn.id) fail(`${innNpcName} 배치가 객잔이 아님: ${JSON.stringify(innNpc)}`);
    await api(admin, "DELETE", `/api/admin/rooms/${inn.id}`, undefined, { expect: 204 });
    const unplaced = await api(admin, "GET", `/api/admin/npcs/${innNpc.id}`);
    if (unplaced.roomId !== null || unplaced.zoneId !== null) fail(`방을 지웠는데 배치가 남음: ${JSON.stringify(unplaced)}`);

    step(`16. 관리자 — 아이템 종류 ${bagName}·${pillName}·${hideName}·${swordName}, 목록 앞 넷은 영석`);
    const itemType = (name, category, baseValue, { description = "", bagGrade = null } = {}) =>
      api(admin, "POST", "/api/admin/item-types",
        { name, description, category, baseValue, bagGrade }, { expect: 201 });
    const bag = await itemType(bagName, "BAG", 30, { bagGrade: "LOW" });
    if (bag.categoryName !== "저물대·저물탁" || bag.bagGradeName !== "하급 저물대" || bag.bagSlots !== 20) {
      fail(`저물대 응답이 다름: ${JSON.stringify(bag)}`);
    }
    const pill = await itemType(pillName, "PILL", 3);
    const hide = await itemType(hideName, "MATERIAL", 2, { description: "거칠고 질긴 늑대의 가죽이다." });
    const sword = await itemType(swordName, "ARTIFACT", 40);
    if (pill.categoryName !== "단약" || hide.categoryName !== "재료" || sword.categoryName !== "법기") {
      fail(`아이템 종류 이름이 다름: ${[pill, hide, sword].map((t) => t.categoryName).join(", ")}`);
    }
    const itemTypes = await api(admin, "GET", "/api/admin/item-types");
    const firstFour = itemTypes.slice(0, 4).map((t) => t.name);
    if (JSON.stringify(firstFour) !== JSON.stringify(["하품 영석", "중품 영석", "상품 영석", "극품 영석"])) {
      fail(`아이템 종류 목록 앞 넷이 영석이 아님: ${JSON.stringify(firstFour)}`);
    }
    const lowStone = itemTypes[0];

    step(`17. 관리자 — 광장 바닥에 ${hideName} 3개(재등장 없음) → 방 상세 floorItems remaining 3`);
    await api(admin, "POST", `/api/admin/rooms/${square.id}/floor-items`,
      { itemTypeId: hide.id, count: 3, respawnMinutes: null }, { expect: 201 });
    const floorRoom = await api(admin, "GET", `/api/admin/rooms/${square.id}`);
    const placed = floorRoom.floorItems.find((f) => f.itemTypeId === hide.id);
    if (placed?.remaining !== 3 || placed.itemName !== hideName || placed.respawnMinutes !== null) {
      fail(`광장 바닥 물건이 다름: ${JSON.stringify(floorRoom.floorItems)}`);
    }

    step(`18. 관리자 — 시작 소지품 비우기 → D(${nameD}) 생성(0/2칸) → 시작 소지품 넷 저장, 칸 넘침 400`);
    originalStartingItems = (await api(admin, "GET", "/api/admin/world/starting-items"))
      .map(({ itemTypeId, count }) => ({ itemTypeId, count }));
    const emptied = await api(admin, "PUT", "/api/admin/world/starting-items", { items: [] });
    if (emptied.length !== 0) fail(`시작 소지품이 비지 않음: ${JSON.stringify(emptied)}`);
    const playerD = await devLogin(`smoke-d-${runId}@example.com`);
    const d = connect("D", playerD.cookie);
    await createCharacter(d, nameD);
    const inventoryOf = async (client) => {
      const m = client.send("소지품");
      return client.waitFor((msg) => msg.type === "inventory", `${client.label} 소지품`, m);
    };
    const emptyInventory = await inventoryOf(d);
    if (emptyInventory.usedSlots !== 0 || emptyInventory.totalSlots !== 2 || !emptyInventory.text.startsWith("[소지품 0/2칸]")) {
      fail(`D 소지품이 빈 0/2칸이 아님: ${JSON.stringify(emptyInventory)}`);
    }
    const startingItems = [
      { itemTypeId: bag.id, count: 1 },
      { itemTypeId: lowStone.id, count: 30 },
      { itemTypeId: pill.id, count: 3 },
      { itemTypeId: sword.id, count: 1 },
    ];
    const saved = await api(admin, "PUT", "/api/admin/world/starting-items", { items: startingItems });
    if (JSON.stringify(saved.map((s) => [s.itemName, s.count]))
      !== JSON.stringify([[bagName, 1], ["하품 영석", 30], [pillName, 3], [swordName, 1]])) {
      fail(`저장한 시작 소지품이 다름: ${JSON.stringify(saved)}`);
    }
    const overflow = await api(admin, "PUT", "/api/admin/world/starting-items",
      { items: startingItems.slice(1) }, { expect: 400 });
    if (overflow.detail !== "시작 소지품이 칸을 넘습니다. (필요 3칸, 칸 2)") fail(`칸 넘침 detail 이 다름: ${overflow.detail}`);

    step(`19. 관리자 — NPC ${merchantName} 을 광장에 두고 단약방 [${pillName}], 판매가 1 → 400, 쓰이는 ${pillName} 삭제 → 409`);
    const merchant = await api(admin, "POST", "/api/admin/npcs", {
      name: merchantName, greeting: "무엇을 찾으시오?", defaultAnswer: "모르오.", topics: [], llmEnabled: false, roomId: square.id,
    }, { expect: 201 });
    const shopNpc = await api(admin, "PUT", `/api/admin/npcs/${merchant.id}/shop`,
      { kind: "PILL_SHOP", items: [{ itemTypeId: pill.id, price: null }] });
    if (shopNpc.shop?.kindName !== "단약방" || shopNpc.shop.items[0]?.effectivePrice !== 3 || shopNpc.shop.items[0].price !== null) {
      fail(`상점 응답이 다름: ${JSON.stringify(shopNpc.shop)}`);
    }
    const unfair = await api(admin, "PUT", `/api/admin/npcs/${merchant.id}/shop`,
      { kind: "PILL_SHOP", items: [{ itemTypeId: pill.id, price: 1 }] }, { expect: 400 });
    if (unfair.detail !== `판매가는 매입가의 2배보다 커야 합니다: ${pillName} (매입가 0.75, 판매가 1)`) {
      fail(`판매가 detail 이 다름: ${unfair.detail}`);
    }
    await api(admin, "DELETE", `/api/admin/item-types/${pill.id}`, undefined, { expect: 409 });

    step(`20. C(${nameC}) 생성 → 소지품 3/22칸, 하품 영석 30`);
    const playerC = await devLogin(`smoke-c-${runId}@example.com`);
    const c = connect("C", playerC.cookie);
    await createCharacter(c, nameC);
    const startInventory = await inventoryOf(c);
    if (startInventory.usedSlots !== 3 || startInventory.totalSlots !== 22
      || JSON.stringify(startInventory.stones) !== JSON.stringify([{ name: "하품 영석", count: 30 }])
      || startInventory.text.split("\n")[0] !== "[소지품 3/22칸]  하품 영석 30") {
      fail(`C 시작 소지품이 다름: ${JSON.stringify(startInventory)}`);
    }
    const stonesOf = async (client) => (await inventoryOf(client)).stones;
    /** 보낸 명령 뒤 처음 오는 system 메시지가 기대 문구인지 */
    const expectSystem = async (client, input, expected, tone) => {
      const m = client.send(input);
      const msg = await client.waitFor((x) => x.type === "system", `${client.label} ${input} system`, m);
      if (msg.text !== expected || (tone && msg.tone !== tone)) {
        fail(`${client.label} ${input} 결과가 다름. 기대 ${JSON.stringify(expected)}${tone ? `(${tone})` : ""}, 실제 ${JSON.stringify(msg)}`);
      }
      return msg;
    };
    /** 보낸 명령 뒤 처음 오는 system 메시지의 tone 만 확인하고 돌려준다 */
    const expectSystemTone = async (client, input, tone) => {
      const m = client.send(input);
      const msg = await client.waitFor((x) => x.type === "system", `${client.label} ${input} system`, m);
      if (msg.tone !== tone) fail(`${client.label} ${input} tone 이 ${tone} 이 아님: ${JSON.stringify(msg)}`);
      return msg;
    };

    step(`21. C 보기·줍기·버리기·물건 보기 — D가 pick·drop 알림과 줄어든 바닥의 조용한 방 상태(quiet)를 받음`);
    const floorLook = async () => {
      const m = c.send("보기");
      return (await c.waitFor(isRoom("광장"), "C 보기 room", m)).items;
    };
    let items = await floorLook();
    if (!items.some((i) => i.name === hideName && i.count === 3)) fail(`광장 바닥에 ${hideName} 3 이 없음: ${JSON.stringify(items)}`);
    let dMark = d.mark();
    await expectSystem(c, `줍기 ${hideName} 2`, `${hideName} 2개를 주웠습니다.`, "item");
    await d.waitFor(isNotice("pick", nameC), `${nameC} pick`, dMark);
    await d.waitFor(isQuietRoom("광장", (m) => m.items?.some((i) => i.name === hideName && i.count === 1)),
      `줍기 뒤 ${hideName} 1 인 quiet room`, dMark);
    dMark = d.mark();
    await expectSystem(c, `버리기 ${hideName} 1`, `${hideName} 1개를 버렸습니다.`, "item");
    await d.waitFor(isNotice("drop", nameC), `${nameC} drop`, dMark);
    items = await floorLook();
    if (!items.some((i) => i.name === hideName && i.count === 2)) fail(`광장 바닥 ${hideName} 이 2(배치 1 + 버린 1)가 아님: ${JSON.stringify(items)}`);
    await expectSystem(c, `보기 ${hideName}`, `${hideName} — 거칠고 질긴 늑대의 가죽이다.`, "item");

    step(`22. 관리자가 C를 연기 1층으로 지정(growth 알림) → C 목록·사기·팔기·값 — 영석 30 → 24 → 25, 재료는 단약방이 사지 않음`);
    const characterNamed = async (name) => {
      const found = (await api(admin, "GET", "/api/admin/characters")).find((ch) => ch.name === name);
      if (!found) fail(`캐릭터 목록에 ${name} 이 없음`);
      return found;
    };
    /** 관리자가 경지를 지정하고 그 캐릭터가 받은 growth 알림 문구를 확인한다 */
    const designate = async (client, name, realm, expectedText) => {
      const character = await characterNamed(name);
      const m = client.mark();
      const detail = await api(admin, "PUT", `/api/admin/characters/${character.id}/realm`, { realm });
      if (detail.realm !== realm) fail(`${name} 경지 지정 응답이 ${realm} 이 아님: ${JSON.stringify(detail)}`);
      const notice = await client.waitFor((x) => x.type === "system" && x.tone === "growth", `${client.label} 경지 알림`, m);
      if (notice.text !== expectedText) fail(`${client.label} 경지 알림이 다름. 기대 ${JSON.stringify(expectedText)}, 실제 ${JSON.stringify(notice.text)}`);
      return detail;
    };
    await designate(c, nameC, "QI_1", "경지가 연기 1층으로 바뀌었습니다. (육신 +5, 근력 +2, 영력 +5, 의식 +10, 정혈 +10)");
    mark = c.send("목록");
    const shopList = await c.waitFor((m) => m.type === "system", "목록", mark);
    if (!shopList.text.includes(`[${merchantName} 단약방]`) || !shopList.text.includes(`  1. ${pillName}  3`)
      || !shopList.text.includes("사 주는 것: 단약·영약")) {
      fail(`목록이 다름: ${JSON.stringify(shopList.text)}`);
    }
    await expectSystem(c, `사기 ${pillName} 2`, `${pillName} 2개를 샀습니다. (하품 영석 -6)`, "item");
    let stones = await stonesOf(c);
    if (JSON.stringify(stones) !== JSON.stringify([{ name: "하품 영석", count: 24 }])) fail(`사기 뒤 영석이 24 가 아님: ${JSON.stringify(stones)}`);
    await expectSystem(c, `팔기 ${pillName} 2`, `${pillName} 2개를 팔았습니다. (하품 영석 +1)`, "item");
    stones = await stonesOf(c);
    if (JSON.stringify(stones) !== JSON.stringify([{ name: "하품 영석", count: 25 }])) fail(`팔기 뒤 영석이 25 가 아님: ${JSON.stringify(stones)}`);
    await expectSystem(c, `값 ${pillName}`, `${pillName} 1개에 하품 영석 0.75 — 3개면 2`, "item");
    await expectSystem(c, `팔기 ${hideName}`, `${merchantName}: "그런 물건은 받지 않소." (재료는 재료상에 팔 수 있습니다)`, "error");

    step(`23. C 귀속 ${swordName} → 지운 명령이라 모르는 명령 응답`);
    await expectSystem(c, `귀속 ${swordName}`, "무슨 말인지 모르겠습니다. ('도움말' 로 명령을 볼 수 있습니다)", "error");

    step(`24. C 주기 ${nameD} — 회춘단·하품 5, 칸 없는 D는 가죽 거절, 없는 사람, 도움말`);
    dMark = d.mark();
    await expectSystem(c, `주기 ${nameD} ${pillName} 1`, `${nameD}에게 ${pillName} 1개를 주었습니다.`, "item");
    const handed = await d.waitFor((m) => m.type === "system" && m.tone === "item", "D 건넸습니다", dMark);
    if (handed.text !== `${withIGa(nameC)} ${pillName} 1개를 건넸습니다.`) fail(`D가 받은 문구가 다름: ${JSON.stringify(handed)}`);
    await expectSystem(c, `주기 ${nameD} 하품 5`, `${nameD}에게 하품 영석 5개를 주었습니다.`, "item");
    stones = await stonesOf(d);
    if (JSON.stringify(stones) !== JSON.stringify([{ name: "하품 영석", count: 5 }])) fail(`D 영석이 5 가 아님: ${JSON.stringify(stones)}`);
    await expectSystem(c, `주기 ${nameD} ${hideName} 1`, `${nameD}의 저물대에 자리가 없습니다.`, "error");
    await expectSystem(c, `주기 없는사람 ${pillName}`, "여기에 없는사람은 없습니다.", "error");
    mark = c.send("도움말");
    const help3 = await c.waitFor((m) => m.type === "system" && m.text.startsWith("명령:"), "3차 도움말", mark);
    if (help3.text !== HELP_6) fail(`도움말이 6차 문구와 다름: ${JSON.stringify(help3.text)}`);

    step("25. 관리자 — 시작 소지품을 처음 값으로 되돌림");
    const restored = await api(admin, "PUT", "/api/admin/world/starting-items", { items: originalStartingItems });
    const restoredItems = restored.map(({ itemTypeId, count }) => ({ itemTypeId, count }));
    if (JSON.stringify(restoredItems) !== JSON.stringify(originalStartingItems)) {
      fail(`시작 소지품이 처음 값으로 돌아가지 않음: ${JSON.stringify(restored)}`);
    }
    // 시작 소지품이 비어 있으면(정리 뒤 기본값) E는 기본 2칸이다(단계 34)
    const startingItemsEmpty = originalStartingItems.length === 0;
    originalStartingItems = null;

    // ---- 4차 ----
    step(`26. 관리자 — 공법 ${springName}(범인부터 9성)·${greenName}(연기 7층부터 8성), 명령어 이름 신통 400, 경지 목록 18`);
    const stage = (insightRequired, values = {}) => ({
      insightRequired, aptitudeRequired: 0, manaBonus: 0, senseBonus: 0, spiritBonus: 0, essenceBonus: 0,
      bodyBonus: 0, strengthBonus: 0, aptitudeGain: 0, ...values,
    });
    // 6차: 신통마다 이펙트 문장(기본은 한 줄 `{name} 기운이 퍼진다.`)과 공격이면 단계별 값(배우는 단계~마지막 단계)
    const ability = (name, learnStage, kind, elements, target, costAmount, cooldownSeconds, usableWhen,
      { effectLines = [`${name} 기운이 퍼진다.`], stagePowers = [] } = {}) => ({
      name, learnStage, kind, elements, bodyRefining: false, target, costStat: "MANA", costAmount,
      costPerTick: false, castSeconds: 0, cooldownSeconds, usableWhen, effectLines, stagePowers,
    });
    /** from~to 성의 단계별 값. 기본 위력은 base + step × (s − from), 소수 첫째 자리 */
    const stagePowers = (from, to, base, stepValue, coefficient) => Array.from({ length: to - from + 1 }, (_, i) => ({
      stage: from + i, basePower: Number((base + stepValue * i).toFixed(1)), coefficient,
    }));
    // D117 화탄술 이펙트 문장(admin-tool.md 예 셋)과 1~9성 2 + 0.2 × (s − 1) · 1.2
    const fireBall = {
      effectLines: [
        "손끝에서 붉은 불덩이가 피어올라 {대상}을 덮친다!",
        "{시전자}의 손바닥에서 화탄이 튀어 나가 {대상}에게 터진다!",
        "작은 불덩이가 꼬리를 그으며 {대상}에게 날아든다!",
      ],
      stagePowers: stagePowers(1, 9, 2, 0.2, 1.2),
    };
    // technique.md 장춘공 단계 표(법력·의식·영력·목영근 자질)
    const springStages = [
      stage(0),
      stage(120, { manaBonus: 2, senseBonus: 1, aptitudeGain: 1 }),
      stage(190, { manaBonus: 2, aptitudeGain: 1 }),
      stage(280, { manaBonus: 2, senseBonus: 1, aptitudeGain: 1 }),
      stage(400, { manaBonus: 2, spiritBonus: 1, aptitudeGain: 1 }),
      stage(560, { manaBonus: 2, senseBonus: 1, aptitudeGain: 1 }),
      stage(800, { manaBonus: 2, aptitudeGain: 1 }),
      stage(1100, { manaBonus: 2, senseBonus: 1, aptitudeGain: 1 }),
      stage(1540, { manaBonus: 2, spiritBonus: 1, aptitudeGain: 1 }),
    ];
    const springRequest = {
      name: springName, description: "범인도 익힐 수 있는 목 속성 입문 공법이다.", elements: ["WOOD"], startRealm: "MORTAL",
      stages: springStages,
      abilities: [
        ability("화탄술", 1, "ATTACK", ["FIRE"], "SINGLE", 5, 3, "COMBAT_ONLY", fireBall),
        ability("어풍결", 3, "ESCAPE", ["WOOD"], "SELF", 10, 10, "ANYTIME"),
      ],
    };
    const spring = await api(admin, "POST", "/api/admin/techniques", springRequest, { expect: 201 });
    if (spring.stages.length !== 9 || spring.startRealmName !== "범인" || spring.abilities.length !== 2) {
      fail(`${springName} 응답이 다름: ${JSON.stringify(spring)}`);
    }
    // technique-list.md 오행 연기 후반 공법 표(법력·의식·영력·자질 조건)
    const greenStages = [
      stage(0, { aptitudeRequired: 15 }),
      stage(1800, { aptitudeRequired: 15, manaBonus: 3, senseBonus: 1 }),
      stage(2600, { aptitudeRequired: 15, manaBonus: 3 }),
      stage(3500, { aptitudeRequired: 15, manaBonus: 3, senseBonus: 1 }),
      stage(4800, { aptitudeRequired: 20, manaBonus: 3 }),
      stage(6500, { aptitudeRequired: 20, manaBonus: 3, senseBonus: 1 }),
      stage(8700, { aptitudeRequired: 20, manaBonus: 3 }),
      stage(12000, { aptitudeRequired: 20, manaBonus: 3, senseBonus: 1, spiritBonus: 1 }),
    ];
    const green = await api(admin, "POST", "/api/admin/techniques", {
      name: greenName, description: "연기 후반의 목 속성 공법이다.", elements: ["WOOD"], startRealm: "QI_7",
      stages: greenStages,
      abilities: [
        ability("청목전", 1, "ATTACK", ["WOOD"], "SINGLE", 12, 3, "COMBAT_ONLY", {
          effectLines: ["푸른 나뭇잎이 칼날처럼 {대상}을 벤다!"], stagePowers: stagePowers(1, 8, 3, 0, 1.2),
        }),
        ability("목령갑", 4, "DEFENSE", ["WOOD"], "SELF", 18, 15, "ANYTIME"),
      ],
    }, { expect: 201 });
    if (green.stages.length !== 8 || green.startRealmName !== "연기 7층") fail(`${greenName} 응답이 다름: ${JSON.stringify(green)}`);
    const reserved = await api(admin, "POST", "/api/admin/techniques", {
      ...springRequest, name: `막힘${runNo}`,
      abilities: [ability("상태", 1, "ATTACK", ["FIRE"], "SINGLE", 5, 3, "COMBAT_ONLY", fireBall)],
    }, { expect: 400 });
    if (reserved.detail !== "명령어와 같은 이름은 쓸 수 없습니다: 상태") fail(`명령어 이름 신통 detail 이 다름: ${reserved.detail}`);
    const realms = await api(admin, "GET", "/api/admin/realms");
    if (realms.length !== 18) fail(`경지 목록 길이가 18 이 아님: ${realms.length}`);

    step(`27. 관리자 — 옥간 ${springScrollName}·${greenScrollName}, 단약에 공법 400, 옥간 있는 공법 삭제 409, 광장 바닥에 둠`);
    const scrollType = (name, baseValue, techniqueId) => api(admin, "POST", "/api/admin/item-types",
      { name, description: "", category: "SCROLL", baseValue, bagGrade: null, techniqueId }, { expect: 201 });
    const springScroll = await scrollType(springScrollName, 100, spring.id);
    const greenScroll = await scrollType(greenScrollName, 500, green.id);
    if (springScroll.techniqueName !== springName || greenScroll.techniqueName !== greenName) {
      fail(`옥간 techniqueName 이 다름: ${springScroll.techniqueName}, ${greenScroll.techniqueName}`);
    }
    const pillWithTechnique = await api(admin, "POST", "/api/admin/item-types",
      { name: `공법단${runNo}`, description: "", category: "PILL", baseValue: 1, bagGrade: null, techniqueId: spring.id },
      { expect: 400 });
    if (pillWithTechnique.detail !== "공법은 옥간·서책에만 담을 수 있습니다.") fail(`단약 공법 detail 이 다름: ${pillWithTechnique.detail}`);
    await api(admin, "DELETE", `/api/admin/techniques/${spring.id}`, undefined, { expect: 409 });
    await api(admin, "POST", `/api/admin/rooms/${square.id}/floor-items`,
      { itemTypeId: springScroll.id, count: 2, respawnMinutes: null }, { expect: 201 });
    await api(admin, "POST", `/api/admin/rooms/${square.id}/floor-items`,
      { itemTypeId: greenScroll.id, count: 1, respawnMinutes: null }, { expect: 201 });

    step(`28. E(${nameE}) 생성 — 겹친 영근 거절, 목 화 확인 prompt 네 줄`);
    const playerE = await devLogin(`smoke-e-${runId}@example.com`);
    const e = connect("E", playerE.cookie);
    await e.opened;
    await e.waitFor((m) => m.type === "session" && m.state === "creating", "session(creating)");
    await e.waitFor((m) => m.type === "prompt" && m.text.startsWith("이름을 정하십시오"), "이름 prompt");
    mark = e.send(nameE);
    await e.waitFor((m) => m.type === "prompt" && m.text.startsWith(ROOT_PROMPT), "영근 prompt", mark);
    mark = e.send("목 목");
    const badRoot = await e.waitFor((m) => m.type === "system" && m.tone === "error", "겹친 영근 error", mark);
    if (badRoot.text !== "오행(금 목 수 화 토) 가운데 1~5개를 겹치지 않게 띄어 쓰십시오.") fail(`겹친 영근 문구가 다름: ${badRoot.text}`);
    await e.waitFor((m) => m.type === "prompt" && m.text.startsWith(ROOT_PROMPT), "다시 영근 prompt", mark);
    mark = e.send("목 화");
    const rootConfirm = await e.waitFor((m) => m.type === "prompt" && m.text.startsWith("목·화"), "영근 확인 prompt", mark);
    const expectedConfirm = ["목·화 진영근 — 목 50, 화 50", "────────",
      `이름 ${nameE} · 영근 목·화 진영근 (목 50, 화 50)`, "이대로 만들려면 '확인', 고치려면 '뒤로'."].join("\n");
    if (rootConfirm.text !== expectedConfirm) fail(`영근 확인 prompt 가 다름: ${JSON.stringify(rootConfirm.text)}`);
    mark = e.send("확인");
    await e.waitFor((m) => m.type === "session" && m.state === "playing" && m.characterName === nameE, "session(playing)", mark);
    await e.waitFor(isRoom("광장"), "E 광장 room", mark);

    step("29. E 상태 — 범인(기혈 10, 수위 없음), 범인은 목록 막힘");
    const statusOf = async (client) => {
      const m = client.send("상태");
      return client.waitFor((msg) => msg.type === "status", `${client.label} 상태`, m);
    };
    const gauge = (g) => JSON.stringify({ current: g?.current, max: g?.max });
    const expectGauge = (label, g, value) => {
      if (gauge(g) !== JSON.stringify({ current: value, max: value })) fail(`${label} 이 {${value},${value}} 가 아님: ${JSON.stringify(g)}`);
    };
    const expectCultivation = (status, required) => {
      if (JSON.stringify(status.cultivation) !== JSON.stringify({ current: 0, required })) {
        fail(`수위가 {0,${required}} 가 아님: ${JSON.stringify(status.cultivation)}`);
      }
    };
    let status = await statusOf(e);
    if (status.realm?.code !== "MORTAL" || status.cultivation !== null) fail(`E 범인 상태가 다름: ${JSON.stringify(status)}`);
    expectGauge("E 기혈", status.stats?.qiBlood, 10);
    if (status.text.split("\n")[0] !== `${nameE} · 범인 · 목·화 진영근 (목 50, 화 50)`) fail(`E 상태 첫 줄이 다름: ${status.text}`);
    await expectSystem(e, "목록", "아직 범인의 몸이라 할 수 없습니다.", "error");

    step(`30. E 줍기 옥간, 보기 ${springScrollName} 담긴 공법, 읽기 → 연기 1층·화탄술, 다시 읽기 거절`);
    await expectSystem(e, `줍기 ${springScrollName} 2`, `${springScrollName} 2개를 주웠습니다.`, "item");
    mark = e.send(`줍기 ${greenScrollName}`);
    await e.waitFor((m) => m.type === "system" && m.tone === "item", `줍기 ${greenScrollName}`, mark);
    mark = e.send(`보기 ${springScrollName}`);
    const scrollLook = await e.waitFor((m) => m.type === "system", `보기 ${springScrollName}`, mark);
    if (scrollLook.text.split("\n")[1] !== `담긴 공법: ${springName} (목 · 오행 · 9성 · 범인부터)`) {
      fail(`옥간 보기 둘째 줄이 다름: ${JSON.stringify(scrollLook.text)}`);
    }
    await expectSystem(e, `읽기 ${springScrollName}`,
      `옥간을 이마에 대자 구결이 머릿속으로 흘러든다. ${springName} 1성에 이르렀습니다!\n단전에 따뜻한 기운이 맺힌다. 연기 1층이 되었습니다. 새 신통: 화탄술`,
      "growth");
    status = await statusOf(e);
    expectGauge("E 기혈", status.stats?.qiBlood, 60);
    expectGauge("E 법력", status.stats?.mana, 25);
    expectCultivation(status, 190);
    await expectSystem(e, `읽기 ${springScrollName}`, `이미 깨친 내용입니다. (${springName} 1/9성)`, "error");

    step(`31. E 읽기 ${greenScrollName} → 연기 7층 필요, 옥간은 소지품에 남음`);
    await expectSystem(e, `읽기 ${greenScrollName}`, "지금은 이 공법을 익힐 수 없습니다. (연기 7층 필요)", "error");
    let eInventory = await inventoryOf(e);
    if (!eInventory.items.some((i) => i.name === greenScrollName)) fail(`E 소지품에 ${greenScrollName} 이 없음: ${JSON.stringify(eInventory.items)}`);

    step("32. 관리자 — 캐릭터 목록의 E, 연기 7층 지정 → E growth 알림·스탯");
    const eSummary = await characterNamed(nameE);
    if (eSummary.realm !== "QI_1" || eSummary.spiritRootName !== "목·화 진영근" || eSummary.techniqueCount !== 1 || eSummary.online !== true) {
      fail(`캐릭터 목록의 E 가 다름: ${JSON.stringify(eSummary)}`);
    }
    const qi7 = await designate(e, nameE, "QI_7", "경지가 연기 7층으로 바뀌었습니다. (육신 +6, 근력 +3, 영력 +6, 의식 +12, 정혈 +12)");
    if (qi7.stats?.qiBlood !== 120) fail(`연기 7층 지정 응답 stats.qiBlood 가 120 이 아님: ${JSON.stringify(qi7.stats)}`);
    status = await statusOf(e);
    expectGauge("E 기혈", status.stats?.qiBlood, 120);
    expectGauge("E 법력", status.stats?.mana, 55);
    expectCultivation(status, 1700);

    step(`33. E 읽기 ${greenScrollName} → 청목전, 공법·신통 출력`);
    const greenRead = await expectSystemTone(e, `읽기 ${greenScrollName}`, "growth");
    if (greenRead.text.split("\n")[1] !== "새 신통을 깨쳤습니다: 청목전") fail(`${greenName} 읽기 둘째 줄이 다름: ${JSON.stringify(greenRead.text)}`);
    await expectSystem(e, "공법", [
      `${springName}  1/9성 · 목 · 오행  다음 2성: 목 깨달음 120 (지금 0)`,
      `${greenName}  1/8성 · 목 · 오행  다음 2성: 목 깨달음 1,800 (지금 0)`,
    ].join("\n"), "info");
    await expectSystem(e, "신통", [
      `[${springName} 1성]`, "  화탄술  공격·화  법력 5  대기 3초",
      `[${greenName} 1성]`, "  청목전  공격·목  법력 12  대기 3초",
    ].join("\n"), "info");

    step(`34. 관리자 — ${braceletName}(저물탁) 광장 바닥, E 줍기 → (축기 초기 필요), 칸 그대로`);
    const slotsBefore = eInventory.totalSlots;
    const bracelet = await api(admin, "POST", "/api/admin/item-types",
      { name: braceletName, description: "", category: "BAG", baseValue: 5000, bagGrade: "BRACELET_LOW", techniqueId: null },
      { expect: 201 });
    if (bracelet.bagGradeName !== "하급 저물탁" || bracelet.bagSlots !== 100) fail(`저물탁 응답이 다름: ${JSON.stringify(bracelet)}`);
    await api(admin, "POST", `/api/admin/rooms/${square.id}/floor-items`,
      { itemTypeId: bracelet.id, count: 1, respawnMinutes: null }, { expect: 201 });
    await expectSystemTone(e, `줍기 ${braceletName}`, "item");
    eInventory = await inventoryOf(e);
    if (!eInventory.text.includes(`${braceletName} (축기 초기 필요)`) || eInventory.totalSlots !== slotsBefore) {
      fail(`저물탁을 주운 E 소지품이 다름(칸 ${slotsBefore} 그대로여야 함): ${JSON.stringify(eInventory)}`);
    }
    if (startingItemsEmpty && eInventory.totalSlots !== 2) fail(`시작 소지품이 빈데 E 칸이 2 가 아님: ${eInventory.totalSlots}`);

    step("35. 관리자 — E 축기 초기 지정 → growth 알림, 저물탁 칸 열림, 도움말 6차 문구");
    await designate(e, nameE, "FOUNDATION_EARLY", "경지가 축기 초기로 바뀌었습니다. (육신 +24, 근력 +12, 영력 +23, 의식 +56, 정혈 +56)");
    eInventory = await inventoryOf(e);
    if (eInventory.totalSlots !== slotsBefore + 100 || eInventory.text.includes("(축기 초기 필요)")) {
      fail(`축기 초기 E 소지품이 다름(칸 ${slotsBefore + 100}): ${JSON.stringify(eInventory)}`);
    }
    mark = e.send("도움말");
    const help4 = await e.waitFor((m) => m.type === "system" && m.text.startsWith("명령:"), "4차 도움말", mark);
    if (help4.text !== HELP_6) fail(`도움말이 6차 문구와 다름: ${JSON.stringify(help4.text)}`);

    // ---- 5차 ----
    step(`36. 관리자 — 재료 ${rabbitHideName}, 몹 ${rabbitName}(일반·드롭)·${bossName}(두목), 사냥터 ${huntName}(광장 동), 배치, 쓰이는 재료 삭제 409`);
    const rabbitHide = await itemType(rabbitHideName, "MATERIAL", 1);
    const mobRequest = (values) => ({
      description: "", kind: "BEAST", attackName: "뒷발차기", attackCoefficient: null, respawnSeconds: null,
      fixedDrops: [], tierDrops: [], ...values,
    });
    const rabbit = await api(admin, "POST", "/api/admin/mobs", mobRequest({
      name: rabbitName, realm: "QI_1", tier: "NORMAL", respawnSeconds: 10,
      fixedDrops: [{ itemTypeId: rabbitHide.id, count: 1 }],
    }), { expect: 201 });
    if (JSON.stringify(rabbit.stats) !== JSON.stringify({ qiBlood: 42, body: 7, power: 7 }) || rabbit.rankName !== "1급"
      || rabbit.fixedDrops[0]?.itemTypeName !== rabbitHideName) {
      fail(`${rabbitName} 응답이 다름: ${JSON.stringify(rabbit)}`);
    }
    const bossRequest = mobRequest({ name: bossName, realm: "FOUNDATION_PEAK", tier: "BOSS", attackName: "독니", respawnSeconds: 1800 });
    const noCoefficient = await api(admin, "POST", "/api/admin/mobs", bossRequest, { expect: 400 });
    if (noCoefficient.detail !== "두목의 공격 계수는 0.4~0.8로 정하십시오.") fail(`두목 공격 계수 detail 이 다름: ${noCoefficient.detail}`);
    const boss = await api(admin, "POST", "/api/admin/mobs", { ...bossRequest, attackCoefficient: 0.6 }, { expect: 201 });
    if (JSON.stringify(boss.stats) !== JSON.stringify({ qiBlood: 5400, body: 78, power: 57 })) {
      fail(`${bossName} stats 가 {5400,78,57} 이 아님: ${JSON.stringify(boss)}`);
    }
    const hunt = await api(admin, "POST", "/api/admin/rooms",
      { zoneId: zone.id, name: huntName, description: "풀이 무성한 사냥터다." }, { expect: 201 });
    await api(admin, "PUT", `/api/admin/rooms/${square.id}/exits/EAST`, { targetRoomId: hunt.id, twoWay: true });
    const placeMob = async (room, mob) => {
      const placement = await api(admin, "POST", `/api/admin/rooms/${room.id}/mobs`, { mobId: mob.id, count: 1 }, { expect: 201 });
      mobPlacementIds.push(placement.id);
      return placement;
    };
    await placeMob(square, rabbit);
    await placeMob(hunt, boss);
    await api(admin, "DELETE", `/api/admin/item-types/${rabbitHide.id}`, undefined, { expect: 409 });

    step(`37. E 보기 되풀이 → ${rabbitName} 나타남, 보기 ${rabbitName}, 공격 거절 셋`);
    /** 보기를 되풀이해 그 방 room 메시지가 조건에 맞을 때까지 기다린다 */
    const lookUntil = (client, roomName, predicate, description, timeoutMs) => repeatUntil(() => {
      const m = client.send("보기");
      return client.waitFor(isRoom(roomName), `${client.label} 보기 room`, m);
    }, predicate, { intervalMs: 300, timeoutMs, description: `${client.label}: ${description}` });
    const mobLine = (room, name) => room.mobs?.find((mob) => mob.name === name);
    const expectMobLine = (room, expected) => {
      const line = mobLine(room, expected.name);
      if (!line || ["name", "rank", "tier", "count", "fighting"].some((key) => line[key] !== expected[key])) {
        fail(`${room.name} 의 몹 줄이 ${JSON.stringify(expected)} 가 아님: ${JSON.stringify(room.mobs)}`);
      }
    };
    const rabbitRoom = await lookUntil(e, "광장", (room) => mobLine(room, rabbitName), `광장의 ${rabbitName}`, 5000);
    expectMobLine(rabbitRoom, { name: rabbitName, rank: "1급", tier: null, count: 1, fighting: false });
    await expectSystem(e, `보기 ${rabbitName}`, `${rabbitName} (1급) — 멀쩡하다.`, "info");
    await expectSystem(e, "공격", "누구를 공격하시겠습니까? (예: 공격 늑대)", "error");
    await expectSystem(e, "공격 곰", "이곳에 '곰'은 없습니다.", "error");
    const npcRefusal = await expectSystemTone(e, `공격 ${npcName}`, "error");
    if (!npcRefusal.text.startsWith(npcName) || !npcRefusal.text.endsWith(" 공격할 수 있는 상대가 아닙니다.")) {
      fail(`NPC 공격 거절 문구가 다름: ${JSON.stringify(npcRefusal)}`);
    }

    step(`38. 관리자 — E 연기 7층 지정, 공격 ${rabbitName} → 교전·일격, 이동·종료 막힘, 상태 전투 줄, 맞음·처치(≤ 12초), E·D 가 처치 뒤 quiet room`);
    await designate(e, nameE, "QI_7", "경지가 연기 7층으로 바뀌었습니다. (육신 -24, 근력 -12, 영력 -23, 의식 -56, 정혈 -56)");
    const combatLine = (text) => (m) => m.type === "system" && m.tone === "combat" && m.text === text;
    const dAttackMark = d.mark();
    const attackMark = e.send(`공격 ${rabbitName}`);
    await e.waitFor(combatLine(`${rabbitName}와 싸움이 벌어진다.`), "교전 시작", attackMark);
    await e.waitFor(combatLine(`${rabbitName}에게 일격을 날린다. (피해 19)`), "첫 일격", attackMark);
    await expectSystem(e, "동", "싸우는 중에는 움직일 수 없습니다.", "error");
    await expectSystem(e, "종료", "싸우는 중에는 떠날 수 없습니다.", "error");
    if (e.closeEvent) fail(`E 연결이 종료로 닫힘: ${JSON.stringify(e.closeEvent)}`);
    status = await statusOf(e);
    if (!status.text.includes(`[전투 중] 상대: ${rabbitName}`)) fail(`E 상태에 전투 줄이 없음: ${JSON.stringify(status.text)}`);
    await e.waitFor(combatLine(`${rabbitName}가 뒷발차기로 덮친다! (피해 1, 기혈 119/120)`), "들토끼 공격", attackMark, 12000);
    await e.waitFor(combatLine(`${rabbitName}를 쓰러뜨렸습니다.`), "들토끼 처치", attackMark, 12000);
    const rabbitKilledRoom = isQuietRoom("광장", (m) => !mobLine(m, rabbitName)
      && m.corpses?.some((corpse) => corpse.name === `${rabbitName}의 시체`));
    await e.waitFor(rabbitKilledRoom, `처치 뒤 ${rabbitName} 없고 시체 있는 quiet room`, attackMark);
    await d.waitFor(rabbitKilledRoom, `처치 뒤 ${rabbitName} 없고 시체 있는 quiet room`, dAttackMark);

    step(`39. E 보기 → 시체, 수습 ${rabbitName} → ${rabbitHideName} 1, 소지품`);
    mark = e.send("보기");
    const corpseRoom = await e.waitFor(isRoom("광장"), "처치 뒤 보기", mark);
    if (JSON.stringify(corpseRoom.corpses) !== JSON.stringify([{ name: `${rabbitName}의 시체`, collectible: true }])
      || corpseRoom.mobs.length !== 0) {
      fail(`처치 뒤 광장의 시체·몹이 다름: ${JSON.stringify({ mobs: corpseRoom.mobs, corpses: corpseRoom.corpses })}`);
    }
    await expectSystemTone(e, `버리기 ${springScrollName}`, "item");
    // 수습이 자리 없음으로 실패하는 경우를 가려내려고 빈칸이 있음을 먼저 본다
    eInventory = await inventoryOf(e);
    if (eInventory.usedSlots >= eInventory.totalSlots) fail(`수습 전 E 소지품에 빈칸이 없음: ${JSON.stringify(eInventory)}`);
    const collected = await expectSystemTone(e, `수습 ${rabbitName}`, "item");
    // 칸이 모자라면 "저물대에 자리가 없습니다" 문구는 별도 error 메시지로 오므로 여기서는 첫 줄만 본다
    const [collectedFirst] = collected.text.split("\n");
    if (!collectedFirst.startsWith(`${rabbitName}의 시체를 수습했습니다: ${rabbitHideName} 1`)) {
      fail(`수습 문구가 다름: ${JSON.stringify(collected.text)}`);
    }
    eInventory = await inventoryOf(e);
    if (!eInventory.items.some((i) => i.name === rabbitHideName)) fail(`E 소지품에 ${rabbitHideName} 이 없음: ${JSON.stringify(eInventory.items)}`);

    step(`40. 재등장(≤ 16초) — ${rabbitName}가 나타난다(respawn), 숨을 고름(기혈 120/120), 보기 count 1`);
    await e.waitFor((m) => m.type === "notice" && m.event === "respawn" && m.text === `${rabbitName}가 나타난다.`,
      "재등장 알림", attackMark, 16000);
    await e.waitFor((m) => m.type === "system" && m.text === "숨을 고르자 기운이 돌아온다. (기혈 120/120)",
      "기혈 다시 참", attackMark, 16000);
    mark = e.send("보기");
    const respawnRoom = await e.waitFor(isRoom("광장"), "재등장 뒤 보기", mark);
    expectMobLine(respawnRoom, { name: rabbitName, rank: "1급", tier: null, count: 1, fighting: false });

    step(`41. 관리자 — E 연기 1층 지정, 동 → ${huntName} 의 ${bossName}, 공격 → 질 때(≤ 10초), 깨어남 기혈 1, 다시 참(≤ 15초)`);
    await designate(e, nameE, "QI_1", "경지가 연기 1층으로 바뀌었습니다. (육신 -6, 근력 -3, 영력 -6, 의식 -12, 정혈 -12)");
    mark = e.send("동");
    const huntRoom = await e.waitFor(isRoom(huntName), `${huntName} room`, mark);
    expectMobLine(huntRoom, { name: bossName, rank: "3급", tier: "두목", count: 1, fighting: false });
    const bossMark = e.send(`공격 ${bossName}`);
    await e.waitFor(combatLine("기혈이 다했습니다. 눈앞이 캄캄해진다…"), "질 때", bossMark, 10000);
    await e.waitFor(combatLine(`${square.name}에서 깨어났습니다. (기혈 1/60)`), "깨어남", bossMark, 10000);
    await e.waitFor(isRoom(square.name), "시작 방 room", bossMark, 10000);
    status = await statusOf(e);
    if (gauge(status.stats?.qiBlood) !== JSON.stringify({ current: 1, max: 60 })) fail(`진 뒤 E 기혈이 {1,60} 이 아님: ${JSON.stringify(status.stats?.qiBlood)}`);
    await e.waitFor((m) => m.type === "system" && m.text === "숨을 고르자 기운이 돌아온다. (기혈 60/60)",
      "진 뒤 기혈 다시 참", bossMark, 15000);
    status = await statusOf(e);
    expectGauge("E 기혈", status.stats?.qiBlood, 60);

    step("42. E 도움말 6차 문구(이 실행의 몹 배치 지우기·시작 소지품 되돌리기는 단계 51 로 옮김)");
    mark = e.send("도움말");
    const help5 = await e.waitFor((m) => m.type === "system" && m.text.startsWith("명령:"), "5차 도움말", mark);
    if (help5.text !== HELP_6) fail(`도움말이 6차 문구와 다름: ${JSON.stringify(help5.text)}`);

    // ---- 6차 ----
    step(`43. 관리자 — 기존 신통 채우기 확인, ${blazeName}·${emberName} 이펙트 문장 0·6개 400 → 셋 201, 옥간, 몹 ${ghostName} 배치`);
    const techniqueList = await api(admin, "GET", "/api/admin/techniques");
    for (const summary of techniqueList) {
      const detail = await api(admin, "GET", `/api/admin/techniques/${summary.id}`);
      for (const a of detail.abilities) {
        if (!(a.effectLines?.length >= 1)) fail(`공법 ${detail.name} 의 신통 ${a.name} 에 이펙트 문장이 없음: ${JSON.stringify(a)}`);
        const expectedPowers = detail.stages.length - a.learnStage + 1;
        if (a.kind === "ATTACK" && a.stagePowers?.length !== expectedPowers) {
          fail(`공법 ${detail.name} 의 공격 신통 ${a.name} 단계별 값이 ${expectedPowers}개가 아님: ${JSON.stringify(a.stagePowers)}`);
        }
      }
    }
    const emberLines = [
      "붉은 불씨가 {대상}을 감싸 터진다!",
      "{시전자}의 손끝에서 튄 불꽃이 {대상}에게 꽂힌다!",
      "작은 화염이 꼬리를 끌며 {대상}로 날아든다!",
      "{대상}이마를 스친다!",
      "{대상}으로부터 불티가 튄다!",
    ];
    const blazeRequest = (effectLines) => ({
      name: blazeName, description: "", elements: ["WOOD"], startRealm: "MORTAL",
      stages: [stage(0), stage(200)],
      abilities: [ability(emberName, 1, "ATTACK", ["FIRE"], "SINGLE", 10, 3, "COMBAT_ONLY", {
        effectLines, stagePowers: [{ stage: 1, basePower: 2, coefficient: 1.2 }, { stage: 2, basePower: 2.2, coefficient: 1.2 }],
      })],
    });
    const lineCountDetail = `이펙트 문장은 1~5개로 정하십시오: ${emberName}`;
    const noLines = await api(admin, "POST", "/api/admin/techniques", blazeRequest([]), { expect: 400 });
    if (noLines.detail !== lineCountDetail) fail(`이펙트 문장 0개 detail 이 다름: ${noLines.detail}`);
    const sixLines = await api(admin, "POST", "/api/admin/techniques",
      blazeRequest([...emberLines, "불씨가 사방으로 흩어진다!"]), { expect: 400 });
    if (sixLines.detail !== lineCountDetail) fail(`이펙트 문장 6개 detail 이 다름: ${sixLines.detail}`);
    const blaze = await api(admin, "POST", "/api/admin/techniques", blazeRequest(emberLines), { expect: 201 });
    const blazeDetail = await api(admin, "GET", `/api/admin/techniques/${blaze.id}`);
    const ember = blazeDetail.abilities.find((a) => a.name === emberName);
    if (JSON.stringify(ember?.effectLines) !== JSON.stringify(emberLines) || Number(ember.stagePowers[1]?.basePower) !== 2.2
      || ember.usableInGame !== true) {
      fail(`${emberName} 상세가 다름: ${JSON.stringify(ember)}`);
    }
    const blazeScroll = await scrollType(blazeScrollName, 100, blaze.id);
    await api(admin, "POST", `/api/admin/rooms/${square.id}/floor-items`,
      { itemTypeId: blazeScroll.id, count: 1, respawnMinutes: null }, { expect: 201 });
    const ghost = await api(admin, "POST", "/api/admin/mobs", mobRequest({
      name: ghostName, kind: "GHOST", realm: "QI_1", tier: "ELITE", attackName: "서늘한 손길", respawnSeconds: 30,
    }), { expect: 201 });
    if (JSON.stringify(ghost.stats) !== JSON.stringify({ qiBlood: 120, body: 3, power: 7 })) {
      fail(`${ghostName} stats 가 {120,3,7} 이 아님: ${JSON.stringify(ghost)}`);
    }
    await placeMob(square, ghost);

    step(`44. E 버리기 ${rabbitHideName} → 빈칸, 줍기·읽기 ${blazeScrollName} → ${emberName}, 신통 목록, 법력 25/25`);
    await expectSystemTone(e, `버리기 ${rabbitHideName}`, "item");
    eInventory = await inventoryOf(e);
    if (eInventory.usedSlots >= eInventory.totalSlots) fail(`버린 뒤에도 E 소지품에 빈칸이 없음: ${JSON.stringify(eInventory)}`);
    await expectSystem(e, `줍기 ${blazeScrollName}`, `${blazeScrollName} 1개를 주웠습니다.`, "item");
    const blazeRead = await expectSystemTone(e, `읽기 ${blazeScrollName}`, "growth");
    const [blazeFirst, blazeSecond] = blazeRead.text.split("\n");
    if (blazeFirst !== `옥간을 이마에 대자 구결이 머릿속으로 흘러든다. ${blazeName} 1성에 이르렀습니다!`
      || blazeSecond !== `새 신통을 깨쳤습니다: ${emberName}`) {
      fail(`${blazeScrollName} 읽기 문구가 다름: ${JSON.stringify(blazeRead.text)}`);
    }
    mark = e.send("신통");
    const abilityList = await e.waitFor((m) => m.type === "system" && m.text.startsWith("["), "신통 목록", mark);
    if (!abilityList.text.split("\n").includes(`  ${emberName}  공격·화  법력 10  대기 3초`)) {
      fail(`신통 목록에 ${emberName} 줄이 없음: ${JSON.stringify(abilityList.text)}`);
    }
    status = await statusOf(e);
    if (!status.text.includes("법력  25 / 25")) fail(`E 상태에 법력  25 / 25 가 없음: ${JSON.stringify(status.text)}`);

    step(`45. E 보기 → ${ghostName}, ${emberName} ${ghostName} → 교전 시작·이펙트 줄 (피해 11, 법력 15/25)`);
    await lookUntil(e, "광장", (room) => mobLine(room, ghostName), `광장의 ${ghostName}`, 5000);
    const emberRenders = [
      `붉은 불씨가 ${ghostName}를 감싸 터진다!`,
      `${nameE}의 손끝에서 튄 불꽃이 ${ghostName}에게 꽂힌다!`,
      `작은 화염이 꼬리를 끌며 ${ghostName}로 날아든다!`,
      // 이름에 바로 붙은 '이마' 는 조사가 아니라 그대로, '으로부터' 는 조사라 받침에 맞춰 고침(#19)
      `${ghostName}이마를 스친다!`,
      `${ghostName}로부터 불티가 튄다!`,
    ];
    const isEffectLine = (m) => m.type === "system" && m.tone === "combat"
      && emberRenders.some((r) => m.text.startsWith(`${r} (피해 `));
    const emberCommand = `${emberName} ${ghostName}`;
    const ghostMark = e.send(emberCommand);
    const engaged = await e.waitFor(combatLine(`${ghostName}와 싸움이 벌어진다.`), "음혼 교전 시작", ghostMark);
    const engagedIndex = e.messages.indexOf(engaged);
    const firstEffect = e.messages[engagedIndex + 1] ?? await e.waitFor(() => true, "교전 시작 다음 줄", engagedIndex + 1);
    if (!isEffectLine(firstEffect) || !firstEffect.text.endsWith(" (피해 11, 법력 15/25)")) {
      fail(`교전 시작 다음 줄이 이펙트 줄 (피해 11, 법력 15/25) 이 아님: ${JSON.stringify(firstEffect)}`);
    }

    step(`46. E 곧바로 ${emberName} → 공통 대기 거절, 재사용 대기 거절(≤ 3초), 다시 성공(≤ 4초) → 법력 5~9/25`);
    /** 신통 명령을 보내고 그 응답(error 줄 또는 이펙트 줄)을 받는다 */
    const useEmber = async () => {
      const m = e.send(emberCommand);
      return e.waitFor((x) => (x.type === "system" && x.tone === "error") || isEffectLine(x), `${emberCommand} 응답`, m);
    };
    /** 300ms 간격으로 신통 명령을 되풀이해 조건에 맞는 응답을 받는다 */
    const useEmberUntil = (predicate, description, timeoutMs) =>
      repeatUntil(useEmber, predicate, { intervalMs: 300, timeoutMs, description: `E: ${description}` });
    const globalRefusal = await useEmber();
    if (globalRefusal.tone !== "error" || !globalRefusal.text.startsWith("아직 기운을 고르는 중입니다. (")) {
      fail(`곧바로 쓴 ${emberName} 가 공통 대기 거절이 아님: ${JSON.stringify(globalRefusal)}`);
    }
    await useEmberUntil((x) => x.tone === "error" && x.text.startsWith(`${emberName}는 아직 쓸 수 없습니다. (`),
      "재사용 대기 거절", 3000);
    const secondEffect = await useEmberUntil(isEffectLine, "두 번째 이펙트 줄", 4000);
    if (!/ \(피해 11, 법력 [5-9]\/25\)$/.test(secondEffect.text)) {
      fail(`두 번째 이펙트 줄이 (피해 11, 법력 5~9/25) 로 끝나지 않음: ${JSON.stringify(secondEffect.text)}`);
    }
    status = await statusOf(e);
    const manaAfterCasts = status.stats?.mana?.current;
    if (!(manaAfterCasts < 10)) fail(`두 번 쓴 뒤 E 법력이 10 미만이 아님: ${JSON.stringify(status.stats?.mana)}`);

    step(`47. 재사용 대기가 끝날 때까지 되풀이(≤ 4초) → 법력이 모자랍니다`);
    const lowMana = await useEmberUntil((x) => isEffectLine(x) || (x.tone === "error" && x.text.startsWith("법력이 모자랍니다. (")),
      "법력 부족 거절", 4000);
    const lowManaMatch = /^법력이 모자랍니다\. \(필요 10, 남은 것 (\d+)/.exec(lowMana.text);
    if (!lowManaMatch || Number(lowManaMatch[1]) >= 10) fail(`법력 부족 거절이 다름: ${JSON.stringify(lowMana)}`);

    step("48. E 상태 깨달음 줄·status.insight, 공법 (지금 10), 관리자 캐릭터 상세 insight");
    status = await statusOf(e);
    if (!status.text.split("\n").includes("깨달음  금 0 · 목 10 · 수 0 · 화 20 · 토 0 · 연체 0")) {
      fail(`E 상태 깨달음 줄이 다름: ${JSON.stringify(status.text)}`);
    }
    const insightAmount = (list, kind) => list?.find((i) => i.kind === kind)?.amount;
    if (insightAmount(status.insight, "FIRE") !== 20 || insightAmount(status.insight, "WOOD") !== 10) {
      fail(`E status.insight 가 FIRE 20·WOOD 10 이 아님: ${JSON.stringify(status.insight)}`);
    }
    mark = e.send("공법");
    const techniqueText = await e.waitFor((m) => m.type === "system" && m.tone === "info" && m.text.includes(springName), "공법", mark);
    const blazeLine = techniqueText.text.split("\n").find((line) => line.startsWith(`${blazeName}  `));
    if (!blazeLine?.includes("목 깨달음 200 (지금 10)")) fail(`공법의 ${blazeName} 줄이 다름: ${JSON.stringify(techniqueText.text)}`);
    const eDetail = await api(admin, "GET", `/api/admin/characters/${eSummary.id}`);
    const fireInsight = eDetail.insight?.find((i) => i.kind === "FIRE");
    if (JSON.stringify(fireInsight) !== JSON.stringify({ kind: "FIRE", name: "화", amount: 20 })) {
      fail(`관리자 캐릭터 상세 insight 의 FIRE 가 다름: ${JSON.stringify(eDetail.insight)}`);
    }

    step(`49. 기다림(≤ 45초) — ${ghostName}를 쓰러뜨렸습니다, E 는 지지 않음, ${ghostName} 없는 quiet room`);
    const ghostEnd = await e.waitFor((m) => combatLine(`${ghostName}를 쓰러뜨렸습니다.`)(m)
      || (m.type === "system" && m.text.startsWith("기혈이 다했습니다.")), "음혼 처치", ghostMark, 45000);
    if (ghostEnd.text.startsWith("기혈이 다했습니다.")) fail(`E 가 ${ghostName} 에게 짐: ${JSON.stringify(ghostEnd)}`);
    await e.waitFor(isQuietRoom("광장", (m) => !mobLine(m, ghostName)), `처치 뒤 ${ghostName} 없는 quiet room`, ghostMark);

    step(`50. 법력 회복 — E 상태 되풀이(≤ 15초) → 법력이 ${manaAfterCasts} 보다 커짐`);
    await repeatUntil(() => statusOf(e), (s) => s.stats?.mana?.current > manaAfterCasts,
      { intervalMs: 1000, timeoutMs: 15000, description: `E: 법력이 ${manaAfterCasts} 보다 커진 상태` });

    step("51. E 도움말 6차 문구, 관리자 — 이 실행의 몹 배치 지움 → 광장에서 몹이 사라짐, 시작 소지품 되돌림");
    mark = e.send("도움말");
    const help6 = await e.waitFor((m) => m.type === "system" && m.text.startsWith("명령:"), "6차 도움말", mark);
    if (help6.text !== HELP_6) fail(`도움말이 6차 문구와 다름: ${JSON.stringify(help6.text)}`);
    while (mobPlacementIds.length > 0) {
      await api(admin, "DELETE", `/api/admin/mob-placements/${mobPlacementIds[0]}`, undefined, { expect: 204 });
      mobPlacementIds.shift();
    }
    await lookUntil(e, "광장", (room) => !mobLine(room, rabbitName) && !mobLine(room, ghostName),
      `광장에서 ${rabbitName}·${ghostName} 사라짐`, 5000);
    const backToInitial = await api(admin, "PUT", "/api/admin/world/starting-items", { items: initialStartingItems });
    if (JSON.stringify(backToInitial.map(({ itemTypeId, count }) => ({ itemTypeId, count }))) !== JSON.stringify(initialStartingItems)) {
      fail(`시작 소지품이 스모크 시작 때 값으로 돌아가지 않음: ${JSON.stringify(backToInitial)}`);
    }
    initialStartingItems = null;

    // 부채 정리 2차 F-2: #18(교전 중 막힌 확인)은 스모크로 보지 않고 단위 테스트로만 본다
    step("52. G 접속(이름 prompt) → 같은 쿠키로 로그아웃 → G 가 AUTH_REQUIRED 안내를 받고 4401 로 닫힘");
    const playerG = await devLogin(`smoke-g-${runId}@example.com`);
    const g = connect("G", playerG.cookie);
    await g.opened;
    await g.waitFor((m) => m.type === "prompt" && m.text.startsWith("이름을 정하십시오"), "이름 prompt");
    const gMark = g.mark();
    await api(playerG, "POST", "/api/auth/logout", undefined, { expect: 204 });
    const ended = await g.waitFor((m) => m.type === "error", "로그아웃 뒤 error", gMark);
    if (ended.code !== "AUTH_REQUIRED" || ended.text !== "로그인이 끝났습니다. 다시 로그인하십시오.") {
      fail(`로그아웃 뒤 error 가 다름: ${JSON.stringify(ended)}`);
    }
    await g.waitClose(4401);

    step(`53. 관리자 — 몹 ${ratName}(강적 연기 5층) 을 광장에 두고 E 가 공격 → 몹 삭제 204 → 광장에서 사라지고 E 교전이 끝남`);
    const rat = await api(admin, "POST", "/api/admin/mobs", mobRequest({
      name: ratName, realm: "QI_5", tier: "STRONG", attackName: "앞니",
    }), { expect: 201 });
    // 몹 삭제가 이 배치를 함께 지우므로 mobPlacementIds 에 넣지 않는다(끝 정리에서 400 이 남)
    await api(admin, "POST", `/api/admin/rooms/${square.id}/mobs`, { mobId: rat.id, count: 1 }, { expect: 201 });
    await lookUntil(e, "광장", (room) => mobLine(room, ratName), `광장의 ${ratName}`, 5000);
    const ratMark = e.send(`공격 ${ratName}`);
    await e.waitFor(combatLine(`${ratName}와 싸움이 벌어진다.`), `${ratName} 교전 시작`, ratMark);
    await api(admin, "DELETE", `/api/admin/mobs/${rat.id}`, undefined, { expect: 204 });
    await e.waitFor(isQuietRoom("광장", (m) => !mobLine(m, ratName)), `몹 삭제 뒤 ${ratName} 없는 quiet room`, ratMark);
    const killedRat = e.messages.slice(ratMark).find(combatLine(`${ratName}를 쓰러뜨렸습니다.`));
    if (killedRat) fail(`몹 삭제 전에 E 가 ${ratName} 를 쓰러뜨림(삭제 때 교전 중이 아니었음): ${JSON.stringify(killedRat)}`);
    const lost = e.messages.slice(ratMark).find((m) => m.type === "system" && m.text?.startsWith("기혈이 다했습니다."));
    if (lost) fail(`몹 삭제 전에 E 가 ${ratName} 에게 짐: ${JSON.stringify(lost)}`);
    const squareAfterRat = await api(admin, "GET", `/api/admin/rooms/${square.id}`);
    if (squareAfterRat.mobs.some((placement) => placement.mobId === rat.id)) {
      fail(`몹 삭제 뒤 광장 상세 mobs 에 ${ratName} 배치가 남음: ${JSON.stringify(squareAfterRat.mobs)}`);
    }
    const ratGone = await api(admin, "GET", `/api/admin/mobs/${rat.id}`, undefined, { expect: 400 });
    if (ratGone.detail !== `몹을 찾을 수 없습니다. id: ${rat.id}`) fail(`지운 몹 detail 이 다름: ${ratGone.detail}`);
    mark = e.send("동");
    await e.waitFor(isRoom(huntName), `몹 삭제 뒤 ${huntName} room`, mark);
    mark = e.send("서");
    await e.waitFor(isRoom("광장"), "몹 삭제 뒤 돌아온 광장 room", mark);

    step("54. 관리자 — 접속 중 E 삭제 409 → E 종료 → 삭제 204 → 상세 400·목록에 없음 → E 계정 다시 접속하면 이름 prompt");
    const eId = (await characterNamed(nameE)).id;
    const online = await api(admin, "DELETE", `/api/admin/characters/${eId}`, undefined, { expect: 409 });
    if (online.detail !== `접속 중인 캐릭터는 지울 수 없습니다. 접속을 끊은 뒤 다시 시도하십시오: ${nameE}`) {
      fail(`접속 중 캐릭터 삭제 detail 이 다름: ${online.detail}`);
    }
    e.send("종료");
    await e.waitClose(1000);
    await api(admin, "DELETE", `/api/admin/characters/${eId}`, undefined, { expect: 204 });
    const eGone = await api(admin, "GET", `/api/admin/characters/${eId}`, undefined, { expect: 400 });
    if (eGone.detail !== `캐릭터를 찾을 수 없습니다. id: ${eId}`) fail(`지운 캐릭터 detail 이 다름: ${eGone.detail}`);
    const charactersAfter = await api(admin, "GET", "/api/admin/characters");
    if (charactersAfter.some((ch) => ch.name === nameE || ch.id === eId)) fail(`캐릭터 목록에 지운 ${nameE} 이 남음`);
    const e2 = connect("E2", playerE.cookie);
    await e2.opened;
    await e2.waitFor((m) => m.type === "session" && m.state === "creating", "삭제 뒤 session(creating)");
    await e2.waitFor((m) => m.type === "prompt" && m.text === "이름을 정하십시오. (한글 2~6자)", "삭제 뒤 이름 prompt");
    e2.close();

    step(`55. H 접속(${nameH}, 광장) → ㅂ 북쪽 길 → ㄴ 광장 → ㄷ ${huntName} → ㅅ 광장 → 보·봐 광장 room → ㅅㅌ status → 도움말 새 문구 → 종료`);
    const playerH = await devLogin(`smoke-h-${runId}@example.com`);
    const h = connect("H", playerH.cookie);
    await createCharacter(h, nameH);
    for (const [input, roomName] of [["ㅂ", "북쪽 길"], ["ㄴ", "광장"], ["ㄷ", huntName], ["ㅅ", "광장"], ["보", "광장"], ["봐", "광장"]]) {
      mark = h.send(input);
      await h.waitFor(isRoom(roomName), `H ${input} → ${roomName} room`, mark);
    }
    mark = h.send("ㅅㅌ");
    const hStatus = await h.waitFor((m) => m.type === "status" || m.type === "system", "H ㅅㅌ", mark);
    if (hStatus.type !== "status") fail(`H ㅅㅌ 가 상태가 아님: ${JSON.stringify(hStatus)}`);
    mark = h.send("도움말");
    const helpH = await h.waitFor((m) => m.type === "system", "H 도움말", mark);
    if (helpH.text !== HELP_6) fail(`H 도움말이 방위 초성 약자 문구와 다름: ${JSON.stringify(helpH.text)}`);
    h.send("종료");
    await h.waitClose(1000);

    console.log(`SMOKE OK (지역 "스모크 ${runId}" id=${zone.id}, 시작 방 광장 id=${square.id}, 성 ${castleName}, NPC ${npcName}·${merchantName}, 공법 ${springName}·${greenName}·${blazeName}, 몹 ${ghostName})`);
  } finally {
    clients.forEach((c) => c.close());
    if (admin && originalStartingItems) {
      try {
        await api(admin, "PUT", "/api/admin/world/starting-items", { items: originalStartingItems });
        console.log("- (정리) 시작 소지품을 처음 값으로 되돌림");
      } catch (error) {
        console.error(`- (정리) 시작 소지품 되돌리기 실패: ${error.message ?? error}`);
      }
    }
    if (admin) {
      for (const placementId of mobPlacementIds) {
        try {
          await api(admin, "DELETE", `/api/admin/mob-placements/${placementId}`, undefined, { expect: 204 });
          console.log(`- (정리) 몹 배치 ${placementId} 지움`);
        } catch (error) {
          console.error(`- (정리) 몹 배치 ${placementId} 지우기 실패: ${error.message ?? error}`);
        }
      }
    }
    if (admin && initialStartingItems) {
      try {
        await api(admin, "PUT", "/api/admin/world/starting-items", { items: initialStartingItems });
        console.log("- (정리) 시작 소지품을 스모크 시작 때 값으로 되돌림");
      } catch (error) {
        console.error(`- (정리) 시작 소지품 되돌리기 실패: ${error.message ?? error}`);
      }
    }
  }
}

run().then(
  () => process.exit(0),
  (error) => {
    console.error(`SMOKE FAIL: ${error instanceof SmokeFailure ? error.message : error.stack ?? error}`);
    process.exit(1);
  },
);
