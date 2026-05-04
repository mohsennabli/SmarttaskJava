#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Tahwissa - Face Recognition Service
===================================
Capture un visage via webcam, calcule un embedding (128D) et compare deux embeddings.
"""

import os
# Suppress GUI and noisy logs BEFORE importing OpenCV / Qt
import sys
os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")
os.environ.setdefault("OPENCV_LOG_LEVEL", "SILENT")
os.environ.setdefault("QT_LOGGING_RULES", "*.debug=false;qt.qpa.*=false")

import json
import io
import time
from datetime import datetime

# Force stdout to UTF-8 FIRST to ensure clean JSON output
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

# Redirect stderr to /dev/null AFTER setting stdout to suppress warnings from native libraries
try:
    sys.stderr = open(os.devnull, 'w')
except Exception:
    pass

import cv2
import numpy as np
import face_recognition


def _result(success, message, embedding=None, distance=None):
    payload = {
        "success": bool(success),
        "message": message,
        "timestamp": datetime.now().isoformat(),
        "embedding": embedding,
        "distance": distance,
    }
    print(json.dumps(payload, ensure_ascii=True))


def _capture_embedding(duration=10):
    camera = cv2.VideoCapture(0)
    if not camera.isOpened():
        return None, "Impossible d'acceder a la webcam"

    for _ in range(10):
        camera.read()

    start_time = time.time()
    best_embedding = None
    best_frame = None

    try:
        while (time.time() - start_time) < duration:
            ret, frame = camera.read()
            if not ret:
                continue

            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            face_locations = face_recognition.face_locations(rgb, model="hog")
            face_count = len(face_locations)

            if face_count == 1:
                encodings = face_recognition.face_encodings(rgb, face_locations)
                if encodings:
                    best_embedding = encodings[0]
                    best_frame = frame.copy()

        if best_embedding is not None and best_frame is not None:
            return best_embedding, "Visage detecte automatiquement"

        return None, "Aucun visage valide detecte"
    finally:
        camera.release()


def _load_embedding(path):
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    return np.array(data, dtype=np.float64)


def main():
    if len(sys.argv) < 2:
        _result(False, "Usage: python face_recognition_service.py <enroll|verify> [args]")
        sys.exit(1)

    mode = sys.argv[1].lower()

    if mode == "enroll":
        duration = int(sys.argv[2]) if len(sys.argv) > 2 else 10
        embedding, message = _capture_embedding(duration)
        if embedding is None:
            _result(False, message)
            sys.exit(1)
        _result(True, message, embedding=embedding.tolist(), distance=None)
        sys.exit(0)

    if mode == "verify":
        if len(sys.argv) < 3:
            _result(False, "Chemin d'embedding requis")
            sys.exit(1)
        stored_path = sys.argv[2]
        duration = int(sys.argv[3]) if len(sys.argv) > 3 else 10
        threshold = float(sys.argv[4]) if len(sys.argv) > 4 else 0.55

        stored_embedding = _load_embedding(stored_path)
        live_embedding, message = _capture_embedding(duration)
        if live_embedding is None:
            _result(False, message)
            sys.exit(1)

        distance = float(face_recognition.face_distance([stored_embedding], live_embedding)[0])
        success = distance <= threshold
        msg = "Visage reconnu" if success else "Visage non reconnu"
        _result(success, msg, embedding=None, distance=distance)
        sys.exit(0 if success else 1)

    _result(False, "Mode inconnu. Utilisez 'enroll' ou 'verify'")
    sys.exit(1)


if __name__ == "__main__":
    main()

