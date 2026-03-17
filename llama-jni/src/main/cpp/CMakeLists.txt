cmake_minimum_required(VERSION 3.22.1)
project(llama_jni)

set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Build tests" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Build examples" FORCE)
set(LLAMA_ANDROID ON CACHE BOOL "Android build" FORCE)

add_compile_options(-march=armv8-a+fp+simd)

include(FetchContent)
FetchContent_Declare(
    llama
    GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
    GIT_TAG        master
)
FetchContent_MakeAvailable(llama)

add_library(llama_jni SHARED llama_jni.cpp)

target_link_libraries(llama_jni llama android log m)
