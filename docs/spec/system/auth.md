# 인증

→ [목차](index.md)

- 인증은 Google 로그인 기능을 사용한다. → [플랫폼: 기술 스택](platform.md#기술-스택)
- 1차 이터레이션 범위에 인증이 들어간다. → [이터레이션 계획](index.md#이터레이션-계획)

## 계정

- play-client와 admin-console 둘 다 Google 로그인을 쓰고, 같은 계정 체계를 쓴다. → [플랫폼: 저장소 구성](platform.md#저장소-구성)
- Google 계정 하나에 캐릭터는 1개다. → [룰북: 캐릭터 생성](../game-rule/character.md#캐릭터-생성)

## 관리자

- 관리자는 서버 설정(환경변수 등)에 관리자 Google 이메일 목록을 적어 정한다. → [관리 툴](admin-tool.md)

## 열린 질문

- WebSocket 연결 시 인증 정보를 어떻게 전달하는가? (구현 때 제안) → [플랫폼: 실시간 통신과 월드 상태](platform.md#실시간-통신과-월드-상태)
