@echo off

cmake -S . -B build-x86-64 -A x64
cmake --build build-x86-64 --config Release

cmake -S . -B build-x86 -A Win32
cmake --build build-x86 --config Release

cmake -S . -B build-arm64 -A ARM64
cmake --build build-arm64 --config Release

pause