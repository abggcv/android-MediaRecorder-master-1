//
// Created by vishal on 29/6/16.
//

#ifndef ANDROID_MEDIARECORDER_MASTER_1_COMMONINTERNAL_H
#define ANDROID_MEDIARECORDER_MASTER_1_COMMONINTERNAL_H

#include <jni.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include <android/bitmap.h>

#define LOG_TAG "KIBA-JNILAYER"
#define LOGI(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

jobject mat_to_bitmap(JNIEnv * env, cv::Mat & src, bool needPremultiplyAlpha, jobject bitmap_config);


#endif //ANDROID_MEDIARECORDER_MASTER_1_COMMONINTERNAL_H
