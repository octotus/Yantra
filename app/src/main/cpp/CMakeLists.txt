cmake_minimum_required(VERSION 3.22.1)

project(yantra_ephemeris)

set(SWISSEPH_DIR ${CMAKE_SOURCE_DIR}/../../../../third_party/swisseph)

add_library(
    yantra_ephemeris
    SHARED
    yantra_ephemeris.c
    ${SWISSEPH_DIR}/sweph.c
    ${SWISSEPH_DIR}/swephlib.c
    ${SWISSEPH_DIR}/swedate.c
    ${SWISSEPH_DIR}/swehouse.c
    ${SWISSEPH_DIR}/swemplan.c
    ${SWISSEPH_DIR}/swemmoon.c
    ${SWISSEPH_DIR}/swejpl.c
)

target_include_directories(
    yantra_ephemeris
    PRIVATE
    ${SWISSEPH_DIR}
)

target_compile_definitions(yantra_ephemeris PRIVATE NO_SIGNAL)

find_library(log-lib log)
target_link_libraries(yantra_ephemeris ${log-lib} m)
