# 방위 초성 약자(direction-abbrev) 진행

상태: **방위 초성 약자 완료 (통합 확인 통과)**

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 스펙·룰북·계획 | 00675a3 | command.md `## 방위 초성 약자 (direction-abbrev)`, index.md 결정 요약, 룰북 줄임말 표·출구·방 보기(`ㅂ`·`ㅅ` 고침), admin-tool.md 예약어 수(116), README 명령 표·도움말 |
| 2. world-server 명령 해석 | 0ef1f7f | `Direction` 에 `initials`(`ㄷ`…`ㅇㄹ`), `fromCommand` 가 이름·영문·초성을 한곳에서 읽음. `CommandParser`: `보기`/`봐`/`보` → `Look(인자 그대로)`, `상태`/`ㅅㅌ`, `ㅂ`·`ㅅ` 은 `parseOther` 방위로. 예약어 11개 더함(105 → 116, 끝은 `공격` 그대로). `GameService.HELP` 새 문구. RED 16 실패 → GREEN, `./gradlew build` 1562 통과(SpotBugs·ArchUnit). 바뀐 기존 기대값: `CommandParserTest.look`(`ㅂ`→`봐`·`보`)·`notCommandIsAbility`(`보`→`보다`)·`status`·`statusWithArgumentFail`(`ㅅ`→`ㅅㅌ`), `ReservedWordsTest.listMatchesPlan`(105→116), `GameConnectorTest.lookShowsOthersExcludingSelf`(`ㅂ`→`보`·`봐`)·`statusShortcut`(`ㅅ`→`ㅅㅌ`)·`helpText`·`unknownCommand`(`보`→`보다`) |
| 3. play-client | b109a9f | 코드 변경 없음. 지키는 테스트만: `Terminal.test` 초성 그대로 보냄·조합 중 Enter 무시(가드를 빼면 실패함 확인), `complete.test` 초성 + Tab 은 그대로. 133 통과·tsc·build |
| 4. 스모크 | 919f271·21c7e4b | `HELP_6` 새 문구, 단계 55 `SMOKE OK` 앞. 리뷰 Important 1: 처음 짠 `ㅂ`→북쪽 길은 단계 9 에서 지운 방이라 고침(21c7e4b) — 단계 55 가 광장 북에 `북뜰<실행 번호>`(양방향)를 만들고 H: `ㅂ`→북뜰, `ㄴ`→광장, `ㄷ`→사냥터, `ㅅ`→광장, `ㅂㄷ`→`그쪽으로는 길이 없습니다.`(error), `보`·`봐` 광장 room, `ㅅㅌ` 은 `status` 를 직접 기다림, 도움말 새 문구, `종료` 1000 → 북뜰 DELETE 204 → 광장에 북 출구 없음 확인 |
| 5. 세 모듈 검증 | — | world-server build 1562, play-client 133, admin-console 209, 둘 다 tsc·build (Node 26.13.1) |
| 6. 리뷰 Minor (문서) | 80f0871 | README `보기 <무엇>` 줄에 `봐`/`보 <무엇>`, command.md 1차 표 `보기`, `봐`, `보` (1차~6차는 `ㅂ`) |
| 7. 통합 확인 — 사본 stack, 기존 데이터 사본, 스모크 전체 | `docs(impl): 방위 약자 통합 확인 기록` | 원래 stack(project `mud-mk3`)은 건드리지 않음. 사본 project `mud-mk3-wt-direction-abbrev`, 저장소 밖 override `../direction-abbrev.compose.yaml`(포트 18080·13000·13001 `!override`, `MUD_PLAY_CLIENT_URL`·`MUD_ADMIN_CONSOLE_URL` 사본 포트, 클라이언트 빌드 인자 `NEXT_PUBLIC_WORLD_SERVER_URL=http://localhost:18080`, `MUD_CORS_ALLOWED_ORIGINS` 는 사본 포트 + 3000·3001 문자열 — 스모크가 Origin 을 `localhost:3000`·`3001` 로 고정해 보내기 때문, 포트를 열지는 않음). `.env` 는 원래 디렉토리에서 복사, 관리자 이메일은 셸에서 `smoke-admin@example.com` 으로 덮어씀. `config` 로 호스트 포트 18080·13000·13001 뿐·볼륨 `mud-mk3-wt-direction-abbrev_pgdata` 뿐 확인. 사본 postgres 만 먼저 → `docker exec mud-mk3-postgres-1 pg_dump -U mud -Fc mud`(읽기 전용) → 사본에 `pg_restore --no-owner`(종료 0) → 덤프 삭제. 원래·사본 테이블 30·`player_character` 51·`mob_placement` 0·`mob_type` 16 같음. `MUD_DEV_LOGIN_ENABLED=true` build 한 번 → up → 기동 로그 ERROR/Exception 0(WARN 은 기존 HHH160246 하나) → 스모크 단계 1~55 `SMOKE OK` 첫 실행(실행 id `mwojbqxi`, 번호 294090, 고친 것 없음, 재빌드 없음) → 스모크 뒤 로그 ERROR/Exception 0, 사본 `mob_placement` 0. 정리: `down -v --rmi local`(사본 볼륨·네트워크·worktree 이미지 셋 삭제), `.env` 사본·override 삭제. `builder prune` 등 전역 정리 안 함. 원래 컨테이너 넷 시작 시각 04:03:30Z·04:03:35Z 그대로·running, `mud-mk3_pgdata` 남음, 원래 디렉토리 `git status` 비어 있음 |

## 알려진 한계

- 이미 `보`·`봐`·`ㅇ` 같은 이름으로 저장된 신통이 있으면 명령이 먼저라 쓸 수 없다(L-4, 이주 없음).
- `봐 동` 은 다음 일. 지금은 `동` 이라는 대상을 찾는다.
