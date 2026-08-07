# Project Rules for SoundLog

## Release & GitHub Workflow Rules

1. **개발 및 소스 수정 단계 (수동 배포 모드)**:
   - 사용자의 개별 수정/개선 요청 시에는 매번 버전업, 빌드, 깃허브 푸시, 릴리즈를 수행하지 않고 소스코드 변경사항을 누적합니다.

2. **명시적 릴리즈/배포 요청 시 일괄 작업 (Batch Release)**:
   - 사용자가 "배포해줘", "릴리즈해줘", "버전 올려서 깃허브 등록해줘" 등 **명시적으로 릴리즈를 요청할 때에만** 아래 일련의 릴리즈 작업을 한 번에 수행합니다:
     1. 누적 변경사항 `git add .` 및 `git commit -m "..."`
     2. 현재 Git 커밋 수(`git rev-list --count HEAD`) 기반으로 버전 산출 (`v1.0.[커밋수]`)
     3. APK 자동 빌드 (`SoundLog-v1.0.[커밋수]-rel.apk`)
     4. `git push origin main` 소스코드 푸시
     5. 버전 태그(`v1.0.[커밋수]`) 생성 및 `git push origin --tags` 푸시
     6. `CHANGELOG.md` 문서에 묶인 마이너 개선 내역 한눈에 정리
     7. GitHub Release 게시 및 `SoundLog-v1.0.[커밋수]-rel.apk` 바이너리 업로드
     8. 사용자에게 릴리즈 링크, APK 직접 다운로드 링크, Release Notes 전문 제공
