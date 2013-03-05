cmake_minimum_required(VERSION 2.4.6)
include($ENV{ROS_ROOT}/core/rosbuild/rosbuild.cmake)
 
# Append to CPACK_SOURCE_IGNORE_FILES a semicolon-separated list of
# directories (or patterns, but directories should suffice) that should
# be excluded from the distro. This is not the place to put things that
# should be ignored everywhere, like "build" directories; that happens in
# rosbuild/rosbuild.cmake. Here should be listed packages that aren't
# ready for inclusion in a distro.
#
# This list is combined with the list in rosbuild/rosbuild.cmake. Note
# that CMake 2.6 may be required to ensure that the two lists are combined
# properly. CMake 2.4 seems to have unpredictable scoping rules for such
# variables.
#list(APPEND CPACK_SOURCE_IGNORE_FILES /core/experimental)
rosbuild_make_distribution(0.1.0)
rosbuild_find_ros_stack(android_core)
set(android_gingerbread_mr1_path "${android_core_STACK_PATH}/android_gingerbread_mr1")
EXECUTE_PROCESS(COMMAND ${CMAKE_COMMAND} -E create_symlink
 ${android_gingerbread_mr1_path} "${PROJECT_SOURCE_DIR}/android_gingerbread_mr1"
)