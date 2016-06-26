#ifndef FEATURE_H
#define FEATURE_H

#include <iostream>
#include <opencv2/opencv.hpp>
#include "projections.hpp"
//#include "readwrite.h"
using namespace cv;
using namespace std;
class Feature {
 public:
    void init(int height, int width, float fps, float windowSizeInSec);
    void provideFrame(Mat gray,double timeStamp);
    void WriteToFile(std::string fileName, vector<Point> vec);
 private:
 
    int currFrameCount;
    Mat prevFrame;
    Mat diffFrame;
    Mat morphFilter1;
    Mat morphFilter2;

};
#endif
