# 기술 부채 정리 1차(debt-1) 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| Task 1 | (이 커밋) | enum CHECK 일반 정리(판정 규칙·로그), `LegacySchemaCleaner` 테스트 보강(#9 셋), 스펙 기술 설명 고침. #1 은 Flyway 미룸으로 열어 둠. H2 는 Hibernate enum CHECK 를 만들지 않아 `dropsHibernateEnumCheck` 대신 숫자 CHECK 남음을 봄 |
| Task 2 | (이 커밋) | orm.xml `PlayerCharacter` 의 `room_id`·`realm` 에 `updatable="false"`(insertable 그대로). `CharacterRepository` 에 JPQL `@Modifying(flushAutomatically = true)` `writeRoomId`·`writeRoomIdForAll`·`writeRealm`(네이티브 불필요 — Hibernate 가 `updatable=false` 열의 JPQL 일괄 갱신을 막지 않음). `CharacterModifyService` 의 `moveTo`·`relocateAll`·`designate`·`initiate` 가 도메인 메서드로 검사·메모리 값을 맞춘 뒤 열 쿼리로 씀(`save` 는 `create`·`chooseSpiritRoot` 만). 새 `CharacterRowWriteTest`(커밋, 방은 FK 가 없어 id 상수만 쓰고 만든 캐릭터만 지움). `CharacterRepositoryTest` 의 엔티티 저장 테스트 둘은 방이 그대로(10)임을 보게 고침(P2) + 열 쓰기 테스트 셋, `BaseGameTest.setRealm` 은 `writeRealm`. platform.md 경지 지정의 반영·5차 깎인 기혈 줄의 행 전체 경합 설명 고침 |
