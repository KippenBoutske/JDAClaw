package dev.kippenboutske;

import dev.kippenboutske.commands.compile;
import dev.kippenboutske.commands.sleep;
import dev.kippenboutske.listeners.onMessageReceived;
import dev.kippenboutske.managers.slashCommandManager;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {
    public static void main(String[] arguments) throws Exception {
        Dotenv env = Dotenv.load();

        JDA api = JDABuilder.createDefault(env.get("TOKEN"))
                .addEventListeners(
                        new onMessageReceived(),
                        new slashCommandManager(),
                        new compile(),
                        new sleep()
                )
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();
    }
}
