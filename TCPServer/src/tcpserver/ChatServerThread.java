/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcpserver;

import java.net.*;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
//import java.sql.*;
import java.util.*;
import java.text.*;
public class ChatServerThread extends Thread
{  
    private TCPServer        server     = null;
    private Socket           socket     = null;
    private int              ID         = -1;
    private DataInputStream  streamIn   =  null;
    private DataOutputStream streamOut  = null;
    private int              userID     = -1;
    private int              friendID   = -1;
    private int              adminID    = -1;
    private int              chatroomID = -1;
    private String           userName   = null;
    private Boolean          joinRoom   = false;
    protected Boolean        conversation = false;
    private Boolean          loggedIn   = false;
    private static final     DBConnect connect = new DBConnect();
    private ResultSet        rs, rs1, rs2;
    private String[]         command;
    private String           original;
    //private int conversationID2;
    //private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public ChatServerThread(TCPServer _server, Socket _socket)
    {  
        super();
        server = _server;
        socket = _socket;
        ID     = socket.getPort();
    }
    public void send(String msg)
    {   
        try {  
            streamOut.writeUTF(msg);
            streamOut.flush();
        } catch(IOException ioe) {  
            System.out.println(ID + " ERROR sending: " + ioe.getMessage());
            server.remove(ID);
            stop();
        }
    }
    public int getID()
    {    
        return ID;
    }
    public int getUserID()
    {    
        return userID;
    }
    public int getChatroomID()
    {    
        return chatroomID;
    }
    public boolean conversation() 
    {
        return conversation;
    }
    public void kick()
    {
        joinRoom = false;
        chatroomID = -1;
        server.handle("***** YOU HAVE BEEN KICKED OUT OF THE CHAT ROOM! *****", ID);
    }
    @Override
    public void run()
    {  
        System.out.println("Server Thread " + ID + " running.");
        server.handle("--------- Welcome to Multichat Server ---------\n"
                + "****** Signup or Login to get started ******", ID);
        while (true) {   
            try {
                Date dNow = new Date( );
                SimpleDateFormat ft = new SimpleDateFormat ("hh:mm");
                original = streamIn.readUTF();  //original command
                if(joinRoom == false && conversation == false) {
                    command = original.split(" ");  //splited
                    switch(command[0]) {
                        case "login": 
                            if(loggedIn == false) {
                                //System.out.println("login initiated");
                                //if(userID != -1){
                                if(server.loggedIn(connect.validateLogin(command[1],command[2]))) {
                                    userID = connect.validateLogin(command[1],command[2]);
                                    if(userID != -1){
                                        server.handle("***** You are now logged in. Welcome "+command[1]
                                                + " to MULTICHAT SERVER! *****", ID);
                                        userName = command[1];
                                        loggedIn = true;
                                        rs = connect.checkNewMsg(userID);   //unread msg
                                        int count = 0;
                                        while(rs.next()) {
                                            count++;
                                            if(count == 1)
                                                server.handle("***** You have new message/s from *****", ID);
                                            server.handle(count+". "+connect.getName(rs.getInt("friend_id")), ID);
                                        }
                                        if(count == 0)
                                            server.handle("You have no new message. type pm friend_name to start a conversation.", ID);
                                        rs1 = connect.getFriendList(userID,0);  //pending request
                                        if (rs1.next())
                                            server.handle("You have pending friend request! Type flist to see details!", ID);
                                    } else
                                        server.handle("Failed to login! Invalid username/password", ID);
                                } else
                                    server.handle("You are already logged in to an another window. Please log out first!", ID);
                            } else 
                                server.handle("You are already logged in as "+userName, ID);
                            break;
                        case "logout":
                            if(command.length > 1)
                                throw new ArrayIndexOutOfBoundsException();
                            if(loggedIn == true) {
                                loggedIn = false;
                                userID = -1;
                                server.handle("You are now logged out", ID);
                            } else
                                server.handle("You must log in first!", ID);
                            break;
                        case "signup":
                            connect.createUser(command[1],command[2]);
                            server.handle("Sign up successful! Login to continue ... ", ID);
                            break;
                        case "showroom":
                            if(command.length > 1)
                                throw new ArrayIndexOutOfBoundsException();
                            rs = connect.getChatrooms();
                            server.handle("----- MULTICHAT SERVER CHAT ROOM LIST -----", ID);
                            while(rs.next()) {
                                String room = rs.getString("name");
                                int id = rs.getInt("id");
                                //int count = connect.userCount(id);
                                int count = server.userCount(id);
                                server.handle(room+" -- "+count+" online", ID);
                            }
                            server.handle("----- TYPE IN A CHAT ROOM NAME TO JOIN -----", ID);
                            break;
                        case "createroom":
                            if(loggedIn == true) {
                                String[] result = original.split(" ", 2);
                                String roomName = result[1];                                           
                                connect.createChatroom(roomName,userID);
                                server.handle("----- YOU HAVE SUCCESSFULLY CREATED THE CHATROOM "+roomName+"! -----", ID);
                            }
                            break;
                        case "join":
                            if(loggedIn == true) {
                                String[] result = original.split(" ", 2);
                                String roomName = result[1];                                           
                                chatroomID = connect.joinUserToRoom(roomName);
                                adminID = connect.getAdmin(chatroomID);
                                //System.out.println(chatroomID);
                                if(chatroomID != -1) {
                                    joinRoom = true;
                                    server.handle("----- WELCOME TO "+roomName+" -----", ID);
                                    server.handle("***** ADMIN: "+connect.getName(adminID)+" *****", ID);
                                    server.readChat(ID, chatroomID);
                                    server.chat(ft.format(dNow)+" "+userName+" Joined the chatroom! ", ID, chatroomID);  
                                } else {
                                    server.handle("Failed to join the specified chat room!", ID);
                                } 
                            } else
                                server.handle("You must login first to join a chatroom!", ID);
                            break;
                        case ".leave":
                            server.handle("You must be inside a chatroom to leave!", ID);
                            break;
                        case "frq":
                            if(loggedIn == true) {
                                if(connect.sendFriendRequest(userID,command[1])) {
                                    server.handle("Friend Request Sent!", ID);
                                    server.notify("***** MULTICHAT SERVER: "
                                            +userName+ " wants to be your friend! *****", connect.getId(command[1]));
                                } else 
                                    server.handle("Failed to add friend!", ID);
                            } else
                                server.handle("Login first to send a friend request!", ID);    
                            break;
                        case "add":
                            if(loggedIn == true) {
                                if(connect.addFriend(userID,command[1])) {
                                    server.handle("Request Approved!", ID);
                                    server.notify("***** MULTICHAT SERVER: "
                                            +userName+" accepted your friend request! *****", connect.getId(command[1]));
                                } else 
                                    server.handle("Failed to approve request!", ID);
                            } else
                                server.handle("Login first to approve a friend request!", ID);    
                            break;
                        case "remove":
                            if(loggedIn == true) {
                                if(connect.removeFriend(userID, command[1])) {
                                    server.handle("Succefully removed the user from the list!", ID);
                                } else
                                    server.handle("Failed to remove the user from the list!", ID);
                            }
                            break;
                        case "flist":
                            if(loggedIn == true) {
                                rs = connect.getFriendList(userID, 1);
                                server.handle("----- FRIEND LIST -----", ID);
                                int count = 0;
                                while(rs.next()) {
                                    count++;
                                    int id = rs.getInt("friend_id");
                                    //System.out.println("SAS:: "+id);
                                    String name = connect.getName(id);
                                    boolean status = server.loggedIn(id);
                                    if(status == false)
                                        server.handle(count+". "+name+" - Online", ID);
                                    else
                                        server.handle(count+". "+name, ID);
                                }
                                if(count == 0)
                                    server.handle("You currently have no friends. Type frq user_name to send a friend request!", ID);
                                rs1 = connect.getFriendList(userID, 0); //get pending list
                                int count1 = 1;
                                while(rs1.next()) {
                                    server.handle("----- REQUEST PENDING FOR APPROVAL -----", ID);
                                    int id = rs1.getInt("friend_id");
                                    String name = connect.getName(id);
                                    server.handle(count1+". "+name, ID);
                                    count1++;
                                }
                                rs2 = connect.getSentRequestList(userID, 0); //get sent request
                                int count2 = 1;
                                while(rs2.next()) {
                                    server.handle("----- SENT REQUEST -----", ID);
                                    int id = rs2.getInt("user_id");
                                    String name = connect.getName(id);
                                    server.handle(count2+". "+name, ID);
                                    count2++;
                                }
                            } else
                                server.handle("Login first to see your friend list!", ID);    
                            break;
                        case "pm":
                            friendID = connect.getId(command[1]);
                            if(friendID != -1) {    //validate username
                                if(connect.validateFriend(userID, friendID, 0)) {   //validate friend
                                    conversation = true;
                                    server.handle("----- YOUR CONVERSATION WITH "+command[1]+" -----", ID);
                                    connect.setNewMsgStatus(userID, friendID, 0);
                                    if(userID < friendID)
                                        server.readChat(ID,userID,friendID);
                                    else
                                        server.readChat(ID,friendID,userID);
                                    //server.handle("Success!", ID);
                                    //server.notify("***** MULTICHAT SERVER: "+userName+" sent you a personal message! *****", friendID);
                                } else {
                                    server.handle("You must be friend with "+command[1]+" to send personal message!", ID);
                                }
                            } else
                                server.handle("Invalid username "+command[1]+". Try again!", ID);
                            break;
                        case "exit":
                            server.handle("exit", ID);
                            server.remove(ID);
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                } else if(joinRoom == true && conversation == false) {              //chatroom messaging
                    String[] result = original.split(" ");
                    switch(result[0]) {
                        case ".kick":
                            if(connect.validateRoomAdmin(chatroomID,userID)) {
                                int kickedUserID = connect.getId(result[1]);
                                if(kickedUserID != 0 && kickedUserID != userID) {
                                    if(server.kick(chatroomID,kickedUserID)) {
                                        server.handle("***** You Have Kicked The User "+result[1]+"! *****", ID);
                                        server.chat(ft.format(dNow)+" "+result[1]+"WAS KICKED OUT OF THE CHATROOM BY ADMIN!",ID,chatroomID);
                                    } else 
                                        server.handle("***** Failed to Kick The User "+result[1]+"! *****", ID);
                                } else
                                    server.handle("***** You can not kick yourself dummy! *****", ID);
                            } else
                                server.handle("***** Only admin can KICK inside a chatroom *****", ID);
                            break;
                        case ".leave":
                            joinRoom = false;
                            server.handle("You have left the chatroom", ID);
                            server.chat(ft.format(dNow)+" "+userName+" Left the chatroom! ", ID, chatroomID);
                            chatroomID = -1;
                            break;
                        default:
                            server.chat(ft.format(dNow)+" "+userName+": "+original,ID,chatroomID);
                    }
                } else {                                                            //conversation messaging
                    switch(original) {
                        case ".leave":
                            conversation = false;
                            server.handle("You have left the conversation.", ID);
                            break;
                        default:
                            server.notify("***** MULTICHAT SERVER: "+userName+" sent you a message! *****", friendID);
                            server.chat(ft.format(dNow)+" "+userName+": "+original, userID, friendID, ID);
                    }
                }
            } catch(IOException|ArrayIndexOutOfBoundsException ioe) {  
                System.out.println(ID + " ERROR reading: " + ioe.getMessage());
                server.handle("Too few/many arguments. Try again!",ID);
                //server.remove(ID);
                //stop();
            } catch(IllegalArgumentException iae) {
                System.out.println(ID + " ERROR reading: " + iae.getMessage());
                server.handle("Command not recognized!",ID);
            } catch(SQLException sqle) {
                System.out.println(ID + " ERROR reading: " + sqle.getMessage());
                server.handle("Failed to perform specified action! "+sqle.getMessage(),ID);
            }
        }
    }
    public void open() throws IOException
    {  
        streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        streamOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }
    public void close() throws IOException
    {  
        if (socket != null)    socket.close();
        if (streamIn != null)  streamIn.close();
        if (streamOut != null) streamOut.close();
    }
}