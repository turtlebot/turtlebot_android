##############################################################################
# CMake
##############################################################################

cmake_minimum_required(VERSION 2.8.3)
project(turtlebot_android_core_components)

##############################################################################
# Catkin
##############################################################################

find_package(catkin REQUIRED rosjava_tools)
catkin_android_setup(debug)
catkin_package()
