# Project Rules for SoundLog

공통 개발/릴리즈 규칙은 전역(`~\.gemini\config\AGENTS.md`)을 따릅니다.
아래는 Android 공통 규칙(`~\.gemini\config\rules\android-template.md`에서 복사됨, 도구가
import를 지원하지 않아 수동 복사 — 원본 수정 시 이 섹션도 수동으로 재반영 필요)과
SoundLog 프로젝트 고유 규칙입니다.

## Android 공통 규칙

### 버전 정책

1. **버전 형식**: `v[Major].[Minor].[Patch]`
2. **Patch(마지막 숫자)**: 직전 릴리즈 대비 **순차적으로 +1 자동 증가**시킵니다.
3. **Major/Minor**: 사용자가 명시적으로 상향을 지정한 경우에만 올립니다.
4. **`versionCode`(빌드 번호)**: 버전명의 Patch 숫자와는 별개로, 기존 값보다 항상 증가하는 정수로 관리합니다.

### 테스트 빌드 절차

1. Gradle 디버그 빌드 실행: `.\gradlew assembleDebug`
2. 빌드 결과물을 `releases/test/` 폴더에 `app_debug_YYMMDD_HHmm.apk` 형식의 파일명으로 복사 (예: `app_debug_260811_2009.apk`)
3. `Test-Path`로 생성 검증 후 절대 경로 안내
4. 사용자 사이드로드 테스트 후 **"배포해줘"** 명령 전까지 버전업/푸시/릴리즈 보류

### Gradle 빌드 규칙

- 빌드 전 `./gradlew --stop`, 빌드 중 강제종료 금지
- 실패 시 캐시/환경 문제(cannot find symbol 등)와 실제 코드 문제를 구분
- 원인 불명 시 `app/build`, `.gradle` 삭제 후 클린 빌드
- 세션 종료 전 `./gradlew --stop`

## SoundLog 프로젝트 고유 규칙

1. **버전 표기 갱신 위치**: 설정 화면 하단 버전 표시(`SettingsScreen.kt`)
2. **릴리즈 산출물 명명**: `releases/SoundLog-v[Major].[Minor].[Patch]-rel.apk`
3. **Release 제목 예시**: `--title "SoundLog v1.0.21"`
