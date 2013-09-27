cmake_minimum_required(VERSION 2.8.3)
project(turtlebot_android)

find_package(catkin REQUIRED rosjava_build_tools)
####find_package(catkin REQUIRED uploadArchives)

catkin_android_setup(assemble uploadArchives)

catkin_package()

