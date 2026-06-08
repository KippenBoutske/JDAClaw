package dev.kippenboutske.listeners;

import dev.kippenboutske.tools.FileTools;
import dev.kippenboutske.tools.HomeAssistantTools;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.request.ThinkMode;
import io.github.ollama4j.tools.Tools;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class onMessageReceived extends ListenerAdapter {

    private final Ollama ollama = new Ollama("http://127.0.0.1:11434/");

    public onMessageReceived() {
        ollama.setRequestTimeoutSeconds(300);

        Tools.Tool readFileTool = Tools.Tool.builder()
                .toolSpec(Tools.ToolSpec.builder()
                        .name("read_file")
                        .description("Reads the content of a file from the data directory.")
                        .parameters(Tools.Parameters.of(Map.of(
                                "fileName", Tools.Property.builder()
                                        .type("string")
                                        .description("The name of the file to read (e.g., 'notes.txt')")
                                        .required(true)
                                        .build()
                        )))
                        .build())
                .toolFunction(args -> {
                    System.out.println("● | Read File used: " + args);
                    return new FileTools.ReadFileFunction("").apply(args);
                })
                .build();

        // Define Write File Tool
        Tools.Tool writeFileTool = Tools.Tool.builder()
                .toolSpec(Tools.ToolSpec.builder()
                        .name("write_file")
                        .description("Writes content to a file in the data directory. Overwrites if it exists.")
                        .parameters(Tools.Parameters.of(Map.of(
                                "fileName", Tools.Property.builder()
                                        .type("string")
                                        .description("The name of the file to write to")
                                        .required(true)
                                        .build(),
                                "content", Tools.Property.builder()
                                        .type("string")
                                        .description("The content to write into the file")
                                        .required(true)
                                        .build()
                        )))
                        .build())
                .toolFunction(args -> {
                    System.out.println("● | Write File used: " + args);
                    return new FileTools.WriteFileFunction("", "").apply(args);
                })
                .build();

        // Register them to the Ollama instance
        ollama.registerTool(readFileTool);
        ollama.registerTool(writeFileTool);

        // Define List Files Tool
        Tools.Tool listFilesTool = Tools.Tool.builder()
                .toolSpec(Tools.ToolSpec.builder()
                        .name("list_files")
                        .description("Lists all files in the data directory.")
                        .parameters(Tools.Parameters.of(Map.of()))
                        .build())
                .toolFunction(args -> {
                    System.out.println("● | List files used...");
                    return new FileTools.ListFilesFunction().apply(args);
                })
                .build();

        ollama.registerTool(listFilesTool);

        Tools.Tool listHAEntities = Tools.Tool.builder()
                .toolSpec(Tools.ToolSpec.builder()
                        .name("list_ha_entities")
                        .description("Lists all Home Assistant entities.")
                        .parameters(Tools.Parameters.of(Map.of()))
                        .build())
                .toolFunction(args -> {
                    System.out.println("● | List HA entities used...");
                    return new HomeAssistantTools.ListEntitiesFunction();
                })
                .build();

        ollama.registerTool(listHAEntities);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.getChannel().asTextChannel().getId().equals("1504192183786410165")) return;

        event.getChannel().sendTyping().queue();
        event.getMessage().addReaction(Emoji.fromUnicode("\uD83D\uDC40")).queue();

        CompletableFuture.runAsync(() -> {
            try {
                List<Message> history = event.getChannel().getHistoryBefore(event.getMessageId(), 15).complete().getRetrievedHistory().reversed();

                String systemPrompt = "You are an AI assistant based on 2 files: 'HEART.md' and 'SOUL.md', HEART tells you what you can do, SOUL tells you who you are. " +
                        "Here are the contents of HEART: " + Files.readString(Path.of("data/HEART.md")) +
                        " | Here are the contents of SOUL.md: " + Files.readString(Path.of("data/SOUL.md")) +
                        " | You are now chatting with the user you are designed to assist: " + event.getAuthor().getEffectiveName() +
                        ". If you need to use a tool, output ONLY the tool call. Do not explain what you are doing first, and do not confirm it is done until you receive the tool execution result.";

                // 1. Create a List to hold the entire conversation flow
                List<OllamaChatMessage> chatHistory = new java.util.ArrayList<>();

                // 2. Add System Prompt
                chatHistory.add(new OllamaChatMessage(OllamaChatMessageRole.SYSTEM, systemPrompt));

                // 3. Add Discord History
                for (Message message : history) {
                    OllamaChatMessageRole role = message.getAuthor().isBot() ? OllamaChatMessageRole.ASSISTANT : OllamaChatMessageRole.USER;
                    chatHistory.add(new OllamaChatMessage(role, message.getContentRaw()));
                }

                // 4. Add the current User Message
                chatHistory.add(new OllamaChatMessage(OllamaChatMessageRole.USER, event.getMessage().getContentRaw()));

                // 5. Build the FIRST request using the list
                OllamaChatRequest requestModel = OllamaChatRequest.builder()
                        .withModel("ssfdre38/gemma4-turbo:e4b")
                        .withMessages(chatHistory) // Pass the entire list here
                        .build();
                requestModel.setThink(ThinkMode.DISABLED);

                // 6. Send to Ollama
                var chatResult = ollama.chat(requestModel, null);
                var aiMessage = chatResult.getResponseModel().getMessage();

                // 7. Check for tools
                if (aiMessage.getToolCalls() != null && !aiMessage.getToolCalls().isEmpty()) {

                    // Add the AI's tool call directly to our history list! (This fixes your error)
                    chatHistory.add(aiMessage);

                    for (var toolCall : aiMessage.getToolCalls()) {
                        String functionName = toolCall.getFunction().getName();
                        Map<String, Object> args = toolCall.getFunction().getArguments();
                        String executionResult = "Error: Tool not found";

                        if (functionName.equals("write_file")) {
                            System.out.println("● | Executing Write File...");
                            executionResult = (String) new FileTools.WriteFileFunction("", "").apply(args);
                        } else if (functionName.equals("read_file")) {
                            System.out.println("● | Executing Read File...");
                            executionResult = (String) new FileTools.ReadFileFunction("").apply(args);
                        } else if (functionName.equals("list_files")) {
                            System.out.println("● | Executing List Files...");
                            executionResult = (String) new FileTools.ListFilesFunction().apply(args);
                        } else if (functionName.equals("list_ha_entities")) {
                            System.out.println("● | Executing HA Entities...");
                            executionResult = (String) new HomeAssistantTools.ListEntitiesFunction().apply(args);
                        }

                        // 8. Add the Java execution result back into the history list as a TOOL
                        chatHistory.add(new OllamaChatMessage(OllamaChatMessageRole.TOOL, executionResult));
                    }

                    // 9. Build the SECOND request with the updated list (User -> AI Tool Call -> Tool Result)
                    OllamaChatRequest secondRequest = OllamaChatRequest.builder()
                            .withModel("ssfdre38/gemma4-turbo:e4b")
                            .withMessages(chatHistory) // Pass the updated list
                            .build();
                    secondRequest.setThink(ThinkMode.DISABLED);

                    var finalResult = ollama.chat(secondRequest, null);
                    String finalResponse = finalResult.getResponseModel().getMessage().getResponse();

                    // 10. Send the final, verified answer to Discord
                    event.getMessage().reply(finalResponse).queue();

                } else {
                    // Fallback: No tools used, reply normally
                    event.getMessage().reply(aiMessage.getResponse()).queue();
                }

                event.getMessage().removeReaction(Emoji.fromUnicode("\uD83D\uDC40")).queue();
                event.getMessage().addReaction(Emoji.fromFormatted("✅")).queue();

            } catch (Exception e) {
                event.getMessage().reply("Fout bij het genereren van antwoord: " + e.getMessage()).queue();
                e.printStackTrace();
            }
    });
}}
