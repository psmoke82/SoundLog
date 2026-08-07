# 🎵 SoundLog (음악 실시간 식별 & 텔레그램 자동 공유 서비스)

[![Android SDK](https://img.shields.io/badge/Android-8.0%2B%20%28API%2026%2B%20Oreo%29-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room DB](https://img.shields.io/badge/Database-Room%202.6.1-4285F4?style=flat-square)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

> **SoundLog**는 특정 공간(매장, 카페, 사무실 등)의 스피커에서 재생되는 음악을 **안드로이드 공기계 1대에서 24시간 상시 가동**하며, **Shazam(샤잠) 앱을 주기적으로 자동 실행**하여 실시간으로 음악을 무료 식별하고 지정된 **텔레그램 채널/그룹에 자동 공유**하는 자율 운용 솔루션입니다.

---

## 🌟 주요 특징 (Key Features)

- 🔄 **24시간 상시 백그라운드 구동 (`Foreground Service` & `WakeLock`)**:
  - 시스템에 의한 강제 종료를 방지하는 상단 고정 Notification 탑재.
  - 인식 주기(기본 5분) 도달 시에만 화면을 ON하고 Shazam 인식 완료 즉시 화면 OFF 및 `WakeLock`을 필수 해제하여 배터리/발열 최소화.

- 🤖 **Shazam 접근성 자동화 (`AccessibilityService`)**:
  - 좌표 방식이 아닌 **5단계 Fallback 노드 검색 기술** 적용 (`viewId` ➔ `contentDescription` ➔ `text` ➔ `clickable` ➔ `Gesture Tap`).
  - **동적 타임아웃(Dynamic Timeout)**: 최대 대기시간(기본 12초) 내 결과 감지 시 즉시 조기 종료하여 빠른 처리 보장.

- 🎼 **곡명 정규화 & 중복 발송 방지**:
  - 특수문자/공백 제거 및 대소문자 정규화 (`Artist|Title`).
  - 지정된 시간(기본 10분) 이내 동일한 곡이 중복 인식될 경우 텔레그램 발송 자동 스킵.

- 📦 **Room DB & 오프라인 전송 큐 (`Telegram Queue`)**:
  - Wi-Fi 일시 단선 등 네트워크 장애 시 Room DB에 `PENDING` 상태로 안전하게 저장 후 지연 재시도.
  - Android Keystore (`EncryptedSharedPreferences`)를 사용하여 Telegram Bot Token 및 Chat ID를 안전하게 암호화 보관.

- 🛡️ **자체 장애 복구 Watchdog & 부팅 자동 시작 (`BootReceiver`)**:
  - 3회 연속 자동화/시스템 실패 시 Shazam 앱 데이터/프로세스 강제 종료 후 자체 복구.
  - 단말기 재부팅(`ACTION_BOOT_COMPLETED`) 시 별도 조작 없이 자동으로 서비스 시작.

- 📱 **관리자 전용 대시보드 UI (Jetpack Compose)**:
  - 🟢/🔴 서비스 실시간 동작 제어 스위치.
  - **앱 수행 필수 체크리스트**: Shazam 미설치 시 플레이스토어 바로가기, 접근성/배터리/알림/그리기 권한 미설정 시 관련 설정 화면 직통 바로가기 제공.
  - `[지금 인식 테스트]`, `[Telegram 테스트]`, `[서비스 재시작]` 유틸리티 기능.
  - 스텝별 음악 인식 성공/실패 실행 로그 모니터링 화면 제공.

---

## 🏗️ 시스템 동작 아키텍처

```text
[ 관리자 전용 Android 공기계 (24시간 상시 가동) ]
                                                                         
  [ Foreground Service ] ◄──────────────────────────┐ (상태 감시)
          │                                         │
          ▼ (3~5분 주기)                            │
      Scheduler ───(WakeLock 짧게 획득 / 화면 ON)    │
          │                                         │
          ▼                                         │
  [ AccessibilityService ]                          │
          │                                         │
          ├─► ① Shazam 앱 자동 실행                 │
          ├─► ② 5단계 Fallback 노드 탐색 & 클릭     │ [ Watchdog ]
          └─► ③ 동적 타임아웃(최대 12초) 응답 수음   │ (3회 연속 실패 시
          │                                         │  강제종료 & 복구)
          ▼                                         │
    Artist / Title 추출                             │
          │                                         │
          ▼                                         │
   SongKey 정규화 & 중복 검사 (10분 이내 동일곡 스킵) │
          │                                         │
          ▼                                         │
   [ Room DB ] (SongResult & ExecutionLog 저장)     │
          │                                         │
          ▼                                         │
   [ Telegram Queue ] ──(성공: SENT / 실패: PENDING)│
          │                                         │
          ▼ (HTTP POST)                             │
   [ Telegram Bot API ] ────────────────────────────┴───────────────────┘
          │
          ▼
  [ 텔레그램 채널/그룹 메시지 전송 ]
```

---

## 📋 사전 세팅 가이드 (사용자 준비 사항)

### 1. 텔레그램 봇 및 채널 준비
1. 텔레그램에서 `@BotFather`를 통해 새 봇을 생성하고 **`HTTP API Token`**을 발급받습니다.
2. 음악 정보를 공유할 **'텔레그램 채널/그룹'**을 생성하고 생성한 **'봇을 관리자(Admin)'**로 추가합니다.
3. 해당 채널의 `Chat ID` (예: `@my_music_channel` 또는 `-100123456789`)를 확인합니다.

### 2. 안드로이드 공기계 단말기 설정
1. Google Play 스토어에서 공식 **'Shazam'** 앱을 설치하고, 최초 실행 후 **'마이크 권한'**을 **'항상 허용'**으로 설정합니다.
2. 플레이스토어에서 **'Shazam 앱 자동 업데이트 비활성화'**를 설정합니다. (UI 변경 방지)
3. 단말기 **[설정] ➔ [접근성] ➔ [설치된 앱] ➔ [SoundLog]**를 선택하여 **'켜짐(ON)'**으로 활성화합니다.
4. **배터리 최적화 제외**: 단말기 **[설정] ➔ [애플리케이션] ➔ [SoundLog] ➔ [배터리] ➔ '제한 없음'**으로 설정합니다.
5. **화면 잠금 방식**: **'없음'**으로 설정하고, 단말기 충전 케이블을 연결하여 고정 배치합니다.

---

## ⚙️ 동적 설정 항목 (Configuration)

| 설정 항목 | 기본값 | 설명 |
| :--- | :---: | :--- |
| **인식 주기 (Interval)** | `5분` | Shazam을 자동 실행하여 음악을 식별하는 주기 |
| **최대 타임아웃 (Timeout)** | `12초` | Shazam 결과 노드를 기다리는 최대 동적 대기시간 |
| **중복 방지 시간 (Dedup Window)** | `10분` | 동일한 곡이 연속 인식될 경우 발송을 스킵하는 시간 범위 |
| **Telegram 재시도 횟수** | `3회` | 통신 장애 시 큐(`PENDING`) 저장 후 최대 재전송 시도 횟수 |

---

## 🛠️ 기술 스택 (Tech Stack)

- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose (Material3 Dark Design System)
- **Database**: Room Database 2.6.1
- **Security**: AndroidX Security Crypto (EncryptedSharedPreferences)
- **Network**: Retrofit 2.9.0, OkHttp 4.12.0, Gson
- **Background**: Android AccessibilityService, Foreground Service, Coroutines & Flow

---

## 📄 라이선스 (License)

This project is licensed under the [MIT License](LICENSE).
