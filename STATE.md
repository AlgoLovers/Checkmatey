# STATE.md — Checkmatey 현재 상태

> 루프의 "기억". 컨텍스트 윈도우 밖에서 살아남는 상태. 작업 시작 전 여기부터 읽는다.
> 계약/런북은 [`LOOP.md`](LOOP.md), 규칙은 [`CLAUDE.md`](CLAUDE.md).

- **성숙도**: L1 (보고 전용)
- **현재 마일스톤**: M0 — 하네스 + 빌드 가능한 스켈레톤 (완료)

## 마일스톤

- [x] **M0 — 하네스 & 스켈레톤**: Loop Engineering 문서, GitHub 설정, 폰/태블릿 Compose 스켈레톤,
      순수 Kotlin 체스 모델 시드(+테스트).
- [ ] **M1 — 체스 코어**: `core/chess`에 합법수 생성·이동·체크/체크메이트 판정 + 유닛 테스트.
- [ ] **M2 — Play**: 보드에서 실제로 두기(선택·이동·하이라이트), FEN/PGN 왕복.
- [ ] **M3 — Learn**: 규칙/전술 레슨 흐름.
- [ ] **M4 — Puzzles**: 전술 퍼즐(레이팅) + 진행 저장.
- [ ] **M5 — Profile**: 진행·레이팅·설정.

## 결정 로그 (Decision Log)

- **2026-07-17** · 스택 = Kotlin + Jetpack Compose (Material 3), minSdk 24 / target 35.
- **2026-07-17** · 체스 엔진 = **순수 Kotlin 로직으로 시작**. Stockfish 네이티브는 `core/chess`
  인터페이스 뒤에 나중에. (사용자 결정)
- **2026-07-17** · 폰+태블릿 = `NavigationSuiteScaffold`(자동 적응) + `WindowSizeClass`.
- **2026-07-17** · 원격 = github.com/AlgoLovers/Checkmatey (**public**). (사용자 결정)
- **2026-07-17** · 개발 방식 = Loop Engineering, 현재 L1.

## 다음 액션 (Next Actions)

1. **M1 시작**: `core/chess`에 `Move`, 각 기물의 유사수(pseudo-legal) 생성부터.
   각 기물별로 테스트 먼저 작성 → 구현 → `./gradlew testDebugUnitTest` 초록불.
2. 체크/핀/합법수 필터링 추가.
3. `Play` 화면을 `core/chess`에 연결(탭으로 기물 선택 → 합법 목적지 하이라이트 → 이동).

## 열린 질문 / 리스크

- 기보/퍼즐 데이터 출처(예: Lichess 오픈 DB) 라이선스 확인 필요 — M4 전에.
- 온디바이스 분석(힌트/블런더 감지)이 필요해지면 Stockfish 통합 시점 재검토.

## 최근 루프 런 요약

- (아직 없음) — LOOP-2 트리아지가 처음 돌면 여기에 요약이 쌓인다.
