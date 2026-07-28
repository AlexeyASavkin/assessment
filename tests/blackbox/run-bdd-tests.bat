@echo off
cd /d "%~dp0..\.."
.\gradlew.bat -p tests\blackbox test
