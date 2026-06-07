package dev.kippenboutske.managers;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;

public class slashCommandManager extends ListenerAdapter {
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commands = new ArrayList<>(); // Add commands here for slash commands.

        commands.add(Commands.slash("compile", "Compiles current conversation and sends it off into memory."));
        commands.add(Commands.slash("sleep", "Set JDAclaw to sleep."));


        event.getGuild().updateCommands().addCommands(commands).queue();
    }
}