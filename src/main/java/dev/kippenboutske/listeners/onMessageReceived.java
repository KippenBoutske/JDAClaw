package dev.kippenboutske.listeners;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.request.ThinkMode;
import io.github.ollama4j.utils.OptionsBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class onMessageReceived extends ListenerAdapter {

    // Maak de API-client één keer aan (efficiënter)
    private final Ollama ollama = new Ollama("http://127.0.0.1:11434/");

    public onMessageReceived() {
        // Verhoog de timeout standaard naar bijv. 5 minuten voor trage modellen
        ollama.setRequestTimeoutSeconds(300);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        // Toon dat de bot typt
        event.getChannel().sendTyping().queue();
        event.getMessage().addReaction(Emoji.fromUnicode("\uD83D\uDC40")).queue();

        // Start het proces in een aparte thread
        CompletableFuture.runAsync(() -> {
            try {
                // Tip: pullModel alleen doen bij opstarten, niet bij elk bericht!
                // ollama.pullModel("gemma3n:e2b");

                System.out.println(event.getMessage().getContentRaw());

                OllamaChatRequest requestModel = OllamaChatRequest.builder()
                        .withModel("gemma4:e2b")

                        .withMessage(OllamaChatMessageRole.SYSTEM, "Try to be as helpfull as possible but dont make your responses too long. You are currently chatting with the user named: " + event.getMessage().getAuthor().getEffectiveName() + ". You can use emoji's. ")
                        .withMessage(OllamaChatMessageRole.USER, event.getMessage().getContentRaw())
                        .build();

                requestModel.setThink(ThinkMode.DISABLED);

                // Gebruik de synchrone chat call binnen de async wrapper
                // Voeg 'null' toe als tweede argument voor de TokenHandler
                var chatResult = ollama.chat(requestModel, null);

                String response = chatResult.getResponseModel().getMessage().getResponse();

                // Stuur het antwoord terug naar Discord
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
