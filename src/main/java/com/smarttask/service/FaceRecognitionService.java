package com.smarttask.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smarttask.dao.UserDAO;
import com.smarttask.model.User;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FaceRecognitionService {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final ExecutorService IO_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "smarttask-face-io");
        thread.setDaemon(true);
        return thread;
    });

    private static final String PYTHON_EXECUTABLE = readConfig(
            "SMARTTASK_PYTHON_EXECUTABLE",
            "/home/mohsen-nabli/IdeaProjects/smarttask-javafx/face_env/bin/python3"
    );
    private static final Path FACE_RECOGNITION_SCRIPT = resolveScript("SMARTTASK_FACE_RECOGNITION_SCRIPT", "face_recognition_service.py");
    private static final Path HUMAN_VERIFICATION_SCRIPT = resolveScript("SMARTTASK_HUMAN_VERIFICATION_SCRIPT", "human_verification.py");
    private static final double FACE_MATCH_THRESHOLD = readDoubleConfig("SMARTTASK_FACE_MATCH_THRESHOLD", 0.60d);
    private static final int CAPTURE_DURATION_SECONDS = readIntConfig("SMARTTASK_FACE_CAPTURE_DURATION_SECONDS", 10);
    private static final int CAMERA_INDEX = readIntConfig("SMARTTASK_FACE_CAMERA_INDEX", 0);

    private final UserDAO userDAO = new UserDAO();

    public FaceLoginResult loginWithFace() {
        try {
            ensureScriptExists(HUMAN_VERIFICATION_SCRIPT, "human verification");
            ensureScriptExists(FACE_RECOGNITION_SCRIPT, "face recognition");

            // Step 1: Capture a live face from webcam
            FaceCaptureResult captureResult = captureFaceImage();
            if (!captureResult.success) {
                return FaceLoginResult.failure(captureResult.message, null, captureResult.faceCount, null, null, null, null, null);
            }

            // Step 2: Get all users with stored face embeddings from database
            List<User> candidates = userDAO.findUsersWithFaceEmbeddings();
            if (candidates.isEmpty()) {
                return FaceLoginResult.failure(
                        "Aucun utilisateur actif avec une empreinte faciale n'a été trouvé.",
                        null,
                        captureResult.faceCount,
                        null,
                        null,
                        null,
                        null,
                        null
                );
            }

            // Step 3: Try to recognize the captured face against all candidates
            FaceRecognitionResult recognitionResult = recognizeFaceAgainstCandidates(candidates);
            if (!recognitionResult.success) {
                return FaceLoginResult.failure(
                        recognitionResult.message,
                        null,
                        recognitionResult.faceCount,
                        recognitionResult.userId,
                        recognitionResult.email,
                        recognitionResult.confidence,
                        recognitionResult.distance,
                        recognitionResult.name
                );
            }

            // Step 4: Get the matched user from database
            User matchedUser = null;
            if (recognitionResult.userId != null) {
                matchedUser = userDAO.findById(recognitionResult.userId);
            }
            if (matchedUser == null && recognitionResult.email != null && !recognitionResult.email.isBlank()) {
                matchedUser = userDAO.findByEmail(recognitionResult.email);
            }

            if (matchedUser == null) {
                return FaceLoginResult.failure(
                        "Le visage reconnu ne correspond plus à aucun compte existant.",
                        null,
                        recognitionResult.faceCount,
                        recognitionResult.userId,
                        recognitionResult.email,
                        recognitionResult.confidence,
                        recognitionResult.distance,
                        recognitionResult.name
                );
            }

            // Step 5: Verify the account is enabled
            if (!matchedUser.isEnabled()) {
                return FaceLoginResult.failure(
                        "Le compte associé à ce visage est désactivé.",
                        null,
                        recognitionResult.faceCount,
                        matchedUser.getIduser(),
                        matchedUser.getEmail(),
                        recognitionResult.confidence,
                        recognitionResult.distance,
                        matchedUser.getName()
                );
            }

            return FaceLoginResult.success(
                    matchedUser,
                    null,
                    recognitionResult.faceCount,
                    recognitionResult.confidence,
                    recognitionResult.distance,
                    recognitionResult.message
            );
        } catch (Exception e) {
            return FaceLoginResult.failure("Face login failed: " + friendlyMessage(e.getMessage()), null, 0, null, null, null, null, null);
        }
    }

     private FaceCaptureResult captureFaceImage() throws IOException, InterruptedException {
         List<String> command = List.of(
                 PYTHON_EXECUTABLE,
                 HUMAN_VERIFICATION_SCRIPT.toString(),
                 "webcam",
                 String.valueOf(CAPTURE_DURATION_SECONDS)
         );

         System.out.println("[DEBUG] Launching Python human verification: " + String.join(" ", command));

         ProcessOutcome processOutcome = runProcess(command, Duration.ofSeconds(CAPTURE_DURATION_SECONDS + 30L));
         System.out.println("[DEBUG] Python human verification JSON output: " + processOutcome.stdout);

         HumanVerificationResponse response = parseResponse(processOutcome.stdout, HumanVerificationResponse.class, processOutcome.stderr);
         boolean success = response.success;
         return new FaceCaptureResult(
                 success,
                 success ? response.message : friendlyMessage(response.message),
                 null,
                 response.face_count != null ? response.face_count : 0
         );
     }

     private FaceRecognitionResult recognizeFaceAgainstCandidates(List<User> candidates) throws IOException, InterruptedException {
         for (User candidate : candidates) {
             if (candidate.getFaceEmbedding() == null || candidate.getFaceEmbedding().isBlank()) {
                 continue;
             }

             try {
                 // Create a temporary file to store the candidate embedding
                 Path embeddingPath = createTempEmbeddingFile(candidate.getFaceEmbedding());
                 try {
                     FaceRecognitionResult result = verifyFaceAgainstEmbedding(embeddingPath, candidate);
                     if (result.success) {
                         return result;
                     }
                 } finally {
                     deleteQuietly(embeddingPath);
                 }
             } catch (Exception e) {
                 // Continue with next candidate if this one fails
                 System.out.println("[DEBUG] Failed to verify against candidate " + candidate.getIduser() + ": " + e.getMessage());
             }
         }

         return new FaceRecognitionResult(
                 false,
                 "Aucun visage correspondant n'a été trouvé parmi les utilisateurs enregistrés.",
                 0, null, null, null, 0.0d, Double.POSITIVE_INFINITY, FACE_MATCH_THRESHOLD, candidates.size()
         );
     }

     private FaceRecognitionResult verifyFaceAgainstEmbedding(Path embeddingPath, User candidate) throws IOException, InterruptedException {
         List<String> command = List.of(
                 PYTHON_EXECUTABLE,
                 FACE_RECOGNITION_SCRIPT.toString(),
                 "verify",
                 embeddingPath.toString(),
                 String.valueOf(CAPTURE_DURATION_SECONDS),
                 String.valueOf(FACE_MATCH_THRESHOLD)
         );

         System.out.println("[DEBUG] Launching Python face verification against user " + candidate.getIduser() + ": " + String.join(" ", command));

         ProcessOutcome processOutcome = runProcess(command, Duration.ofSeconds(CAPTURE_DURATION_SECONDS + 45L));
         System.out.println("[DEBUG] Python face verification JSON output: " + processOutcome.stdout);

         FaceRecognitionResponse response = parseResponse(processOutcome.stdout, FaceRecognitionResponse.class, processOutcome.stderr);

         // If successful, include the candidate information
         if (response.success) {
             response.user_id = candidate.getIduser();
             response.email = candidate.getEmail();
             response.name = candidate.getName();
         }

         return new FaceRecognitionResult(
                 response.success,
                 response.message,
                 response.face_count != null ? response.face_count : 0,
                 response.user_id,
                 response.email,
                 response.name,
                 response.confidence != null ? response.confidence : 0.0d,
                 response.distance != null ? response.distance : Double.POSITIVE_INFINITY,
                 response.threshold != null ? response.threshold : FACE_MATCH_THRESHOLD,
                 response.candidate_count != null ? response.candidate_count : 1
         );
     }

     private FaceRecognitionResult recognizeFace(Path imagePath, Path candidatesPath) throws IOException, InterruptedException {
        List<String> command = List.of(
                PYTHON_EXECUTABLE,
                FACE_RECOGNITION_SCRIPT.toString(),
                "verify",
                "--image",
                imagePath.toString(),
                "--candidates",
                candidatesPath.toString(),
                "--threshold",
                String.valueOf(FACE_MATCH_THRESHOLD)
        );

        ProcessOutcome processOutcome = runProcess(command, Duration.ofSeconds(45));
        FaceRecognitionResponse response = parseResponse(processOutcome.stdout, FaceRecognitionResponse.class, processOutcome.stderr);

        return new FaceRecognitionResult(
                response.success,
                response.message,
                response.face_count != null ? response.face_count : 0,
                response.user_id,
                response.email,
                response.name,
                response.confidence != null ? response.confidence : 0.0d,
                response.distance != null ? response.distance : Double.POSITIVE_INFINITY,
                response.threshold != null ? response.threshold : FACE_MATCH_THRESHOLD,
                response.candidate_count != null ? response.candidate_count : 0
        );
    }

     private Path writeCandidatesFile(List<User> candidates) throws IOException {
         List<CandidatePayload> payload = new ArrayList<>();
         for (User user : candidates) {
             if (user == null || user.getFaceEmbedding() == null || user.getFaceEmbedding().isBlank()) {
                 continue;
             }

             try {
                 double[] embedding = GSON.fromJson(user.getFaceEmbedding(), double[].class);
                 if (embedding == null || embedding.length == 0) {
                     continue;
                 }
                 payload.add(new CandidatePayload(user.getIduser(), user.getEmail(), user.getName(), embedding));
             } catch (Exception ignored) {
                 // Skip malformed embeddings and continue with the rest.
             }
         }

         if (payload.isEmpty()) {
             throw new IOException("Aucune empreinte faciale valide n'est disponible pour la comparaison.");
         }

         Path tempFile = Files.createTempFile("smarttask-face-candidates-", ".json");
         Files.writeString(tempFile, GSON.toJson(payload), StandardCharsets.UTF_8);
         return tempFile;
     }

     /**
      * Enroll a new face for the given user.
      * Captures a face from the webcam and stores it as a JSON embedding in the database.
      *
      * @param userId the ID of the user enrolling their face
      * @return EnrollmentResult containing success status and embedding data or error message
      */
     public EnrollmentResult enrollFace(int userId) {
         try {
             ensureScriptExists(FACE_RECOGNITION_SCRIPT, "face recognition");

             List<String> command = List.of(
                     PYTHON_EXECUTABLE,
                     FACE_RECOGNITION_SCRIPT.toString(),
                     "enroll",
                     String.valueOf(CAPTURE_DURATION_SECONDS)
             );

             System.out.println("[DEBUG] Launching Python face enrollment: " + String.join(" ", command));

             ProcessOutcome processOutcome = runProcess(command, Duration.ofSeconds(CAPTURE_DURATION_SECONDS + 45L));
             System.out.println("[DEBUG] Python face enrollment JSON output: " + processOutcome.stdout);

             FaceEnrollmentResponse response = parseResponse(processOutcome.stdout, FaceEnrollmentResponse.class, processOutcome.stderr);

             if (!response.success) {
                 return new EnrollmentResult(false, friendlyMessage(response.message), null);
             }

             if (response.embedding == null || response.embedding.isEmpty()) {
                 return new EnrollmentResult(false, "Le visage n'a pas produit d'empreinte valide.", null);
             }

             String embeddingJson = GSON.toJson(response.embedding);

             // Save embedding to database
             if (!userDAO.saveFaceEmbedding(userId, embeddingJson)) {
                 return new EnrollmentResult(false, "Impossible de sauvegarder l'empreinte faciale dans la base de données.", null);
             }

             return new EnrollmentResult(true, "Visage enregistré avec succès!", embeddingJson);
         } catch (IOException e) {
             return new EnrollmentResult(false, "Erreur d'entrée/sortie lors de l'enregistrement du visage: " + friendlyMessage(e.getMessage()), null);
         } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
             return new EnrollmentResult(false, "Le processus d'enregistrement du visage a été interrompu.", null);
         } catch (Exception e) {
             return new EnrollmentResult(false, "Erreur inattendue lors de l'enregistrement du visage: " + friendlyMessage(e.getMessage()), null);
         }
     }

     /**
      * Helper class for face enrollment results
      */
     public static final class EnrollmentResult {
         private final boolean success;
         private final String message;
         private final String embeddingJson;

         public EnrollmentResult(boolean success, String message, String embeddingJson) {
             this.success = success;
             this.message = message;
             this.embeddingJson = embeddingJson;
         }

         public boolean isSuccess() {
             return success;
         }

         public String getMessage() {
             return message;
         }

         public String getEmbeddingJson() {
             return embeddingJson;
         }
     }

     private ProcessOutcome runProcess(List<String> command, Duration timeout) throws IOException, InterruptedException {
         System.out.println("[DEBUG] Running Python process: " + String.join(" ", command));
         
         ProcessBuilder processBuilder = new ProcessBuilder(command);
         processBuilder.redirectErrorStream(true);  // Redirect stderr to stdout to capture all output
         Process process = processBuilder.start();

         // Read streams in background threads to avoid deadlock
         Future<String> stdoutFuture = IO_EXECUTOR.submit(() -> readStream(process.getInputStream()));
         // Since we redirectErrorStream, we don't need to read stderr separately, but keep it for safety
         Future<String> stderrFuture = IO_EXECUTOR.submit(() -> "");

         boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
         if (!finished) {
             process.destroyForcibly();
             System.err.println("[ERROR] Python process timeout after " + timeout.toMillis() + "ms");
             throw new IOException("Le script Python a dépassé le délai autorisé.");
         }

         int exitCode = process.exitValue();
         String stdout = getFuture(stdoutFuture);
         String stderr = getFuture(stderrFuture);
         
         System.out.println("[DEBUG] Process exit code: " + exitCode);
         System.out.println("[DEBUG] Process output length: " + stdout.length());
         if (stdout.length() > 200) {
             System.out.println("[DEBUG] Process output (first 200 chars): " + stdout.substring(0, 200));
         } else {
             System.out.println("[DEBUG] Process output: " + stdout);
         }
         
         return new ProcessOutcome(exitCode, stdout, stderr);
     }

    private static String readStream(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
    }

    private static String getFuture(Future<String> future) throws IOException {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new IOException("Unable to read Python process output.", e.getCause());
        } catch (Exception e) {
            throw new IOException("Unable to read Python process output.", e);
        }
    }

    private static <T> T parseResponse(String stdout, Class<T> type, String stderr) throws IOException {
        if (stdout == null || stdout.isBlank()) {
            throw new IOException(stderr == null || stderr.isBlank() ? "Le script Python n'a renvoyé aucune sortie JSON." : stderr);
        }

        // Extract the JSON line (first line that starts with '{' or '[')
        String[] lines = stdout.split("\\r?\\n");
        String jsonLine = null;
        for (String l : lines) {
            if (l == null) continue;
            String t = l.trim();
            if (t.startsWith("{") || t.startsWith("[")) {
                jsonLine = t;
                break;
            }
        }

        if (jsonLine == null) {
            // No JSON-looking line, include full output in exception
            throw new IOException("Impossible de trouver une ligne JSON dans la sortie du script Python: " + (stderr == null || stderr.isBlank() ? stdout : stdout + " | stderr: " + stderr));
        }

        try {
            T response = GSON.fromJson(jsonLine, type);
            if (response == null) {
                throw new IOException("Réponse JSON vide.");
            }
            return response;
        } catch (Exception e) {
            throw new IOException("Impossible de parser la réponse JSON du script Python: " + jsonLine + (stderr == null || stderr.isBlank() ? "" : " | stderr: " + stderr), e);
        }
    }

     private static Path createTempImagePath() throws IOException {
         return Files.createTempFile("smarttask-face-capture-", ".jpg");
     }

     private static Path createTempEmbeddingFile(String embeddingJson) throws IOException {
         Path tempFile = Files.createTempFile("smarttask-face-embedding-", ".json");
         Files.writeString(tempFile, embeddingJson, StandardCharsets.UTF_8);
         return tempFile;
     }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best effort cleanup only.
        }
    }

    private static void ensureScriptExists(Path scriptPath, String label) throws IOException {
        if (!Files.exists(scriptPath)) {
            throw new IOException("Le script Python de " + label + " est introuvable: " + scriptPath);
        }
    }

    private static String friendlyMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Une erreur inattendue est survenue.";
        }

        return message.startsWith("ERROR:") ? message.substring("ERROR:".length()).trim() : message.trim();
    }

    private static String readConfig(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }

        value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }

        return defaultValue;
    }

    private static Path resolveScript(String key, String fallbackFileName) {
        String configured = readConfig(key, null);
        Path path = configured != null ? Paths.get(configured) : Paths.get(System.getProperty("user.dir"), fallbackFileName);
        return path.toAbsolutePath().normalize();
    }

    private static int readIntConfig(String key, int defaultValue) {
        String value = readConfig(key, null);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static double readDoubleConfig(String key, double defaultValue) {
        String value = readConfig(key, null);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public static final class FaceLoginResult {
        private final boolean success;
        private final String message;
        private final User user;
        private final Path captureImagePath;
        private final int faceCount;
        private final Integer matchedUserId;
        private final String matchedEmail;
        private final Double confidence;
        private final Double distance;
        private final String matchedName;

        private FaceLoginResult(boolean success, String message, User user, Path captureImagePath, int faceCount,
                                Integer matchedUserId, String matchedEmail, Double confidence, Double distance,
                                String matchedName) {
            this.success = success;
            this.message = message;
            this.user = user;
            this.captureImagePath = captureImagePath;
            this.faceCount = faceCount;
            this.matchedUserId = matchedUserId;
            this.matchedEmail = matchedEmail;
            this.confidence = confidence;
            this.distance = distance;
            this.matchedName = matchedName;
        }

        public static FaceLoginResult success(User user, Path captureImagePath, int faceCount, double confidence, double distance, String message) {
            return new FaceLoginResult(true, message, user, captureImagePath, faceCount, user.getIduser(), user.getEmail(), confidence, distance, user.getName());
        }

        public static FaceLoginResult failure(String message, Path captureImagePath, int faceCount, Integer matchedUserId,
                                              String matchedEmail, Double confidence, Double distance, String matchedName) {
            return new FaceLoginResult(false, message, null, captureImagePath, faceCount, matchedUserId, matchedEmail, confidence, distance, matchedName);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }

        public Path getCaptureImagePath() {
            return captureImagePath;
        }

        public int getFaceCount() {
            return faceCount;
        }

        public Integer getMatchedUserId() {
            return matchedUserId;
        }

        public String getMatchedEmail() {
            return matchedEmail;
        }

        public Double getConfidence() {
            return confidence;
        }

        public Double getDistance() {
            return distance;
        }

        public String getMatchedName() {
            return matchedName;
        }
    }

    public static final class FaceCaptureResult {
        private final boolean success;
        private final String message;
        private final Path imagePath;
        private final int faceCount;

        private FaceCaptureResult(boolean success, String message, Path imagePath, int faceCount) {
            this.success = success;
            this.message = message;
            this.imagePath = imagePath;
            this.faceCount = faceCount;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Path getImagePath() {
            return imagePath;
        }

        public int getFaceCount() {
            return faceCount;
        }
    }

    public static final class FaceRecognitionResult {
        private final boolean success;
        private final String message;
        private final int faceCount;
        private final Integer userId;
        private final String email;
        private final String name;
        private final Double confidence;
        private final Double distance;
        private final Double threshold;
        private final Integer candidateCount;

        private FaceRecognitionResult(boolean success, String message, int faceCount, Integer userId, String email,
                                      String name, Double confidence, Double distance, Double threshold, Integer candidateCount) {
            this.success = success;
            this.message = message;
            this.faceCount = faceCount;
            this.userId = userId;
            this.email = email;
            this.name = name;
            this.confidence = confidence;
            this.distance = distance;
            this.threshold = threshold;
            this.candidateCount = candidateCount;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getFaceCount() {
            return faceCount;
        }

        public Integer getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }

        public Double getConfidence() {
            return confidence;
        }

        public Double getDistance() {
            return distance;
        }

        public Double getThreshold() {
            return threshold;
        }

        public Integer getCandidateCount() {
            return candidateCount;
        }
    }

    private static final class CandidatePayload {
        private final int user_id;
        private final String email;
        private final String name;
        private final double[] face_embedding;

        private CandidatePayload(int userId, String email, String name, double[] faceEmbedding) {
            this.user_id = userId;
            this.email = email;
            this.name = name;
            this.face_embedding = faceEmbedding;
        }
    }

    private static final class ProcessOutcome {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        private ProcessOutcome(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }

    private static final class HumanVerificationResponse {
        private boolean success;
        private String message;
        private Integer face_count;
        private String image_path;
        private String captured_image_path;
    }

     private static final class FaceRecognitionResponse {
         private boolean success;
         private String message;
         private Integer face_count;
         private Integer user_id;
         private String email;
         private String name;
         private Double confidence;
         private Double distance;
         private Double threshold;
         private Integer candidate_count;
     }

     private static final class FaceEnrollmentResponse {
         private boolean success;
         private String message;
         private java.util.List<Double> embedding;
     }
}


