@echo off
cd /d "%~dp0.."
echo ATENCAO: este comando substitui os dados locais de demonstracao.
call mvn compile exec:java -Dexec.args="--reset-demo"
pause
