//
// Created by vishal on 29/6/16.
//

#include "com_example_android_mediarecorder_MainActivity.h"

#include <android/log.h>
#include <opencv2/opencv.hpp>

#include <android/bitmap.h>
#include "feature.hpp"

#include "commonInternal.hpp"

Feature *feat;
string yamlFileName;

#define APPNAME "JNIVIDFEATURE"

JNIEXPORT void JNICALL Java_com_example_android_mediarecorder_MainActivity_callInit(JNIEnv *env, jobject thiz, jint width,
                                                                                       jint height, jfloat fps, jfloat windowSizeInSec,
                                                                                       jstring jYamlFilePath)
{
    LOGI("features init called");
    //call init
    if (feat) {
        delete feat;
        feat = NULL;
    }

    feat = new Feature();
    feat->init(height, width, fps, windowSizeInSec);

    //filename to save to
    const char *fnameptr = env->GetStringUTFChars(jYamlFilePath, NULL);

    string fileName(fnameptr);
    yamlFileName = fileName;
    LOGI("yaml file name set to: %s", fnameptr);

}

JNIEXPORT void JNICALL Java_com_example_android_mediarecorder_MainActivity_provideFrame(JNIEnv *env, jobject thiz,
                                                                                        jobject bitmap, jdouble timestamp)
{
    //__android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Scaning getString");
    int ret;
    AndroidBitmapInfo info;
    void* pixels = 0;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,
                            "jniVidFeature::_MainActivity_provideFrame::AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 )
    {       __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,
                                "jniVidFeature::_MainActivity_provideFrame::Bitmap format is not RGBA_8888!");
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,
                            "jniVidFeature::_MainActivity_provideFrame::AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }

    // init our output image
    cv::Mat mbgra(info.height, info.width, CV_8UC4, pixels);
    //Mat dst = scan(mbgra,x1,y1,x2,y2,x3,y3,x4,y4);

    // __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Image brightness: %f", meanPxl.val[1]);

    cv::Mat gray(info.height, info.width, CV_8UC1);
    cv::cvtColor(mbgra, gray, CV_BGRA2GRAY);

    if (feat)
        feat->provideFrame(gray, timestamp);

    AndroidBitmap_unlockPixels(env, bitmap);
    return ;
}


//function to write brightness values to yaml file --- input required jFilePath is String containing path to yaml file including
// name of file and extension .yaml where yaml file can be written
JNIEXPORT void JNICALL Java_com_example_android_mediarecorder_MainActivity_writeOutYAMLAndDeinit(JNIEnv *env, jobject thiz)
{
    if (feat)
    {
        feat->WriteToFile(yamlFileName);
        delete feat;
        feat = NULL;
    }

}





