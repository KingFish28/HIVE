package rsystems.events;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import rsystems.HiveBot;

import javax.annotation.Nonnull;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class OnlineStatusListener extends ListenerAdapter {

    @Override
    public void onUserUpdateOnlineStatus(UserUpdateOnlineStatusEvent event) {
        if (event.getUser().isBot()) {
            return;
        }

        if (event.getNewOnlineStatus().equals(OnlineStatus.ONLINE)) {
            /*
            KARMA SYSTEM
             */

            //Initiate the formatter for formatting the date into a set format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
            //Get the current date
            LocalDate currentDate = LocalDate.now();
            //Format the current date into a set format
            String formattedCurrentDate = formatter.format(currentDate);

            //Get the last date of karma increment
            String lastSeenKarma = null;
            try {
                lastSeenKarma = HiveBot.karmaSQLHandler.getDate(event.getMember().getId());
            } catch (SQLException e) {
                e.printStackTrace();
            }

            //Insert new user if not found in DB
            if (lastSeenKarma.isEmpty()) {
                /*if (karmaSQLHandler.insertUser(event.getMember().getId(), event.getUser().getAsTag(), formattedCurrentDate, "KARMA")) {
                    System.out.println("Failed to add member to database");
                } else {
                    karmaSQLHandler.overrideKarmaPoints(event.getMember().getId(), 5);
                }

                 */
            } else {
                long daysPassed = ChronoUnit.DAYS.between(LocalDate.parse(lastSeenKarma, formatter), currentDate);
                if (daysPassed >= 1) {
                    try {
                        HiveBot.karmaSQLHandler.addKarmaPoints(event.getMember().getId(), formattedCurrentDate, false);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
