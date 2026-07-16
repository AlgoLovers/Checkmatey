# Checkmatey ♟️

체스를 처음 배우는 사람을 위한 **안드로이드 학습 앱**. 규칙 → 전술 → 오프닝 → 실전을 단계적으로.
**폰과 태블릿을 하나의 코드베이스로** 지원한다.

## 스택

- Kotlin · Jetpack Compose (Material 3)
- Gradle (Kotlin DSL + 버전 카탈로그) · JDK 17
- minSdk 24 · target/compile SDK 35
- 체스 로직: 순수 Kotlin (`core/chess`) — Stockfish 네이티브는 인터페이스 뒤에 나중에

## 빠른 시작

```bash
# 유닛 테스트 (순수 Kotlin 체스 로직)
./gradlew testDebugUnitTest

# 디버그 APK 빌드
./gradlew :app:assembleDebug

# 연결된 기기/에뮬레이터에 설치
./gradlew installDebug
```

Android Studio로 열어도 된다. SDK 경로는 `local.properties`의 `sdk.dir`(커밋 안 됨).

## 구조

```
com.checkmatey
├─ MainActivity / CheckmateyApp   진입점 + 적응형 네비게이션 (폰=하단바 / 태블릿=레일)
├─ ui/theme, ui/board             Material 3 테마, ChessBoard
├─ feature/{learn,play,puzzles,profile}
└─ core/chess                     순수 Kotlin 체스 도메인 (JVM 테스트 가능)
```

자세한 내용: [`docs/architecture.md`](docs/architecture.md) · [`docs/product-vision.md`](docs/product-vision.md)

## 개발 방식 — Loop Engineering

이 프로젝트는 **루프 엔지니어링**으로 개발한다. 규칙과 계약이 코드와 함께 저장소에 있다:

- [`CLAUDE.md`](CLAUDE.md) — 에이전트/기여 규칙, 완료 기준, 안전 규칙 (핵심 하네스)
- [`LOOP.md`](LOOP.md) — 루프 계약 & 런북
- [`STATE.md`](STATE.md) — 현재 마일스톤·결정 로그·다음 액션 (루프의 기억)
- [`loop-constraints.md`](loop-constraints.md) — 안전 경계 (denylist / allowlist / 예산 / 에스컬레이션)
- [`docs/loop-design-checklist.md`](docs/loop-design-checklist.md) — 새 루프 설계 8문항

CI는 [`.github/workflows/ci.yml`](.github/workflows/ci.yml)에서 유닛테스트+빌드+린트를 돌린다.

## 라이선스

TBD.
