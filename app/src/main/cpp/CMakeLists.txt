# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
project("qemu_bridge")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
add_library( # Sets the name of the library.
             qemu-bridge

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             qemu-bridge.cpp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.
find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
target_link_libraries( # Specifies the target library.
                       qemu-bridge

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )

# Note: To fully compile the real QEMU emulator, you would add the following:
# include_directories(qemu_source_dir/include)
# target_link_libraries(qemu-bridge libqemu-system-aarch64.a glib-2.0 pcre ...)
