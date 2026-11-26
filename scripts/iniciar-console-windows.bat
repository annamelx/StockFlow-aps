@echo off
cd /d "%~dp0.."
echo Iniciando o sistema APS Grupo 12 em modo console...
call mvn compile exec:java -Dexec.args="--console"
pause
