package me.itchyditchy.spamfilter;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.Arrays;

public class Bot {

    private static Bot instance;
    private static JDA jda;

    public Bot() {
        if (System.getenv("TOKEN") == null) return;
        SpamFilterListener spamFilterListener = new SpamFilterListener();
        jda = JDABuilder.create(System.getenv("TOKEN"), Arrays.asList(GatewayIntent.values()))
                .addEventListeners(spamFilterListener)
                .build();
        spamFilterListener.registerCommand();
    }

    public static Bot getInstance() {
        return instance;
    }

    public static JDA getJda() {
        return jda;
    }

}
