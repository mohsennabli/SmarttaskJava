#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Tahwissa - Face Recognition Service
===================================
Compare a captured face image against multiple candidate embeddings (for face login).
Also supports enrollment mode to capture and store a new face embedding.
"""

import json
import sys
import io
import time
import argparse
from datetime import datetime

import cv2
import numpy as np
import face_recognition

# ── Force stdout to UTF-8 on Windows (prevents cp1252 → PHP malformed UTF-8) ──
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')


def _result(success, message, embedding=None, distance=None, face_count=None,
            user_id=None, email=None, name=None, confidence=None, threshold=None, candidate_count=None):
    payload = {
        "success": bool(success),
        "message": message,
        "timestamp": datetime.now().isoformat(),
        "embedding": embedding,
        "distance": distance,
        "face_count": face_count,
        "user_id": user_id,
        "email": email,
        "name": name,
        "confidence": confidence,
        "threshold": threshold,
        "candidate_count": candidate_count,
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


def _load_image_embedding(image_path):
    """Load an image from disk and compute its face embedding."""
    image = cv2.imread(image_path)
    if image is None:
        return None, None, 0, "Impossible de charger l'image"

    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    face_locations = face_recognition.face_locations(rgb, model="hog")
    face_count = len(face_locations)

    if face_count != 1:
        return None, None, face_count, f"Image doit contenir exactement 1 visage, trouvé {face_count}"

    encodings = face_recognition.face_encodings(rgb, face_locations)
    if not encodings:
        return None, None, face_count, "Impossible de calculer l'embedding facial"

    return encodings[0], image, face_count, "Embedding calculé avec succès"


def _load_candidates(candidates_path):
    """Load candidates from JSON file."""
    try:
        with open(candidates_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        return data if isinstance(data, list) else [], None
    except Exception as e:
        return [], str(e)


def _verify_against_candidates(image_embedding, candidates, threshold):
    """Compare image embedding against all candidates and return best match."""
    if not candidates:
        return False, "Aucun candidat disponible", None, None, None, None

    best_distance = float('inf')
    best_match = None

    for candidate in candidates:
        try:
            candidate_embedding = np.array(candidate.get("face_embedding", []), dtype=np.float64)
            if len(candidate_embedding) == 0:
                continue

            distance = float(face_recognition.face_distance([candidate_embedding], image_embedding)[0])
            if distance < best_distance:
                best_distance = distance
                best_match = candidate
        except Exception:
            continue

    if best_match is None:
        return False, "Aucune correspondance trouvée", None, None, None, None

    success = best_distance <= threshold
    msg = "Visage reconnu" if success else "Visage non reconnu"

    return success, msg, best_distance, best_match.get("user_id"), best_match.get("email"), best_match.get("name")


def main():
    parser = argparse.ArgumentParser(description="Face Recognition Service")
    parser.add_argument("mode", choices=["enroll", "verify"], help="Mode: enroll or verify")
    parser.add_argument("--image", type=str, help="Path to image file (for verify mode)")
    parser.add_argument("--candidates", type=str, help="Path to candidates JSON file (for verify mode)")
    parser.add_argument("--threshold", type=float, default=0.60, help="Distance threshold for matching (default: 0.60)")
    parser.add_argument("--duration", type=int, default=10, help="Capture duration in seconds (for enroll mode)")

    try:
        args = parser.parse_args()
    except SystemExit:
        _result(False, "Arguments invalides")
        sys.exit(1)

    mode = args.mode.lower()

    if mode == "enroll":
        embedding, message = _capture_embedding(args.duration)
        if embedding is None:
            _result(False, message)
            sys.exit(1)
        _result(True, message, embedding=embedding.tolist(), distance=None)
        sys.exit(0)

    if mode == "verify":
        # Verify mode with image file and candidates JSON
        if not args.image:
            _result(False, "Chemin d'image requis pour le mode verify")
            sys.exit(1)

        if not args.candidates:
            _result(False, "Chemin d'embeddings candidats requis pour le mode verify")
            sys.exit(1)

        # Load image and compute embedding
        image_embedding, image, face_count, embed_msg = _load_image_embedding(args.image)
        if image_embedding is None:
            _result(False, embed_msg, face_count=face_count)
            sys.exit(1)

        # Load candidates
        candidates, error = _load_candidates(args.candidates)
        if error:
            _result(False, f"Erreur lors du chargement des candidats: {error}", face_count=face_count)
            sys.exit(1)

        if not candidates:
            _result(False, "Aucun candidat disponible", face_count=face_count, candidate_count=0)
            sys.exit(1)

        # Compare and find best match
        success, msg, distance, user_id, email, name = _verify_against_candidates(
            image_embedding, candidates, args.threshold
        )

        confidence = 1.0 - distance if distance is not None else 0.0

        _result(
            success,
            msg,
            embedding=None,
            distance=distance,
            face_count=face_count,
            user_id=user_id,
            email=email,
            name=name,
            confidence=confidence,
            threshold=args.threshold,
            candidate_count=len(candidates)
        )
        sys.exit(0 if success else 1)

    _result(False, "Mode inconnu")
    sys.exit(1)


if __name__ == "__main__":
    main()

