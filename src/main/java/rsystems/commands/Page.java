package rsystems.commands;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import rsystems.Config;
import rsystems.HiveBot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

public class Page extends ListenerAdapter{

    boolean cooldown = false;

        public void onGuildMessageReceived(GuildMessageReceivedEvent event){
            //Escape if message came from a bot account
            if(event.getMessage().getAuthor().isBot()){
                return;
            }

            String[] args = event.getMessage().getContentRaw().split("\\s+");
            if(event.getMessage().getMember().hasPermission(Permission.ADMINISTRATOR)) {
                if (args[0].equalsIgnoreCase((HiveBot.prefix + "page"))) {

                    int anon = new Random().nextInt(10);
                    if(anon == 6){
                        event.getChannel().sendMessage("> " + event.getMessage().getContentDisplay() + "\n" + event.getAuthor().getAsMention() + "umm... no").queue();
                        return;
                    }

                    final String POSTS_API_URL = Config.get("notifyLight");
                    if(!cooldown) {
                        try {
                            HttpClient client = HttpClient.newHttpClient();

                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(POSTS_API_URL))
                                    .header("Content-Type", "text/plain; charset=UTF-8")
                                    .POST(HttpRequest.BodyPublishers.ofString("trigger"))
                                    .build();

                            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                            System.out.println(response);
                            event.getChannel().sendMessage(event.getMessage().getAuthor().getAsMention() + " Sending hook...").queue();

                            cooldown = true;


                            new Thread( new Runnable() {
                                public void run()  {
                                    try  { Thread.sleep( 10000 ); }
                                    catch (InterruptedException ie)  {}
                                    cooldown = false;
                                }
                            } ).start();


                        } catch (IllegalArgumentException e) {
                            System.out.println("Something went wrong");
                        } catch (InsufficientPermissionException e) {
                            event.getChannel().sendMessage("I am lacking permissions to perform this action").queue();
                        } catch (IndexOutOfBoundsException e) {
                            event.getChannel().sendMessage("Nice try " + event.getAuthor().getAsMention() + "! I see what you did there!").queue();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            event.getChannel().sendMessage("Something went wrong...").queue();
                        } catch (IOException e) {
                            e.printStackTrace();
                            event.getChannel().sendMessage("Something went wrong...").queue();
                        }
                    } else {
                        String[] rand = {" Listen!  I'm working on it!", " Look! Do you want to be the bot?", " This hook is smoking hot! Lets let it cool down some!",
                                " There's only so much squirreling I can help to prevent", "Nope.  The doc is off in his own little world here",
                                " I cannot break the laws of bots"};
                        int index = new Random().nextInt(rand.length);

                        event.getChannel().sendMessage(event.getMessage().getAuthor().getAsMention() + rand[index]).queue();
                    }
                }
            }
        }

}