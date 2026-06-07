package dev.kippenboutske.commands;

import dev.kippenboutske.tools.FileTools;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.models.request.ThinkMode;
import io.github.ollama4j.tools.Tools;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jspecify.annotations.NonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class sleep extends ListenerAdapter {

    private final Ollama ollama = new Ollama("http://127.0.0.1:11434/");

    public sleep() {
        ollama.setRequestTimeoutSeconds(300);

        // Define Read File Tool - Fixed path to "data/"
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
                    System.out.println("● | Tool Triggered: read_file -> " + args);
                    return new FileTools.ReadFileFunction("data/").apply(args);
                })
                .build();

        // Define Write File Tool - Fixed path to "data/"
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
                    System.out.println("● | Tool Triggered: write_file -> " + args);
                    return new FileTools.WriteFileFunction("data/", "").apply(args);
                })
                .build();

        Tools.Tool listFilesTool = Tools.Tool.builder()
                .toolSpec(Tools.ToolSpec.builder()
                        .name("list_files")
                        .description("Lists all files in the data directory.")
                        .parameters(Tools.Parameters.of(Map.of()))
                        .build())
                .toolFunction(args -> {
                    System.out.println("● | Tool Triggered: list_files");
                    return new FileTools.ListFilesFunction().apply(args);
                })
                .build();

        ollama.registerTool(readFileTool);
        ollama.registerTool(writeFileTool);
        ollama.registerTool(listFilesTool);
    }
    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("sleep")) {
            event.getJDA().getPresence().setStatus(OnlineStatus.IDLE);
            event.getJDA().getPresence().setActivity(Activity.watching("Watching my memories"));

            event.reply("Starting sleep, don't interrupt.").queue();

            CompletableFuture.runAsync(() -> {
                try {

                    String systemPrompt = "You are an AI assistant based on 2 files: 'HEART.md' (capabilities) and 'SOUL.md' (personality). " +
                            "Contents of HEART: " + Files.readString(Path.of("data/HEART.md")) +
                            " | Contents of SOUL: " + Files.readString(Path.of("data/SOUL.md")) +
                            ". TASK: List all files, read each memory file, and rewrite them to keep only important info. " +
                            "CRITICAL: Once you have finished all file operations, make sure you have overwritten the file: '(user)_Notes.md' and you MUST provide a written summary of what you did for the user.";

                    OllamaChatRequest builder = OllamaChatRequest.builder()
                            .withModel("gemma4:e4b")
                            .withMessage(OllamaChatMessageRole.SYSTEM, systemPrompt);

                    builder.setThink(ThinkMode.HIGH);

                    OllamaChatResult chatResult = ollama.chat(builder.build(), null);


                    System.out.println(chatResult.getResponseModel().getDoneReason());
                    String response = chatResult.getResponseModel().getMessage().getResponse();
                    event.getChannel().sendMessage(response).queue();

                    event.getJDA().getPresence().setActivity(Activity.watching("Watching for your next command"));
                    event.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);

                } catch (Exception e) {
                    event.getChannel().sendMessage("Failed to compile: " + e.getMessage()).queue();
                    e.printStackTrace();
                }
            });
        }
    }

}

