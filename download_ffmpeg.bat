@echo off
set "OUT=desktop\app\src\main\resources\ffmpeg.exe"
if exist "%OUT%" (
    echo ffmpeg.exe already exists
    exit /b 0
)
echo Downloading ffmpeg (55MB)...
powershell -Command "Invoke-WebRequest -Uri 'https://github.com/GyanD/codexffmpeg/releases/download/7.1/ffmpeg-7.1-essentials_build.zip' -OutFile '%TEMP%\ffmpeg.zip'"
echo Extracting...
powershell -Command "Expand-Archive -Path '%TEMP%\ffmpeg.zip' -DestinationPath '%TEMP%\ffmpeg_extracted' -Force"
for /r "%TEMP%\ffmpeg_extracted" %%f in (ffmpeg.exe) do copy "%%f" "%OUT%"
del "%TEMP%\ffmpeg.zip"
rmdir /s /q "%TEMP%\ffmpeg_extracted"
echo ffmpeg.exe ready at %OUT%
