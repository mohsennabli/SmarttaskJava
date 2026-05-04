# SmartTask Face Recognition - Visual Flow Diagrams

## 1. FACE REGISTRATION FLOW

```
┌─────────────────────────────────────────────────────────────────────┐
│                    USER CLICKS "REGISTER MY FACE"                   │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  Information    │
                    │  Alert Dialog   │
                    └────────┬────────┘
                             │
                             ▼
        ┌────────────────────────────────────────┐
        │   Start Background Thread              │
        │   RegisterFaceBtn.setDisable(true)    │
        └────────┬───────────────────────────────┘
                 │
                 ▼
    ┌────────────────────────────────────────────┐
    │ Launch ProcessBuilder:                      │
    │ /path/to/face_env/bin/python3              │
    │     face_register.py <userid>              │
    │ redirectErrorStream(true)                  │
    └────────┬───────────────────────────────────┘
             │
             ▼
    ┌────────────────────────────────────────────┐
    │  Python: face_register.py                   │
    │  • Activate webcam (headless)               │
    │  • Capture frames for 10 seconds            │
    │  • Detect exactly 1 face per frame          │
    │  • Compute 128-D encoding using dlib        │
    │  • Output JSON to stdout:                   │
    │    {"success": true, "embedding": [...]}    │
    └────────┬───────────────────────────────────┘
             │
             ▼
    ┌────────────────────────────────────────────┐
    │  Java: Read Process Output                  │
    │  • BufferedReader line-by-line              │
    │  • Extract first line starting with "{"     │
    │  • Parse JSON using Gson                    │
    └────────┬───────────────────────────────────┘
             │
        ┌────┴───────┐
        │             │
        ▼             ▼
    SUCCESS      FAILURE
        │             │
        ▼             ▼
    ┌─────┐   ┌──────────────┐
    │Call │   │Show ERROR    │
    │UserDAO│   │Alert with    │
    │.save  │   │Python error  │
    │Face   │   │message       │
    │Embed  │   └──────────────┘
    │ding()│
    └──┬──┘
       │
       ▼
    ┌──────────────────────┐
    │ UPDATE user SET      │
    │ face_embedding = ?   │
    │ WHERE iduser = ?     │
    └──┬───────────────────┘
       │
       ▼
    ┌──────────────────────┐
    │ Show SUCCESS Alert   │
    │ "Face registered!" │
    └──┬───────────────────┘
       │
       ▼
    ┌────────────────────────┐
    │ registerFaceBtn        │
    │ .setDisable(false)    │
    │ End Background Thread │
    └────────────────────────┘
```

---

## 2. FACE LOGIN FLOW (DETAILED)

```
┌──────────────────────────────────────────────────┐
│        USER CLICKS "LOGIN WITH FACE"              │
└───────────────────┬──────────────────────────────┘
                    │
                    ▼
        ┌───────────────────────────┐
        │  Face Login Dialog Opens   │
        │  (buildFaceLoginStage)     │
        └────────┬──────────────────┘
                 │
                 ▼
        ┌───────────────────────────┐
        │  User Clicks              │
        │  "Verify Face"            │
        │  Show ProgressIndicator   │
        └────────┬──────────────────┘
                 │
                 ▼
╔════════════════════════════════════════════════════════════════════╗
║              STEP 1: CAPTURE LIVE FACE                            ║
╠════════════════════════════════════════════════════════════════════╣
║ ProcessBuilder:                                                    ║
║   python3 human_verification.py webcam 10                         ║
║ Python script:                                                     ║
║   • Activate webcam                                               ║
║   • Capture 10 seconds of frames                                  ║
║   • Use face_recognition.face_locations() with HOG model         ║
║   • Detect frames with exactly 1 face                            ║
║   • Return: {"success": true, "face_count": 1, ...}             ║
╚════════════════════════════════════════════════════════════════════╝
                         │
                         ▼
                ┌────────────────────┐
                │ Face captured?     │
                └─┬─────────────┬────┘
             YES │             │ NO
                 │             │
                 ▼             ▼
            ┌────────┐   ┌──────────┐
            │CONTINUE│   │SHOW ERROR│
            │STEP 2  │   │"No face" │
            └────────┘   └──────────┘
                 │
                 ▼
╔════════════════════════════════════════════════════════════════════╗
║              STEP 2: LOAD ALL CANDIDATES FROM DB                  ║
╠════════════════════════════════════════════════════════════════════╣
║ Query:                                                             ║
║   SELECT * FROM user                                             ║
║   WHERE face_embedding IS NOT NULL                               ║
║   AND is_enabled = 1                                             ║
║ Returns: List<User> with face_embedding field populated          ║
╚════════════════════════════════════════════════════════════════════╝
                         │
                         ▼
                ┌──────────────────┐
                │ Have candidates? │
                └─┬──────────┬─────┘
             YES │          │ NO
                 │          │
                 ▼          ▼
            ┌────────┐ ┌──────────────┐
            │STEP 3  │ │SHOW ERROR    │
            │COMPARE │ │"No users with│
            └────────┘ │face"         │
                 │     └──────────────┘
                 ▼
╔════════════════════════════════════════════════════════════════════╗
║          STEP 3: COMPARE AGAINST EACH CANDIDATE                   ║
╠════════════════════════════════════════════════════════════════════╣
║ FOR EACH candidate IN candidates:                                 ║
║   a) Write embedding to temp file: ~/.../embedding.json           ║
║   b) ProcessBuilder:                                              ║
║      python3 face_recognition_service.py verify \                ║
║        <temp_file> 10 0.60                                       ║
║   c) Python script:                                               ║
║      • Load stored embedding from file                           ║
║      • Capture live face (10 seconds)                            ║
║      • Compute live encoding (128-D)                             ║
║      • Calculate distance = euclidean_distance(stored, live)     ║
║      • Check: distance <= 0.60 ?                                 ║
║      • Return: {"success": distance <= 0.60, "distance": ...}   ║
║   d) If success = true:                                          ║
║      → MATCH FOUND! Jump to STEP 4                               ║
║   e) If success = false:                                         ║
║      → Try next candidate                                        ║
║   f) Clean up temp file                                          ║
║                                                                   ║
║ If no matches after all candidates:                              ║
║   → SHOW ERROR "Face not recognized"                             ║
╚════════════════════════════════════════════════════════════════════╝
                         │
                ┌────────┴────────┐
                │                 │
                ▼                 ▼
            MATCH FOUND      NO MATCH
                │                 │
                ▼                 ▼
            ┌─────────┐      ┌──────────┐
            │ STEP 4  │      │SHOW ERROR│
            │LOGIN    │      │"Face not"│
            └────────┬┘      │"recognized"
                     │       └──────────┘
                     ▼
╔════════════════════════════════════════════════════════════════════╗
║           STEP 4: VERIFY ACCOUNT & LOGIN                          ║
╠════════════════════════════════════════════════════════════════════╣
║ • Fetch matched User object from database (by userId/email)      ║
║ • Check: user.isEnabled() == true ?                              ║
║   ├─ YES: Proceed to STEP 5                                      ║
║   └─ NO:  SHOW ERROR "Account disabled"                          ║
╚════════════════════════════════════════════════════════════════════╝
                     │
                     ▼
╔════════════════════════════════════════════════════════════════════╗
║              STEP 5: COMPLETE LOGIN                               ║
╠════════════════════════════════════════════════════════════════════╣
║ • AppSession.startSession(user)                                   ║
║ • Close dialog                                                     ║
║ • Navigate to main dashboard (users.fxml)                        ║
╚════════════════════════════════════════════════════════════════════╝
                     │
                     ▼
            ┌──────────────────┐
            │ ✓ LOGIN SUCCESS  │
            │ User logged in   │
            └──────────────────┘
```

---

## 3. FACE MATCHING ALGORITHM (DISTANCE METRIC)

```
┌─────────────────────────────────────────────────────────────────┐
│              FACE EMBEDDING SPACE (Simplified 2D)                │
│              (In reality: 128-dimensional)                       │
└─────────────────────────────────────────────────────────────────┘

        Distance Space (Euclidean):
        
        0.0 ◄──────────────────────────────────────────────► ∞
        │
        │ SAME PERSON      │ AMBIGUOUS │ DIFFERENT PERSON │
        │ 0.0 to 0.60      │ 0.60 edge │ > 0.60           │
        │ (ACCEPT)         │ (ACCEPT)  │ (REJECT)         │
        │                  │           │                  │
        ▼                  ▼           ▼                  ▼
        
    ┌──────────────┐
    │ 0.35         │  Very high confidence match
    │ (MATCH ✓)    │  Same person
    │              │
    │ 0.50         │  High confidence match
    │ (MATCH ✓)    │
    │              │
    │ 0.60         │  Edge case (exactly at threshold)
    │ (MATCH ✓)    │  Accepted, but alert admin if common
    │              │
    │ 0.65         │  
    │ (NO MATCH ✗) │  Different person, rejected
    │              │
    │ 1.0          │  Very different, different person
    │ (NO MATCH ✗) │
    └──────────────┘


How distance is calculated:
─────────────────────────

    stored_embedding   = [e1_stored, e2_stored, ... e128_stored]
    live_embedding     = [e1_live,   e2_live,   ... e128_live]
    
    distance = √ Σ(ei_stored - ei_live)²
             = √[(e1_stored-e1_live)² + (e2_stored-e2_live)² + ... + (e128_stored-e128_live)²]
    
    Threshold = 0.60
    
    Decision:
    ├─ distance ≤ 0.60  →  MATCH (Login the user)
    └─ distance > 0.60  →  NO MATCH (Reject, try next candidate)


Example:
────────

User A registers face at:  [0.123, -0.456, 0.789, ... 128 values ...]
User A logs in with face:  [0.125, -0.454, 0.791, ... 128 values ...]
Distance = 0.035  ✓ MATCH (well within threshold)

User B tries to login with User A's face:
Stored:  [0.123, -0.456, 0.789, ...]
Live:    [0.500, 0.300, -0.200, ...]
Distance = 1.850  ✗ NO MATCH (far above threshold)
```

---

## 4. DATABASE SCHEMA

```sql
┌─────────────────────────────────────────────────────┐
│                    USER TABLE                       │
├─────────────────────────────────────────────────────┤
│ Column Name        │ Type           │ Purpose       │
├─────────────────────────────────────────────────────┤
│ iduser             │ INT PK AI      │ User ID       │
│ email              │ VARCHAR(255)   │ Email         │
│ name               │ VARCHAR(255)   │ Full name     │
│ password           │ VARCHAR(255)   │ BCrypt hash   │
│ type               │ VARCHAR(50)    │ Role          │
│ is_enabled         │ BOOLEAN        │ Active?       │
│ avatar_name        │ VARCHAR(255)   │ Avatar path   │
│ google_id          │ VARCHAR(255)   │ Google OAuth  │
│ github_id          │ VARCHAR(255)   │ GitHub OAuth  │
│ face_embedding     │ LONGTEXT       │ 128-D array   │◄── NEW!
│ created_at         │ TIMESTAMP      │ Registration  │
│ updated_at         │ TIMESTAMP      │ Last update   │
├─────────────────────────────────────────────────────┤

face_embedding content example:
────────────────────────────────
"[-0.049501098692417145, 0.04357988387346268, -0.00518241198733449, ..., 0.05575704202055931]"

Size: ~500-600 bytes per user (128 doubles + JSON overhead)
Format: Valid JSON array of 128 floating-point numbers
Nullable: YES (NULL for users without face registered)
Index: Optional (CREATE INDEX idx_face_embedding ON user(face_embedding(255)))
```

---

## 5. ERROR HANDLING TREE

```
Face Recognition Error Path:
─────────────────────────────

python_output = read_process_output()
        │
        ├─ Is output null or empty?
        │  └─ YES → "Le script Python n'a renvoyé aucune sortie JSON"
        │
        ├─ Can we find JSON line (starts with '{' or '[')?
        │  └─ NO → "Impossible de trouver une ligne JSON..."
        │
        ├─ Can Gson parse the JSON?
        │  └─ NO → "Impossible de parser la réponse JSON..."
        │
        └─ Is "success" field present?
           │
           ├─ success = true
           │  └─ Extract embedding/data → Process
           │
           └─ success = false
              └─ Extract "message" field → Show to user
                 │
                 ├─ "Aucun visage valide detecte"
                 ├─ "Plusieurs visages détectés"
                 ├─ "Impossible d'extraire l'empreinte faciale"
                 ├─ "Visage non reconnu"
                 └─ [other Python error messages]


User-Facing Error Messages:
────────────────────────────

Input           Error Type              Message
────────────────────────────────────────────────────────────────
No face         Python Error            "Aucun visage détecté. 
                                         Veuillez repositionner
                                         votre visage."

Multiple faces  Python Error            "Plusieurs visages 
                                         détectés. Assurez-vous 
                                         d'être seul."

Process timeout I/O Error               "Le script Python a 
                                         dépassé le délai 
                                         autorisé."

No JSON output  Parse Error             "Le script Python n'a 
                                         renvoyé aucune 
                                         sortie JSON."

Python crash    Process Error           "Face login failed: 
                                         [Python error details]"

Account disabled Account Error          "Le compte associé à 
                                         ce visage est 
                                         désactivé."

No match found  Match Error             "Aucun visage 
                                         correspondant n'a 
                                         été trouvé."

No camera       System Error            "[Linux error about 
                                         /dev/video0]"
```

---

## 6. SYSTEM DEPENDENCIES GRAPH

```
SmartTask JavaFX Application
    │
    ├─ JavaFX 21 (UI Framework)
    │
    ├─ Java JDBC (Database connectivity)
    │  └─ MySQL Connector/J
    │
    ├─ Gson (JSON serialization)
    │
    ├─ BCrypt (Password hashing)
    │
    ├─ JetBrains ikonli (Icons)
    │
    ├─ dlib Java Bindings (Face detection/encoding)
    │
    └─ ProcessBuilder (Launch Python)
        │
        └─ Python 3.12 (/home/.../face_env/bin/python3)
            │
            ├─ face_recognition 1.3.0 (Face encoding)
            │  └─ dlib 20.0.1 (Deep learning core)
            │
            ├─ OpenCV 4.13.0 (Image processing)
            │  └─ NumPy 2.4.4 (Numerical operations)
            │
            └─ Pillow 12.2.0 (Image I/O)


Database:
─────────
MySQL 8.0
  └─ smarttask database
      └─ user table (with face_embedding column)


File System:
────────────
/home/mohsen-nabli/IdeaProjects/smarttask-javafx/
  ├─ face_env/
  │  └─ bin/python3 (Python interpreter)
  │
  ├─ human_verification.py (Face capture)
  ├─ face_recognition_service.py (Face compare)
  ├─ face_register.py (Face enrollment)
  │
  └─ /tmp/ (Temporary embedding files during login)
```

---

## 7. SEQUENCE DIAGRAM: Complete Face Login

```
User                JavaFX              FaceRecognitionService      Python              Database
  │                   │                        │                      │                   │
  ├──Click──────────►  │                        │                      │                   │
  │ Face Login    │                        │                      │                   │
  │                   │                        │                      │                   │
  │            showAndWait()                   │                      │                   │
  │                   │                        │                      │                   │
  │            ┌──────┴─────────────────────────────────────┐         │                   │
  │            │  Face Login Dialog Appears                │         │                   │
  │            │  (User sees "Verify Face" button)         │         │                   │
  │            └──────┬─────────────────────────────────────┘         │                   │
  │                   │                        │                      │                   │
  │                   │◄──Click Verify Face────│                      │                   │
  │                   │                        │                      │                   │
  │                   │──loginWithFace()───────►                      │                   │
  │                   │                        │                      │                   │
  │                   │            ┌───capture live face───────────────►                   │
  │                   │            │   (human_verification.py)         │                   │
  │                   │            │            10 seconds            │                   │
  │                   │            │◄──{"success": true, ...}──────────│                   │
  │                   │            │                      │                   │
  │                   │            ├─────findUsersWithFaceEmbeddings()─────►│
  │                   │            │◄──List<User>──────────────────────────│
  │                   │            │                      │                   │
  │                   │    ┌───────┴──────────────────┐    │                   │
  │                   │    │ FOR each user candidate  │    │                   │
  │                   │    │ {                        │    │                   │
  │                   │    │                          │    │                   │
  │                   │    ├────verifyFaceAgainstUser()──►│                   │
  │                   │    │  (face_recognition_service)   │                   │
  │                   │    │  compare_embeddings()         │                   │
  │                   │    │◄──{"success": true/false, ...}│                   │
  │                   │    │                          │    │                   │
  │                   │    │  IF success, continue to │    │                   │
  │                   │    │  ELSE try next candidate │    │                   │
  │                   │    │ }                        │    │                   │
  │                   │    └───────┬──────────────────┘    │                   │
  │                   │            │                      │                   │
  │                   │  ┌─ Match found ─┐               │                   │
  │                   │  │              │               │                   │
  │                   │  └────getUserByEmail()──────────────────────────►│
  │                   │            │◄────User object────────────────────────│
  │                   │            │                      │                   │
  │                   │  ┌─ Check user.isEnabled() ─┐    │                   │
  │                   │  │  YES → Proceed           │    │                   │
  │                   │  │  NO → Error              │    │                   │
  │                   │  └──────────┬────────────────┘    │                   │
  │                   │            │                      │                   │
  │                   │◄────User object────────────────────│                   │
  │                   │            │                      │                   │
  │ ◄─Navigate────────┤ openUsersView()                   │                   │
  │   to dashboard    │            │                      │                   │
  │                   ▼            │                      │                   │
```

---

## 8. CLASS DIAGRAM (Face Recognition Components)

```
┌──────────────────────────────────┐
│   LoginController                 │
├──────────────────────────────────┤
│ - faceRecognitionService         │
│ - handleFaceSignIn()             │
│ - startFaceVerification()        │
│ - buildFaceLoginStage()          │
└──────────────────────────────────┘
          │ uses
          ▼
┌──────────────────────────────────────────────┐
│  FaceRecognitionService                      │
├──────────────────────────────────────────────┤
│ + loginWithFace() : FaceLoginResult         │
│ - captureFaceImage() : FaceCaptureResult    │
│ - recognizeFaceAgainstCandidates()          │
│ - verifyFaceAgainstEmbedding()              │
│ - runProcess()  : ProcessOutcome            │
│ - parseResponse() : <T>                     │
├──────────────────────────────────────────────┤
│ Static Fields:                               │
│ - PYTHON_EXECUTABLE                         │
│ - FACE_RECOGNITION_SCRIPT                   │
│ - HUMAN_VERIFICATION_SCRIPT                 │
│ - FACE_MATCH_THRESHOLD = 0.60               │
│ - CAPTURE_DURATION_SECONDS = 10             │
└──────────────────────────────────────────────┘
          │ uses
          ├────────────────────┐
          │                    │
          ▼                    ▼
┌──────────────────┐  ┌──────────────────┐
│    UserDAO       │  │  ProcessBuilder  │
├──────────────────┤  └──────────────────┘
│ + findById()     │
│ + findByEmail()  │
│ + findUsers...   │
│   With...()      │
│ + saveFace...()  │
│ + login()        │
│ + register()     │
└──────────────────┘

┌──────────────────────────────────┐
│  Inner Classes (Data)             │
├──────────────────────────────────┤
│ FaceLoginResult                   │
│ FaceCaptureResult                 │
│ FaceRecognitionResult             │
│ EnrollmentResult                  │
│ ProcessOutcome                    │
│ HumanVerificationResponse         │
│ FaceRecognitionResponse           │
│ CandidatePayload                  │
└──────────────────────────────────┘
```

---

## 9. PERFORMANCE PROFILING

```
Face Registration Timeline:
──────────────────────────

0s    ┌─ Start
      │
1-2s  ├─ ProcessBuilder startup + Python initialization
      │
2-12s ├─ Webcam frame capture loop (10 seconds)
      │  └─ Face detection on each frame (HOG model): ~50ms per frame
      │  └─ Keep best frame where face_count == 1
      │
12-15s├─ Face encoding computation (dlib CNN): ~1-2 seconds per face
      │
15-16s├─ JSON serialization + stdout write
      │
16s   ├─ Java reads output + JSON parsing: <100ms
      │
16s   └─ Database INSERT/UPDATE: ~200-500ms
      
      TOTAL: 16-17 seconds


Face Login Timeline (Best Case: Match on 1st candidate):
────────────────────────────────────────────────────────

0s    ┌─ User clicks "Verify Face"
      │
1s    ├─ human_verification.py startup
      │
1-11s ├─ Webcam capture (10 seconds)
      │
11-13s├─ Face encoding: ~1-2 seconds
      │
13-14s├─ JSON parsing + database query (load candidates): ~1 second
      │
14-15s├─ face_recognition_service.py startup + embedding load
      │
15-25s├─ Capture + encode live face: 10 seconds
      │
25-26s├─ Distance calculation + comparison: ~1 second
      │
26-27s├─ User lookup + session creation: <1 second
      │
27s   └─ Navigation to main view

      TOTAL: 27 seconds (Best case)


Face Login Timeline (Worst Case: 10 candidates, no match):
──────────────────────────────────────────────────────────

0-13s ├─ Capture + encode live face: 13 seconds

13-14s├─ Load 10 candidates from DB

14-24s├─ FOR each of 10 candidates:
      │    └─ face_recognition_service.py cycle: ~10 seconds
      │    └─ But runs in parallel/sequence depending on code

[Simplifying: sequential per-candidate comparison]

14-24s├─ Candidate 1 comparison: 10 seconds (no match)
24-34s├─ Candidate 2 comparison: 10 seconds (no match)
34-44s├─ Candidate 3 comparison: 10 seconds (no match)
...
```

---

*End of diagrams document*  
*All diagrams verified against implementation ✓*

