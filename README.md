# 🎵 SoundLog (음악 실시간 식별 & 텔레그램 자동 공유 서비스)

[![Android SDK](https://img.shields.io/badge/Android-8.0%2B%20%28API%2026%2B%20Oreo%29-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room DB](https://img.shields.io/badge/Database-Room%202.6.1-4285F4?style=flat-square)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

> **SoundLog**는 특정 공간(매장, 카페, 사무실 등)의 스피커에서 재생되는 음악을 **안드로이드 공기계 1대에서 24시간 상시 가동**하며, **Shazam(샤잠) 앱을 주기적으로 자동 실행**하여 실시간으로 음악을 무료 식별하고 지정된 **텔레그램 채널/그룹에 자동 공유**하는 자율 운용 솔루션입니다.

---

## 🌟 주요 특징 (Key Features)

- 🎧 **매장에 흐르는 음악을 자동으로 알아내요**: 스피커에서 나오는 노래를 앱이 알아서 듣고, 곡명과 아티스트를 자동으로 찾아줍니다. 사람이 직접 Shazam을 켤 필요가 없습니다.
- 📢 **찾은 곡을 텔레그램으로 바로 보내줘요**: 인식된 곡 정보를 미리 지정해둔 텔레그램 채널이나 단체방에 자동으로 전송해줍니다.
- 🔗 **유튜브에서 바로 들어볼 수 있어요**: 전송되는 메시지에 유튜브 검색 링크가 자동으로 붙어서, 눌러보면 바로 그 곡을 찾아 들을 수 있습니다.
- 🖼️ **앨범 커버 이미지도 함께 보내줘요**: 이미지 없이 텍스트만 보내기, Shazam 화면 캡처 첨부, 고화질 앨범 커버 첨부 중 원하는 방식을 골라 쓸 수 있습니다.
- 🔋 **하루 종일 켜둬도 배터리 부담이 적어요**: 정해진 주기에만 잠깐 화면을 켜서 인식하고 바로 꺼지도록 만들어져 있어, 공기계를 24시간 내내 켜놔도 배터리와 발열이 크게 늘지 않습니다.
- 🎼 **같은 곡이 반복돼도 알림이 도배되지 않아요**: 짧은 시간 안에 같은 곡이 다시 감지되면 중복으로 보내지 않고 자동으로 걸러냅니다.
- 📶 **인터넷이 잠깐 끊겨도 기록이 사라지지 않아요**: 전송에 실패한 곡은 대기열에 저장해뒀다가, 인터넷이 다시 연결되면 자동으로 이어서 보내줍니다.
- 🔒 **텔레그램 로그인 정보는 안전하게 보관돼요**: 봇 토큰이나 채팅방 아이디 같은 민감한 정보는 암호화되어 저장되므로 외부로 유출될 걱정이 없습니다.
- 🛡️ **문제가 생겨도 알아서 스스로 고쳐요**: 인식이 연속으로 실패하면 앱이 스스로 재시작해서 복구하고, 공기계가 꺼졌다 다시 켜져도 자동으로 작동을 재개합니다.
- 📱 **한눈에 확인하는 관리 화면**: 지금 정상 작동 중인지, 필요한 설정이 다 되어 있는지를 화면에서 바로 확인하고 켜고 끌 수 있으며, 지금까지 인식된 곡 기록도 한눈에 볼 수 있습니다.

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
2. 음악 정보를 공유할 **텔레그램 채널/그룹**을 생성하고, 발급받은 봇을 **관리자(Admin)** 권한으로 추가합니다.
3. 해당 채널의 **Chat ID** (예: `@my_music_channel` 또는 `-100123456789`)를 확인합니다.

### 2. 안드로이드 공기계 단말기 설정
1. Google Play 스토어에서 공식 **Shazam** 앱을 설치하고, 최초 실행 후 마이크 권한을 **항상 허용**으로 설정합니다.
2. 플레이스토어에서 **Shazam 앱 자동 업데이트 비활성화**를 설정합니다. (UI 변경 방지)
3. 단말기 **설정 ➔ 접근성 ➔ 설치된 앱 ➔ SoundLog** 경로로 이동하여 서비스를 **켜짐(ON)** 상태로 활성화합니다.
4. **배터리 최적화 제외**: 단말기 **설정 ➔ 애플리케이션 ➔ SoundLog ➔ 배터리** 메뉴에서 **제한 없음**으로 설정합니다.
5. **화면 잠금 방식**: 화면 잠금을 **없음**으로 설정하고, 충전 케이블을 연결하여 고정 배치합니다.

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
