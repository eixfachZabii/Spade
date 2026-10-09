"""
This is the main script responsible for starting the flask server and providing the API endpoints
with HTTPS support for secure WebSocket connections.
"""

import os
from pathlib import Path

import cv2
from flask import Flask
from flask_socketio import SocketIO
from flask_cors import CORS

from ultralytics import YOLO

from camera import Camera
from utils import get_comm_cards, get_n_cards, process_raw_image

MODEL_PATH = str(Path(__file__).resolve().parent / "models" / "best_60_23.pt")
CAM_INDEX = 0

# Certificate paths
CERT_FILE = os.environ.get('CERT_FILE', './certificates/cert.pem')
KEY_FILE = os.environ.get('KEY_FILE', './certificates/key.pem')

# Init Flask app and socketIO
app = Flask(__name__)
# Remove SSL config from SocketIO init - we'll apply it only in socketio.run()
socketio = SocketIO(
    app,
    cors_allowed_origins="*",
    async_mode='eventlet'  # use eventlet for better websocket support
)
CORS(app)

# Init AI model
model = YOLO(MODEL_PATH)

# Init Camera
camera = None


# --- Endpoints ---
@socketio.on("connect")
def handle_connect():
    print('Client connected')

@socketio.on("disconnect")
def handle_disconnect():
    print('Client disconnected')

@socketio.on('frame')
def handle_frame(data):
    """
    Gets a frame from one of the users and checks if cards are detected
    :param data: dict with keys "n" for amount of cards "needed" and "image" with the raw image data
    :return: dict with predictions and boolean value saying if amount of cards were found
    """
    num_cards = data["n"]
    image_raw = data["image"]
    image = process_raw_image(image_raw)
    preds = get_n_cards(model, image, num_cards)

    return {
        'predictions': preds,
        'found': len(preds) >= num_cards,
    }

@socketio.on('comm_cards')
def handle_comm_cards(data):
    """
    gets the community cards from the webcam and returns them
    :param data: dict with key n
    :return: dict with predictions and boolean value saying if amount of cards were found
    """
    num_cards = data["n"]

    comm_cards = get_comm_cards(model, num_cards)

    return {
        'predictions': comm_cards,
        'found': len(comm_cards) >= num_cards,
    }


@socketio.on('getFrame')
def handle_get_frame(data):
    """
    Gets the frame from the webcam on top of the table
    :return: buffer image in a format compatible with the frontend
    """
    # Get the frame from the camera
    try:
        frame = camera.getFrame()
    except Exception as e:
        return {'success': False, 'message': str(e)}

    # Check if the frame is valid
    if frame is None or frame.size == 0:
        return {'success': False, 'message': 'Failed to capture frame'}

    # Convert the OpenCV frame to JPEG format
    ret, buffer = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 85])

    if not ret:
        return {'success': False, 'message': 'Failed to encode image'}

    # Convert to bytes for transmission
    frame_bytes = buffer.tobytes()

    # Return the image data
    return {'success': True, 'image': frame_bytes}

@socketio.on('recalibrate')
def recalibrate(data):
    try:
        camera.calibrate()
    except Exception as e:
        return {'success': False, 'message': str(e)}
    return {'success': True}

# Run the app
if __name__ == '__main__':
    camera = Camera()
    # Start the socketio server with SSL
    socketio.run(
        app,
        debug=True,
        host='0.0.0.0',
        port=5001,
    )
