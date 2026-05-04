#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Tahwissa - Verification Biometrique Humaine
===========================================
Script de detection faciale pour empecher les inscriptions automatisees par des bots.
Utilise OpenCV avec les classificateurs Haar Cascade pour detecter les visages humains.

Auteur: Tahwissa Team
Date: 2026
"""

import cv2
import sys
import io
import json
import time
import os
from datetime import datetime

# ── Force stdout to UTF-8 on Windows (prevents cp1252 encoding issues) ──────
# Without this, Python on Windows encodes stdout in the system code page
# (e.g. cp1252), causing PHP's json_decode to see malformed UTF-8 bytes.
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')


class HumanVerification:
    """Classe pour gérer la vérification biométrique faciale"""
    
    def __init__(self):
        """Initialise le système de vérification"""
        self.cascade_path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
        self.face_cascade = cv2.CascadeClassifier(self.cascade_path)
        
        if self.face_cascade.empty():
            raise Exception("ERROR: Impossible de charger le classificateur de visages")

        print("OK: Classificateur de visages chargé avec succès", file=sys.stderr)

    def detect_faces(self, frame):
        """
        Détecte les visages dans une image
        
        Args:
            frame: Image OpenCV (numpy array)
            
        Returns:
            tuple: (nombre_de_visages, rectangles_des_visages)
        """
        # Convertir en niveaux de gris pour améliorer la détection
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        
        # Égalisation d'histogramme pour améliorer le contraste
        gray = cv2.equalizeHist(gray)
        
        # Détecter les visages
        faces = self.face_cascade.detectMultiScale(
            gray,
            scaleFactor=1.1,
            minNeighbors=5,
            minSize=(50, 50),
            flags=cv2.CASCADE_SCALE_IMAGE
        )
        
        return len(faces), faces
    
    def draw_face_rectangles(self, frame, faces):
        """
        Dessine des rectangles autour des visages détectés
        
        Args:
            frame: Image OpenCV
            faces: Liste des rectangles de visages
            
        Returns:
            Image avec les rectangles dessinés
        """
        for (x, y, w, h) in faces:
            # Couleur violet/bleu (en BGR)
            cv2.rectangle(frame, (x, y), (x+w, y+h), (234, 51, 147), 3)
            
            # Ajouter un label
            cv2.putText(
                frame, 
                'Visage detecte', 
                (x, y-10), 
                cv2.FONT_HERSHEY_SIMPLEX, 
                0.6, 
                (234, 51, 147), 
                2
            )
        
        return frame
    
    def verify_webcam(self, duration=10, save_image=False, output_path=None):
        """
        Vérifie la présence d'un visage humain via webcam
        
        Args:
            duration: Durée de la vérification en secondes
            save_image: Sauvegarder l'image capturée
            output_path: Chemin de sauvegarde de l'image
            
        Returns:
            dict: Résultat de la vérification
        """
        result = {
            'success': False,
            'message': '',
            'face_count': 0,
            'timestamp': datetime.now().isoformat(),
            'image_path': None
        }
        
        # Ouvrir la webcam
        camera = cv2.VideoCapture(0)

        if not camera.isOpened():
            result['message'] = "ERROR: Impossible d'accéder à la webcam"
            return result
        
        print("INFO: Webcam activée. Positionnez-vous face à la caméra...", file=sys.stderr)

        # Attendre que la caméra se stabilise
        for _ in range(10):
            camera.read()
        
        start_time = time.time()
        best_frame = None
        best_area = 0
        max_faces_detected = 0
        total_frames = 0
        frames_with_one_face = 0
        
        # Fenêtre de visualisation
        window_name = 'Tahwissa - Verification Biometrique (Appuyez sur ESPACE pour capturer, ESC pour annuler)'
        cv2.namedWindow(window_name, cv2.WINDOW_NORMAL)
        cv2.resizeWindow(window_name, 800, 600)

        try:
            while (time.time() - start_time) < duration:
                ret, frame = camera.read()

                if not ret:
                    continue

                total_frames += 1

                # Détecter les visages
                face_count, faces = self.detect_faces(frame)

                # Dessiner les rectangles
                display_frame = self.draw_face_rectangles(frame.copy(), faces)

                # Ajouter des informations à l'écran
                elapsed = int(time.time() - start_time)
                remaining = duration - elapsed

                # Texte de statut
                status_text = f"Temps restant: {remaining}s"
                cv2.putText(display_frame, status_text, (10, 30),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

                if face_count == 0:
                    status = "Aucun visage detecte"
                    color = (0, 0, 255)  # Rouge
                elif face_count == 1:
                    status = "Visage detecte - Appuyez sur ESPACE"
                    color = (0, 255, 0)  # Vert
                    frames_with_one_face += 1

                    # Garder la meilleure image
                    if best_frame is None:
                        best_frame = frame.copy()
                else:
                    status = f"{face_count} visages detectes - Une seule personne requise"
                    color = (0, 165, 255)  # Orange

                cv2.putText(display_frame, status, (10, 60),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)

                # Instructions
                cv2.putText(display_frame, "ESPACE: Capturer | ESC: Annuler", (10, display_frame.shape[0] - 20),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

                # Afficher le frame
                cv2.imshow(window_name, display_frame)

                max_faces_detected = max(max_faces_detected, face_count)

                # Gestion des touches
                key = cv2.waitKey(1) & 0xFF

                if key == 27:  # ESC
                    result['message'] = "ERROR: Vérification annulée par l'utilisateur"
                    break

                elif key == 32:  # SPACE
                    if face_count == 1:
                        result['success'] = True
                        result['face_count'] = face_count
                        result['message'] = "OK: Visage humain vérifié avec succès!"

                        # Sauvegarder l'image si demandé
                        if save_image and output_path:
                            cv2.imwrite(output_path, frame)
                            result['image_path'] = output_path
                            print(f"INFO: Image sauvegardée: {output_path}", file=sys.stderr)

                        break
                    else:
                        print(f"WARN: Impossible de capturer: {face_count} visage(s) détecté(s)", file=sys.stderr)

            # Si aucune capture manuelle et au moins un visage détecté
            if not result['success'] and frames_with_one_face > total_frames * 0.3:
                result['success'] = True
                result['face_count'] = 1
                result['message'] = "OK: Visage humain détecté (vérification automatique)"

                if save_image and output_path and best_frame is not None:
                    cv2.imwrite(output_path, best_frame)
                    result['image_path'] = output_path

            elif not result['success'] and result['message'] == '':
                if max_faces_detected == 0:
                    result['message'] = "ERROR: Aucun visage détecté. Veuillez réessayer."
                elif max_faces_detected > 1:
                    result['message'] = f"ERROR: Plusieurs visages détectés ({max_faces_detected}). Une seule personne doit être visible."
                else:
                    result['message'] = "ERROR: Temps écoulé sans capture valide."

        except Exception as e:
            result['message'] = f"ERROR: {str(e)}"
            print(f"ERROR: Exception: {e}", file=sys.stderr)

        finally:
            camera.release()
            cv2.destroyAllWindows()

        return result
    
    def verify_image(self, image_path):
        """
        Vérifie la présence d'un visage dans une image existante
        
        Args:
            image_path: Chemin de l'image à vérifier
            
        Returns:
            dict: Résultat de la vérification
        """
        result = {
            'success': False,
            'message': '',
            'face_count': 0,
            'timestamp': datetime.now().isoformat()
        }
        
        if not os.path.exists(image_path):
            result['message'] = f"ERROR: Image introuvable: {image_path}"
            return result
        
        try:
            # Charger l'image
            image = cv2.imread(image_path)
            if image is None:
                result['message'] = "ERROR: Impossible de charger l'image"
                return result
            
            # Détecter les visages
            face_count, faces = self.detect_faces(image)

            result['face_count'] = face_count
            
            if face_count == 1:
                result['success'] = True
                result['face_count'] = face_count
                result['message'] = "OK: Visage humain vérifié avec succès!"
            elif face_count == 0:
                result['message'] = "ERROR: Aucun visage détecté dans l'image"
            else:
                result['message'] = f"ERROR: Plusieurs visages détectés ({face_count}). Un seul requis."

        except Exception as e:
            result['message'] = f"ERROR: {str(e)}"

        return result


def main():
    """Fonction principale"""
    
    # Vérifier les arguments
    if len(sys.argv) < 2:
        print(json.dumps({
            'success': False,
            'message': "ERROR: Usage: python human_verification.py <mode> [options]",
            'usage': {
                'webcam': 'python human_verification.py webcam [duration] [save_path]',
                'image': 'python human_verification.py image <image_path>'
            }
        }))
        sys.exit(1)
    
    mode = sys.argv[1].lower()
    
    try:
        verifier = HumanVerification()
        
        if mode == 'webcam':
            # Mode webcam
            duration = int(sys.argv[2]) if len(sys.argv) > 2 else 10
            save_path = sys.argv[3] if len(sys.argv) > 3 else None

            print(f"Mode webcam activé (durée: {duration}s)", file=sys.stderr)

            result = verifier.verify_webcam(
                duration=duration,
                save_image=(save_path is not None),
                output_path=save_path
            )
            
        elif mode == 'image':
            # Mode image
            if len(sys.argv) < 3:
                print(json.dumps({
                    'success': False,
                    'message': "ERROR: Chemin de l'image requis"
                }))
                sys.exit(1)
            
            image_path = sys.argv[2]
            print(f"Mode image activé: {image_path}", file=sys.stderr)

            result = verifier.verify_image(image_path)
        
        else:
            print(json.dumps({
                'success': False,
                'message': f"ERROR: Mode inconnu: {mode}. Utilisez 'webcam' ou 'image'"
            }))
            sys.exit(1)
        
        # Afficher le resultat en JSON (ensure_ascii=True: no raw accented chars on stdout)
        print(json.dumps(result, ensure_ascii=True))
        
        # Code de sortie
        sys.exit(0 if result['success'] else 1)
    
    except Exception as e:
        print(json.dumps({
            'success': False,
            'message': f"ERROR: {str(e)}",
            'error': str(e)
        }))
        sys.exit(1)


if __name__ == '__main__':
    main()
