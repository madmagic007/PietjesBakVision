import cv2
import numpy as np
from collections import deque

def nothing(x):
    pass

def getVal(name):
    return cv2.getTrackbarPos(str(name), "controls")

cv2.namedWindow("controls")
cv2.createTrackbar("0", "controls", 10, 30, nothing)
cv2.createTrackbar("1", "controls", 15, 30, nothing)

webCam = cv2.VideoCapture(1)
webCam.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc('M','J','P','G'))
webCam.set(cv2.CAP_PROP_FRAME_WIDTH, 3840)
webCam.set(cv2.CAP_PROP_FRAME_HEIGHT, 2160)

def show(title, img, scale = 0.15):
    scaled = cv2.resize(img, None, fx=scale, fy=scale, interpolation=cv2.INTER_AREA)
    cv2.imshow(title, scaled)

def showGrid(imgs):
    row1 = cv2.hconcat(imgs[0:3])
    row2 = cv2.hconcat(imgs[3:6])
    row3 = cv2.hconcat(imgs[6:9])
    concat = cv2.vconcat([row1, row2, row3])

    show("combined", concat)

def onImg(img):
    blurVal = getVal(0)
    if blurVal % 2 == 0:
        blurVal += 1

    img = cv2.GaussianBlur(img, (blurVal, blurVal), 0)

    return img


def onCol(img):
    out = []
    for i in range(img.shape[2]):
        c = img[:, :, i]
        out.append(onImg(c))

    return out


while True:
    _, bgr = webCam.read()
    show("frame", bgr, .3)

    hsv = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)
    lab = cv2.cvtColor(bgr, cv2.COLOR_BGR2Lab)

    imgs = []
    imgs += onCol(bgr)
    imgs += onCol(hsv)
    imgs += onCol(lab)

    showGrid(imgs)

    res = cv2.waitKey(1)
    if res & 0xFF == ord('q'):
        break