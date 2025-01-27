package rsystems.events;

import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import rsystems.HiveBot;

import javax.annotation.Nonnull;
import javax.lang.model.element.ElementVisitor;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class NicknameListener extends ListenerAdapter {

    static List<Member> inProcess = new ArrayList<>();

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event){
        if(event.getUser().isBot()){
            return;
        }

        final Member member = event.getMember();
        if(member != null) {
            String name = member.getEffectiveName();
            String newNick = null;
            try {
                newNick = processName(member, name);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (newNick != null) {
                event.getGuild().modifyNickname(event.getMember(), newNick).reason("Nickname Parser").queue();
            }
        }
    }

    @Override
    public void onUserUpdateName(UserUpdateNameEvent event) {
        if(event.getUser().isBot()){
            return;
        }

        final Member member = HiveBot.mainGuild().getMemberById(event.getUser().getIdLong());
        if(member != null){
            if(member.getEffectiveName().equalsIgnoreCase(event.getNewName())){
                String newNick = null;
                try {
                    newNick = processName(member, event.getNewName());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if(newNick != null){
                    HiveBot.mainGuild().modifyNickname(member,newNick).reason("Nickname Parser").queue();
                }
            }
        }
    }

    @Override
    public void onUserUpdateOnlineStatus(UserUpdateOnlineStatusEvent event){
        if(event.getUser().isBot()){
            return;
        }

        final Member member = HiveBot.mainGuild().getMemberById(event.getUser().getIdLong());
        if(member != null){
            String nickname = member.getEffectiveName();
            String processedNickname = null;
            try {
                processedNickname = processName(member, nickname);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if(processedNickname != null){
                HiveBot.mainGuild().modifyNickname(member,processedNickname).reason("Nickname Parser").queue();
            }
        }
    }

    @Override
    public void onGuildMemberRoleRemove(@Nonnull GuildMemberRoleRemoveEvent event) {
        if(event.getUser().isBot()){
            return;
        }

        final Member member = HiveBot.mainGuild().getMemberById(event.getUser().getIdLong());
        if(member != null){
            String nickname = member.getEffectiveName();
            String processedNickname = null;
            try {
                processedNickname = processName(member, nickname);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if(processedNickname != null){
                HiveBot.mainGuild().modifyNickname(member,processedNickname).reason("Nickname Parser").queue();
            }
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        // Remove emoji's from username on join
        final String currentNick = event.getMember().getEffectiveName();
        if (!EmojiParser.extractEmojis(currentNick).isEmpty()) {
            if (HiveBot.mainGuild().getSelfMember().hasPermission(Permission.NICKNAME_MANAGE))
                event.getGuild().modifyNickname(event.getMember(), EmojiParser.removeAllEmojis(currentNick)).reason("Removing emoji's on join").reason("Nickname Parser").queue();
            else
                System.out.println("Emoji's found for member: " + event.getMember().getId());
        }
    }

    public static String processName(Member member, String name) throws SQLException {
        if (HiveBot.mainGuild().getSelfMember().canInteract(HiveBot.mainGuild().getMemberById(member.getIdLong()))) {

            if (!inProcess.contains(member)) {
                inProcess.add(member);

                String newName = name;

                if (newName == null)
                    return null;

                List<String> acceptedEmoji = new ArrayList<>();
                for(Role role:member.getRoles()){
                    if(HiveBot.emojiPerkMap.containsKey(role.getIdLong())){
                        acceptedEmoji.addAll(HiveBot.emojiPerkMap.get(role.getIdLong()));
                    }
                }

                String newKarmaSymbol = HiveBot.karmaSQLHandler.getKarmaSymbol(member.getId());
                if(newKarmaSymbol != null){
                    acceptedEmoji.add(newKarmaSymbol);
                }

                if (EmojiParser.extractEmojis(name).size() >= 1) {
                    for (String s : EmojiParser.extractEmojis(name)) {

                        String emojiAlias = EmojiParser.parseToAliases(s);

                        if (acceptedEmoji.contains(emojiAlias))
                            continue;
                        else
                            //System.out.println("Not allowing: " + emojiAlias);
                        newName = newName.replaceAll(s, "");

                    }
                }
                if (name.equalsIgnoreCase(newName)) {
                    //System.out.println("null");
                    inProcess.remove(member);
                    return null;
                }

                inProcess.remove(member);
                return newName;
            }
        }
        return null;
    }

    public static void handleKarmaNickname(Long userID) throws SQLException {

        Member member = HiveBot.mainGuild().getMemberById(userID);
        if(member != null) {
            String newKarmaSymbol = HiveBot.karmaSQLHandler.getKarmaSymbol(String.valueOf(userID));

            if(newKarmaSymbol != null) {
                List<String> allKarmaSymbols = HiveBot.karmaSQLHandler.getAllKarmaSymbols();

                String currentNickname = member.getEffectiveName();
                boolean karmaEmojiFound = false;

                for (String emoji : allKarmaSymbols) {

                    if (currentNickname.contains(EmojiParser.parseToUnicode(emoji))) {
                        karmaEmojiFound = true;
                        if (!newKarmaSymbol.equalsIgnoreCase(emoji)) {
                            System.out.println(String.format("%s does not equal %s\nSetting new nickname for user: %d",emoji,newKarmaSymbol,member.getId()));

                            String newNick = currentNickname;
                            newNick = newNick.replace(EmojiParser.parseToUnicode(emoji), EmojiParser.parseToUnicode(newKarmaSymbol));
                            HiveBot.mainGuild().modifyNickname(member, newNick).queue();
                            return;
                        }
                    }
                }

                if(!karmaEmojiFound) {
                    String newNick = currentNickname;
                    newNick = newNick + EmojiParser.parseToUnicode(newKarmaSymbol);
                    HiveBot.mainGuild().modifyNickname(member, newNick).queue();
                }
            }
        }

    }
}
