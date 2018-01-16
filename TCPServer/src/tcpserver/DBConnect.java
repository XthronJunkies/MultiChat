/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcpserver;

import java.sql.*;
/**
 *
 * @author acer
 */
public class DBConnect {
    private static final String DB_DRIVER     = "com.mysql.jdbc.Driver";
    private static final String DB_CONNECTION = "jdbc:mysql://localhost:3306/multichat";
    private static final String DB_USER       = "root";
    private static final String DB_PASSWORD   = "";
    private static Connection   con;
    private Statement           st;
    private ResultSet           rs;
    
    public DBConnect() {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        try {
            con = DriverManager.getConnection(DB_CONNECTION, DB_USER,DB_PASSWORD);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public int validateLogin(String username, String password) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT * FROM USERS WHERE username=? AND password=?");
        ps.setString(1, username);
        ps.setString(2, password);
        rs = ps.executeQuery();
        if(rs.next())
            return rs.getInt("id");
        else 
            throw new SQLException("incorrect username/password!");
    }
    public void createUser(String username, String password) throws SQLException {
        PreparedStatement ps = con.prepareStatement("INSERT INTO USERS(username, password) VALUES(?,?)");
        ps.setString(1, username);
        ps.setString(2, password);
        ps.executeUpdate();
    }
    public ResultSet getChatrooms() throws SQLException {
        st = con.createStatement();
        String query = "SELECT * FROM chatrooms";
        rs = st.executeQuery(query);
        return rs;
    }
    /*
    public int userCount(int id) {
        try {
            con = DBConnect();
            PreparedStatement ps1 = con.prepareStatement("SELECT count(*) AS rowcount FROM chatrooms_users WHERE chatroom_id=?");
            ps1.setInt(1, id);
            rs = ps1.executeQuery();
            rs.next();
            //System.out.println(rs1.getInt("rowcount"));
            return rs.getInt("rowcount");
            //System.out.println(id);
            //return 1;
        } catch(Exception e) {
            System.out.println("Errors: "+ e);
            return 0;
        }     
    }
    */
    public void createChatroom(String roomname, int userID) throws SQLException {
        PreparedStatement ps = con.prepareStatement("INSERT INTO chatrooms(name, admin_id) VALUES(?,?)");
        ps.setString(1, roomname);
        ps.setInt(2, userID);
        ps.executeUpdate();
    }
    public int getAdmin(int chatroomID) throws SQLException{
        PreparedStatement ps = con.prepareStatement("SELECT * FROM chatrooms WHERE id=?");
        ps.setInt(1, chatroomID);
        rs = ps.executeQuery();
        rs.next();
        return rs.getInt("admin_id");
    }
    public int joinUserToRoom(String name) throws SQLException {
        
        PreparedStatement ps = con.prepareStatement("SELECT * FROM chatrooms where name=?");
        ps.setString(1, name);
        rs = ps.executeQuery();
        if(rs.next())
            return rs.getInt("id");
        else
            return -1;
            //throw new SQLException("Specified name does not match with any chatroom!");
        
        //return rs;
    }
//    public boolean leaveRoom(int id) throws SQLException {
//        PreparedStatement ps = con.prepareStatement("DELETE FROM chatrooms_users where user_id=?");
//        ps.setInt(1, id);
//        ps.execute();
//        return true;
//    }
    public boolean validateRoomAdmin(int roomID, int userID) throws SQLException{
        PreparedStatement ps = con.prepareStatement("SELECT * FROM chatrooms WHERE id=? AND admin_id=?");
        ps.setInt(1, roomID);
        ps.setInt(2, userID);
        rs = ps.executeQuery();
        if(rs.next())
            return true;
        else
            return false;
    }
    public boolean validateFriend(int userID, int friendID, int flag) throws SQLException {
        if(flag == 1) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM friend_list WHERE user_id=? AND friend_id=?");
            ps.setInt(1, userID);
            ps.setInt(2, friendID);
            rs = ps.executeQuery();
        } else if(flag == 2) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM friend_list WHERE user_id=? AND friend_id=? AND status=0");
            ps.setInt(1, userID);
            ps.setInt(2, friendID);
            rs = ps.executeQuery();
        }
        else {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM friend_list WHERE user_id=? AND friend_id=? AND status=1");
            ps.setInt(1, userID);
            ps.setInt(2, friendID);
            rs = ps.executeQuery();
        }
        if(rs.next())
            return true;
        else 
            return false;
    }
    public boolean sendFriendRequest(int userID, String friendName) throws SQLException {
        int friendID = getId(friendName);
        if (userID == friendID)
            return false;
        if(validateFriend(friendID, userID, 1) == false) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO friend_list(user_id, friend_id) VALUES(?,?)");
            ps.setInt(1, friendID);
            ps.setInt(2, userID);
            ps.executeUpdate();
            return true;
        } else {
            return false;
        }
    }
    public boolean addFriend(int userID, String friendName) throws SQLException {
        int friendID = getId(friendName);
        if(validateFriend(userID, friendID, 2) == true) {
            //System.out.println("WELL DONE");
            PreparedStatement ps = con.prepareStatement("UPDATE friend_list SET status = 1 WHERE user_id=?");
            ps.setInt(1, userID);
            ps.executeUpdate();
            PreparedStatement ps1 = con.prepareStatement("INSERT INTO friend_list(user_id, friend_id, status) VALUES(?,?,1)");
            ps1.setInt(1, friendID);
            ps1.setInt(2, userID);
            ps1.executeUpdate();
            return true;
        } else {
            return false;
        }
    }
    public boolean removeFriend(int userID, String friendName) throws SQLException {
        int friendID = getId(friendName);
        PreparedStatement ps = con.prepareStatement("DELETE FROM friend_list WHERE user_id=? AND friend_id=?");
        if(validateFriend(userID, friendID, 1) == true) {
            ps.setInt(1, userID);
            ps.setInt(2, friendID);
            ps.execute();
            ps.setInt(2, userID);
            ps.setInt(1, friendID);
            ps.execute();
            return true;
        } else if(validateFriend(userID, friendID, 0) == true){
            ps.setInt(1, userID);
            ps.setInt(2, friendID);
            ps.execute();
            return true;
        } else if(validateFriend(friendID, userID, 2) == true){
            ps.setInt(1, friendID);
            ps.setInt(2, userID);
            ps.execute();
            return true;
        } else
            return false;
    }
    public ResultSet getFriendList(int userID, int status) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT * FROM friend_list WHERE user_id=? AND status=?");
        ps.setInt(1, userID);
        ps.setInt(2, status);
        rs = ps.executeQuery();
        return rs;
    }
    public ResultSet getSentRequestList(int userID, int status) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT * FROM friend_list WHERE friend_id=? AND status=?");
        ps.setInt(1, userID);
        ps.setInt(2, status);
        rs = ps.executeQuery();
        return rs;
    }
    public String getName(int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE id=?");
        ps.setInt(1, id);
        rs = ps.executeQuery();
        if(rs.next()) {
            return rs.getString("username");
        } else 
            return null;
    }
    public int getId(String username) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE username=?");
        ps.setString(1, username);
        rs = ps.executeQuery();
        if(rs.next()) {
            return rs.getInt("id");
        } else 
            return -1;
    }
    public ResultSet checkNewMsg(int userID) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT * FROM friend_list WHERE user_id=? AND new_msg_status=1");
        ps.setInt(1, userID);
        rs = ps.executeQuery();
        return rs;
    }
    public void setNewMsgStatus(int ID1, int ID2, int status) throws SQLException {
        PreparedStatement ps = con.prepareStatement("UPDATE friend_list SET new_msg_status=? WHERE user_id=? AND friend_id=?");
        ps.setInt(1, status);
        ps.setInt(2, ID1);
        ps.setInt(3, ID2);
        ps.executeUpdate();
    }
}