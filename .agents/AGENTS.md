# Project Rules for SoundLog

## Release & GitHub Workflow Rules

1. **개발 및 소스 수정 단계 (수동 배포 모드)**:
   - 사용자의 개별 수정/개선 요청 시에는 매번 버전업, 깃허브 푸시, 릴리즈를 수행하지 않고 소스코드 변경사항을 누적합니다.
   - **소소한 변경/개선 시에도 `CHANGELOG.md` 미확정 영역에 즉시 실시간 기록**: UI 수정, 자원 변경(mipmap, xml 등), 버그 수정, 설정 변경 등이 발생할 때마다 `CHANGELOG.md` 상단에 수정을 한 줄로 즉시 기록하여 누락을 방지합니다.

2. **코드 수정 후 테스트 빌드 제공 (Test Build)**:
   - 사용자의 개선/수정 요청에 따른 소스 변경 완료 후, 아래 절차로 테스트용 APK를 빌드하여 사용자에게 제공합니다:
     1. Gradle 디버그 빌드 실행: `.\gradlew assembleDebug`
     2. 빌드 결과물(`app/build/outputs/apk/debug/app-debug.apk`)을 `releases/test/` 폴더에 현재 시각 기반 파일명으로 복사:
        - 파일명 형식: `app_debug_YYMMDD_HHmm.apk` (예: `app_debug_260811_2009.apk`)
     3. `Test-Path`로 파일 생성 여부 100% 검증 후, 사용자에게 로컬 절대 경로(`releases/test/app_debug_YYMMDD_HHmm.apk`)를 안내합니다.
     4. 사용자가 스마트폰에 사이드로드하여 직접 테스트한 후, 최종 **"배포해줘"** 명령을 내릴 때까지 버전업/푸시/릴리즈 작업을 수행하지 않습니다.

3. **명시적 릴리즈/배포 요청 시 일괄 작업 (Batch Release)**:
   - 사용자가 "배포해줘", "릴리즈해줘", "버전 올려서 깃허브 등록해줘" 등 **명시적으로 릴리즈를 요청할 때에만** 아래 일련의 릴리즈 작업을 한 번에 수행합니다:
     1. **`git log [직전태그]..HEAD` 및 `git status / diff --stat` 전수검사 필수 실행**: 직전 릴리즈 이후 변경된 모든 커밋과 파일 변경 목록을 100% 교차 검증하여 `CHANGELOG.md` 및 `README.md`에 단 하나의 누락도 없이 포함시킵니다.
     2. 누적 변경사항 `git add .` 및 `git commit -m "..."` (`releases/` 폴더 내 산출물은 `.gitignore` 처리되어 Git 커밋/푸시 대상에서 제외)
     3. **동적 버전 산출 규칙 (`v[Major].[Minor].[커밋수]`)**:
        - 기본 버전 형식은 `v[Major].[Minor].[커밋수]` (예: 현재 `v1.0.[커밋수]`)를 따르며, 패치 버전 숫자는 현재 Git 커밋 수(`git rev-list --count HEAD`) 기반으로 자동 산출합니다.
        - 사용자가 Major/Minor 버전 상향을 명시할 경우(예: v1.1, v2.0 등), 지정된 버전 숫자를 기반으로 산출합니다 (예: `v1.1.[커밋수]`).
     4. **버전 연동 필수 갱신**:
        - 설정 화면 하단 버전 표시 (`SettingsScreen.kt`)를 이번 릴리즈 버전명(`v[Major].[Minor].[커밋수]`)으로 갱신
        - `README.md` 문서 내 최신 버전 및 릴리즈 내역 갱신
     5. APK 자동 빌드 후 로컬 `releases/SoundLog-v[Major].[Minor].[커밋수]-rel.apk`로 저장
     6. `git push origin main` 소스코드 푸시 (`releases/` 폴더 파일은 git에 푸시하지 않고 배제)
     7. 버전 태그(`v[Major].[Minor].[커밋수]`) 생성 및 `git push origin --tags` 푸시
     8. `CHANGELOG.md` 문서에 전수검사된 전체 개선 내역 정리
     9. GitHub Release 게시 (Release 제목/Title은 수식어 없이 앱이름과 버전명만 사용, 예: `--title "SoundLog v1.0.21"`) 및 **GitHub의 Release 탭에만 최종 빌드된 APK 파일 업로드**
     10. **릴리즈 링크 및 웹 페이지 생존 직접 검증**: `git push` 및 `gh release` 등록 후, 해당 릴리즈 페이지 URL 및 다운로드 링크가 실제로 정상 작동(HTTP 200 또는 `gh release view` 확인)하는지 최종 직접 확인한 후에만 사용자에게 릴리즈 링크, APK 다운로드 링크, Release Notes 전문을 알림 제공.


4. **릴리즈 문제 발생 원인 및 예방 트러블슈팅 규칙 (Troubleshooting Checklist)**:
   - **인증 토큰 오염 방지 (`401 Bad credentials`)**:
     - `gh release` 명령 실행 전 세션에 무효한 토큰이 덮어씌워지지 않도록 반드시 `$env:GITHUB_TOKEN="" ; $env:GH_TOKEN=""`으로 셸 인증 환경변수를 리셋하고 실행합니다.
   - **로컬 APK 바이너리 생성 및 경로 검증**:
     - Gradle 빌드 후 APK 파일이 지정 경로로 정상 복사되었는지 `Test-Path`로 100% 검증 후 GitHub Release 탭에 업로드합니다. (`releases/` 폴더 내 파일은 절대 git push하지 않음)
   - **Draft(초안) 상태 즉시 해제 & 정식 Public 게시 (`Latest`)**:
     - CLI로 릴리즈 생성 후 `gh release edit v1.0.[커밋수] --draft=false`를 수행하여 공개 릴리즈(`Latest`) 상태로 전환하고, `gh release list`에서 `Draft`가 아닌 `Latest`로 표시되는지 확인합니다.
   - **실시간 웹 생존 검증 후 알림**:
     - `gh release view` 및 `read_url_content`로 해당 Release 페이지와 APK 다운로드 링크가 실제로 정상 구동(HTTP 200 / Live)되는지 확인한 후에만 사용자에게 알림을 전송합니다.

## Markdown & Documentation Rules (GitHub Markdown Parser Optimization)

1. **GitHub Markdown 파서 웹 렌더링 최적화**:
   - `README.md` 및 `CHANGELOG.md` 등 GitHub 웹페이지 및 Release 탭에 게시되는 모든 마크다운 문서 작성 시, GitHub Markdown 파서의 렌더링 특성과 가독성을 최우선으로 고려합니다.

2. **강조 기호 및 따옴표 기호 중첩 금지**:
   - `**` 굵은 글씨 강조 태그 내부에 불필요한 백틱(``` ` ```)이나 작은따옴표(`'`)를 중첩하지 않습니다.
   - **잘못된 예**: `**'마이크 권한'**`, ``**`HTTP API Token`**``
   - **올바른 예**: `**마이크 권한**`, `**HTTP API Token**`

3. **한 줄 내 복수 굵은 글씨 및 특수기호 결합 파싱 무효화 방지**:
   - 한 문장/한 라인 내에서 `**A** ... **B**`와 같이 `**` 구문을 연발하거나, `**[설정] ➔ [접근성]**` 처럼 대괄호(`[` `]`) 및 특수기호(`➔`)가 `**`와 복잡하게 결합할 경우 GFM 파서가 단어 경계를 인식하지 못해 `**` 문자가 그대로 노출될 수 있습니다.
   - 문장 내 핵심 키워드 1개 위주로만 `**`를 지정하고, 경로 표현식 등은 `**설정 ➔ 접근성**`과 같이 깔끔한 단순 텍스트 결합 구성을 사용합니다.

4. **ASCII 텍스트 아키텍처 다이어그램 줄맞춤 준수**:
   - 마크다운 텍스트 다이어그램 작성 시, 다바이스/폰트에 따른 전각·반각 폭 차이로 인해 세로줄이 틀어지는 복잡한 오른쪽 루프선(`│`) 구성을 지양합니다.
   - 단방향 흐름 블록(`① -> ② -> ③`)과 핵심 메커니즘(예: Watchdog)을 하단에 별도 정돈된 블록으로 분리하여 모바일 및 깃허브 웹 렌더링 시 완벽한 가독성을 보장합니다.

