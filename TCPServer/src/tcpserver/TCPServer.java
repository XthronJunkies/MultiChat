package tcpserver;
import java.net.*;
import java.io.*;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TCPServer implements Runnable
{  
    private ChatServerThread clients[] = new ChatServerThread[50];
    private ServerSocket server = null;
    private Thread       thread = null;
    private int clientCount = 0;
    private static final DBConnect connect = new DBConnect();

    public TCPServer (int port) {  
        try {   
            System.out.println("Binding to port " + port + ", please wait  ...");
            server = new ServerSocket(port);  
            System.out.println("Server started: " + server);
            start();
            //connect.getUser();
        } catch(IOException ioe) {  
            System.out.println("Can not bind to port " + port + ": " + ioe.getMessage()); 
        }
    }
@Override
    public void run() {  
        while (thread != null) {  
            try {   
                System.out.println("Waiting for a client ..."); 
                addThread(server.accept()); 
            } catch(IOException ioe) {  
                System.out.println("Server accept error: " + ioe); 
                stop(); 
            }
        }
    }
    public void start() { 
        if (thread == null) {  
            thread = new Thread(this); 
            thread.start();
        }
    }
    public void stop() { 
        if (thread != null) {  
            thread.stop(); 
            thread = null;
        }
    }
    private int findClient(int ID) {  
        for (int i = 0; i < clientCount; i++)
            if (clients[i].getID() == ID)
                return i;
        return -1;
    }
    private int findUser(int userID) {
        for (int i = 0; i < clientCount; i++)
            if (clients[i].getUserID() == userID)
                return i;
        return -1;
    }
    
    public synchronized void handle(String input, int ID) {  
       // for (int i = 0; i < clientCount; i++)
            clients[findClient(ID)].send(input);
        
        /*if (input.equals(".bye")) {  
            clients[findClient(ID)].send(".bye");
            remove(ID); 
        } else {
            for (int i = 0; i < clientCount; i++)
                //clients[i].send(ID + ": " + input);
                clients[i].send(input);
        }*/
    }
    public synchronized void notify(String input, int notifyToID) {
        for(int i = 0; i < clientCount; i++) {
            if(clients[i].getUserID() == notifyToID) {
                clients[i].send(input);
            }
        }
        //clients[findUser(notifyToID)].send(input);
    }
    public synchronized void readChat(int threadID, int chatroomID) {  
        try {
            File f = new File("D:\\AUST\\4.2\\Network Programming Lab\\Lab 3\\MultiChat\\TCPServer\\Chat Log\\chatroom"+chatroomID+".txt");
            if(f.exists() == false) {
                f.createNewFile();
            } else {
                FileReader fr = new FileReader(f);
                BufferedReader br = new BufferedReader(fr);
                String line;
                while((line = br.readLine()) != null) {
                    clients[findClient(threadID)].send(line);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(TCPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public synchronized void readChat(int threadID, int ID1, int ID2) {  
        try {
            File f = new File("D:\\AUST\\4.2\\Network Programming Lab\\Lab 3\\MultiChat\\TCPServer\\Chat Log\\pm"+ID1+"-"+ID2+".txt");
            if(f.exists() == false) {
                f.createNewFile();
            } else {
                FileReader fr = new FileReader(f);
                BufferedReader br = new BufferedReader(fr);
                String line;
                while((line = br.readLine()) != null) {
                    clients[findClient(threadID)].send(line);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(TCPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public synchronized void chat(String input, int threadID, int chatroomID) {
        try {
            File f = new File("D:\\AUST\\4.2\\Network Programming Lab\\Lab 3\\MultiChat\\TCPServer\\Chat Log\\chatroom"+chatroomID+".txt");
            write(input,f);
        } catch (IOException ex) {
            Logger.getLogger(TCPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        for(int i = 0; i < clientCount; i++) {
            if(clients[i].getChatroomID() == chatroomID && clients[i].getID() != threadID)
                clients[i].send(input);
        }
    }
    public synchronized void chat(String input, int userID, int friendID, int threadID) {
        File f;
        try {
            if(userID < friendID)
                f = new File("D:\\AUST\\4.2\\Network Programming Lab\\Lab 3\\MultiChat\\TCPServer\\Chat Log\\pm"+userID+"-"+friendID+".txt");
            else 
                f = new File("D:\\AUST\\4.2\\Network Programming Lab\\Lab 3\\MultiChat\\TCPServer\\Chat Log\\pm"+friendID+"-"+userID+".txt");
            write(input,f);
            for(int i = 0; i < clientCount; i++) {
                if(clients[i].getUserID() == friendID) {
                    if(clients[i].conversation == true) {
                        clients[i].send(input);
                    } 
                } else {
                    try {
                        connect.setNewMsgStatus(friendID, userID, 1);
                    } catch (SQLException ex) {
                        Logger.getLogger(TCPServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(TCPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public synchronized boolean kick(int chatroomID, int kickedUserID) {
        for(int i = 0; i < clientCount; i++) {
            if(clients[i].getChatroomID() == chatroomID && clients[i].getUserID() == kickedUserID) {
                clients[i].kick();
                return true;
            }
        }
        return false;
    }
//    public  synchronized void checkConversation(String input, int userID, int friendID) {
//        File f;
//        try {
//            if(userID > friendID) {
//                f = new File("D:\\AUST\\4.2\\Network Programming Lab\\Lab 3\\MultiChat\\TCPClient\\Chat Log\\PM\\user-"+userID+"-"+friendID+".txt");
//            } else {
//                f = new File("D:\\AUST\\4.2\\Network Programming Lab\\Lab 3\\MultiChat\\TCPClient\\Chat Log\\PM\\user-"+userID+"-"+friendID+".txt");
//            }
//            if(f.exists() == false) {
//                f.createNewFile();
//            } else {
//                FileReader fr = new FileReader(f);
//                BufferedReader br = new BufferedReader(fr);
//                String line;
//                while((line = br.readLine()) != null) {
//                    clients[findUser(friendID)].send(line);
//                    write(input,f);
//                }
//            }
//        } catch (IOException ex) {
//            Logger.getLogger(TCPServer.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    public synchronized void remove(int ID) {  
        int pos = findClient(ID);
        if (pos >= 0) {  
            ChatServerThread toTerminate = clients[pos];
            System.out.println("Removing client thread " + ID + " at " + pos);
            if (pos < clientCount-1)
                for (int i = pos+1; i < clientCount; i++)
                    clients[i-1] = clients[i];
            clientCount--;
            try {  
                toTerminate.close(); 
            } catch(IOException ioe) {  
                System.out.println("Error closing thread: " + ioe); }
                toTerminate.stop(); 
        }
    }
    private void addThread(Socket socket) {  
        if (clientCount < clients.length) {  
            System.out.println("Client accepted: " + socket);
            clients[clientCount] = new ChatServerThread(this, socket);
            try {  
                clients[clientCount].open(); 
                clients[clientCount].start();  
                clientCount++; 
            } catch(IOException ioe) {  
                System.out.println("Error opening thread: " + ioe); 
            } 
        } else
            System.out.println("Client refused: maximum " + clients.length + " reached.");
    }
    public void write(String S, File f) throws IOException{
        FileWriter fw = new FileWriter(f, true);
        fw.write(S);
        fw.write(System.lineSeparator());
        fw.close();
    }
    public int userCount(int chatroomID) {
        int count = 0;
        for(int i = 0; i < clientCount; i++) {
            if(clients[i].getChatroomID() == chatroomID)
                count++;
        }
        return count;
    }
    public boolean loggedIn(int id) {
        //System.out.println("Old"+id);
        for(int i = 0; i < clientCount; i++) {
            if(clients[i].getUserID() == id)
            {   
                return false;
            }
        } 
        return true;
    }
    public static void main(String args[]) { 
        TCPServer server = null;
        server = new TCPServer(2000); 
   }
}