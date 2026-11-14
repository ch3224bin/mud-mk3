#!/usr/bin/env node
// compose 로 띄운 세 서비스(world-server)에 대고 1차·2차·3차 시나리오를 자동으로 확인한다.
// 사용: node scripts/smoke-test.mjs [baseUrl=http://localhost:8080]
// 전제: world-server 가 SPRING_PROFILES_ACTIVE=local, MUD_DEV_LOGIN_ENABLED=true 로 떠 있고
//       MUD_ADMIN_EMAILS 에 관리자 이메일(기본 smoke-admin@example.com, 바꾸려면 SMOKE_ADMIN_EMAIL)이 들어 있다.
// Node 26 내장 fetch·WebSocket 만 쓴다(의존성 없음). 실패하면 무엇이 기대와 달랐는지 출력하고 종료 코드 1.
// 실행마다 지역·플레이어 이메일·캐릭터 이름·성·종문·NPC·아이템 종류 이름에 실행 번호를 섞는다. 만든 데이터는 지우지 않는다.
// 시작 소지품은 3차 단계에서 바꾸고 끝에(실패해도) 처음 값으로 되돌린다. 시작 방은 그 실행의 광장으로 남는다.

const baseUrl = (process.argv[2] ?? "http://localhost:8080").replace(/\/$/, "");
const wsUrl = baseUrl.replace(/^http/, "ws") + "/ws/game";
const adminEmail = process.env.SMOKE_ADMIN_EMAIL ?? "smoke-admin@example.com";
const ADMIN_ORIGIN = "http://localhost:3001";
const PLAY_ORIGIN = "http://localhost:3000";
const TIMEOUT_MS = 5000;

const runId = Date.now().toString(36);
// NPC 이름은 띄어쓰기 없이 12자까지라 숫자 6자리만 붙인다(성·종문·NPC 이름)
const runNo = String(Date.now() % 1_000_000).padStart(6, "0");
const castleName = `청석성${runNo}`;
const sectName = `황풍곡${runNo}`;
const npcName = `왕씨${runNo}`;
const innNpcName = `점소이${runNo}`;
const randomHangul = () => String.fromCodePoint(0xac00 + Math.floor(Math.random() * (0xd7a3 - 0xac00 + 1)));
const nameA = "갑사" + randomHangul() + randomHangul();
const nameB = "을사" + randomHangul() + randomHangul();
const nameC = "병사" + randomHangul() + randomHangul();
const nameD = "정사" + randomHangul() + randomHangul();
// 3차 아이템 종류·상점 NPC. 숫자로 끝나는 이름은 조사 규칙상 받침 없음(회춘단123456는·를·와·가)
const bagName = `하급 저물대${runNo}`;
const pillName = `회춘단${runNo}`;
const hideName = `늑대 가죽${runNo}`;
const swordName = `비검${runNo}`;
const merchantName = `상인${runNo}`;

class SmokeFailure extends Error {}

function fail(message) {
  throw new SmokeFailure(message);
}

function step(text) {
  console.log(`- ${text}`);
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
  console.log(`스모크 테스트 ${baseUrl} (실행 번호 ${runId}, 관리자 ${adminEmail}, 캐릭터 ${nameA}·${nameB}·${nameC}·${nameD})`);
  const clients = [];
  let admin = null;
  let originalStartingItems = null;
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
    if (bag.categoryName !== "저물대" || bag.bagGradeName !== "하급" || bag.bagSlots !== 20) {
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

    step(`21. C 보기·줍기·버리기·물건 보기 — D가 pick·drop 알림을 받음`);
    const floorLook = async () => {
      const m = c.send("보기");
      return (await c.waitFor(isRoom("광장"), "C 보기 room", m)).items;
    };
    let items = await floorLook();
    if (!items.some((i) => i.name === hideName && i.count === 3)) fail(`광장 바닥에 ${hideName} 3 이 없음: ${JSON.stringify(items)}`);
    let dMark = d.mark();
    await expectSystem(c, `줍기 ${hideName} 2`, `${hideName} 2개를 주웠습니다.`, "item");
    await d.waitFor(isNotice("pick", nameC), `${nameC} pick`, dMark);
    dMark = d.mark();
    await expectSystem(c, `버리기 ${hideName} 1`, `${hideName} 1개를 버렸습니다.`, "item");
    await d.waitFor(isNotice("drop", nameC), `${nameC} drop`, dMark);
    items = await floorLook();
    if (!items.some((i) => i.name === hideName && i.count === 2)) fail(`광장 바닥 ${hideName} 이 2(배치 1 + 버린 1)가 아님: ${JSON.stringify(items)}`);
    await expectSystem(c, `보기 ${hideName}`, `${hideName} — 거칠고 질긴 늑대의 가죽이다.`, "item");

    step(`22. C 목록·사기·팔기·값 — 영석 30 → 24 → 25, 재료는 단약방이 사지 않음`);
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
    if (!help3.text.includes("주기 <대상> <물건> [수량]") || help3.text.includes("귀속")) {
      fail(`도움말에 주기가 없거나 귀속이 남아 있음: ${help3.text}`);
    }

    step("25. 관리자 — 시작 소지품을 처음 값으로 되돌림");
    const restored = await api(admin, "PUT", "/api/admin/world/starting-items", { items: originalStartingItems });
    const restoredItems = restored.map(({ itemTypeId, count }) => ({ itemTypeId, count }));
    if (JSON.stringify(restoredItems) !== JSON.stringify(originalStartingItems)) {
      fail(`시작 소지품이 처음 값으로 돌아가지 않음: ${JSON.stringify(restored)}`);
    }
    originalStartingItems = null;

    console.log(`SMOKE OK (지역 "스모크 ${runId}" id=${zone.id}, 시작 방 광장 id=${square.id}, 성 ${castleName}, NPC ${npcName}·${merchantName})`);
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
  }
}

run().then(
  () => process.exit(0),
  (error) => {
    console.error(`SMOKE FAIL: ${error instanceof SmokeFailure ? error.message : error.stack ?? error}`);
    process.exit(1);
  },
);
