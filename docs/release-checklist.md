# Play 스토어 출시 체크리스트 — Checkmatey

## 앱 상태 (자동화된 부분 — 완료)

- [x] 규칙 완결: 체크메이트/스테일메이트 + **50수 규칙, 3회 반복, 기물 부족 무승부**
- [x] 무르기(undo), 게임종료 다이얼로그, 온보딩, 사운드, 인셋(edge-to-edge)
- [x] 커리큘럼: 레슨 11개(전 기물+체크 대응+특수규칙) + 엔드게임 실습 + 퍼즐 + 명국 + 복기
- [x] `versionName 1.0.0` / `versionCode 2`, minSdk 24 / target 35
- [x] **릴리스 빌드(R8/proguard) 통과** — `app-release-unsigned.apk` ~1.5MB
- [x] 유닛 테스트 50+ (규칙은 perft, 레슨/퍼즐 데이터는 자동 검증)

## 사람이 해야 하는 것 (출시 절차)

1. **서명 키 생성** (한 번만, 분실 금지 — 백업 필수):
   ```bash
   keytool -genkeypair -v -keystore ~/checkmatey-release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 -alias checkmatey
   ```
2. **서명 설정**: `keystore.properties`(커밋 금지, .gitignore에 이미 있음)에 경로/비밀번호 저장 후
   `app/build.gradle.kts`에 `signingConfigs` 추가 (요청 시 코드 생성해 드림).
3. **AAB 빌드**: `./gradlew bundleRelease` → `app-release.aab` 업로드.
4. **Play Console**: 앱 생성 → 스토어 등록정보(아이콘 512px, 피처 그래픽 1024x500, 스크린샷 폰/태블릿 각 2+),
   콘텐츠 등급 설문(전체이용가), **데이터 안전**: "수집하는 데이터 없음"(전부 온디바이스, 네트워크 미사용).
5. **개인정보처리방침 URL**: 수집 데이터가 없어도 링크 필요 — "이 앱은 어떤 데이터도 수집·전송하지 않습니다"
   한 줄이면 충분 (GitHub Pages로 호스팅 가능, 요청 시 생성).
6. 내부 테스트 트랙 → 프로덕션.

## v1.1 백로그 (출시 후)

- 승격 기물 선택 UI(현재 자동 퀸) · 흑으로 두기 · 퍼즐 대량(Lichess CC0 CSV 필요)
- 게임리뷰 실수 테마 → 약점 퍼즐 연계 · 실습 완료 영속 · 접근성(TalkBack 좌표)
