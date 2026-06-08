package dev.kippenboutske.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class neosleep extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("neosleep")) {
            File memory = new File("data/" + event.getMember().getEffectiveName() + "_Memory.MD");

            if (!memory.exists()) {
                try {
                    memory.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                FileWriter write = new FileWriter("data/" + event.getMember().getEffectiveName() + "_Memory.MD");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
