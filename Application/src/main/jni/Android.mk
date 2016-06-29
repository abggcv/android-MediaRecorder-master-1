LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
include $(LOCAL_PATH)/../../../../openCVLibrary310/src/main/jni/OpenCV.mk

LOCAL_MODULE    := Scanner
LOCAL_SRC_FILES := jniVidFeature.cpp jniBrightness.cpp feature.cpp projections.cpp commonInternal.cpp
LOCAL_LDLIBS    += -lm -llog -landroid
LOCAL_LDFLAGS += -ljnigraphics
LOCAL_C_INCLUDES += $(LOCAL_PATH)
include $(BUILD_SHARED_LIBRARY)