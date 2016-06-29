#include "feature.hpp"

void Feature::init(int height, int width, float fps, float windowSizeInSec)
{
    currFrameCount = 0;
}

void Feature::provideFrame(Mat gray, double currTimeStamp)
{
    if(gray.rows == 0)
        return;

    currFrameCount++;

    if(currFrameCount > 1)
    {
        absdiff(gray,prevFrame,diffFrame);
        threshold(diffFrame,diffFrame,10,255,THRESH_BINARY);

        dilate(diffFrame,diffFrame, morphFilter1);
        erode(diffFrame, diffFrame, morphFilter1);
        erode(diffFrame, diffFrame, morphFilter2);
        dilate(diffFrame,diffFrame, morphFilter2);


    }
    prevFrame = gray.clone();

    return;
}

void Feature::WriteToFile(std::string fileName)
{
    FileStorage fs;
    fs.open(fileName, FileStorage::WRITE);
    
    cv::Mat temp = cv::Mat::zeros(640,360, CV_8UC1);

    /*
    //fs << "temp" << temp;
    fs << "frameCountTimestamps" << "["; //frameCountTimestamps;

    for(int i = 0; i < frameCountTimestamps.size(); i++)
        fs << "{ frameCount: " << frameCountTimestamps[i].x << ", timestamps: " << frameCountTimestamps[i].y << "}";

    fs << "]";
    */

    fs.release();
}
