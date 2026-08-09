cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_STANDARD 99)

project(blake3-kmp)

add_library(blake3-kmp SHARED
    ../../native/Blake3Kmp.c
    ../../../BLAKE3/c/blake3.c
    ../../../BLAKE3/c/blake3_dispatch.c
    ../../../BLAKE3/c/blake3_portable.c
    ../../../BLAKE3/c/blake3_neon.c
)

target_include_directories(blake3-kmp PRIVATE
    "../../../BLAKE3/c"
    "../../native/include/share"
    "../../native/include/unix"
)

target_compile_definitions(blake3-kmp PRIVATE BLAKE3_USE_NEON=1)

target_link_options(blake3-kmp PRIVATE "-Wl,-z,max-page-size=16384")
