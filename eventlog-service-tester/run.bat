@echo off

set launcher=%TMP%\_launcher.bat

if exist "%launcher%" (
	del /q /f "%launcher%"
)

echo.@echo off>>"%launcher%"
echo.ConsoleSize 200 20 200 9999>>"%launcher%"
echo.cd build\install\eventlog-service-tester\bin>>"%launcher%"
echo.call eventlog-service-tester.bat>>"%launcher%"

start cmd /c "%launcher%"
