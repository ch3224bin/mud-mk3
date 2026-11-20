# mud-mk3 작업 규칙

코딩 관례는 [.claude/rules/README.md](.claude/rules/README.md), 기술 선택은 [docs/spec/system/platform.md](docs/spec/system/platform.md) 를 따른다.

## 코드 작업은 git worktree 에서 한다

창호가 원래 디렉토리(`mud-mk3`, main 체크아웃)에서 서버를 돌려 확인하는 경우가 있다. 그래서 스펙·구현·테스트·커밋은 원래 디렉토리가 아니라 worktree 에서 한다. (창호 지시, 2026-11-20)

- **시작:** 원래 디렉토리에서 `git fetch && git worktree add ../mud-mk3-wt/<작업이름> -b <작업이름> origin/main`. 그 안에서 스펙·구현·테스트·커밋을 한다.
- 원래 디렉토리에 아직 안 올린 변경이 생겼으면 worktree 로 옮기고 원래 디렉토리는 깨끗하게 되돌린다.
- **반영:** worktree 에서 `git fetch && git rebase origin/main` 뒤 `git push origin HEAD:main`. 원래 디렉토리에서는 merge·checkout·pull 을 하지 않는다(창호가 pull 한다).
- **compose·포트·pgdata:** 원래 디렉토리에서 도는 서버와 겹치지 않게 한다. 다른 project 이름(`docker compose -p mud-mk3-wt-<작업이름>`), 다른 호스트 포트, 따로 만든 pgdata 볼륨을 쓴다. 기존 데이터로 확인할 때는 원래 pgdata 볼륨을 복사한 사본을 쓴다. 겹칠 수밖에 없으면 진행하지 말고 묻는다.
- **끝:** `git worktree remove ../mud-mk3-wt/<작업이름>` 과 `git branch -D <작업이름>`, 만든 compose project·사본 볼륨도 지운다. 완료 보고에 '원래 디렉토리는 git pull 필요' 를 적는다.
