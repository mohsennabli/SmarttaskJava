# SmartTask JavaFX

## Face Registration (Profile)

This project includes a face registration flow that captures a single face from the webcam and stores a 128-d embedding in the `user.face_embedding` column.

### Requirements

- Python venv: `/home/mohsen-nabli/IdeaProjects/smarttask-javafx/face_env/bin/python3`
- Python packages: `face_recognition`, `opencv-python`, `numpy`
- Webcam access

### Quick Test (CLI)

Run the face registration script directly (outputs JSON only):

```bash
/home/mohsen-nabli/IdeaProjects/smarttask-javafx/face_env/bin/python3 face_register.py 1
```

### In-App Flow

Open **Profile** and click **Register My Face** to capture a face and save the embedding to the database.

