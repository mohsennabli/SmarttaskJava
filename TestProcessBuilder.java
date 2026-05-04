import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

/**
 * Diagnostic script to test ProcessBuilder invocation of Python face registration
 */
public class TestProcessBuilder {
    public static void main(String[] args) throws Exception {
        String pythonExecutable = "/home/mohsen-nabli/IdeaProjects/smarttask-javafx/face_env/bin/python3";
        String faceRegisterScript = "/home/mohsen-nabli/IdeaProjects/smarttask-javafx/face_register.py";
        String userId = "1";
        
        System.out.println("[DEBUG] Testing ProcessBuilder invocation...");
        System.out.println("[DEBUG] Python executable: " + pythonExecutable);
        System.out.println("[DEBUG] Script path: " + faceRegisterScript);
        System.out.println("[DEBUG] User ID: " + userId);
        
        ProcessBuilder processBuilder = new ProcessBuilder(
            pythonExecutable,
            faceRegisterScript,
            userId
        );
        processBuilder.redirectErrorStream(true);
        
        System.out.println("[DEBUG] ProcessBuilder command: " + String.join(" ", processBuilder.command()));
        
        try {
            Process process = processBuilder.start();
            
            // Read stdout
            String output = readStream(process.getInputStream());
            int exitCode = process.waitFor();
            
            System.out.println("[DEBUG] Process exit code: " + exitCode);
            System.out.println("[DEBUG] Process stdout length: " + output.length());
            System.out.println("[DEBUG] Process stdout:\n" + output);
            
            if (output.isBlank()) {
                System.out.println("[ERROR] Output is blank!");
            } else {
                System.out.println("[OK] Output received");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String readStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder output = new StringBuilder();
        String line;
        int lineCount = 0;
        while ((line = reader.readLine()) != null) {
            lineCount++;
            System.out.println("[LINE " + lineCount + "] " + line);
            output.append(line).append(System.lineSeparator());
        }
        return output.toString().trim();
    }
}

