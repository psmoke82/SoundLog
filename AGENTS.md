# Project Rules for SoundLog

## Release & GitHub Workflow Rules

1. **개발 및 소스 수정 단계 (수동 배포 모드)**:
   - 사용자의 개별 수정/개선 요청 시에는 매번 버전업, 깃허브 푸시, 릴리즈를 수행하지 않고 소스코드 변경사항을 누적합니다.

2. **코드 수정 후 테스트 빌드 제공 (Test Build)**:
   - 사용자의 개선/수정 요청에 따른 소스 변경 완료 후, 아래 절차로 테스트용 APK를 빌드하여 사용자에게 제공합니다:
     1. Gradle 디버그 빌드 실행: `.\gradlew assembleDebug`
     2. 빌드 결과물(`app/build/outputs/apk/debug/app-debug.apk`)을 `releases/test/` 폴더에 현재 시각 기반 파일명으로 복사:
        - 파일명 형식: `app_debug_YYMMDD_HHmm.apk` (예: `app_debug_260811_2009.apk`)
     3. `Test-Path`로 파일 생성 여부 100% 검증 후, 사용자에게 로컬 절대 경로(`releases/test/app_debug_YYMMDD_HHmm.apk`)를 안내합니다.
     4. 사용자가 스마트폰에 사이드로드하여 직접 테스트한 후, 최종 **"배포해줘"** 명령을 내릴 때까지 버전업/푸시/릴리즈 작업을 수행하지 않습니다.

3. **명시적 릴리즈/배포 요청 시 일괄 작업 (Batch Release)**:
   - 사용자가 "배포해줘", "릴리즈해줘", "버전 올려서 깃허브 등록해줘" 등 **명시적으로 릴리즈를 요청할 때에만** 아래 일련의 릴리즈 작업을 한 번에 수행합니다:
     1. 누적 변경사항 `git add .` 및 `git commit -m "..."` (`releases/` 폴더 내 산출물은 `.gitignore` 처리되어 Git 커밋/푸시 대상에서 제외)
     2. 현재 Git 커밋 수(`git rev-list --count HEAD`) 기반으로 버전 산출 (`v1.0.[커밋수]`)
     3. **버전 연동 필수 갱신**:
        - 설정 화면 하단 버전 표시 (`SettingsScreen.kt`)를 이번 릴리즈 버전명(`v1.0.[커밋수]`)으로 갱신
        - `README.md` 문서 내 최신 버전 및 릴리즈 내역 갱신
     4. APK 자동 빌드 후 로컬 `releases/SoundLog-v1.0.[커밋수]-rel.apk`로 저장
     5. `git push origin main` 소스코드 푸시 (`releases/` 폴더 파일은 git에 푸시하지 않고 배제)
     6. 버전 태그(`v1.0.[커밋수]`) 생성 및 `git push origin --tags` 푸시
     7. `CHANGELOG.md` 문서에 묶인 마이너 개선 내역 한눈에 정리
     8. GitHub Release 게시 (Release 제목/Title은 수식어 없이 앱이름과 버전명만 사용, 예: `--title "SoundLog v1.0.21"`) 및 **GitHub의 Release 탭에만 최종 빌드된 APK 파일 업로드**
     9. **릴리즈 링크 및 웹 페이지 생존 직접 검증**: `git push` 및 `gh release` 등록 후, 해당 릴리즈 페이지 URL 및 다운로드 링크가 실제로 정상 작동(HTTP 200 또는 `gh release view` 확인)하는지 최종 직접 확인한 후에만 사용자에게 릴리즈 링크, APK 다운로드 링크, Release Notes 전문을 알림 제공.


4. **릴리즈 문제 발생 원인 및 예방 트러블슈팅 규칙 (Troubleshooting Checklist)**:
   - **인증 토큰 오염 방지 (`401 Bad credentials`)**:
     - `gh release` 명령 실행 전 세션에 무효한 토큰이 덮어씌워지지 않도록 반드시 `$env:GITHUB_TOKEN="" ; $env:GH_TOKEN=""`으로 셸 인증 환경변수를 리셋하고 실행합니다.
   - **로컬 APK 바이너리 생성 및 경로 검증**:
     - Gradle 빌드 후 APK 파일이 지정 경로로 정상 복사되었는지 `Test-Path`로 100% 검증 후 GitHub Release 탭에 업로드합니다. (`releases/` 폴더 내 파일은 절대 git push하지 않음)
   - **Draft(초안) 상태 즉시 해제 & 정식 Public 게시 (`Latest`)**:
     - CLI로 릴리즈 생성 후 `gh release edit v1.0.[커밋수] --draft=false`를 수행하여 공개 릴리즈(`Latest`) 상태로 전환하고, `gh release list`에서 `Draft`가 아닌 `Latest`로 표시되는지 확인합니다.
   - **실시간 웹 생존 검증 후 알림**:
     - `gh release view` 및 `read_url_content`로 해당 Release 페이지와 APK 다운로드 링크가 실제로 정상 구동(HTTP 200 / Live)되는지 확인한 후에만 사용자에게 알림을 전송합니다.

