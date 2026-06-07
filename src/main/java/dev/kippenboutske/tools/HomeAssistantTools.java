package dev.kippenboutske.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ollama4j.tools.ToolFunction;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class HomeAssistantTools {

    // PLACEHOLDERS: Replace these with your actual Home Assistant IP/URL and Long-Lived Access Token
    private static final String HA_URL = "http://192.168.0.184:8123";
    private static final String HA_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJmOGYxNjE3MjU2YmU0MjRhODZiODc0NDQ1MTRmNzI1YiIsImlhdCI6MTc3ODcwMDI5OSwiZXhwIjoyMDk0MDYwMjk5fQ.-mVOUVxnpCJgsL7uUENDyZGsTxHCxbfCWb1xAy1-2n8";

    public static class ListEntitiesFunction implements ToolFunction {
        @Override
        public Object apply(Map<String, Object> arguments) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(HA_URL + "/api/states"))
                        .header("Authorization", "Bearer " + HA_TOKEN)
                        .header("Content-Type", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    return "Error: Failed to fetch entities from Home Assistant. HTTP Status: " + response.statusCode();
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.body());

                StringBuilder sb = new StringBuilder();
                sb.append("Home Assistant Entities:\n");
                
                int count = 0;
                for (JsonNode node : rootNode) {
                    String entityId = node.has("entity_id") ? node.get("entity_id").asText() : "unknown";
                    String state = node.has("state") ? node.get("state").asText() : "unknown";
                    
                    sb.append("- ").append(entityId).append(": ").append(state).append("\n");
                    count++;
                }

                if (count == 0) {
                    return "No entities found in Home Assistant.";
                }
                System.out.println(sb.toString());
                return sb.toString().trim();

            } catch (Exception e) {
                return "Error connecting to Home Assistant: " + e.getMessage();
            }
        }
    }
}