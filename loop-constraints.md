# loop-constraints.md — 안전 경계 & 거버넌스

> 루프가 "무엇을 해도 되고 안 되는지"의 기계적 경계. 자율성이 올라갈수록(L2→L3) 여기가 방어선이다.
> 사람 게이트가 항상 최종 통제점.

## 절대 금지 (Denylist — 사람 확인 없이 절대 X)

- 비밀정보/서명키 커밋: `*.keystore`, `*.jks`, `keystore.properties`, `.env*`, `local.properties`
- 파괴적 명령: `rm -rf`, `git reset --hard`, `git push --force`(공유 브랜치), 히스토리 재작성
- `main`에 직접 커밋/푸시 (항상 브랜치 → PR)
- `--no-verify`로 훅/CI 우회
- 대용량 바이너리 커밋(APK/AAB/모델weight). 필요 시 릴리스 아티팩트나 LFS.
- 외부로 코드/데이터 전송(새 서드파티 서비스) — 사람 승인 필요
- 라이선스 불명확한 데이터셋(기보/퍼즐) 번들링

## 자동 허용 (Allowlist — L2에서 자동 진행 가능)

- `feature/**`, `docs/**`, `app/src/test/**` 안에서의 코드/문서/테스트 변경
- 브랜치 생성, 커밋, **PR 열기** (머지는 사람)
- `./gradlew testDebugUnitTest`, `assembleDebug`, `lint` 실행
- Dependabot 논브레이킹 패치 업데이트(CI 초록불 시 머지 후보로 라벨)

## 예산 (Budget — 언제 멈추나)

- 단일 루프 런: 최대 **~10 턴** 또는 목표 1개 완료 중 먼저.
- 반복이 3회 연속 진전 없으면 멈추고 에스컬레이션(무한 재시도 금지).
- 토큰 급증은 서브에이전트·장기 런에서 발생 — 큰 팬아웃 전에 견적/확인.

## 에스컬레이션 (Escalation — 언제 사람에게)

- 아키텍처가 갈리거나 요구가 상충 → **멈추고** 옵션 2~3개 + 트레이드오프 제시(CLAUDE.md 혼란 프로토콜).
- 검증 신호가 없거나 애매 → 사람에게.
- Denylist에 닿는 작업이 필요 → 사람 승인 요청.
- CI가 빨간불인데 원인이 불명확 → 로그와 함께 보고.

## 검증 게이트 (Verification Gate)

머지/완료 전 반드시 통과:

1. `./gradlew testDebugUnitTest` — 초록불
2. `./gradlew :app:assembleDebug` — 빌드 성공
3. `./gradlew lint` — 신규 심각 경고 없음
4. 코드와 테스트가 같은 diff에 있음

> 이 파일과 [`STATE.md`](STATE.md)가 어긋나면(드리프트) 사람이 조정한다. 자율성은 여기 규칙이
> 신뢰될 때만 L2→L3로 올린다.
