# AGENTS.md

크로스툴(코딩 에이전트 공통) 규칙 파일. **전체 규칙은 [`CLAUDE.md`](CLAUDE.md)를 읽어라.**
이 파일은 Codex / Cursor / Gemini CLI 등 `AGENTS.md`를 읽는 도구를 위한 포인터다.

## 핵심 규칙 (요약 — 세부는 CLAUDE.md)

1. **작업 전 [`STATE.md`](STATE.md)를 읽고, 목표 하나를 정한다.** (Loop Engineering, 현재 L1)
2. **코드와 테스트를 같은 커밋에.** 순수 Kotlin 로직은 `./gradlew testDebugUnitTest`로 검증.
3. **폰+태블릿을 하나의 코드로.** 네비게이션은 `NavigationSuiteScaffold`, 분기는 `WindowSizeClass`.
4. **`core/`는 Android 비의존 순수 Kotlin.** `feature/`는 서로 독립.
5. **완료 보고는 DONE / DONE_WITH_CONCERNS / BLOCKED / NEEDS_CONTEXT 중 하나.** "부분 완료" 없음.
6. **비밀정보·`local.properties`·바이너리 커밋 금지.** 파괴적 명령은 확인 없이 실행 금지.

빌드: `./gradlew :app:assembleDebug` · 테스트: `./gradlew testDebugUnitTest` · 린트: `./gradlew lint`
