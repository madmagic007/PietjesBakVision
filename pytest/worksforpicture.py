import cv2
import numpy as np
from sklearn import cluster

dicePic = cv2.imread("./dice2.jpg", cv2.IMREAD_COLOR)
img = cv2.resize(dicePic, None, fx=0.25, fy=0.25, interpolation=cv2.INTER_AREA)
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

def nothing(x):
    pass

cv2.namedWindow("controls")
cv2.createTrackbar("contour_blur", "controls", 9, 15, nothing)
cv2.createTrackbar("canny_low", "controls", 120, 255, nothing)
cv2.createTrackbar("canny_high", "controls", 60, 255, nothing)
cv2.createTrackbar("dist", "controls", 30, 200, nothing)
cv2.createTrackbar("circularity", "controls", 5, 10, nothing)
cv2.createTrackbar("min_area", "controls", 30, 200, nothing)
cv2.createTrackbar("max_area", "controls", 150, 500, nothing)
cv2.createTrackbar("render_contours", "controls", 1, 1, nothing)
cv2.createTrackbar("render_pips", "controls", 0, 1, nothing)
cv2.createTrackbar("render_area", "controls", 0, 1, nothing)
cv2.createTrackbar("render_circularity", "controls", 0, 1, nothing)
cv2.createTrackbar("render_dice_val", "controls", 0, 1, nothing)

def detect_pips_with_dice_contours(img_bgr, dice_contours, dist, circularity_lower, min_area, max_area, r_pips, r_area, r_circ, r_val):
    vis = img_bgr.copy()

    all_pip_centers = []

    for die in dice_contours:
        x, y, w, h = cv2.boundingRect(die)

        pad = int(0.1 * max(w, h))
        x0 = max(x - pad, 0)
        y0 = max(y - pad, 0)
        x1 = min(x + w + pad, img_bgr.shape[1])
        y1 = min(y + h + pad, img_bgr.shape[0])

        roi = img_bgr[y0:y1, x0:x1]

        # LAB B-channel for gold pips
        lab = cv2.cvtColor(roi, cv2.COLOR_BGR2LAB)
        b = lab[:, :, 2]

        b = cv2.GaussianBlur(b, (5, 5), 0)
        b = cv2.normalize(b, None, 0, 255, cv2.NORM_MINMAX)
        _, mask = cv2.threshold(b, 200, 255, cv2.THRESH_BINARY)

        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
        mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel)
        mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)

        contours, _ = cv2.findContours(
            mask,
            cv2.RETR_EXTERNAL,
            cv2.CHAIN_APPROX_SIMPLE
        )

        for c in contours:
            area = cv2.contourArea(c)
            if area < min_area or area > max_area:
                continue

            peri = cv2.arcLength(c, True) # noise / single point countour filtering
            if peri == 0:
                continue

            circularity = 4 * np.pi * area / (peri * peri)
            if circularity < circularity_lower:
                continue

            M = cv2.moments(c)
            if M["m00"] == 0:
                continue

            cx = int(M["m10"] / M["m00"]) + x0
            cy = int(M["m01"] / M["m00"]) + y0
            all_pip_centers.append((cx, cy))

            if r_circ:
                cv2.putText(
                    vis,
                    str(circularity),
                    (cx, cy - 12),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.9,
                    (255, 0, 0),
                    2
                )

            if r_area:
                cv2.putText(
                    vis,
                    str(area),
                    (cx, cy - 12),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.9,
                    (255, 0, 0),
                    2
                )

    if len(all_pip_centers) == 0:
        return [], vis

    # estimate pip spacing globally
    dists = []
    for i in range(len(all_pip_centers)):
        for j in range(i + 1, len(all_pip_centers)):
            dists.append(
                np.linalg.norm(
                    np.array(all_pip_centers[i]) - np.array(all_pip_centers[j])
                )
            )

    spacing = np.percentile(dists, 25)
    max_dist = spacing * dist # distance threshold

    clusters = []
    used = [False] * len(all_pip_centers)

    for i, p in enumerate(all_pip_centers):
        if used[i]:
            continue

        cluster = [p]
        used[i] = True
        stack = [i]

        while stack:
            idx = stack.pop()
            for j, q in enumerate(all_pip_centers):
                if used[j]:
                    continue
                if np.linalg.norm(
                    np.array(all_pip_centers[idx]) - np.array(q)
                ) < max_dist:
                    used[j] = True
                    stack.append(j)
                    cluster.append(q)

        clusters.append(cluster)

    results = []

    for cluster in clusters:
        cx = int(np.mean([p[0] for p in cluster]))
        cy = int(np.mean([p[1] for p in cluster]))

        if r_pips:
            for p in cluster:
                cv2.circle(vis, p, 4, (0, 0, 255), -1)

        if r_val:
            cv2.putText(
                vis,
                str(len(cluster)),
                (cx, cy - 12),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.9,
                (255, 0, 0),
                2
            )

        results.append({
            "pip_count": len(cluster),
            "center": (cx, cy),
            "pips": cluster
        })

    return results, vis

def get_dice_contours(img_blurred, low, high):
    edges = cv2.Canny(img_blurred, low, high)
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5))
    edges_thick = cv2.dilate(edges, kernel, iterations=2)
    
    edges_closed = cv2.morphologyEx(
        edges_thick,
        cv2.MORPH_CLOSE,
        kernel,
        iterations=2
    )

    filled = edges_closed.copy()
    h, w = filled.shape
    mask = np.zeros((h + 2, w + 2), np.uint8)

    cv2.floodFill(filled, mask, (0, 0), 255)
    filled = cv2.bitwise_not(filled)
    filled = cv2.bitwise_or(filled, edges_closed)

    contours, _ = cv2.findContours(
        filled,
        cv2.RETR_EXTERNAL,
        cv2.CHAIN_APPROX_SIMPLE
    )

    dice = []
    for c in contours:
        area = cv2.contourArea(c)
        if area < 2000 or area > 20000:
            continue
        dice.append(c)
    return dice

while True:
    cBlur = cv2.getTrackbarPos("contour_blur", "controls")
    low = cv2.getTrackbarPos("canny_low", "controls")
    high = cv2.getTrackbarPos("canny_high", "controls")
    dist = cv2.getTrackbarPos("dist", "controls")
    circ_val = cv2.getTrackbarPos("circularity", "controls")
    min_area = cv2.getTrackbarPos("min_area", "controls")
    max_area = cv2.getTrackbarPos("max_area", "controls")

    r_contours = cv2.getTrackbarPos("render_contours", "controls")
    r_pips = cv2.getTrackbarPos("render_pips", "controls")
    r_area = cv2.getTrackbarPos("render_area", "controls")
    r_circ = cv2.getTrackbarPos("render_circularity", "controls")
    r_val = cv2.getTrackbarPos("render_dice_val", "controls")

    if cBlur %2 == 0:
        cBlur += 1

    blur = cv2.GaussianBlur(gray, (cBlur, cBlur), 0)
    dice = get_dice_contours(blur, low, high)
    results, vis = detect_pips_with_dice_contours(img, dice, dist / 100, circ_val / 10, min_area, max_area, r_pips, r_area, r_circ, r_val)

    if r_contours == 1:
        cv2.drawContours(vis, dice, -1, (0, 0, 255), 2)
    
    cv2.imshow("dice", vis)


    res = cv2.waitKey(1)
    if res & 0xFF == ord('q'):
        break

cv2.destroyAllWindows()