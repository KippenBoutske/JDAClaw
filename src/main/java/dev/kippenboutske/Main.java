import dev.kippenboutske.listeners.onMessageReceived;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public static void main(String[] arguments) throws Exception
{
    Dotenv env = Dotenv.load();

    JDA api = JDABuilder.createDefault(env.get("TOKEN"))
            .addEventListeners(
                    new onMessageReceived()

            )
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .build();

}