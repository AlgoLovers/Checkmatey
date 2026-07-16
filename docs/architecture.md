# 아키텍처 — Checkmatey

## 스택

- **Kotlin** + **Jetpack Compose** (Material 3)
- Gradle Kotlin DSL + 버전 카탈로그(`gradle/libs.versions.toml`)
- minSdk 24 · compile/target SDK 35 · JDK 17
- AGP 8.7 · Kotlin 2.1 · Compose BOM 2024.12.01

## 레이어

```
com.checkmatey
├─ MainActivity          진입점. enableEdgeToEdge + CheckmateyTheme
├─ CheckmateyApp         적응형 네비게이션 루트
├─ ui/theme              Material 3 색·타이포·테마 (다이내믹 컬러 지원)
├─ ui/board              ChessBoard 등 재사용 체스 UI (도메인에 의존, 화면에는 비의존)
├─ feature/<name>        화면 단위: learn · play · puzzles · profile (+ common)
└─ core/chess            순수 Kotlin 도메인. Android 의존 없음 → JVM 유닛테스트로 빠르게 검증
```

의존 방향: `feature → ui → core`. **`core`는 아무것도 위로 의존하지 않는다.**

### 왜 이렇게?
- `core/chess`가 순수 Kotlin이면 `testDebugUnitTest`(수 초)로 규칙을 고정할 수 있다. 체스 규칙은
  결정적이므로 반드시 테스트로 못박는다.
- `feature`가 서로 독립이면 두 세션(또는 두 루프)이 다른 화면을 동시에 작업해도 충돌이 없다.
- 엔진을 나중에 바꾸려면(순수 Kotlin → Stockfish JNI) `core/chess`의 인터페이스만 갈아끼운다.

## 폰 + 태블릿 (핵심 요구사항)

하나의 코드베이스로 폰과 태블릿을 모두 지원한다.

- **네비게이션**: `NavigationSuiteScaffold`(material3-adaptive-navigation-suite)가 창 너비에 따라
  자동 전환한다 — compact=하단 바(폰), medium/expanded=네비게이션 레일/드로어(태블릿·폴더블).
- **분기 판단**: 하드코딩 dp 대신 `WindowSizeClass`(material3-window-size-class).
- **콘텐츠 상한**: 큰 화면에서 무한정 늘리지 않는다. 예) `PlayScreen`은 보드를 `widthIn(max=560.dp)`로 제한.
- **정사각 유지**: 보드 등은 `aspectRatio(1f)`.
- **리스트-디테일**: 태블릿에서 목록+상세를 나란히 두려면 `material3-adaptive`의
  `ListDetailPaneScaffold`를 쓴다(퍼즐/레슨 목록에 적합, M3~M4에서 도입 검토).
- 새 화면은 **폰/태블릿 프리뷰 둘 다** 확인한다.

## 체스 엔진 방향

- **지금**: 순수 Kotlin. `core/chess`에 `Move`, 기물별 수 생성, 체크/체크메이트, FEN/PGN.
- **나중(선택)**: 힌트·블런더 감지·평가바가 필요해지면 Stockfish(NNUE)를 JNI/네이티브로
  `core/chess`의 `Engine` 인터페이스 뒤에 붙인다. 앱 코드는 인터페이스만 안다.

## 테스트 전략

- **게이트(빠름·결정적)**: `app/src/test` — `core/chess` 규칙. `./gradlew testDebugUnitTest`.
- **UI/계기**: `app/src/androidTest` — Compose UI, 네비게이션, 화면 크기 적응.
- **CI**: `.github/workflows/ci.yml`가 유닛테스트+빌드+린트.

## 모듈화 로드맵

지금은 단일 `:app` 모듈. `core/chess`가 커지면 `:core-chess`(순수 Kotlin/JVM) 모듈로 분리해
빌드/테스트를 더 빠르고 병렬로 만든다.
