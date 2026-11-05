@echo off
cd /d "%~dp0.."
echo Executando os testes oficiais com Maven e JUnit...
call mvn clean test
pause
