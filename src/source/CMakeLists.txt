cmake_minimum_required(VERSION 3.21)
project(SMTCBridge LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(SMTCBridge SHARED
    SMTCBridge.cpp
)

target_compile_definitions(SMTCBridge PRIVATE WIN32_LEAN_AND_MEAN)

target_link_libraries(SMTCBridge PRIVATE
    windowsapp
)

set_target_properties(SMTCBridge PROPERTIES
    OUTPUT_NAME "SMTCBridge"
    PREFIX ""
)
