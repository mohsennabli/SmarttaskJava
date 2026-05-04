#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SmartTask - Face Registration
-----------------------------
Capture a single face from the webcam and return a 128-d embedding.
Outputs ONLY a JSON object to stdout.
"""

import os
import sys
import io
import json
import time

# Suppress GUI and noisy logs BEFORE importing OpenCV / Qt
os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")
os.environ.setdefault("OPENCV_LOG_LEVEL", "SILENT")
os.environ.setdefault("QT_LOGGING_RULES", "*.debug=false;qt.qpa.*=false")

# Force stdout to UTF-8 FIRST to ensure clean JSON output
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

# Redirect stderr to /dev/null AFTER setting stdout to suppress warnings from native libraries
try:
    sys.stderr = open(os.devnull, 'w')
except Exception:
    pass

import cv2
import numpy as np
import face_recognition

MIN_FACE_SIZE = 80  # pixels
CAPTURE_SECONDS = 10


def output_json(payload):
    sys.stdout.write(json.dumps(payload, ensure_ascii=True))
    sys.stdout.flush()


def capture_best_face(duration_seconds):
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        return None, "ERROR: Impossible d'acceder a la webcam"

    # Warm up camera.
    for _ in range(8):
        cap.read()

    best_frame = None
    best_location = None
    best_area = 0

    start_time = time.time()
    try:
        while (time.time() - start_time) < duration_seconds:
            ret, frame = cap.read()
            if not ret:
                continue

            rgb = frame[:, :, ::-1]
            locations = face_recognition.face_locations(
                rgb,
                model="hog",
                number_of_times_to_upsample=0
            )

            filtered = []
            for top, right, bottom, left in locations:
                width = right - left
                height = bottom - top
                if width >= MIN_FACE_SIZE and height >= MIN_FACE_SIZE:
                    filtered.append((top, right, bottom, left))

            if len(filtered) == 1:
                top, right, bottom, left = filtered[0]
                area = (right - left) * (bottom - top)
                if area > best_area:
                    best_area = area
                    best_frame = frame.copy()
                    best_location = filtered[0]

        if best_frame is None:
            return None, "ERROR: Aucun visage unique detecte dans le temps imparti"

        # Re-detect face locations on the best frame directly
        rgb_best = best_frame[:, :, ::-1].copy()
        rgb_best = np.ascontiguousarray(rgb_best, dtype=np.uint8)

        # Detect face locations fresh (do NOT pass pre-computed locations to face_encodings)
        detected_locations = face_recognition.face_locations(rgb_best, model="hog")
        if not detected_locations:
            return None, "ERROR: Impossible de detecter le visage sur la meilleure frame"

        # Compute encoding without passing locations — let dlib handle landmarks internally
        encodings = face_recognition.face_encodings(rgb_best, known_face_locations=detected_locations)
        if not encodings:
            return None, "ERROR: Impossible d'extraire l'empreinte faciale"

        return encodings[0], None
    finally:
        cap.release()


def main():
    if len(sys.argv) < 2:
        output_json({
            "success": False,
            "message": "ERROR: iduser requis"
        })
        sys.exit(1)

    try:
        int(sys.argv[1])
    except ValueError:
        output_json({
            "success": False,
            "message": "ERROR: iduser invalide"
        })
        sys.exit(1)

    embedding, error = capture_best_face(CAPTURE_SECONDS)
    if error:
        output_json({
            "success": False,
            "message": error
        })
        sys.exit(1)

    output_json({
        "success": True,
        "embedding": [float(x) for x in embedding]
    })
    sys.exit(0)


if __name__ == "__main__":
    main()




