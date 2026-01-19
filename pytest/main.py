import cv2
import numpy as np
from collections import deque

def nothing(x):
    pass

cv2.namedWindow("controls")
cv2.createTrackbar("0", "controls", 10, 30, nothing)
cv2.createTrackbar("1", "controls", 0, 255, nothing)
cv2.createTrackbar("2", "controls", 0, 255, nothing)
cv2.createTrackbar("3", "controls", 255, 255, nothing)
cv2.createTrackbar("4", "controls", 255, 255, nothing)
cv2.createTrackbar("5", "controls", 255, 255, nothing)
cv2.createTrackbar("6", "controls", 255, 255, nothing)
cv2.createTrackbar("7", "controls", 60, 255, nothing)
cv2.createTrackbar("8", "controls", 140, 255, nothing)
cv2.createTrackbar("9", "controls", 60, 255, nothing)
cv2.createTrackbar("10", "controls", 140, 255, nothing)

def getVal(name):
    return cv2.getTrackbarPos(str(name), "controls")

webCam = cv2.VideoCapture(1)
webCam.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc('M','J','P','G'))
webCam.set(cv2.CAP_PROP_FRAME_WIDTH, 3840)
webCam.set(cv2.CAP_PROP_FRAME_HEIGHT, 2160)

edge_buffer = deque(maxlen=10)

def show(title, img):
    scaled = cv2.resize(img, None, fx=0.25, fy=0.25, interpolation=cv2.INTER_AREA)
    cv2.imshow(title, scaled)

while True:
    _, frame = webCam.read()
    show("frame", frame)
    
    blurVal = getVal(0)
    if blurVal % 2 == 0:
        blurVal += 1

    blur = cv2.GaussianBlur(frame, (blurVal, blurVal), 0)

    hsv = cv2.cvtColor(blur, cv2.COLOR_BGR2HSV)
    lower_blue = np.array([getVal(1), getVal(2), getVal(3)])
    upper_blue = np.array([getVal(4), getVal(5), getVal(6)])
    mask = cv2.inRange(hsv, lower_blue, upper_blue)

    # kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (7, 7))
    # mask = cv2.dilate(mask, kernel, iterations=2)
    show("mask", mask)

    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    gray = cv2.GaussianBlur(gray, (15, 15), 0)
    edges = cv2.Canny(gray, getVal(7), getVal(8))
    show("edges", edges)

    col = cv2.cvtColor(blur, cv2.COLOR_BGR2HSV)
    col = col[:, :, 1]
    col = cv2.GaussianBlur(col, (9, 9), 0)
    col = cv2.normalize(col, None, getVal(9), 255, cv2.NORM_MINMAX)
    _, col = cv2.threshold(col, getVal(10), 255, cv2.THRESH_BINARY)
    show("lab2", col)


    band = cv2.bitwise_and(col, mask)
    show("and", band)

    res = cv2.waitKey(1)
    if res & 0xFF == ord('q'):
        break

cv2.destroyAllWindows()