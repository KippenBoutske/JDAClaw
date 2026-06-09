package dev.kippenboutske.listeners;

import dev.kippenboutske.tools.FileTools;
import dev.kippenboutske.tools.HomeAssistantTools;
import io.github.ollama4j.Ollama;
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

                String systemPrompt = "You are an AI assistant based on 2 files: 'HEART.md' (capabilities) and 'SOUL.md' (personality). " +
                        "Contents of HEART: " + Files.readString(Path.of("data/HEART.md")) +
                        " | Contents of SOUL: " + Files.readString(Path.of("data/SOUL.md")) +
                        " | User: " + event.getMember().getEffectiveName() +
                        ". TASK: Assist the user. You MUST use the 'write_file' tool to save important info to " + event.getAuthor().getEffectiveName() + "_Notes.md";

                OllamaChatRequest builder = OllamaChatRequest.builder()
                        .withModel("gemma4:e4b")
                        .withUseTools(true)
                        .withMessage(OllamaChatMessageRole.SYSTEM, systemPrompt);

                for (Message message : history) {
                    OllamaChatMessageRole role = message.getAuthor().isBot() ? OllamaChatMessageRole.ASSISTANT : OllamaChatMessageRole.USER;
                    builder.withMessage(role, message.getContentRaw());
                }

                builder.withMessage(OllamaChatMessageRole.USER, event.getMessage().getContentRaw());

                OllamaChatRequest requestModel = builder.build();
                requestModel.setThink(ThinkMode.DISABLED);



                var chatResult = ollama.chat(requestModel, null);
                String response = chatResult.getResponseModel().getMessage().getResponse();

                event.getMessage().reply(response).queue();

                event.getMessage().removeReaction(Emoji.fromUnicode("\uD83D\uDC40")).queue();
                event.getMessage().addReaction(Emoji.fromFormatted("✅")).queue();

            } catch (Exception e) {
                event.getMessage().reply("Fout bij het genereren van antwoord: " + e.getMessage()).queue();
                e.printStackTrace();
            }
        });
    }
}
