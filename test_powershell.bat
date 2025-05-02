@echo off
powershell -Command "$message = @\"
제목: PowerShell 변수 방식 테스트

PowerShell 내장 변수와 Here-String을 활용한 커밋 메시지 작성 테스트입니다.
- 멀티라인 문자열 지원
- 마크다운 포맷 유지
- 특수문자 처리 개선

테스트 목적으로만 작성된 커밋입니다.
\"@; $message | Out-File -FilePath 'ps_commit_msg.txt' -Encoding utf8; Write-Host '커밋 메시지 파일이 생성되었습니다.'"
echo 파일이 생성되었습니다. git commit -F ps_commit_msg.txt 명령으로 커밋할 수 있습니다. 