import cv2
import numpy as np
from collections import deque

def nothing(x):
    pass

def getVal(name):
    return cv2.getTrackbarPos(str(name), "controls")

cv2.namedWindow("controls")
cv2.createTrackbar("0", "controls", 19, 30, nothing)
cv2.createTrackbar("1", "controls", 30, 50, nothing)
cv2.createTrackbar("2", "controls", 70, 255, nothing)
cv2.createTrackbar("3", "controls", 5, 20, nothing)
cv2.createTrackbar("4", "controls", 5, 20, nothing)

webCam = cv2.VideoCapture(1)
webCam.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc('M','J','P','G'))
webCam.set(cv2.CAP_PROP_FRAME_WIDTH, 3840)
webCam.set(cv2.CAP_PROP_FRAME_HEIGHT, 2160)

def show(title, img):
    scaled = cv2.resize(img, None, fx=0.3, fy=0.3, interpolation=cv2.INTER_AREA)
    cv2.imshow(title, scaled)


while True:
    _, bgr = webCam.read()
    show("frame", bgr)

    blurVal = getVal(0)
    if blurVal % 2 == 0:
        blurVal += 1

    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    blur = cv2.GaussianBlur(gray, (blurVal, blurVal), 0)

    val = getVal(1)
    kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (val, val))
    bh = cv2.morphologyEx(blur, cv2.MORPH_BLACKHAT, kernel)
    show("bh0", bh)

    bh = cv2.normalize(bh, None, 0, 255, cv2.NORM_MINMAX)
    _, bh = cv2.threshold(bh, getVal(2), 255, cv2.THRESH_BINARY)

    show("bh2", bh)



    res = cv2.waitKey(1)
    if res & 0xFF == ord('q'):
        break