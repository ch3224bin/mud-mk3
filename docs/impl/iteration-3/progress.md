# 3차 이터레이션 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| Task 1 아이템 종류 도메인·영속·관리 서비스, 영석 심기, 시계 | 9155e4c | `item` 슬라이스(ItemType·ItemCategory·BagGrade·SpiritStoneGrade·ItemQuery·ItemNames·Slots·Pricing), 영석 4종 시작 시 심기, `Josa` 은/는·을/를·과/와, `Numbers`, `Clock` 빈과 `MutableClock`. 계획에 더한 것: `SpiritStoneGrade.description()`(D35 묘사), `ItemNames.normalize`(이름 공백 정리 — 도메인과 `ItemTypeSaveRequest` 컴팩트 생성자가 함께 씀), `ItemQuery.hasOrdinal()`. `isInUse` 는 0을 돌려주는 자리(뒤 태스크가 count를 더함). `ItemTypeFinder.spiritStone` 없음 문구 `영석 종류가 없습니다: 하품 영석`. |
| Task 2 아이템 종류 관리 REST API | 55722b6 | `/api/admin/item-types` 5개(`ItemTypeApi`, `ItemTypeResponse.of`), 409 처리 4개(`DuplicateItemTypeNameException`·`ItemTypeInUseException`·`SpiritStoneChangeException`, DB 제약 위반 `DataIntegrityViolationException` — DB 메시지는 내보내지 않고 고정 문구, warn 로그). 리드 판정대로 영석에 형식이 틀린 `PUT` 은 `@Valid` 가 먼저 400(웹 테스트 `updateSpiritStoneInvalidRequestFail`). 단건 조회 없는 id 400. |
| Task 3 소지품 도메인·결제·영속·소지품 서비스 | 6005774, b4dc4ad | `inventory` 슬라이스(Inventory·InventoryEntry·SpiritStonePayment·AddCheck·RemoveCheck·PurchaseResult·SaleResult·GiveResult), 묶음 유일은 도메인(DB 제약 없음), `InventoryFinder`·`InventoryEditor`, 아이템 종류 사용 검사에 소지품(`countInInventories`). 계획에 더한 것: `SpiritStonePayment.paidValue()`. 도메인은 바꿀 내용을 묶음별 증감으로 모아 한 번에 계산·적용하므로 결제에서 하품을 다 내고 거스름을 받으면 같은 묶음의 순증감 하나로 처리된다. `give` 는 귀속된 묶음(`bound=true`)·자기 자신이면 `IllegalStateException`(게임이 먼저 거름), 받는 쪽은 늘 귀속 안 된 묶음. `add(…, bound=true)` 로 영석을 귀속할 수 없다. 관리 API 통합 테스트에 소지품에 든 종류 삭제·종류 변경 409. |
| Task 4 시작 소지품 관리·캐릭터 생성 때 지급 | 9a8ce64 | `WorldSettings.startingItems`(`world_starting_item`), `GET/PUT /api/admin/world/starting-items`, 생성 트랜잭션에서 지급, 기존 캐릭터는 빈 소지품. 계획에 더한 것: `Inventory.addAll(SequencedMap)` — 시작 소지품을 다 넣은 뒤 칸으로 한 번만 검사(저물대가 목록 뒤에 있어도 저장 검사와 같게 들어감). 저장 검사 순서: 줄마다 없는 물건·같은 물건 → 저물대 네 개 이상 → 칸 넘침(수는 `Numbers.count`). 아이템 종류 사용 검사에 `countInStartingItems`. |
| Task 5 방 바닥 물건 — 배치·버린 물건·읽을 때 만료, 관리 REST | 8886153 | `floor` 슬라이스(FloorPlacement·DroppedItem·FloorStacks·RoomFloor), 읽을 때 만료·재등장, 방 삭제 때 정리, 바닥 관리 API 3개와 방 상세 `floorItems`. 계획에 더한 것: `DroppedItem.isEmpty()`(다 주운 행은 서비스가 지움), `FloorStacks` 는 그 시각에 0인 배치·사라진 버린 물건을 묶기 전에 빼므로 순서 자리도 차지하지 않는다. `RoomFloor.take` 는 있는 만큼만 덜고(요청보다 적을 수 있음) 덜 수를 돌려준다. `deleteExpiredDroppedItems` 는 `@Modifying(flushAutomatically, clearAutomatically)`(같은 트랜잭션에 읽힌 버린 물건이 flush를 막지 않게). 종류 사용 검사에 바닥 배치(남은 수 0도 셈)·사라지지 않은 버린 물건. `@ApplicationServiceTest` 에 `MutableClockReset` 확장(테스트마다 시계를 시작 시각으로). |
| Task 6 NPC 상점 역할 — 도메인·관리 서비스·REST (+2차 미룬 NPC Minor) | 5e87134 | `ShopKind`·`ShopItem`·`ShopOffer`·`Npc.openShop`/`closeShop`, `npc.shop_kind`·`npc_shop_item`, 상점 API 2개(`PUT/DELETE /api/admin/npcs/{id}/shop`, 상세 `shop`, 목록 `shopKindName`), 기준가 변경 불변식 검사(409), 종류 사용 검사에 `countInShops`. 2차 Minor: Npc 도메인 길이·낱말 수 검사, NpcSaveInfo 애노테이션 제거, NpcRepository `existsByName*` 둘 제거. 계획에 더한 것: `ShopItem` 컴팩트 생성자가 판매가 1~100,000,000 검사(`판매가는 1 이상 100,000,000 이하로 정하십시오.`), `Pricing.isFairSellPrice(ItemCategory, long, long)` 오버로드(`Math.multiplyExact`), `findShopPrices` 는 `ShopPrice(npcName, price)` 투영 record(NPC 이름순). 기준가 변경 409 문구는 어긴 상점이 여럿이면 `(왕씨 2, 이씨 3)` 처럼 `, ` 로 잇는다. 상점 저장 400 문구의 매입가는 `Numbers.decimal`(쉼표 없음), 판매가는 `Numbers.count`. |
| Task 7 게임 — 물건 명령 해석, 방 바닥, 소지품·줍기·버리기·물건 보기 | 17103d6 | `Amount`·`ItemArguments`·`Command.Inventory/Pick/Drop`, `RoomMessage.items`(`FloorItemView`)·`InventoryMessage`(`StoneView`·`ItemView`)·`SystemMessage.item`·`NoticeMessage.item`(tone `item`), `ItemCommandHandler`(package-private `@Component`), `GameService` 방 보기마다 `roomFloor.look`, 동시 줍기 테스트. 계획에 더한 것: `Amount.MAX_COUNT`(999,999,999, `Count` 컴팩트 생성자 검사 — 앞자리 0은 떼고 셈), 버리기 검사 순서는 주기(D44)와 같게 없음 → 퀘스트 → 귀속 → 수 넘침 → 저물대 칸, 버리기에서 번호 없이 고른 귀속 묶음은 같은 종류의 귀속 안 된 묶음으로(D46), `그런 것은 보이지 않습니다.` 는 tone `error`·사람/NPC는 `info`·물건 묘사는 `item`, 방에 없음 문구의 입력은 `ItemQuery` 로 되살림(번호 포함, 영석 등급 낱말은 `하품 영석` 처럼 전체 이름). 2차 `lookWithArgument` 는 `보기 한립` 이 없는 사람이면 이제 `그런 것은 보이지 않습니다.` 라 한립을 같은 방에 들여 기대값을 지킴. WebSocket 통합 테스트 정리는 `item_type` 을 TRUNCATE 에서 빼고 영석 밖 행만 지움. 도움말(D56)은 Task 8. |
| Task 8 게임 — 상점(목록·사기·팔기·값·확인·취소), 주기, 귀속, 도움말 | ea91fe4 | `Command` 상점·주기·귀속·확인·취소(`ShopList`·`Buy`·`Sell`·`Appraise`·`Give`·`Bind`·`Unbind`·`Confirm`·`Cancel`), `ShopCommandHandler`(package-private `@Component`, `Clock` 주입), `GameSession.PendingPurchase`(10초, 정확히 10초는 아직 유효), 주기 알림(`give`), 귀속·귀속 해제, 도움말 D56. 계획과 다른 점: `PurchaseResult.NoRoomForChange.slotsNeeded` → `slotsShort`(거스름까지 넣으면 실제로 모자란 칸 — 문구 `N칸 필요` 용, 도메인 테스트 추가). `ItemArguments.hasAmount`(값에서 수량을 뺐을 때 가진 수를 쓰려고). 판정: 깨는 돌은 늘 마지막 낸 등급 1개라 확인 문구는 `{등급} 영석 1개`. `그만두었습니다.` 는 tone `info`, `확인할 것이 없습니다.`·`그만둘 것이 없습니다.` 는 `error`, 확인 질문·목록·값·성공은 `item`. 주기 대상: 이름 전체 일치(나 포함) → 앞부분 일치 다른 사람 → 앞부분이 내 이름이면 나(자기 자신 문구). 상점이 둘 이상인데 인자가 비면 `상인이 여럿입니다…`. `확인` 때 상점이 방에 없거나 상점이 아니면 `이곳에서는 할 수 없습니다.`, 물건을 더 팔지 않으면 `{NPC}: "그런 물건은 팔지 않소."`. 귀속·귀속 해제 인자는 이름 전체(수량 해석 없음). |
| Task 9 admin-console — 아이템 종류와 시작 소지품 화면 | bc03045 | `/items/`·`/item/`·`/start-items/`, `items.ts`(종류·저물대 등급·상점 종류, `bagGradeLabel`·`formatCount`), 공통 바 링크 둘(`아이템`·`시작 소지품`), `api.ts` 아이템 종류 5개·시작 소지품 2개, `ItemForm`·`StartItemsEditor`. 판정: 영석이면 폼 칸을 모두 `disabled` 로 두고 `저장`·`삭제` 버튼을 숨김(영석일 때만 종류 선택에 `영석` 을 보임). 새 시작 소지품 행은 목록 첫 물건(하품 영석)·수 1. `SHOP_KINDS.code` 는 `string`(Task 10이 `ShopKindCode` 로 좁힘). 목록의 기준가·겹침은 쉼표(`10,000`). |
| Task 10 admin-console — 방 바닥 물건과 NPC 상점 화면 | 342a2ef | 방 화면 `바닥 물건`(`FloorItemsEditor`), NPC 화면 `상점`(`ShopEditor`, 고칠 때만), NPC 목록 `상점` 열(종류 이름 또는 `-`). `api.ts` 바닥 3개·상점 2개 함수와 `FloorPlacementResponse`·`FloorPlacementSaveRequest`·`ShopKindCode`·`NpcShopResponse`·`ShopSaveRequest`, `RoomDetailResponse.floorItems`·`NpcDetailResponse.shop`·`NpcSummaryResponse.shopKindName`. `SHOP_KINDS.code` 를 `ShopKindCode` 로 좁힘. 방·NPC 화면이 `listItemTypes` 도 함께 읽음. 판정: 바닥 `수정` 은 그 행을 칸(물건·수·재등장(분))으로 바꾸고 `저장`·`취소` 버튼을 둠(고치는 동안 두기 폼은 숨김), 바닥 `삭제` 는 확인 없이 바로 지움(D50에 확인 문구 없음), 상점 저장 성공은 시작 소지품과 같은 `저장했습니다.`, 종류를 바꿔 이제 팔 수 없게 된 물건은 그 행 선택에 남겨 두고 서버 400을 보임, `상점 아님` 이면 물건 표·`물건 추가` 를 숨김. 매입가·실제 판매가(`effectivePrice`)는 화면에 보이지 않음(D50에 없음). |
| Task 11 play-client — 바닥 물건 줄, 소지품·물건 메시지, 자동완성 | 8c40fe7 | `room.items`(`FloorItem`)·`InventoryMessage`·tone `item`(system·notice, notice event `pick`·`drop`·`give`), 방 보기 NPC 다음 바닥 물건 줄(`이름 N개`, 1개면 이름만), `.line-item`(`--item: #9fd4a3`), COMMANDS 3차(D51 순서), 이름 후보에 바닥 물건(occupants 다음). 판정: 이동 notice 넷만 후보를 바꾸고 물건 notice 는 무시(switch 로 명시). `귀속 해제` 는 COMMANDS 에 따로 넣지 않음(D51 목록 그대로, `귀속` 뒤 낱말은 이름 후보). |
| Task 12 스펙 반영 — 제안 결정을 시스템 스펙에 `(제안)` 으로 | 13837d0, 6d9780f, b9b1d37 | D32~D56을 `index.md`(3차 범위에 계획 링크·구현 날짜 2026-11-14, 3차 열린 질문 30개에 `→ (제안)` 답)·`admin-tool.md`(`## 3차 아이템 데이터`(D32~D35·D38)·`## 3차 화면`(D50)·`## 3차 REST API`(D49)·반영 절 D54·표의 아이템·상점 줄)·`command.md`(`### 물건 이름 찾기와 수량`·`줍기·버리기·소지품·물건 보기`·`상점 명령`·`결제와 확인`·`귀속`·`주기`·`3차 같은 방 알림`·`3차 도움말`, 1·2차 `보기 <무엇이든>` 문구를 3차 동작에 맞춤)·`world-structure.md`(`### 3차 데이터 모델`)·`npc.md`(`### 3차 상점 역할 데이터`)·`platform.md`(`### 3차 WebSocket 메시지 변경`·`### 3차 월드 상태와 저장`(D37·D39·D53·D55)·플레이 화면 D51·저장소 구성 D52)에 옮김. 룰북보다 줄인 것(D43 정혈 비용 없음, D45 `(꺼냄)`·내구도 없음, D42 목록 효과 설명 없음)을 명시. 구현 판정 반영: 확인은 실제로 깰 때만·정확히 10초까지 유효, 거스름 칸 수 `slotsShort`, 기준가 409 여러 상점 `, ` 잇기, 판매가 상한 문구, 매입가 쉼표 없음, 주기 앞부분 일치는 다른 사람 먼저, 방에 없음 문구는 정규화 이름, 수량 상한, 귀속 묶음 안전망, 바닥 시각 열 `timestamp with time zone`(계획은 `timestamp`), DB 제약 위반 전역 409(2차 spec에 해당 500 문구는 없어 맞출 것 없음), admin-console 바닥 수정·삭제·영석 화면, `귀속 해제` 자동완성 없음. 계획과 코드의 차이(구현 판정은 계획이 아니라 코드 기준으로 적음): 바닥 시각 열 `timestamp with time zone`, 거스름 칸 수 `slotsNeeded` → `slotsShort`(실제로 모자란 칸), 확인 질문에서 여러 등급을 `, ` 로 잇는 경우와 거스름 0 일 때 문장을 빼는 경우는 생기지 않음(깨는 등급은 하나, 거스름은 늘 0보다 큼). 리뷰 수정: 귀속 실패 문구를 룰북에 맞춤(`회춘단은 귀속할 수 없습니다.` → `단약은 귀속할 수 없습니다.`(종류 이름), `이미 귀속된 물건입니다.` → `이미 당신에게 귀속된 물건입니다.`), 확인 질문 문구는 룰북 item.md 출력 예와 같음 |
| Task 13 README — 3차 시나리오와 명령, 도움말 대조 | 28d66f2, a53ed3a | 모듈 소개에 3차 한 줄, `## 3차 시나리오`(아이템 종류 다섯·바닥 물건·시작 소지품·상점·play-client 소지품~주기), `## 되는 명령` 3차 줄·수량 문법·`보기 <무엇>` 줄·앞부분 선택 대상, 브라우저 수동 확인 3차 항목, `pgdata` 스키마 한 줄. 대조: README 도움말 문구는 `GameService.HELP` 와 글자 그대로 같음, 명령 이름은 `CommandParser`, 소지품 표시는 `ItemCommandHandler.inventoryText`, 5분은 `DroppedItem.LIFETIME`, admin 화면 이름(`아이템`·`시작 소지품`·`바닥 물건`·`상점`)은 코드와 같음. play-client `COMMANDS` 는 D51과 같고 `귀속 해제` 는 자동완성에 없어(서버 명령·도움말엔 있음) README에 그렇게 적음. 스모크 절은 3차 항목 없이 그대로(Task 14). |
| Task 14 통합 확인 — compose 한 번 빌드·실행, 스모크 3차 시나리오, 정리 | 7c45f2f | `smoke-test.mjs` 에 단계 16~25(아이템 종류 넷·영석 목록 → 광장 바닥 늑대 가죽 3 → 시작 소지품 비우고 D 생성(0/2칸)·시작 소지품 넷 저장·칸 넘침 400 → 상점 NPC 단약방·판매가 400·쓰이는 종류 삭제 409 → C 생성 3/22칸 → 줍기·버리기(D 알림)·바닥 묶음·물건 보기 → 목록·사기·팔기·값(30→24→25)·재료 거절 → 귀속·거절 셋·귀속 해제 → D에게 주기(단약·하품 5)·받는 쪽 칸 없음·없는 사람·도움말 → 시작 소지품 되돌림, `finally` 에서도 되돌림). 이름에 숫자 6자리 실행 번호(조사는 받침 없음), 받는 문구의 `{이/가}` 는 스크립트 `withIGa`. compose 빌드 한 번, 스모크 첫 실행에 통과, 모듈 코드 변경 없음. 디스크: 시작 전 남은 54Gi·이미지 2.382GB·빌드 캐시 63.45MB, 빌드 뒤 52Gi·2.445GB·1.602GB, 정리 뒤 53Gi·2.382GB·62.21MB. README 스모크 절 3차 항목. |
| 최종 리뷰 수정 | 8947f47 | 최종 전체 리뷰 ready(Critical·Important 없음). play-client 방 보기 바닥 물건 줄의 수에 천 단위 쉼표(`하품 영석 50,000개`, 서버 `Numbers.count` 와 같게, 로캘에 기대지 않는 `formatCount`). 같은 파일에서 수를 직접 찍는 곳은 이 줄뿐(소지품은 서버 `text` 그대로) |

## 상태

- **3차 이터레이션 구현 완료** (2026-11-14). 남은 것: 최종 전체 리뷰, 브라우저 수동 확인(README 체크리스트 3차 항목).
- 최종 전체 리뷰(opus): ready, Critical·Important 없음. Minor 1(바닥 물건 수 쉼표)은 고침(8947f47), 나머지는 `## 다음 할 일`.
- 최종 리뷰 수정 뒤 play-client `npm ci && npm test && npm run build` — 6 파일 76개 통과, 정적 export 빌드 성공.
- 검증 (Task 14):
  - world-server `./gradlew clean build` — BUILD SUCCESSFUL, 테스트 709개 통과(실패·건너뜀 0), SpotBugs·ArchUnit 포함.
  - play-client `npm ci && npm test && npm run build` — 6 파일 75개 통과, 정적 export 빌드 성공.
  - admin-console `npm ci && npm test && npm run build` — 12 파일 70개 통과, 정적 export 빌드 성공.
  - compose: `docker compose build` 한 번(세 이미지) → `docker compose up -d` → `node scripts/smoke-test.mjs` 단계 1~25 `SMOKE OK`(첫 실행에 통과, 다시 빌드 없음).
  - 기존 `pgdata` 에서 올림. 기동 로그에 스키마 오류 없음(`ddl-auto=update` 가 새 유니크 제약을 걸기 전 `constraint ... does not exist, skipping` 경고 셋 — `uk_inventory_character_id`·`uk_item_type_name`·`uk_item_type_spirit_stone_grade`, 첫 생성 때의 PostgreSQL 안내라 문제 아님).
  - `.env` 는 고치지 않았다(개발용 로그인 꺼짐·관리자 본인 이메일). 빌드는 `.env` 그대로, `up` 때만 셸 값으로 `MUD_DEV_LOGIN_ENABLED=true`, `MUD_ADMIN_EMAILS=<기존>,smoke-admin@example.com` 을 덮어썼다. 시작 소지품은 스모크가 처음 값(`[]`)으로 되돌렸고(확인함), 시작 방은 끝난 뒤 원래 방(`태초 마을 / 태초 객잔`, id 1)으로 되돌렸다. 스모크 데이터(지역 `스모크 mwg640pn`, 아이템 종류 `회춘단368923` 등, NPC `상인368923`)는 `pgdata` 에 남아 있다.
- 디스크 (Task 14):
  - 시작 전 `df -h .` 남은 공간 54Gi. `docker system df`: 이미지 2.382GB, 빌드 캐시 63.45MB.
  - compose 빌드 뒤 `df -h .` 52Gi, `docker system df`: 이미지 2.445GB, 빌드 캐시 1.602GB.
  - 정리: `docker compose down`(볼륨 `pgdata` 남김) → `docker image prune -f` → `docker builder prune -f`(988MB) → `world-server/build`, `play-client/{.next,out}`, `admin-console/{.next,out}` 삭제(`git check-ignore` 로 확인). `node_modules` 는 남김.
  - 정리 뒤 dangling 이미지 0개, `docker system df`: 이미지 2.382GB(`mud-mk3-*` 세 이미지는 남김), 빌드 캐시 62.21MB(회수 가능 0B). `df -h .` 남은 공간 53Gi.

## 리드 판정 (Ruling)

- 작업 위치: main 에서 일하고 태스크마다 push — 1·2차 선례, 창호 지시(`commit & push`) — 틀렸을 때 비용: main 이력에 남음.
- Task 2: 영석에 형식이 틀린 수정 요청은 409 대신 400 허용 — `@Valid` 검사가 먼저 돈다 — 비용: 응답 코드 차이뿐.
- Task 3: 상품·극품 영석 확인 질문은 룰북대로 실제로 영석을 깰 때만 — 룰북 우선 — 비용: 확인 분기 수정.
- Task 4: 영석 심기 시점(`ApplicationReadyEvent`)은 그대로 — 빈 DB 첫 기동 순간에만 영향 — 비용: 첫 기동 직후 `사기` 가 실패할 수 있음.
- Task 6: 매입가는 소수라 `Numbers.decimal` 그대로(쉼표 없음) — 매입가가 소수일 수 있음 — 비용: 큰 값에서 판매가(쉼표)와 표시 모양이 섞임.
- Task 7: `이곳에 …은 없습니다.` 에 정규화된 이름(`하품` → `하품 영석`) 허용 — 입력을 `ItemQuery` 로 되살림 — 비용: 문구가 입력과 다를 수 있음(Task 12 에서 D45 에 기록).
- Task 8: 주기 대상 앞부분 일치는 다른 사람 먼저 — 자기에게 주기는 뜻이 없음 — 비용: D44 문구와 차이(Task 12 에서 기록).
- Task 10: 바닥 `수정` 에 `취소` 버튼, 바닥 `삭제` 는 확인 없이 바로, 매입가·실제 판매가는 화면에 안 보임 — D50 에 없음 — 비용: 실수로 지울 수 있음(다음에 확인 추가 후보), 값 확인은 API 로.
- Task 12: 귀속 실패 문구를 코드에서 룰북 문구로 맞춤 — 룰북 우선 — 비용: 테스트 기대값 수정.
- 최종 리뷰: 거래 불가 물건 버리기 허용(룰북은 귀속·퀘스트 물건만 금지) — 룰북에 금지가 없음 — 비용: 버려서 다른 사람에게 넘기기 가능, 다음 이터레이션 판단 후보.
- 최종 리뷰: 저물대 한도로 일부만 줍는 경우의 문구 그대로 — 드문 경우 — 비용: 그때 문구가 어색함.
- Task 14: 모듈 코드 변경 없어 따로 태스크 리뷰 없이 최종 전체 리뷰로 갈음(2차 Task 10 선례).
- 리뷰 모델: 모든 리뷰는 opus. 문서 옮기기 Task 12·13 구현은 sonnet.

## 다음 할 일

- 창호: 브라우저 수동 확인(README `수동 확인 — 브라우저` 3차 체크리스트 — 바닥 물건 줄 색, 소지품 표시, 영석을 깨는 거래의 `확인`·`취소`·10초 넘김, 버린 물건 5분 뒤 사라짐, `줍기 늑` + Tab). `(제안)` 결정 검토(docs/spec/system 의 `(제안)`, 특히 index.md 3차 열린 질문 답 30개), 거래 불가 물건 버리기를 허용할지 판단.
- 다음 이터레이션으로 미룬 Minor(머지 막지 않음):
  - world-server: 아이템 종류 목록 정렬 대소문자, 테스트 이름 `highUsedUpMiddleBroken` 이 뜻과 반대, `WorldApi` 두 트랜잭션 사이 삭제 NPE(희박), 시작 소지품 FK 없음 TOCTOU, 테스트 `$[0]` 정렬 의존, 도메인 중복 문구의 id, `FloorPlacementSaveInfo` 의 쓰이지 않는 검증 애노테이션, `MutableClockReset` 이 before 만, `ShopPrice` 가 `defaultSellPrice` 를 다시 구현, `NpcShopResponse` `requireNonNull` 경합 500, 행 곱 회귀 테스트 없음, `PricingTest` 어색한 assert, 빈 줄 모양, 동시 줍기 테스트가 `전부` 로 약함, 저물대 한도 부분 줍기 문구(최종 리뷰도 그대로 둠), clock reset 중복, 테스트 `id()` 중복, import 순서, 저물대 3개일 때 사기·팔기 `BAG_IN_USE`/`NO_ROOM` 테스트 없음, `confirmQuestion` 죽은 분기.
  - admin-console: 목록에 없는 시작 소지품 id 표시 어긋남, 저장 뒤 서버 응답 무시, 빈 기준가 → 0, 페이지 연결 테스트 없음, 바닥 삭제 확인 없음(확인 추가 후보), `min=1` 브라우저 검사, 상점 표 머리 없음.
  - play-client: 여러 낱말 물건 Tab, 이름 중복 후보, 주운 뒤 후보 남음, `귀속 해` Tab.
  - README 주기 예 `한`(앞부분 일치).
  - 최종 리뷰 Minor 2·3: 거래 불가 물건 버리기 허용(위 판정, 창호 판단), 저물대 한도 부분 줍기 문구(위 판정).
  - 최종 리뷰가 판단하지 않고 둔 것: 이름이 `해제 ` 로 시작하는 물건(`귀속 해제 …` 해석과 겹침), 상점이 여럿인 방에서 인자 없는 `사기`.
- 스모크가 못 보는 것(단위 테스트로 덮음, `MutableClock`): 영석을 깨야 하는 거래의 확인 질문(상위 영석으로 치르기)·10초 만료, 버린 물건 5분 만료·배치 재등장, 저물대가 든 물건 거절.
- 1·2차에서 이월한 것(2차 '그대로 둠' Minor, 동시 방 삭제 500, 자동 스크롤, 세션 secure·DB화 등)은 그대로.
- 다음 순서: 4차(공법·신통 배우기 + 임의 경지 상승).
