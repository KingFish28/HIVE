package rsystems.handlers;

import java.sql.*;
import java.util.*;

import org.mariadb.jdbc.MariaDbPoolDataSource;
import rsystems.HiveBot;
import rsystems.objects.LED;

public class SQLHandler {
    protected static MariaDbPoolDataSource pool = null;

    public SQLHandler(String URL, String user, String pass) {

        try {
            pool = new MariaDbPoolDataSource(URL);
            pool.setUser(user);
            pool.setPassword(pass);
            pool.setMaxPoolSize(10);
            pool.setMinPoolSize(2);

            pool.initialize();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * Initialize the class using a pool that is already created.
     *
     * @param pool The PoolDataSource
     */
    public SQLHandler(MariaDbPoolDataSource pool) {
        SQLHandler.pool = pool;
    }

    public String getValue(String tableName, String valueColumn, String identifierColumn, Long identifier) throws SQLException {
        String output = null;
        Connection connection = pool.getConnection();

        try {
            ResultSet rs = connection.createStatement().executeQuery(String.format("SELECT %s FROM %s WHERE %s = %d", valueColumn,tableName,identifierColumn,identifier));
            while (rs.next()) {
                output = rs.getString(valueColumn);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }
        return output;

    }

    /**
     * Check the blacklist table for a userID, if found Ignore the user.
     *
     * @param userID The userid of the user to be checked.
     * @return Wether of not the user is blacklisted
     */
    public boolean checkBlacklist(Long userID) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();

        try {
            ResultSet rs = connection.createStatement().executeQuery(String.format("SELECT UserID FROM HIVE_Blacklist WHERE UserID = %d", userID));
            while (rs.next()) {
                if (rs.getLong("UserID") == userID) {
                    output = true;
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }
        return output;

    }

    // Get date of last seen using ID
    public String getDate(String id) throws SQLException {
        String date = "";

        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT DATE FROM LastSeenTable WHERE ID = " + id);
            while (rs.next()) {
                date = rs.getString("DATE");
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return date;
    }


    // Get date of last seen using ID
    public LED getLED(String ledName) throws SQLException {
        String date = "";

        Connection connection = pool.getConnection();

        LED led = new LED(ledName);
        boolean ledFound = false;

        try {

            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(String.format("SELECT Voltage, WhitePixel, WattageTheory, WattageTested FROM LED_Table WHERE ledname = \"%s\"", ledName));

            while (rs.next()) {
                ledFound = true;
                led.setLedVoltage(rs.getInt("Voltage"));
                led.setWhiteIncluded(rs.getBoolean("WhitePixel"));
                led.setWattagePerPixel_Tested(rs.getFloat("WattageTested"));
                led.setWattagePerPixel_Theoretical(rs.getFloat("WattageTheory"));
            }

            connection.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        if(ledFound)
            return led;
        else
            return null;
    }

    public ArrayList<String> getLEDList() throws SQLException {
        Connection connection = pool.getConnection();

        ArrayList<String> ledList = new ArrayList<>();
        try {

            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT ledName FROM LED_Table");

            while (rs.next()) {

                ledList.add(rs.getString("ledName"));
            }

            connection.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return ledList;
    }


    public Map<String, LED> getLEDMap() throws SQLException {
        Connection connection = pool.getConnection();

        Map<String, LED> ledList = new HashMap();
        try {

            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT ledName, Voltage, WhitePixel, WattageTheory, WattageTested FROM LED_Table");

            while (rs.next()) {

                LED led = new LED(rs.getString("ledName"));
                led.setLedVoltage(rs.getInt("Voltage"));
                led.setWhiteIncluded(rs.getBoolean("WhitePixel"));
                led.setWattagePerPixel_Tested(rs.getFloat("WattageTested"));
                led.setWattagePerPixel_Theoretical(rs.getFloat("WattageTheory"));

                ledList.putIfAbsent(led.getLedName(),led);
            }

            connection.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return ledList;
    }

    
    public String getDate(String id, String table) throws SQLException {
        String date = "";

        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery("SELECT DATE FROM " + table + " WHERE ID = " + id);
            while (rs.next()) {
                date = rs.getString("DATE");
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return date;
    }

    // Set the last seen date using id
    
    public int setDate(String table, String id, String date) throws SQLException {

        int updateCount = 0;
        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            updateCount = st.executeUpdate("UPDATE " + table + " SET DATE = \"" + date + "\" WHERE ID = " + id);
            connection.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }
        return updateCount;
    }

    // Get date of last seen using ID
    public String getName(String id) throws SQLException {
        String name = "";
        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery("SELECT NAME FROM LastSeenTable WHERE ID = " + id);
            while (rs.next()) {
                name = rs.getString("NAME");
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return name;
    }

    // Set the name of the user using id
    public int setName(String id, String name) throws SQLException {
        int output = 0;

        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            output = (st.executeUpdate("UPDATE LastSeenTable SET NAME = \"" + name + "\" WHERE ID = " + id));

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }
        return output;
    }

    // Insert NEW users into the DB
    public boolean insertUser(String id, String name, String date) throws SQLException {
        boolean output = false;
        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            output = st.execute("INSERT INTO LastSeenTable (ID,NAME,DATE) VALUES(" + id + ",\"" + name + "\",\"" + date + "\");");

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    // Insert NEW users into the DB
    public boolean insertUser(String id, String name, String date, String column) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            output = st.execute("INSERT INTO " + column + " (ID,NAME,DATE) VALUES(" + id + ",\"" + name + "\",\"" + date + "\");");
            connection.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    // Remove using from DB
    public boolean removeUser(String id) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            output = st.execute("DELETE FROM LastSeenTable WHERE ID = " + id);

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }
        return output;
    }

    // Return an array list of all values in the DB
    public ArrayList<String> getAllUsers(String column) throws SQLException {
        ArrayList<String> values = new ArrayList<>();
        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery("SELECT " + column + " FROM LastSeenTable");
            while (rs.next()) {
                values.add(rs.getString(column));
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }
        return values;
    }

    // Return an array list of all values in the DB
    
    public HashMap<String, String> getAllUsers() {
        HashMap<String, String> idMap = new HashMap<>();
        try {
            Connection connection = pool.getConnection();
            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery("SELECT ID, NAME FROM LastSeenTable");
            while (rs.next()) {
                idMap.put(rs.getString("ID"), rs.getString("NAME"));
            }
            connection.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return idMap;
    }

    // Return an array list of all values in the DB
    
    public HashMap<String, String> getAllUserDates() {
        HashMap<String, String> idMap = new HashMap<>();

        try {
            Connection connection = pool.getConnection();
            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery("SELECT ID, DATE FROM LastSeenTable");
            while (rs.next()) {
                idMap.put(rs.getString("ID"), rs.getString("DATE"));
            }
            connection.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return idMap;
    }

    
    public int getDBSize() {
        int output = 0;
        try {
            Connection connection = pool.getConnection();
            Statement st = connection.createStatement();

            int rowCount = 0;
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM KARMA");
            while (rs.next()) {
                rowCount = rs.getInt(1);
            }

            output = rowCount;
            connection.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return output;
    }

    /**
     * Record the amount of times a command/reference has been used.  If the command/reference does not exist in the database table yet.  It will add it.
     *
     * @param commandName The command/reference to be logged.
     */
    public void logCommandUsage(String commandName) throws SQLException {

        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery(String.format("SELECT UsageCount FROM HIVE_CommandTracker WHERE Name = \"%s\"", commandName));

            boolean commandFound = false;
            while (rs.next()) {
                commandFound = true;
                int newCount = rs.getInt(1) + 1;

                st.execute(String.format("UPDATE HIVE_CommandTracker SET UsageCount = %d, LastUsage = current_timestamp WHERE Name = \"%s\"", newCount, commandName));
            }
            if (!commandFound) {
                st.execute(String.format("INSERT INTO HIVE_CommandTracker (Name, UsageCount) VALUES (\"%s\", 1)", commandName));
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }
    }

    /**
     * Add a role to the database as as a role with permissions.  This database table controls what roles have what permissions to the bot.
     *
     * @param roleID The role to be given mod permissions.
     * @param roleName The name of the role.
     * @param authLevel The permission index that the role will have.  This uses the binary system as before.
     * @return True - No errors | False - Errors occured on insert
     */
    public boolean addAuthRole(Long roleID, String roleName, Integer authLevel) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            st.execute(String.format("INSERT INTO HIVE_AuthRole (RoleID, RoleName, AuthLevel) VALUES (%d, \"%s\",%d)", roleID, roleName, authLevel));
            if (st.getUpdateCount() >= 1) {
                output = true;
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    /**
     * Remove a role from the autheniation table.
     *
     * @param roleID The role to be removed from the authentication table.
     * @return True - No errors | False - Errors occured on removal
     */
    public boolean removeAuthRole(Long roleID) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            st.execute(String.format("DELETE FROM HIVE_AuthRole WHERE (RoleID = %d)", roleID));
            if (st.getUpdateCount() >= 1) {
                output = true;
            }
            connection.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    /**
     * Update a role that is already in the auth table with a new name
     *
     * @param roleID The role to be updated.
     * @param roleName The new name of the role.
     * @return True - No errors | False - Errors occured on insert
     */
    public boolean updateAuthRole(Long roleID, String roleName) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            st.execute(String.format("UPDATE HIVE_AuthRole SET (RoleName = \"%s\") WHERE (RoleID = %d)", roleName, roleID));
            if (st.getUpdateCount() >= 1) {
                output = true;
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    /**
     * Update a role that is already in the auth table with a new permissions
     *
     * @param roleID The role to be updated.
     * @param authLevel The new permissions for the role.
     * @return True - No errors | False - Errors occured on insert
     */
    public boolean updateAuthRole(Long roleID, Integer authLevel) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            st.execute(String.format("UPDATE HIVE_AuthRole SET AuthLevel = %d WHERE RoleID = %d", authLevel, roleID));
            if (st.getUpdateCount() >= 1) {
                output = true;
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    /**
     * Get a map of the roleID and Permissions for all roles in the auth table.
     *
     * @return Map[RoleID,Role Permissions]
     */
    public Map<Long,Integer> getAuthRoles() throws SQLException {
        Map<Long, Integer> authMap = new HashMap<>();

        Connection connection = pool.getConnection();

        try{
            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery("SELECT RoleID, AuthLevel FROM HIVE_AuthRole");
            while(rs.next()){
                authMap.putIfAbsent(rs.getLong("RoleID"),rs.getInt("AuthLevel"));
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return authMap;
    }

    /**
     * Add an emoji alias to the Emoji Whitelist for a role.
     *
     * @param roleID The roleID that is receiving the emoji to its whitelist.
     * @param emojiUnicode The emoji in ALIAS form that will get stored as a varchar in the database.
     * @return True = Successful insertion | False = Database Error
     */
    public boolean addEmojiToWhitelist(Long roleID, String emojiUnicode) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();

        try {
            Statement st = connection.createStatement();

            boolean emojiFound = false;

            ResultSet rs = st.executeQuery(String.format("SELECT RoleID FROM HIVE_PerksEmoji WHERE (RoleID = %d) AND (EmojiUnicode = \"%s\")", roleID, emojiUnicode));
            while (rs.next()) {
                emojiFound = true;
            }

            if (!emojiFound) {
                st.execute(String.format("INSERT INTO HIVE_PerksEmoji (RoleID, EmojiUnicode) VALUES (%d, \"%s\")", roleID, emojiUnicode));
                if (st.getUpdateCount() >= 1) {
                    output = true;
                }
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    /**
     * Remove an emoji from the whitelist.  This will affect the users nicknames.
     * @param roleID The role to remove the emoji FROM
     * @param emoji The emoji to be removed from the whitelist.
     * @return True - Emoji found and removed | False - No matches found
     */
    public boolean removeEmojiFromWhitelist(Long roleID, String emoji) throws SQLException {

        boolean output = false;

        Connection connection = pool.getConnection();

        try {

            Statement st = connection.createStatement();

            st.execute(String.format("DELETE FROM HIVE_PerksEmoji WHERE RoleID = %d AND EmojiUnicode = \"%s\"", roleID, emoji));
            if (st.getUpdateCount() >= 1) {
                output = true;
            }



        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    public void loadPerkEmojis() throws SQLException {

        Connection connection = pool.getConnection();

        try{

            Statement st = connection.createStatement();

            HiveBot.emojiPerkMap.clear();

            ResultSet rs = st.executeQuery("SELECT RoleID, EmojiUnicode FROM HIVE_PerksEmoji");
            while(rs.next()){

                Long roleID = rs.getLong("RoleID");
                String emoji = rs.getString("EmojiUnicode");

                if(HiveBot.emojiPerkMap.containsKey(roleID)){
                    HiveBot.emojiPerkMap.get(roleID).add(emoji);
                } else {
                    HiveBot.emojiPerkMap.putIfAbsent(roleID,new ArrayList<String>());
                    HiveBot.emojiPerkMap.get(roleID).add(emoji);
                }

            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

    }

    /**
     * Grab the amount of times a command OR reference has been used.
     *
     * @param commandName The command name.
     * @return # of times the command/reference has been called.
     */
    public Integer checkUsage(String commandName) throws SQLException {
        Integer output = null;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery(String.format("SELECT UsageCount, LastUsage FROM HIVE_CommandTracker WHERE Name = \"%s\"", commandName));
            while(rs.next()){

                output = rs.getInt("UsageCount");

            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }
        return output;
    }


    /**
     * Compare the assignable role table to see if it contains the provided role ID.
     *
     * @param roleID The roleID to compare to the database.
     * @return If role is found on the Assignable role table, then returns true.  Otherwise return false.
     */
    public boolean checkAssignableRole(Long roleID) throws SQLException {

        Boolean output = false;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery(String.format("SELECT RoleID FROM HIVE_AssignRole WHERE RoleID = %d", roleID));
            while(rs.next()){
                output = true;
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }
        return output;
    }

    /**
     * Get a list of assignable Roles
     *
     * @return List[RoleIDs] of assignable roles.
     */
    public List<Long> assignableRoleList() throws SQLException {

        List<Long> output = new ArrayList<>();

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery(String.format("SELECT RoleID FROM HIVE_AssignRole"));
            while(rs.next()){
                output.add(rs.getLong("RoleID"));
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }
        return output;
    }


    /**
     * Store the channel and message IDs into a table to allow pulling the message in the future for edits.
     *
     *
     * @param channelID The channelID is used in the future to know what channel to pull from.
     * @param messageID The messageID, to allow grabbing the message in the future.
     * @return true = Successful insert into database | false = Database insertion error
     */
    public boolean storeEmbedData(Long channelID, Long messageID) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            st.execute(String.format("INSERT INTO HIVE_EmbedTable (ChannelID, MessageID) VALUES (%d, %d)",channelID,messageID));
            if(st.getUpdateCount() >= 1){
                output = true;
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    /**
     * Get the channelID associated with a messageID from the database.
     *
     *
     * @param messageID The ID of the message to lookup.
     * @return Returns channelID if messageID was found.  Otherwise returns null.
     */
    public Long getEmbedChannel(Long messageID) throws SQLException {
        Long output = null;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery(String.format("SELECT ChannelID FROM HIVE_EmbedTable WHERE MessageID = %d",messageID));
            while(rs.next()){
                output = rs.getLong("ChannelID");
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    /**
     * Get the channelID associated with a messageID from the database.
     *
     *
     * @param tableName The name of the table to update.
     * @param identifierColumn The column name that holds the identifier to use to identify what row to delete.
     * @param identifier The identifier that will determine what row to delete.
     * @return Returns how many rows were deleted.
     */
    public Integer deleteValue(String tableName, String identifierColumn, Long identifier) throws SQLException {
        Integer output = null;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            st.executeQuery(String.format("DELETE FROM %s WHERE %s = %d",tableName,identifierColumn,identifier));
            output = st.getUpdateCount();


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    /**
     * Delete a row(s) from the table with a identifier.
     * @param tableName The table referencing the row(s) to be deleted
     * @param identifierColumn The column name that holds the identifier
     * @param identifier The identifier, this is used to query what rows to delete
     * @return The amount of rows deleted.
     */
    public Integer deleteValue(String tableName, String identifierColumn, Integer identifier) throws SQLException {
        Integer output = null;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            st.executeQuery(String.format("DELETE FROM %s WHERE %s = %d",tableName,identifierColumn,identifier));
            output = st.getUpdateCount();


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    /**
     * This method will place a string in the rolling queue of the activity pool.
     * @param activity What to add to the queue of messages that get displayed in the activity field.  Limit 32 characters
     * @return Returns the ID of the inserted row
     */
    public Integer insertActivity(String activity) throws SQLException {
        Integer output = 0;

        Connection connection = pool.getConnection();
        try{


            PreparedStatement st = connection.prepareStatement(String.format("INSERT INTO HIVE_ActivityList (ActivityString) VALUES (\"%s\")",activity),new String[] {"ID"});
            st.execute();

            if(st.getUpdateCount() >= 1){
                ResultSet rs = st.getGeneratedKeys();
                while(rs.next()) {
                    output = rs.getInt("ID");
                }
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    /**
     * Get a TreeMap of the activities that are in the database pool.
     * @return TreeMap of entries
     */
    public TreeMap<Integer, String> getActivityMap() throws SQLException {
        TreeMap<Integer, String> activityMap = new TreeMap<>();

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery("SELECT ID, ActivityString FROM HIVE_ActivityList");
            while(rs.next()){
                activityMap.putIfAbsent(rs.getInt("ID"),rs.getString("ActivityString"));
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return activityMap;

    }

    /**
     * This method pulls the next activity from the pool.  If the next activity is null, it returns back to the first row.
     * @param currentIndex The current activities ID
     * @return The next activity found in the database.
     */
    public String nextActivity(Integer currentIndex) throws SQLException {
        String output = null;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery(String.format("SELECT ID, ActivityString FROM HIVE_ActivityList WHERE ID > %d LIMIT 1",currentIndex));
            while(rs.next()){
                output = rs.getString("ActivityString");
                HiveBot.activityStatusIndex = rs.getInt("ID");
            }

            if(output == null){
                rs = st.executeQuery(String.format("SELECT ActivityString FROM HIVE_ActivityList WHERE ID = 1"));
                while(rs.next()){
                    output = rs.getString("ActivityString");
                }

                HiveBot.activityStatusIndex = 1;
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;

    }

    /**
     * This method will check the database for any user authorization overrides.  This is used to add permissions to a certain user rather then a role.  Allowing more control of permissions.
     * @param userID The userid of the user to be checked
     * @return True - Authorized | False - Unauthorized
     */
    public Integer checkAuthOverride(Long userID) throws SQLException {
        Integer output = null;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery(String.format("SELECT AuthLevel FROM HIVE_AuthUser WHERE UserID = %d LIMIT 1",userID));
            while(rs.next()){
                output = rs.getInt("AuthLevel");
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    /**
     * Add a user to the authentication override table.
     * @param userID The userID of the user to be added.
     * @param authLevel The authentication level of the user.
     * @param userTag The user's tag for reference purposes only.
     * @return True - Insert transaction completed | False - Errors Occurred
     */
    public boolean insertAuthUser(Long userID, int authLevel, String userTag) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            st.executeQuery(String.format("INSERT INTO HIVE_AuthUser (UserID, AuthLevel, UserTag) VALUES (%d, %d, '%s')",userID,authLevel,userTag));
            if(st.getUpdateCount() >= 1){
                output = true;
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    public boolean removeAuthUser(Long userID) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            st.executeQuery(String.format("DELETE FROM HIVE_AuthUser WHERE UserID = %d",userID));
            if(st.getUpdateCount() >= 1){
                output = true;
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    public boolean updateAuthUser(Long userID, Integer newAuthLevel) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            st.executeQuery(String.format("UPDATE FROM HIVE_AuthUser SET AuthLevel = %d WHERE UserID = %d",newAuthLevel, userID));
            if(st.getUpdateCount() >= 1){
                output = true;
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    public boolean updateAuthUser(Long userID, String userTag) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            st.executeQuery(String.format("UPDATE FROM HIVE_AuthUser SET UserTag = '%s' WHERE UserID = %d",userTag, userID));
            if(st.getUpdateCount() >= 1){
                output = true;
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }


    /**
     * This method will add a string to the database to be pulled when a user uses the Mini command.
     * @param userID The userid of the user requesting the insert
     * @param message The message to be added
     * @return True - Successful Transaction | False - Error
     */
    public boolean insertUserMessage(Long userID, String message) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            st.executeQuery(String.format("INSERT INTO HIVE_UserMessageTable (UserID, Message) VALUES (%d, '%s')",userID,message));
            if(st.getUpdateCount() >= 1){
                output = true;
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    /**
     * Update the user mini message
     * @param userID The user's ID to be updated
     * @param message The updated message to overwrite the current one.
     * @return True - Successful Transaction | False - Errors
     */
    public boolean updateUserMessage(Long userID, String message) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            st.executeQuery(String.format("UPDATE HIVE_UserMessageTable SET Message = \"%s\" WHERE UserID = %d",message,userID));
            if(st.getUpdateCount() >= 1){
                output = true;
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    /**
     * Update the user mini message color
     * @param userID The user's ID to be updated
     * @param colorCode The updated color to overwrite the current one.
     * @return True - Successful Transaction | False - Errors
     */
    public boolean updateUserMessageColor(Long userID, String colorCode) throws SQLException {
        boolean output = false;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            st.executeQuery(String.format("UPDATE HIVE_UserMessageTable SET Color = \"%s\" WHERE UserID = %d",colorCode,userID));
            if(st.getUpdateCount() >= 1){
                output = true;
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return output;
    }

    public String grabRandomGreeting() throws SQLException {
        String returnString = null;

        Connection connection = pool.getConnection();
        try{

            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery("SELECT Message FROM HIVE_Greetings ORDER BY RAND() LIMIT 1");
            while(rs.next()){
                returnString = rs.getString("Message");
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            connection.close();
        }

        return returnString;
    }

}
