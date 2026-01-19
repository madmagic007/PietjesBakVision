import cv2
import numpy as np
from collections import deque

def getVal(name):
    return cv2.getTrackbarPos(str(name), "controls")

def nothing(x):
    pass

cv2.namedWindow("controls")
cv2.createTrackbar("0", "controls", 5, 30, nothing)
cv2.createTrackbar("1", "controls", 0, 255, nothing)
cv2.createTrackbar("2", "controls", 120, 255, nothing)

webCam = cv2.VideoCapture(1)
webCam.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc('M','J','P','G'))
webCam.set(cv2.CAP_PROP_FRAME_WIDTH, 3840)
webCam.set(cv2.CAP_PROP_FRAME_HEIGHT, 2160)

def show(title, img):
    scaled = cv2.resize(img, None, fx=0.25, fy=0.25, interpolation=cv2.INTER_AREA)
    cv2.imshow(title, scaled)

edge_buffer = deque(maxlen=10)

def tryOn(title, img):
    
    mask = cv2.normalize(img, None, getVal(1), 255, cv2.NORM_MINMAX)
    _, mask = cv2.threshold(mask, getVal(2), 255, cv2.THRESH_BINARY)

    edge_buffer.append(mask)
    accumulated = np.ones_like(mask) * 255
    for e in edge_buffer:
        accumulated = cv2.bitwise_and(accumulated, e)

    show("norm " + title, accumulated)



while True:
    _, frame = webCam.read()

    blurVal = getVal(0)
    if blurVal %2 == 0:
        blurVal += 1
    blur = cv2.GaussianBlur(frame, (blurVal, blurVal), 0)

    col = cv2.cvtColor(blur, cv2.COLOR_BGR2LAB)
    
    # tryOn("0", col[:, :, 0])
    # tryOn("1", col[:, :, 1])
    tryOn("2", col[:, :, 2])

    # show("gold",  mask)


    res = cv2.waitKey(1)
    if res & 0xFF == ord('q'):
        break

cv2.destroyAllWindows()