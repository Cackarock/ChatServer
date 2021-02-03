
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
	protected static Socket s; //socket for server
	protected static ObjectOutputStream out;	//output stream for this client
	protected static boolean inChannel = false;	//if the client is in a channel
	protected static boolean connected = false;	//if the client is connected to a server
	protected static String curChannel = "";	//current channel name
	protected static String serverName = "";	//current server Name
	protected static String[] channels = {"alpha", "beta", "gamma", "delta"};	//list of channels
	
	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	try {
					out.writeObject(new IRCRequest("quit","-"));

			    	out.flush();
				} catch (IOException e) {
				}
		    }
		 });
		System.out.println("Client is up and running. Please connect to a server\n");
		try {
			Scanner scan = new Scanner(System.in);
			String line = "";
			while(true) {
				line = scan.nextLine();
				String[] cmd = line.split(" ");
				if(cmd.length > 3 && line.charAt(0) == '/') {
					System.out.println("Unrecognized command");
					continue;
				}
				switch(cmd[0]) {
				case "/connect":
					if(cmd.length < 3) {
						System.out.println("Usage: /connect <servername> <port>");
						continue;
					}
					try {
					serverName = cmd[1];
					s = new Socket(serverName, Integer.parseInt(cmd[2]));
					out = new ObjectOutputStream(s.getOutputStream());
					connected = true;
					new onClientHandler(s).start();
					System.out.println("Connected to " + serverName + " on port " + cmd[2]);
					}catch(IOException e) {
						System.out.println("Could not connect to server " + serverName);
					}catch(NumberFormatException e) {
						System.out.println("Usage: /connect <servername> <port>");
					}
					break;
					
				case "/list":
					if(!connected) {
						System.out.println("Not connected to any server");
						continue;
					}
					out.writeObject(new IRCRequest("list", ""));
					out.flush();
					break;

				case "/nick":
					if(!connected) {
						System.out.println("Not connected to any server");
						continue;
					}else if(cmd.length != 2) {
						System.out.println("Usage: /nick <name>");
						continue;
					}else {
						out.writeObject(new IRCRequest("nick", cmd[1]));
						out.flush();
						System.out.println("Changing nickname to " + cmd[1]);
					}
					break;

				case "/join":
					if(!connected) {
						System.out.println("Not connected to any server");
						continue;
					}else if(cmd.length < 2){
						System.out.println("Usage: /join <channel>");
						continue;
					}else {
						out.writeObject(new IRCRequest("join", cmd[1]));
						out.flush();
					}
					break;

				case "/quit":
					if(!connected) {
						System.out.println("Not connected to any server");
						continue;
					}else if(!inChannel) {
						out.writeObject(new IRCRequest("quit", "-"));
						out.flush();
						continue;
					}else {
						inChannel = false;
						out.writeObject(new IRCRequest("leave", "-"));
						out.flush();
						out.writeObject(new IRCRequest("quit", "-"));
						out.flush();
						System.out.println("Left channel " + curChannel);
					}
					break;

				case "/leave":
					if(!connected) {
						System.out.println("Not connected to any server");
						continue;
					}else if(!inChannel) {
						System.out.println("Not currently in a channel");
						continue;
					}else {
						inChannel = false;
						out.writeObject(new IRCRequest("leave", "-"));
						out.flush();
						System.out.println("Left channel " + curChannel);
					}
					break;

				case "/help":
					System.out.println("List of commands:\n\t/connect <servername> <port> - connect to a server"
							+ "\n\t/nick <name> - change your nickname"
							+ "\n\t/list - get a list of all the channels on the current server"
							+ "\n\t/join <channel> - join a channel"
							+ "\n\t/leave - leave the current channel"
							+ "\n\t/quit - leave the current channel and server"
							+ "\n\t/help - get a list of commands"
							+ "\n\t/stats - get information about the server");
					break;

				case "/stats":
					if(!connected) {
						System.out.println("Not connected to any server");
						continue;
					}else {
						out.writeObject(new IRCRequest("stats", "-"));
						out.flush();
					}
					break;

				default:
					if(!connected) {
						System.out.println("Not connected to any server");
						continue;
					}else if(!inChannel){
						System.out.println("Not currently in a channel");						
					}else {
						out.writeObject(new IRCRequest("message", line));
						out.flush();
					}
				}
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

}

class onClientHandler extends Thread{
	private Socket server;
	private volatile boolean running = true;
	
	public onClientHandler(Socket s) {
		this.server = s;
	}
	
	public void run() {
		try {
			ObjectInputStream in = new ObjectInputStream(server.getInputStream());
			while(running) {
				IRCRequest r = (IRCRequest) in.readObject();
				switch(r.command){
				case "nick":
					System.out.println(r.information);
					break;
				case "stats":
					System.out.println(r.information);
					break;
				case "quit":
					System.out.println("Left server " + ChatClient.serverName);
					server.close();
					ChatClient.s.close();
					running = false;
					ChatClient.connected = false;
					break;
				case "list":
					String[] counts = r.information.split(",");
					System.out.println("List of channels and their user counts");
					for(int i = 0; i < counts.length;i++) {
						System.out.println("\t" + ChatClient.channels[i] + " -> " + counts[i]);
					}
					break;
				case "message":
					System.out.println(r.information);
					break;
				case "join":
					ChatClient.inChannel = true;
					ChatClient.curChannel = r.information;
					System.out.println("Joined channel " + r.information);
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return;
	}
}

