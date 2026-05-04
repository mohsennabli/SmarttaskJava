#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Tahwissa - Face Recognition Service
===================================
Capture un visage via webcam, calcule un embedding (128D) et compare deux embeddings.
"""

import json
import sys
import io
import time
from datetime import datetime

import cv2
import numpy as np
import face_recognition

# ── Force stdout to UTF-8 on Windows (prevents cp1252 → PHP malformed UTF-8) ──
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')


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

    window_name = "Tahwissa - Face Enrollment (ESPACE: capturer, ESC: annuler)"
    cv2.namedWindow(window_name, cv2.WINDOW_NORMAL)
    cv2.resizeWindow(window_name, 800, 600)

    try:
        while (time.time() - start_time) < duration:
            ret, frame = camera.read()
            if not ret:
                continue

            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            face_locations = face_recognition.face_locations(rgb, model="hog")
            face_count = len(face_locations)

            status = "Aucun visage detecte"
            color = (0, 0, 255)

            if face_count == 1:
                status = "Visage detecte - Appuyez sur ESPACE"
                color = (0, 255, 0)

                encodings = face_recognition.face_encodings(rgb, face_locations)
                if encodings:
                    best_embedding = encodings[0]
                    best_frame = frame.copy()
            elif face_count > 1:
                status = f"{face_count} visages detectes - Une seule personne requise"
                color = (0, 165, 255)

            for (top, right, bottom, left) in face_locations:
                cv2.rectangle(frame, (left, top), (right, bottom), color, 2)

            elapsed = int(time.time() - start_time)
            remaining = max(duration - elapsed, 0)
            cv2.putText(frame, f"Temps restant: {remaining}s", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
            cv2.putText(frame, status, (10, 60),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)
            cv2.putText(frame, "ESPACE: Capturer | ESC: Annuler", (10, frame.shape[0] - 20),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

            cv2.imshow(window_name, frame)
            key = cv2.waitKey(1) & 0xFF

            if key == 27:  # ESC
                return None, "Verification annulee par l'utilisateur"
            if key == 32 and best_embedding is not None:
                return best_embedding, "Visage capture avec succes"

        if best_embedding is not None and best_frame is not None:
            return best_embedding, "Visage detecte automatiquement"

        return None, "Aucun visage valide detecte"
    finally:
        camera.release()
        cv2.destroyAllWindows()


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

