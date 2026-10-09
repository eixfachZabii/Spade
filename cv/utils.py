"""
This module contains all the utility functions needed by the main server
"""

import numpy as np
import cv2


def process_raw_image(image_data):
    """ Convert binary data to OpenCV image """
    nparr = np.frombuffer(image_data, np.uint8)
    return cv2.imdecode(nparr, cv2.IMREAD_COLOR)

def get_n_cards(model, image, n):
    """ Get first n predictions from a model on an image """
    results = model(image)

    # Extract unique class names
    unique_classes = []
    for r in results:
        for box in r.boxes:
            cls_name = model.names[int(box.cls)]
            if cls_name not in unique_classes:
                unique_classes.append(cls_name)

    return unique_classes[:n]

def get_comm_cards(model, num_cards):
    """ Read calibrated camera until num_cards are detected """
    return ["QS", "AS", "KS"]