# CLAUDE.md — Checkmatey 하네스

> 이 파일은 코딩 에이전트(Claude Code, Codex, Cursor 등)가 작업 시작 시 자동으로 읽는
> 프로젝트 규칙서다. `AGENTS.md`는 이 파일을 가리키는 얇은 포인터다.
> 루프 계약은 [`LOOP.md`](LOOP.md), 현재 상태는 [`STATE.md`](STATE.md),
> 안전 규칙은 [`loop-constraints.md`](loop-constraints.md)에 있다.

## 이 프로젝트가 뭔가

**Checkmatey** — 안드로이드 체스 **학습** 앱. 초보자가 규칙 → 전술 → 오프닝 → 실전까지
단계적으로 배우도록 돕는다. **폰과 태블릿을 하나의 코드베이스로** 모두 지원한다.

- 언어/UI: **Kotlin + Jetpack Compose (Material 3)**
- 최소 SDK 24, 컴파일/타깃 SDK 35
- 체스 로직: **순수 Kotlin** 으로 시작(이동 생성·검증·규칙). 네이티브 엔진(Stockfish)은
  `core/chess`의 인터페이스 뒤에 나중에 붙일 수 있게 열어둔다.
- 화면(탭): Learn / Play / Puzzles / Profile

---

## 어떻게 일하는가 — Loop Engineering

이 프로젝트는 **루프 엔지니어링**으로 개발한다. 핵심은 "프롬프트를 잘 짜는 것"이 아니라
**작업을 발견 → 위임 → 검증 → 상태 저장 → 다음 결정 → 반복하는 시스템(루프)을 설계**하는 것이다.

작업을 시작하기 전에 항상 이 순서를 따른다:

1. **[`STATE.md`](STATE.md)를 읽는다** — 지금 어느 마일스톤인지, 마지막 결정과 다음 액션이 뭔지.
2. **하나의 목표**를 정한다 — "개선"이 아니라 관측 가능한 결과 하나.
3. **검증 신호를 먼저 만든다** — 재시도/반복 전에 테스트·컴파일·린트가 존재해야 한다.
4. **작업하고, 검증하고, [`STATE.md`](STATE.md)를 갱신**한다.

루프 성숙도는 단계적으로 올린다: **L1(보고 전용) → L2(저위험 자동수정) → L3(무인 실행)**.
지금은 **L1**이다. 자동 머지·자동 커밋은 [`loop-constraints.md`](loop-constraints.md)의 허용목록에 있을 때만.

루프 계약(Objective/Trigger/Discover/Workspace/Context/Delegation/Verification/State/Budget/
Escalation/Exit)의 전체 정의와 이 프로젝트의 루프 목록은 [`LOOP.md`](LOOP.md)에 있다.

---

## 반드시 지키는 규칙 (Non-Negotiable)

### 1. 완료의 기준은 "완료"다
계획이나 임시방편이 아니라 **끝난 것**을 낸다. 코드 + 테스트 + (필요 시) 문서를 같은 커밋에.
"테스트 나중에 추가"는 금지. 왜 이 코드가 맞고 어디서 깨지는지 설명할 수 있어야 완료다.

### 2. 코드와 테스트는 함께 커밋한다
- **게이트 테스트(Gate tests)**: 결정적·로컬·빠름(전체 몇 초). 순수 Kotlin 로직은 여기서 검증.
  `./gradlew testDebugUnitTest`. 절대 flaky 금지. 예: `core/chess`의 `BoardTest`.
- **UI/계기 테스트**: `androidTest/`. 화면 동작 확인. CI 또는 로컬 에뮬레이터에서.
- 체스 규칙 같은 결정적 로직은 **모델 응답 안에서 계산하지 말고 코드로 짜서** 테스트로 고정한다.
  (같은 질문에 항상 같은 정답이면 그건 스크립트/함수여야 한다.)

### 3. 관측 가능한 결과를 먼저 정한다
"동작함"은 결과가 아니다. 무엇이(사용자가 보는 동작 또는 지표) 바뀌는지 먼저 말한다.

### 4. 만들기 전에 찾는다 (Vanilla by default)
가장 단순하고 검증된 방법이 이긴다. 유틸을 짜기 전에 표준 라이브러리 → 실사용 많은 라이브러리 →
직접 구현 순으로 검토한다. GitHub에서 별점/최근성/이슈 대응을 보고 고른다. 두 선택지가 비등하면
트레이드오프를 한 줄로 말하고 물어본다.

### 5. 두 번 반복한 수작업은 자동화한다
같은 수동 플로우를 두 번째 한다면 스크립트·스킬·워크플로로 고정한다. 일회성 프롬프트는 축적되지 않는다.

---

## 아키텍처 — 관심사 분리, 병렬 친화

패키지 루트: `com.checkmatey`

```
com.checkmatey
├─ MainActivity                # 진입점 (edge-to-edge, 테마 적용)
├─ CheckmateyApp               # 적응형 네비게이션 루트 (폰=하단바 / 태블릿=레일)
├─ ui/theme                    # Material 3 테마·색
├─ ui/board                    # ChessBoard 등 재사용 체스 UI
├─ feature/<name>              # 화면 단위 (learn/play/puzzles/profile). 각자 독립적
└─ core/chess                  # 순수 Kotlin 체스 도메인 (Android 의존성 없음, JVM 테스트 가능)
```

- **`core/`는 Android에 의존하지 않는다.** 순수 Kotlin이라 `testDebugUnitTest`로 빠르게 검증된다.
- **`feature/`는 서로 독립**이어야 한다. 두 개의 세션이 서로 다른 feature를 동시에 건드려도 충돌 없게.
- 경계에는 타입이 있는 인터페이스를 둔다(엔진 교체·목킹을 위해).

### 폰 + 태블릿 (반드시)
- 네비게이션은 `NavigationSuiteScaffold`가 창 크기에 따라 자동 전환한다
  (compact=하단 바 / medium·expanded=네비게이션 레일). 새 최상위 목적지는 여기에 추가한다.
- 크기 분기가 필요하면 **하드코딩 dp가 아니라 `WindowSizeClass`** 로 판단한다.
- 콘텐츠는 큰 화면에서 무한정 늘리지 말고 `widthIn(max = …)` 등으로 상한을 둔다(예: `ChessBoard`).
- 체스보드처럼 정사각 비율이 중요한 요소는 `aspectRatio(1f)`로 유지한다.
- 새 화면을 만들면 **컴팩트(폰)와 확장(태블릿) 미리보기를 둘 다** 확인한다.

---

## 완료 상태 프로토콜

작업 종료 시 아래 중 하나로 보고한다. "부분 완료"는 없다 — 끝내거나 막히거나다.

- **DONE** — 모든 단계 완료, 증거 제시, 테스트가 diff에 포함, 머지 준비됨
- **DONE_WITH_CONCERNS** — 완료했으나 알아야 할 이슈 있음(각 항목을 심각도와 함께 나열)
- **BLOCKED** — 진행 불가. 막힌 지점과 시도한 것을 명시
- **NEEDS_CONTEXT** — 정확히 무엇이 필요한지 명시

---

## Git / GitHub

- 원격: **github.com/AlgoLovers/Checkmatey** (기본 브랜치 `main`).
- 사용자가 요청할 때만 커밋/푸시한다. `main`에 직접 커밋하지 말고 브랜치를 판다.
- PR 템플릿([`.github/pull_request_template.md`](.github/pull_request_template.md))의 체크리스트를 채운다.
- CI([`.github/workflows/ci.yml`](.github/workflows/ci.yml))가 빌드+유닛테스트+린트를 돌린다. 초록불 아니면 머지 금지.
- 커밋 메시지 마지막 줄:
  `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`

---

## 안전 (Safety)

- 비밀정보(`*.keystore`, `keystore.properties`, `.env`, 서명키)를 **절대 커밋하지 않는다**.
  커밋 전 `.gitignore` 확인. `local.properties`도 커밋 금지.
- `rm -rf`, `git reset --hard`, `DROP TABLE` 등 파괴적 명령은 **명시적 확인 없이 실행 금지**.
- `--no-verify`로 훅을 건너뛰지 않는다. 근본 원인을 고친다.
- 바이너리/컴파일 산출물(APK/AAB/이미지 대용량)은 커밋하지 않는다.
- 전체 안전 규칙(경로 denylist, 자동머지 allowlist, 예산, 에스컬레이션)은
  [`loop-constraints.md`](loop-constraints.md).

---

## 자주 쓰는 명령 (Commands)

```bash
./gradlew :app:assembleDebug            # 디버그 APK 빌드
./gradlew testDebugUnitTest             # 순수 Kotlin 유닛 테스트 (게이트)
./gradlew lint                          # Android 린트
./gradlew installDebug                  # 연결된 기기/에뮬레이터에 설치
adb devices                             # 연결 기기 확인
```

- SDK 경로는 `local.properties`의 `sdk.dir`(커밋 안 됨). CI에서는 `ANDROID_HOME` 사용.
- JDK 17 기준.

---

## 어떻게 소통하길 원하는가

- 직접적·짧게·구체적으로. 서론 없이.
- 파일명·함수명·줄번호로 말한다. 모호한 지시어 금지.
- 깨진 건 깨졌다고 그대로 말한다. 테스트 실패면 출력과 함께 보고한다.
- 답변 끝은 요약이 아니라 **다음 액션**으로.
