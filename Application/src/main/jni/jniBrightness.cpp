#include "com_example_android_mediarecorder_MainActivity.h"

#include <android/log.h>
#include <opencv2/opencv.hpp>


#include "commonInternal.hpp"

#define APPNAME "JNIBRIGHTNESS"

using namespace cv;

int frameCount = 0;
vector<double> brightnessValues;
vector<Point> framesTimestamps;

JNIEXPORT jobject JNICALL Java_com_example_android_mediarecorder_MainActivity_computeBrightness(JNIEnv *env, jobject thiz,
                                                                                                jobject bitmap, jdouble timestamp)
{
    //__android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Scaning getString");
    int ret;
    AndroidBitmapInfo info;
    void* pixels = 0;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"AndroidBitmap_getInfo() failed ! error=%d", ret);
        return NULL;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 )
    {       __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"Bitmap format is not RGBA_8888!");
        return NULL;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME,"AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }

    // init our output image
    Mat mbgra(info.height, info.width, CV_8UC4, pixels);
    //Mat dst = scan(mbgra,x1,y1,x2,y2,x3,y3,x4,y4);

    //compute brightness
    //find mean pixel values for all channels separately
    Scalar meanPxl = mean(mbgra);

    brightnessValues.push_back(meanPxl.val[1]);

    LOGI("Brightness value for frame: %d", frameCount);
    LOGI("Image brightness: %f", meanPxl.val[1]);
    LOGI("brightness value from vector: %f", brightnessValues[frameCount]);

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Image brightness: %f", meanPxl.val[1]);

    //get source bitmap's config
    jclass java_bitmap_class = (jclass)env->FindClass("android/graphics/Bitmap");
    jmethodID mid = env->GetMethodID(java_bitmap_class, "getConfig", "()Landroid/graphics/Bitmap$Config;");
    jobject bitmap_config = env->CallObjectMethod(bitmap, mid);
    jobject _bitmap = mat_to_bitmap(env,mbgra,false,bitmap_config);

    Mat gray(info.height, info.width, CV_8UC1);
    cvtColor(mbgra, gray, CV_BGRA2GRAY);

    framesTimestamps.push_back(Point(frameCount, timestamp));

    frameCount++;


    AndroidBitmap_unlockPixels(env, bitmap);
    return _bitmap;
}


//function to write brightness values to yaml file --- input required jFilePath is String containing path to yaml file including
// name of file and extension .yaml where yaml file can be written
JNIEXPORT void JNICALL Java_com_example_android_mediarecorder_MainActivity_WriteBrightnessToYAML(JNIEnv* env, jobject thiz,
                                                                                                 jstring jFilePath)
{
    //filename to save to
    const char *fnameptr = env->GetStringUTFChars(jFilePath, NULL);

    string yamlFileName(fnameptr);

    if(!brightnessValues.empty())
    {
        FileStorage fs(yamlFileName, FileStorage::WRITE);

        fs << "brightnessValues" << brightnessValues;
        fs.release();

        LOGD("Brightness values written to yaml file: %s", fnameptr);
        //LOGD(yamlFileName);
    }
    else
        LOGI("No brightness values found to write to yaml file");

    //empty brightness values
    brightnessValues.clear();
    frameCount = 0;
    //release char pointer
    env->ReleaseStringUTFChars(jFilePath, fnameptr);
}

