package rsystems.commands.adminCommands;

import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import rsystems.Config;
import rsystems.HiveBot;
import rsystems.objects.Command;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmojiWhitelist extends Command {
    @Override
    public Integer getPermissionIndex() {
        return 512;
    }

    @Override
    public void dispatch(User sender, MessageChannel channel, Message message, String content, PrivateMessageReceivedEvent event) {

    }

    @Override
    public void dispatch(User sender, MessageChannel channel, Message message, String content, GuildMessageReceivedEvent event) throws SQLException {
        String[] args = content.split("\\s+");
        if((args != null) && (args.length >= 1)){

            if(args[0].equalsIgnoreCase("add")){
                if(args.length >= 2){
                    Long associatedRole = Long.valueOf(args[1]);
                    if(HiveBot.mainGuild().getRoleById(associatedRole) != null) {

                        List<String> emojiList = EmojiParser.extractEmojis(content);

                        if(emojiList.isEmpty()){
                            reply(event,"No compatible emoji's found");
                        } else {
                            for (String emoji : emojiList) {
                                System.out.println(EmojiParser.parseToAliases(emoji));

                                if (HiveBot.sqlHandler.addEmojiToWhitelist(associatedRole, EmojiParser.parseToAliases(emoji))) {
                                    if (HiveBot.emojiPerkMap.get(associatedRole) != null) {
                                        HiveBot.emojiPerkMap.get(associatedRole).add(emoji);
                                    } else {
                                        HiveBot.emojiPerkMap.put(associatedRole, new ArrayList<String>());
                                        HiveBot.emojiPerkMap.get(associatedRole).add(emoji);
                                    }
                                }
                            }
                        }
                    }
                }

                return;
            }

            if(args[0].equalsIgnoreCase("remove")){
                Long associatedRole = Long.valueOf(args[1]);

                for(String emoji:EmojiParser.extractEmojis(content)){
                    try {
                        HiveBot.sqlHandler.removeEmojiFromWhitelist(associatedRole, EmojiParser.parseToAliases(emoji));
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }
                message.addReaction("✅ ").queue();
            }

            if(args[0].equalsIgnoreCase("list")){
                EmbedBuilder embedBuilder = new EmbedBuilder();

                for(Map.Entry<Long, ArrayList<String>> entry:HiveBot.emojiPerkMap.entrySet()){

                    Role role = HiveBot.mainGuild().getRoleById(entry.getKey());
                    if(role != null) {
                        embedBuilder.appendDescription("**Role:**  " + role.getName() + "\n");
                        embedBuilder.appendDescription("**ID:**  " + entry.getKey() + "\n");

                        StringBuilder emojiString = new StringBuilder();

                        for (String emoji : entry.getValue()) {
                            emojiString.append(EmojiParser.parseToUnicode(emoji)).append("......").append("`").append(emoji).append("`\n");
                        }

                        embedBuilder.appendDescription(emojiString.toString()).appendDescription("\n");
                    }

                }

                reply(event,embedBuilder.build());
                embedBuilder.clear();
            }

        }
    }

    @Override
    public String getHelp() {

        String returnString ="`{prefix}{command} [Sub-Command] [args]`\n\n" +
        "**Add**\n`{prefix}{command} add [RoleID] [Emoji(s)]`\nThis will add all emojis found, to the whitelist for the role.\n\n"+
        "**Remove**\n`{prefix}{command} remove [RoleID]`\nThis will remove all emojis from the whitelist for a role.\n\n"+
        "**List**\n`{prefix}{command} list`\nThis will list all emojis found on the whitelist.\n";

        returnString = returnString.replaceAll("\\{prefix}", Config.get("prefix"));
        returnString = returnString.replaceAll("\\{command}",this.getName());
        return returnString;
    }

    private boolean subCommand_AddEmoji(String content) throws SQLException {
        boolean output = false;

        String[] args = content.split("\\s+");
        if(args.length >= 2){
            Long associatedRole = Long.valueOf(args[0]);
            if(HiveBot.mainGuild().getRoleById(associatedRole) != null) {

                List<String> emojiList = EmojiParser.extractEmojis(content);

                for (String emoji : emojiList) {
                    System.out.println(EmojiParser.parseToAliases(emoji));

                    if(HiveBot.sqlHandler.addEmojiToWhitelist(associatedRole,EmojiParser.parseToAliases(emoji))){
                        if(HiveBot.emojiPerkMap.get(associatedRole) != null){
                            HiveBot.emojiPerkMap.get(associatedRole).add(emoji);
                        } else {
                            HiveBot.emojiPerkMap.put(associatedRole,new ArrayList<String>());
                            HiveBot.emojiPerkMap.get(associatedRole).add(emoji);
                        }
                        output = true;
                    }
                }
            }
        }
        return output;
    }

    @Override
    public String getName() {
        return "NickEmoji";
    }
}
