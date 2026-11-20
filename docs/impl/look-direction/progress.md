# 방위 보기(look-direction) 진행

상태: **방위 보기 완료 (통합 확인 통과)**

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 스펙·룰북·계획 | b92cd80 | command.md `## 방위 보기 (look-direction)`, index.md 결정 요약, 룰북 이동·보기 표·방 보기 |
| 2. world-server | b5a4273 | `Command.LookToward(Direction)`, `CommandParser.parseLook`(인자 전체가 `Direction.fromCommand` 에 맞으면 방위 보기), `GameService.lookToward`(`system/info` `<towardPhrase> <이름><이/가> 보인다.`, 없으면 `NO_EXIT`), `HELP` 에 `보기 <방위>`, README. RED: 컴파일 실패 → 스텁 뒤 8 실패 → GREEN. `./gradlew build` 1568 통과. 바뀐 기존 테스트: `CommandParserTest.lookKeepsDirectionArgument` 지움(뜻이 바뀜, `lookToward`·`lookTowardNeedsWholeArgumentToBeDirection` 이 대신), `GameConnectorTest.lookShortcutsWithTarget` 의 `봐 동`·`보 ㄷ` 기대값(대상 없음 → 길 없음), `helpText` |
| 3. 스모크 | 8c58070 | `HELP_6` 새 문구, 단계 56 `SMOKE OK` 앞(광장 위 `누각<번호>` 를 만들고 끝에 지움, 동은 단계 36 의 사냥터). `node --check` 만 함 |
| 4. 세 모듈 검증 | — | world-server build 1568, play-client 133, admin-console 209, 둘 다 tsc·build (Node 26.13.1). play-client 코드 변경 없음 |
| 5. 리뷰 Minor | e8e1534 | `parseLook` 을 `parseTalk` 주석 위로 옮김, `GameConnectorTest` 의 `ExitSaveRequest` import 자리. `./gradlew build` 통과. 스모크의 늦은 room 검사(Minor 3)는 그대로 |
| 6. 통합 확인 — 사본 stack, 기존 데이터 사본, 스모크 전체 | `docs(impl): 방위 보기 통합 확인 기록` | 원래 stack(project `mud-mk3`)은 건드리지 않음. 사본 project `mud-mk3-wt-look-direction`, 저장소 밖 override `../look-direction.compose.yaml`(포트 18080·13000·13001 `!override`, `MUD_PLAY_CLIENT_URL`·`MUD_ADMIN_CONSOLE_URL` 사본 포트, `MUD_CORS_ALLOWED_ORIGINS` 는 사본 포트 + 스모크 Origin 3000·3001 문자열, 클라이언트 빌드 인자 `NEXT_PUBLIC_WORLD_SERVER_URL=http://localhost:18080`). `.env` 는 원래 디렉토리에서 복사, 셸에서 `MUD_DEV_LOGIN_ENABLED=true`·`SPRING_PROFILES_ACTIVE=local`·`MUD_ADMIN_EMAILS=smoke-admin@example.com`. `config` 로 호스트 포트 18080·13000·13001 뿐·볼륨 `mud-mk3-wt-look-direction_pgdata` 뿐 확인. 사본 postgres 만 먼저 → `docker exec mud-mk3-postgres-1 pg_dump -U mud -Fc mud`(읽기 전용) → 사본에 `pg_restore --no-owner`(종료 0) → 덤프 삭제. 원래·사본 테이블 30·`player_character` 51·`mob_placement` 0·`mob_type` 16 같음. build 한 번 → up → 기동 로그 ERROR/Exception 0(WARN 은 기존 HHH160246 하나) → 스모크 단계 1~56 `SMOKE OK` 첫 실행(Node 22.15.1, 실행 id `mwok8eqe`), Node 26.13.1 로 한 번 더 `SMOKE OK`(`mwoka5be`). 고친 것 없음. 스모크 뒤 로그 ERROR/Exception 0, 사본 `mob_placement` 0. 정리: `down -v --rmi local`(사본 볼륨·네트워크·이미지 삭제), `.env` 사본·override 삭제, 전역 prune 안 함. 원래 컨테이너 넷 시작 시각 04:03:30Z·04:03:35Z 그대로·running, `mud-mk3_pgdata` 남음, 원래 디렉토리 `git status` 비어 있음 |

## 알려진 한계

- 방위 낱말과 같은 이름(`동`·`e`·`ㄷ` 등)의 NPC·물건은 `보기 <그 이름>` 으로 볼 수 없다(방위가 먼저). 관리 툴은 그런 이름을 막지 않는다.
- 이름 있는 출구·비밀문이 생기면 방위 보기 대상 밖으로 두고 `보기 <출구 이름>` 은 그때 정한다.
