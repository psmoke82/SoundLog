# Changelog - SoundLog

## [v1.0.4] - 2026-08-07

### 🎵 주요 릴리즈 패키지 및 수정 내역 (Release v1.0.4)

- **APK 자동 빌드 및 리소스 최적화**:
  - `SoundLog-v1.0.4-rel.apk` 성공적 빌드 완료 및 릴리즈 바이너리 패키징.
  - 벡터 그래픽 지원을 위한 호환 가능한 전용 앱 아이콘 리소스 (`ic_launcher_icon.xml`) 적용.

- **앱 수행 필수 체크리스트 (App Checklist)**:
  - 대시보드 상단 환경/권한 체크리스트 UI 추가.
  - Shazam 앱 미설치 시 플레이스토어 바로가기 (`market://details?id=com.shazam.android`).
  - 접근성(Accessibility) 서비스, 배터리 최적화 제외, 알림 권한, 다른 앱 위에 그리기(Overlay) 미설정 시 안드로이드 설정 직통 바로가기 지원.

- **안정성 및 호환성 개선**:
  - `MainActivity.kt` 의 `onCreate(savedInstanceState)` 상태 복원 파라미터 보장.
  - Material3 Compose 아이콘 컴포넌트 정리.
