# Changelog - SoundLog

## [v1.0.5] - 2026-08-07

### 🔐 Bot Token 보안 마스킹 및 Shazam 자동화 수음 완전 보강

- **Bot Token 마스킹 & Press-and-Hold 보기 아이콘**:
  - `SettingsScreen.kt` 내 Bot Token 입력을 `PasswordVisualTransformation(***)`으로 보안 보호.
  - 우측 Eye 아이콘(`Visibility`/`VisibilityOff`)을 누르고 있는 동안에만 평문으로 확인할 수 있도록 터치 이벤트(`pointerInput`, `detectTapGestures`) 적용.

- **Shazam 뷰 노드 및 클릭 재시도 루프 보강**:
  - `ShazamNodeFinder.kt` 내 최신 Shazam 앱 버전별 메인 버튼 View ID (`shazam_button`, `touch_to_shazam`, `auto_shazam_button`, `v_shazam_button`, `tag_button` 등) 및 contentDescription 대대적 확장.
  - UI 노드 클릭 실패 시 화면 중앙 제스처 터치(`dispatchCenterTap`) Fallback 보장.

- **Shazam 수음 완료 동기 대기 & WakeLock 유지**:
  - `ForegroundSchedulerService.kt`의 비동기 수음 흐름을 `CompletableDeferred`로 동기 대기 처리하여 Shazam 수음이 진행되는 동안 화면(WakeLock)이 조기 꺼짐을 원천 방지.
  - Shazam 인식 결과가 감지되면 즉시 Room DB 저장 및 텔레그램 채널로 메시지 전송.
