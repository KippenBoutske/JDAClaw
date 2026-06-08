package dev.kippenboutske.tools;

import io.github.ollama4j.tools.ToolFunction;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class FileTools {

    private static final Path rootPath = Paths.get("data");

    static {
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ReadFileFunction implements ToolFunction {
        public ReadFileFunction(String fileName) {
        }

        @Override
        public Object apply(Map<String, Object> arguments) {
            String fileName = (String) arguments.get("fileName");
            try {
                Path filePath = rootPath.resolve(fileName);
                if (!Files.exists(filePath)) {
                    return "Error: File '" + fileName + "' does not exist.";
                }
                return Files.readString(filePath);
            } catch (IOException e) {
                return "Error reading file: " + e.getMessage();
            }
        }
    }

    public static class WriteFileFunction implements ToolFunction {
        public WriteFileFunction(String fileName, String content) {
        }

        @Override
        public Object apply(Map<String, Object> arguments) {
            String fileName = (String) arguments.get("fileName");
            String content = (String) arguments.get("content");
            try {
                Path filePath = rootPath.resolve(fileName);
                Files.writeString(filePath, content);
                return "Successfully wrote to '" + fileName + "'.";
            } catch (IOException e) {
                return "Error writing file: " + e.getMessage();
            }
        }
    }

    public static class ListFilesFunction implements ToolFunction {
        @Override
        public Object apply(Map<String, Object> arguments) {
            try (var stream = Files.list(rootPath)) {
                StringBuilder sb = new StringBuilder();
                stream.forEach(path -> sb.append(path.getFileName()).append("\n"));
                if (sb.length() == 0) {
                    return "No files found in the data directory.";
                }
                return "Files in data directory:\n" + sb.toString().trim();
            } catch (IOException e) {
                return "Error listing files: " + e.getMessage();
            }
        }
    }
}
