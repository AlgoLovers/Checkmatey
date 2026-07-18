# DESIGN.md — Checkmatey 디자인 규칙

> 2026-07 리디자인(자매앱 딱풀의 디자인 언어 이식). 목표: **밝고 깨끗하게** — 틴트 캔버스 위에
> 흰 카드가 뜨고, 브랜드 색은 CTA·핵심 강조에만 아껴 쓴다. 색은 취향이 아니라 **역할(role)**.

## 원칙

모든 화면은 **라이트·다크 두 벌이 동시에** 완성되어야 한다. 색은 `MaterialTheme.colorScheme.*`
역할로만 쓰고, 원전 값은 `ui/theme/Color.kt`·`Theme.kt`에 격리한다.

## 하드 룰 (위반 = 버그)

1. **색 리터럴 격리** — `Color(0xFF…)`는 `ui/theme/`에만. 화면 코드는 `colorScheme.*`.
   (예외: 보드 칸·리뷰 화살표처럼 콘텐츠 자체의 색 — `ChessBoard`, `ReviewScreen` arrows)
2. **짝 규칙** — 배경이 `X`면 그 위 글자·아이콘은 반드시 `onX`. 임의 조합 금지.
3. **루트는 반드시 칠한다** — `MainActivity`가 루트 `Surface(background)`로 전 화면을 감싼다.
   Dialog·새 창은 각자 배경 책임. 맨 `Box/Column` 루트 금지.
4. **두 벌 동시** — 새 색 역할은 light/dark 값을 **같은 커밋**에. 창 테마(`values/themes.xml`
   ↔ `values-night/themes.xml`)도 짝. `forceDarkAllowed=false` 유지(앱이 다크를 직접 처리).
5. **대비 숫자** — 본문 4.5:1, 큰 글자·버튼·아이콘 3:1 (WCAG AA). 새 색 짝 추가 시 확인.
6. **타이포·간격 토큰** — `MaterialTheme.typography.*`(Pretendard) + 4dp 그리드.

## 팔레트 (역할 → 값)

원전은 `ui/theme/Color.kt`. 요지:
- 캔버스: 라이트 `F3F5F3`(그린-그레이 틴트, 순백 금지) / 다크 `111512`
- 카드(surfaceContainer): 라이트 `FFFFFF`(뜨는 흰 카드) / 다크 `1D231F` — 20dp 라운드 + 1dp 그림자
- **Primary=에메랄드 그린**(브랜드·긍정) / **Secondary=블루**(정보·더 좋은 수) /
  **Tertiary=앰버**(안내·연속) / **Error=코랄**(실수·오답)
- 이 의미색은 복기 보드 화살표(초록/파랑/앰버/코랄)와 일치한다.

## 디자인 시스템 컴포넌트 (`ui/components/`)

- **`GradientPrimaryButton`** — 주요 CTA. 브랜드 세로 그라데이션 + 컬러 소프트 섀도우(입체감).
- **`SectionCard`** — 틴트 캔버스 위에 뜨는 흰 카드(기본 콘텐츠 컨테이너).
- **`StatTile`** — 라벨+큰 값+파스텔 원형 아이콘 배지(홈·프로필 통계).

## 서체

**Pretendard**(OFL). 앱 실사용 글자만 서브셋해 내장(≈0.23MB, `tools/fonts/subset.py`).
UI 문구를 바꾸면 원본 `PretendardVariable.ttf`로 스크립트를 재실행한다(원본은 대용량이라 미포함).

## QA (UI 변경 시)

| 축 | 값 |
|---|---|
| 테마 | **light / dark (둘 다 필수)** |
| 기기 | phone / tablet (레이아웃 분기가 다르면 둘 다) |
| 폰트 | 1.0 / (릴리스 전) 1.5 |

- 다크 전환: `adb shell cmd uimode night yes|no`.
- ⚠️ SwiftShader 에뮬레이터는 `screencap`이 흰 화면으로 나올 수 있다 — 픽셀 확인은 실기기로.
