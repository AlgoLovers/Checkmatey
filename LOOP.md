# LOOP.md — Checkmatey 루프 계약 & 런북

> 루프 엔지니어링의 "무엇을 언제 왜 반복하는가"를 명시적으로 적는 곳.
> 현재 상태/진행은 [`STATE.md`](STATE.md), 안전 경계는 [`loop-constraints.md`](loop-constraints.md).

## 루프 계약(Loop Contract)이란

신뢰할 수 있는 루프는 아래 11개 요소를 명시해야 한다:

| 요소 | 설계 질문 | 이 프로젝트의 산출물 |
|---|---|---|
| Objective | 무엇을 최적화? | GitHub 이슈 / 마일스톤 / PRD |
| Trigger | 언제 도는가? | `/loop`, 스케줄, PR/이슈 웹훅 |
| Discover | 일이 어떻게 들어오나? | GitHub 이슈·CI 실패·리뷰 코멘트 |
| Workspace | 어디서 안전하게 작업? | git worktree / 브랜치 |
| Context | 어떤 지식이 로드? | `CLAUDE.md`, `docs/`, `STATE.md` |
| Delegation | 누가 무슨 역할? | maker(구현) / checker(검증) 분리 |
| Verification | 무엇이 "됐다"를 판단? | `testDebugUnitTest`, `lint`, 빌드 |
| State | 무엇이 다음 런까지 남나? | `STATE.md`, 이슈 코멘트, 커밋 |
| Budget | 언제 멈추나? | 최대 턴/토큰/시간 (loop-constraints) |
| Escalation | 언제 사람에게? | PR 열기 / 이슈 / 질문 |
| Exit | 언제 끝났다고? | 수용 기준 충족, CI 초록불 |

## 루프 성숙도

- **L1 보고 전용** ← 현재. 루프는 찾아서 보고만, 사람이 모두 승인.
- **L2 저위험 자동수정**. 허용목록 안의 항목만 자동 수정, 애매하면 에스컬레이션.
- **L3 무인**. 엄격한 denylist·거버넌스 하에 자율 실행.

---

## 이 프로젝트의 루프 (초기)

### LOOP-1 · Feature Build Loop (핵심 개발 루프)
- **Trigger**: 사람이 `/loop` 또는 다음 마일스톤 지정
- **Discover**: `STATE.md`의 "다음 액션" + 열린 GitHub 이슈(라벨 `next`)
- **Workspace**: `feature/<name>` 브랜치 (필요 시 git worktree)
- **Delegation**: maker=구현, checker=별도 검증(테스트/린트/빌드)
- **Verification**: `./gradlew testDebugUnitTest :app:assembleDebug lint`
- **State**: 커밋 + `STATE.md` "결정 로그"·"다음 액션" 갱신
- **Escalation**: 아키텍처가 갈리면 멈추고 옵션 2~3개 제시 (CLAUDE.md 혼란 프로토콜)
- **Exit**: 마일스톤 수용 기준 충족 + CI 초록불 + PR 머지
- **성숙도**: L2 (테스트 통과 시 PR까지 자동, 머지는 사람)

### LOOP-2 · Daily Triage (보고 전용, L1)
- **Trigger**: 스케줄(일 1회) 또는 수동
- **Discover**: 새 이슈, 실패한 CI, 스테일 브랜치
- **Verification**: 없음(읽기 전용)
- **State**: `STATE.md`에 요약 추가, 라벨 제안
- **Exit**: 요약 게시. **1주차엔 자동수정 없음.**
- 예: `/loop 1d "LOOP-2 트리아지 실행. STATE.md 갱신. 자동수정 금지."`

### LOOP-3 · Dependency Sweeper (L2, 나중)
- **Trigger**: Dependabot PR (`.github/dependabot.yml`)
- **Discover**: 의존성 업데이트 PR
- **Verification**: CI 통과 여부
- **Escalation**: 메이저/브레이킹 업데이트는 사람에게
- **Exit**: 논브레이킹 패치는 CI 초록불 시 머지 후보

> 새 루프를 추가할 땐 위 11개 요소를 모두 채우고, [`docs/loop-design-checklist.md`](docs/loop-design-checklist.md)의
> 8개 질문을 통과시킨다.
