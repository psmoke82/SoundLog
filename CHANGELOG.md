# Changelog - SoundLog

## [v1.0.1] - 2026-08-07

### 🎵 주요 기능 추가 및 변경 사항 (Initial Release)

- **백그라운드 24시간 스케줄러 & 전원 관리**:
  - Foreground Service (`ForegroundSchedulerService`) 기반 24시간 상시 가동 지원.
  - 인식 주기(3~5분) 마다 최소 `WakeLock` 획득 및 화면 자동 ON/OFF 수명주기 제어 (`PowerManagerHelper`).

- **Shazam UI 자동 제어 & 동적 타임아웃 엔진**:
  - `AccessibilityService` 기반 5단계 Fallback 노드 검색 (viewId ➔ contentDescription ➔ text ➔ clickable ➔ screen tap).
  - 최대 12초 동적 타임아웃 수음으로 결과 감지 시 즉시 조기 종료.

- **곡 정규화 & 중복 발송 방지**:
  - 특수문자/공백 제거 및 대소문자 통일 (`SongKeyNormalizer`).
  - 10분 내 동일 곡 인식 시 텔레그램 발송 자동 스킵.

- **Room DB & 텔레그램 오프라인 재전송 큐**:
  - Room DB 기반 곡 식별 이력 및 실행 스텝 로그 저장.
  - 텔레그램 Bot API 전송 실패 시 PENDING 상태 유지 후 자동 재시도 (최대 3회).
  - Android Keystore (`EncryptedSharedPreferences`) 기반 Bot Token 및 Chat ID 암호화 보관.

- **장애 복구 Watchdog & 부팅 자동 시작**:
  - 3회 연속 실패 시 Shazam 앱 강제 종료 및 자체 자동 복구 (`WatchdogManager`).
  - 단말기 재부팅 시 `ACTION_BOOT_COMPLETED` 수신 후 자동 서비스 가동 (`BootReceiver`).

- **관리자 대시보드 UI (Jetpack Compose)**:
  - 🟢/🔴 실시간 가동 스위치, 최근 식별 곡, 오늘 통계 카운터.
  - 앱 수행 필수 체크리스트 (Shazam 미설치 시 플레이스토어 바로가기, 접근성/배터리/알림/그리기 권한 설정 바로가기).
  - `[지금 인식 테스트]`, `[Telegram 테스트]`, `[서비스 재시작]` 유틸리티.
  - SoundLog 전용 맞춤 앱 아이콘 적용 (`@drawable/ic_launcher_icon`).
