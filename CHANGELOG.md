# Changelog - SoundLog

## [v1.0.5] - 2026-08-08

### 🎵 Shazam 곡 정보 정밀 파싱, UI 접힘 처리 및 텔레그램 연동 완벽 보강

- **Shazam 날짜/시간 헤더 텍스트 릴리즈 정밀 필터링**:
  - `ShazamNodeFinder.kt` 내 정규식 기반 `isDateTimeOrIgnoredText` 구현.
  - Shazam 화면에서 "8월 8일 토요일", "12:06:44", "오전/오후" 등 히스토리 타임스탬프 헤더가 가수/제목으로 오추출되던 현상 완전 차단.
  - 순수 곡명(Title)과 가수명(Artist)만 정밀 파싱하여 텔레그램 전송 및 Room DB에 저장.

- **체크리스트 자동 접힘(Collapse) 기능**:
  - `DashboardScreen.kt` 내 필수 권한/환경이 모두 완료(`allPassed == true`)된 경우 체크리스트 카드가 자동으로 접히도록 개선.

- **대시보드 상태 카드 UI 개선**:
  - 상태 카드 전면의 중첩 아이콘 제거 (단일 12dp 상태 인디케이터로 정리).
  - 텍스트 표기를 `"24시간 정상 가동중"` ➔ `"모니터링 작동"`으로 단정하게 변경.

- **Bot Token 보안 마스킹 & Press-and-Hold 아이콘**:
  - `SettingsScreen.kt` 내 Bot Token 입력을 `PasswordVisualTransformation(***)`으로 보안 보호.
  - Eye 아이콘을 누르고 있는 동안에만 평문으로 볼 수 있는 `pointerInput` `detectTapGestures` 적용.
