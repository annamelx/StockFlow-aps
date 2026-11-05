@echo off
cd /d "%~dp0.."
echo Iniciando o sistema APS Grupo 12...
call mvn compile exec:java
pause
