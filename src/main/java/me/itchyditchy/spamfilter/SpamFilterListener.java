package me.itchyditchy.spamfilter;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpamFilterListener extends ListenerAdapter {

    private boolean toggle = true;
    private final Map<String, List<Message>> messageQueue = new HashMap<>();
    private final int spamLimit = 3;
    private final int floodLimit = 5;
    private final int span = 5*1000;
    private final double similarityLimit = 0.9;

    public SpamFilterListener () {
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        if (!event.getName().equalsIgnoreCase("spamfiltertoggle")) return;
        if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.MANAGE_CHANNEL)) {
            event.reply("You do not have enough permissions! // can be customized to a boolean if ever.").setEphemeral(true).queue();
        }
        toggle = !toggle;
        event.reply("Spam Filter has been " + (toggle ? "enabled" : "disabled") + ".");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        queue(event.getMessage(), event.getAuthor());
    }

    private void queue(Message message, User user) {
        String id = user.getId() + ";" + message.getChannel();
        List<Message> messages = messageQueue.getOrDefault(id, new ArrayList<>());
        if (messages.size() > 10)
            messages.remove(0);
        messages.add(message);
        messageQueue.put(id, messages);
        if (messages.size() < 2)
            return;
        if (!checkSpam(message, user) && !checkSimilarity(message, messages.get(messages.size() - 2)))
            return;
        message.delete().queue();
        // Log / warn the member if needed.
    }

    private boolean checkSpam(Message message, User user) {
        String id = user.getId() + ";" + message.getChannel();
        int count = 0;
        List<Message> messages = messageQueue.get(id);
        if (messages.size() <= 5)
            return false;
        Message prevMsg = null;
        for (int i = messages.size() - 5; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (prevMsg == null) {
                prevMsg = msg;
                continue;
            }
            if (msg.getContentStripped().equalsIgnoreCase(prevMsg.getContentStripped()))
                count++;
        }
        return count > spamLimit;
    }

    private boolean checkSimilarity(Message message, Message preMessage) {
        return message.getContentStripped().length() > 12 && 0.9 <= getSimilarity(message, preMessage);
    }

    private double getSimilarity(Message message, Message preMessage) {
        if (message.getContentStripped().length() <= 12) return 0d;
        double distance = LevenshteinDistance.getDefaultInstance().apply(message.getContentStripped(), preMessage.getContentStripped());
        return 1d - (distance / (double) Math.max(message.getContentStripped().length(), preMessage.getContentStripped().length()));
    }
}
