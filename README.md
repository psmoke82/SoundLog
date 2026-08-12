# 🎵 SoundLog (음악 실시간 식별 & 텔레그램 자동 공유 서비스)

[![Android SDK](https://img.shields.io/badge/Android-8.0%2B%20%28API%2026%2B%20Oreo%29-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room DB](https://img.shields.io/badge/Database-Room%202.6.1-4285F4?style=flat-square)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

> **SoundLog**는 특정 공간(매장, 카페, 사무실 등)의 스피커에서 재생되는 음악을 **안드로이드 공기계 1대에서 24시간 상시 가동**하며, **Shazam(샤잠) 앱을 주기적으로 자동 실행**하여 실시간으로 음악을 무료 식별하고 지정된 **텔레그램 채널/그룹에 자동 공유**하는 자율 운용 솔루션입니다.

---

## 🌟 주요 특징 (Key Features)

- 🔗 **유튜브 검색 링크 자동 연동**:
  - 곡명과 아티스트 기반으로 유튜브 검색 URL(`https://www.youtube.com/results?search_query=...`)을 즉시 생성 및 텔레그램 메시지에 하이퍼링크 연동.
- 🎨 **앱 아이콘 & 브랜드 테마 리뉴얼**:
  - 새로 디자인된 전용 앱 런처 아이콘(`ic_launcher`) 및 시각적 완성도를 높인 브랜드 테마 컬러 적용.
- 🖼️ **앨범 자켓 이미지 전송 (3종 옵션)**:
  - `미전송` / `Shazam 스크린샷` / `iTunes API(1000x1000 고화질 CoverArt)` 선택 전송 기능 제공.
- 🔄 **24시간 백그라운드 구동 & 배터리 최적화**:
  - Foreground Service 및 WakeLock 최적화를 적용하여, 인식 주기(기본 2분)에만 화면을 켜고 인식 즉시 해제하여 배터리/발열 최소화.
- 🤖 **Shazam 접근성 자동화 (`AccessibilityService`)**:
  - 5단계 Fallback 노드 탐색 기술과 동적 타임아웃(Dynamic Timeout)을 통해 빠른 음악 수음 및 인식 보장.
- 🎼 **곡명 정규화 & 중복 발송 방지**:
  - 특수문자/공백 정규화 및 설정된 이력 시간(기본 10분) 내 동일한 곡 중복 인식 시 텔레그램 발송 자동 스킵.
- 📦 **오프라인 전송 큐 & 보안 암호화**:
  - 네트워크 단선 시 Room DB 내 전송 큐(`PENDING`) 저장 후 자동 재시도하며, Bot Token 및 Chat ID는 EncryptedSharedPreferences로 암호화 보관.
- 🛡️ **자체 장애 복구 Watchdog & 부팅 자동 시작**:
  - 3회 연속 인식 실패 시 Shazam 프로세스 강제 종료 후 자체 복구하며, 단말기 재부팅(`ACTION_BOOT_COMPLETED`) 시 자동으로 서비스 시작.
- 📱 **Compose 관리자 대시보드 UI**:
  - 실시간 서비스 제어, 필수 권한 체크리스트 바로가기, 수동 테스트 기능 및 스텝별 실시간 로그 모니터링 화면 제공.

---

## 🏗️ 시스템 동작 아키텍처

```text
[ 관리자 전용 Android 공기계 (24시간 상시 가동) ]

  ① [ Foreground Service ] (24시간 백그라운드 서비스 상시 감시)
          │
          ▼ (인식 주기 도달)
  ② [ Scheduler & WakeLock ] (화면 잠시 ON 및 자원 획득)
          │
          ▼
  ③ [ AccessibilityService ] (Shazam 자동화 수음 처리)
          ├─► 1. Shazam 앱 자동 실행
          ├─► 2. 5단계 Fallback 노드 탐색 및 자동 클릭
          └─► 3. 동적 타임아웃 감지 & Artist / Title 추출
          │
          ▼
  ④ [ 곡명 정규화 & 중복 검사 ] (10분 이내 동일 곡 중복 발송 스킵)
          │
          ▼
  ⑤ [ Room DB 저장 ] (SongResult & ExecutionLog 데이터 저장)
          │
          ▼
  ⑥ [ Telegram Queue ] (성공: SENT / 실패 시 PENDING 저장)
          │
          ▼ (HTTP POST 연동)
  ⑦ [ Telegram Bot API ] ──► 📢 텔레그램 채널/그룹 자동 공유 (유튜브 링크 연동)

─────────────────────────────────────────────────────────────────
🛡️ [ Watchdog 자동 복구 시스템 ]
  - 수음/자동화 3회 연속 실패 감지 시 Shazam 프로세스 강제 종료 후 순차 자체 자동 복구
```

---

## 📋 사전 세팅 가이드 (사용자 준비 사항)

### 1. 텔레그램 봇 및 채널 준비
1. 텔레그램에서 `@BotFather`를 통해 새 봇을 생성하고 **HTTP API Token**을 발급받습니다.
2. 음악 정보를 공유할 **텔레그램 채널/그룹**을 생성하고, 발급된 **봇을 관리자(Admin)**로 추가합니다.
3. 해당 채널의 **Chat ID** (예: `@my_music_channel` 또는 `-100123456789`)를 확인합니다.

### 2. 안드로이드 공기계 단말기 설정
1. Google Play 스토어에서 공식 **Shazam** 앱을 설치하고, 최초 실행 후 **마이크 권한**을 **항상 허용**으로 설정합니다.
2. 플레이스토어에서 **Shazam 앱 자동 업데이트 비활성화**를 설정합니다. (UI 변경 방지)
3. 단말기 **[설정] ➔ [접근성] ➔ [설치된 앱] ➔ [SoundLog]**를 선택하여 **켜짐(ON)**으로 활성화합니다.
4. **배터리 최적화 제외**: 단말기 **[설정] ➔ [애플리케이션] ➔ [SoundLog] ➔ [배터리] ➔ 제한 없음**으로 설정합니다.
5. **화면 잠금 방식**: **없음**으로 설정하고, 단말기 충전 케이블을 연결하여 고정 배치합니다.

### 3. Shazam App 환경 설정
- **앱을 실행할 때 Shazam 자동 시작**: ON
- **결과 알림 진동**: OFF
- **동영상 자동 재생**: OFF

---

## ⚙️ 동적 설정 항목 (Configuration)

| 설정 항목 | 기본값 | 설명 |
| :--- | :---: | :--- |
| **인식 주기 (Interval)** | `2분` | Shazam을 자동 실행하여 음악을 식별하는 주기 |
| **최대 타임아웃 (Timeout)** | `30초` | Shazam 및 결과 노드를 기다리는 최대 동적 대기시간 |
| **중복 방지 시간 (Dedup Window)** | `10분` | 동일한 곡이 연속 인식될 경우 발송을 스킵하는 시간 범위 |
| **Telegram 재시도 횟수** | `2회` | 통신 장애 시 큐(`PENDING`) 저장 후 최대 재전송 시도 횟수 |
| **앨범 아트 옵션** | `미전송 / Shazam / iTunes API` | 텔레그램 메시지와 함께 송신할 커버 아트 옵션 |
| **음악 링크 옵션** | `미전송 / YouTube` | 곡명 클릭 시 이동할 하이퍼링크 추출 옵션 |

---

## 🛠️ 기술 스택 (Tech Stack)

- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose (Material3 Dark Design System)
- **Database**: Room Database 2.6.1
- **Security**: AndroidX Security Crypto (EncryptedSharedPreferences)
- **Network**: Retrofit 2.9.0, OkHttp 4.12.0, Gson
- **Automation**: Android AccessibilityService, YouTube Focus Shift & KeyEvent, Coroutines & Flow


---

## 📄 라이선스 (License)

This project is licensed under the [MIT License](LICENSE).
