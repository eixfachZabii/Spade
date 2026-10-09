"""Smoke tests: the model is real and knows the 52 cards; the pure helpers behave.

These do NOT measure card-reading accuracy (the gate says so in its "not proven" block).
"""
from pathlib import Path

import cv2
import numpy as np

from utils import get_n_cards, process_raw_image

MODEL = Path(__file__).resolve().parent.parent / "models" / "best_60_23.pt"
RANKS = ["2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"]
EXPECTED_LABELS = {rank + suit for rank in RANKS for suit in "CDHS"}


def test_model_file_is_real_not_an_lfs_pointer():
    assert MODEL.is_file(), f"{MODEL} is missing"
    assert MODEL.stat().st_size > 1_000_000, (
        f"{MODEL} is {MODEL.stat().st_size} bytes, so it is a Git LFS pointer: run `git lfs pull`"
    )


def test_model_knows_exactly_the_52_cards():
    from ultralytics import YOLO

    model = YOLO(str(MODEL))
    assert set(model.names.values()) == EXPECTED_LABELS


class _Box:
    def __init__(self, cls):
        self.cls = cls


class _Result:
    def __init__(self, classes):
        self.boxes = [_Box(c) for c in classes]


class _FakeModel:
    names = {0: "AS", 1: "10H", 2: "KD"}

    def __init__(self, classes):
        self._classes = classes

    def __call__(self, image):
        return [_Result(self._classes)]


def test_get_n_cards_dedupes_corner_detections_and_caps_at_n():
    # One physical card is usually detected twice, once per printed corner.
    model = _FakeModel([0, 0, 1, 1, 2])
    assert get_n_cards(model, image=None, n=2) == ["AS", "10H"]


def test_process_raw_image_round_trips_a_jpeg():
    image = np.zeros((8, 8, 3), dtype=np.uint8)
    image[:, :, 2] = 255
    ok, buffer = cv2.imencode(".jpg", image)
    assert ok
    assert process_raw_image(buffer.tobytes()).shape == (8, 8, 3)
