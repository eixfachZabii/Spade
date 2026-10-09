"""
Handles the camera on top of the table, especially calibration of the frame
and saving the community cards
"""
import time

import cv2
import numpy as np


class Camera:
    def __init__(self, cam_index: int = 0, padding = 0.05):
        self.cam = cv2.VideoCapture(cam_index)
        if not self.cam.isOpened():
            raise ValueError(f"Unable to open camera at index {cam_index}")
        self.padding = padding

        # Get the default frame width and height
        frame_width = int(self.cam.get(cv2.CAP_PROP_FRAME_WIDTH))
        frame_height = int(self.cam.get(cv2.CAP_PROP_FRAME_HEIGHT))
        print(frame_width, frame_height)
        self.calibration: np.ndarray = np.array([0, 0, frame_width, frame_height])
        self.spades = []

    def getFrame(self):
        """
        Gets the cropped frame
        """
        ret, frame = self.cam.read()
        x_min, y_min, x_max, y_max = self.calibration
        for x, y, w, h in self.spades:
            cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 0, 255), 3)  # Red border for spades
            cv2.putText(frame, "Spade", (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255),
                        2)  # Nametag for spades
        cropped_image = frame[y_min:y_max, x_min:x_max]

        return cropped_image

    def calibrate(self):
        spades_detected = False
        while not spades_detected:
            ret, image = self.cam.read()
            # Detect regions of interest
            spades, others, mask_visualization = self.detect_regions(image)

            if len(spades) < 2:
                print("No spades detected. Trying calibration...")
                time.sleep(1/20)
                continue
            else:
                spades_detected = True


            # Sort spades by x-coordinate to determine the left and right spades
            spades = sorted(spades, key=lambda x: x[0])
            left_spade = spades[0]
            right_spade = spades[-1]
            self.spades = spades

            # Define the bounding box from the left spade to the right spade
            x_min, y_min, x_max, y_max = self.refine_bounding_box(image, [left_spade, right_spade], self.padding)

            self.calibration = np.array([x_min, y_min, x_max, y_max])

    @staticmethod
    def refine_bounding_box(image, coordinates, padding=0.05):
        """
        Refine the bounding box using provided coordinates of the spades or target regions.
        :param image: Original image.
        :param coordinates: List of coordinates (x, y, w, h) for detected objects (spades, etc.).
        :param padding: Padding percentage to add around the bounding box.
        :return: Refined bounding box (x_min, y_min, x_max, y_max).
        """
        if len(coordinates) == 0:
            return 0, 0, image.shape[1], image.shape[0]  # If no objects, return the full image

        # Initialize bounds
        x_min, y_min, x_max, y_max = image.shape[1], image.shape[0], 0, 0

        # Calculate the bounding box around all provided coordinates
        for x, y, w, h in coordinates:
            x_min = min(x_min, x)
            y_min = min(y_min, y)
            x_max = max(x_max, x + w)
            y_max = max(y_max, y + h)

        # Calculate padding based on image size
        padding_x = int(padding * image.shape[1])
        padding_y = int(padding * image.shape[0])

        # Apply padding and ensure bounds are within image dimensions
        x_min = max(0, x_min - padding_x)
        y_min = max(0, y_min - padding_y)
        x_max = min(image.shape[1], x_max + padding_x)
        y_max = min(image.shape[0], y_max + padding_y)

        return x_min, y_min, x_max, y_max

    @staticmethod
    def detect_regions(image):
        """
        Detect the coordinates of the two red spades and yellow rectangles with yellow outlines.
        :param image: Input image.
        :return: Detected coordinates, visualization mask.
        #TODO PLAY AROUND HERE !!!!!!!
        #TODO PLAY AROUND HERE !!!!!!!
        #TODO PLAY AROUND HERE !!!!!!!
        #TODO PLAY AROUND HERE !!!!!!!
        """
        # Convert the image to HSV color space
        hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)

        # Detect red regions (spades)
        red_lower1 = np.array([0, 120, 70])
        red_upper1 = np.array([10, 255, 255])
        red_lower2 = np.array([170, 120, 70])
        red_upper2 = np.array([180, 255, 255])
        mask_red = cv2.inRange(hsv, red_lower1, red_upper1) + cv2.inRange(hsv, red_lower2, red_upper2)

        # Detect yellow regions (rectangles' outlines)
        yellow_lower = np.array([20, 100, 100])
        yellow_upper = np.array([30, 255, 255])
        mask_yellow = cv2.inRange(hsv, yellow_lower, yellow_upper)

        # Perform edge detection (Canny) to detect outlines
        edges_yellow = cv2.Canny(mask_yellow, 100, 200)  # TODO PLAY AROUND HERE !!!!!!

        # Find contours for both red spades and yellow outlines
        contours_red, _ = cv2.findContours(mask_red, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        contours_yellow, _ = cv2.findContours(edges_yellow, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        spades = []
        rectangles = []

        # Process red contours (spades)
        for contour in contours_red:
            x, y, w, h = cv2.boundingRect(contour)
            aspect_ratio = float(w) / h

            # Get the region of interest (ROI) to check the mean color
            roi = image[y:y + h, x:x + w]
            mean_color = np.mean(roi, axis=(0, 1))  # Calculate mean color for the region (in BGR)

            # Detect spades (red)
            if 0.3 < aspect_ratio < 3 and w > 10 and h > 10:  # Adjusted thresholds for more generous detection
                if Camera.is_color_in_range(mean_color, red_lower1, red_upper1) or Camera.is_color_in_range(mean_color, red_lower2,
                                                                                              red_upper2):
                    spades.append((x, y, w, h))

        # Process yellow contours (rectangles with yellow outline)
        for contour in contours_yellow:
            x, y, w, h = cv2.boundingRect(contour)
            aspect_ratio = float(w) / h

            # Rectangles should be large and rectangular in shape
            if w > 20 and h > 20 and h < 50 and w < 50:  # TODO PLAY AROUND HERE !!!!!!
                rectangles.append((x, y, w, h))

        # Create a visualization of the mask
        mask_visualization = np.zeros_like(image)
        mask_visualization[:, :, 2] = mask_red  # Red areas highlighted in red
        mask_visualization[:, :, 1] = mask_yellow  # Yellow areas highlighted in green

        return spades, rectangles, mask_visualization

    @staticmethod
    def is_color_in_range(mean_color, lower_range, upper_range):
        """
        Check if the mean color is within the specified HSV color range.
        :param mean_color: The mean color to check (BGR format).
        :param lower_range: The lower bound of the color range (HSV format).
        :param upper_range: The upper bound of the color range (HSV format).
        :return: True if the mean color is within the range, otherwise False.
        """
        # Convert BGR color to HSV
        mean_color_hsv = cv2.cvtColor(np.uint8([[mean_color]]), cv2.COLOR_BGR2HSV)[0][0]

        # Check if the color is in range
        return cv2.inRange(np.uint8([[mean_color_hsv]]), lower_range, upper_range)[0][0] > 0

    def __del__(self):
        self.cam.release()

if __name__ == '__main__':
    camera = Camera()
    camera.calibrate()
    print(camera.calibration)
    while True:
        camera.calibrate()
        frame = camera.getFrame()
        if frame is None:
            print("Frame is none lol")
        cv2.imshow('Camera', frame)
        if cv2.waitKey(1) == ord('q'):
            break
    cv2.destroyAllWindows()