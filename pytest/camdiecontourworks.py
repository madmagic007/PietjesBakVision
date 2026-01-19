import cv2
import numpy as np
from collections import deque
import time

def nothing(x):
    pass

cv2.namedWindow("controls")
cv2.createTrackbar("1", "controls", 14, 255, nothing)
cv2.createTrackbar("2", "controls", 60, 255, nothing)
cv2.createTrackbar("3", "controls", 120, 255, nothing)
cv2.createTrackbar("4", "controls", 60, 255, nothing)

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
    t1 = time.time()

    _, frame = webCam.read()
    blur = cv2.GaussianBlur(frame, (31, 31), 0)

    hsv = cv2.cvtColor(blur, cv2.COLOR_BGR2HSV)
    lower_blue = np.array([getVal(1), 0, 0])
    upper_blue = np.array([getVal(2), 255, 255])
    mask = cv2.inRange(hsv, lower_blue, upper_blue)

    edges = cv2.Canny(mask, 0, 0)

    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5))
    edges_closed = cv2.morphologyEx(
        edges,
        cv2.MORPH_CLOSE,
        kernel,
        iterations=1
    )

    norm = cv2.distanceTransform(edges_closed, cv2.DIST_L2, 5)


    norm = cv2.morphologyEx(
        norm,
        cv2.MORPH_CLOSE,
        kernel,
        iterations=1
    )


    show("", edges)
    show("closed", norm)

    print((time.time() - t1) * 1000)

    # kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5))
    # edges_thick = cv2.dilate(edges, kernel, iterations=5)
    
    # edges_closed = cv2.morphologyEx(
    #     edges_thick,
    #     cv2.MORPH_CLOSE,
    #     kernel,
    #     iterations=5
    # )

    # edge_buffer.append(edges_closed)
    # accumulated = np.ones_like(edges_closed) * 255
    # for e in edge_buffer:
    #     accumulated = cv2.bitwise_and(accumulated, e)

    # contours, _ = cv2.findContours(accumulated, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    # filled = np.zeros_like(accumulated)
    # cv2.drawContours(filled, contours, -1, 255, thickness=cv2.FILLED)
    
    # kernel1 = np.ones((5, 5), np.uint8)
    # padded = cv2.dilate(filled, kernel1, iterations=1)

    # dice, _ = cv2.findContours(
    #     filled,
    #     cv2.RETR_EXTERNAL,
    #     cv2.CHAIN_APPROX_SIMPLE
    # )

    # vis = frame.copy()
    # cv2.drawContours(vis, dice, -1, (0, 0, 255), 2)


    res = cv2.waitKey(1)
    if res & 0xFF == ord('q'):
        break

cv2.destroyAllWindows()