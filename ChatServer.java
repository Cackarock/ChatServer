
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ChatServer {
	
	protected static ArrayList<user> global = new ArrayList<user>();
	protected static ArrayList<user> alpha = new ArrayList<user>();
	protected static ArrayList<user> beta = new ArrayList<user>();
	protected static ArrayList<user> gamma = new ArrayList<user>();
	protected static ArrayList<user> delta = new ArrayList<user>();
	
	protected static int numberOfGuests = 0;
	protected static int totalUsers = 0;
	protected static int totalMessages = 0;
	
	protected static boolean isRunning = true;
	 
	protected static int debug;
	protected static int port;
	
	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	for(user u: global) {
		    		try {
						u.out.writeObject(new IRCRequest("message", "Server terminated"));
						u.out.flush();
						u.out.writeObject(new IRCRequest("quit","-"));
						u.out.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
		    	}
		    }
		 });
		parseArguments(args);
		try {
			ServerSocket ss = new ServerSocket(port);
			System.out.println("Server is up and running.\n");
			while(true) {
				Socket s = ss.accept();
				if(debug == 1) {
					System.out.println(s.getInetAddress().getHostAddress() + " joined the ChatServer");
				}
				numberOfGuests++;
				totalUsers++;
				user u =new user(new ObjectOutputStream(s.getOutputStream()));
				global.add(u);
				new clientHandler(s, u).start();
				
			}
		}catch(IOException e) {
			System.err.println(e);
		}
	}
	
	public static void parseArguments(String[] args) {
		String usage = "java ChatChatServer -p <port#> -d <debug-level>";
		
		if(args.length == 0) {
			port = 5161;
			debug = 0;
		}else if(args.length == 2) {
			if(args[0].equals("-p")) {
				port = Integer.parseInt(args[1]);
				debug = 0;
			}else if(args[0].equals("-d")) {
				port = 5161;
				debug = Integer.parseInt(args[1]);
			}else {
				System.err.println("Unrecognized option, usage: \n\t" + usage);
				System.exit(1);
			}
		}else if(args.length == 4 && args[0].equals("-p") && args[2].equals("-d")) {
			port = Integer.parseInt(args[1]);
			debug = Integer.parseInt(args[3]);
		}else {
			System.err.println("Improper number of arguments, usage: \n\t" + usage);
		}
		
		if(port > 5165 || port < 5161 || debug > 1 || debug < 0) {
			System.err.println("Either port number or debug level specified were out of bounds:\n\tPort specified: " 
						+ port + "\n\tDebug level specified: " + debug + "\n\tUsage: " + usage);
			System.exit(1);
		}
	}

}

class user{
	
	protected String name;
	protected ObjectOutputStream out;
	
	public user(ObjectOutputStream out) {
		this.out = out;
		name  = "guest" + ChatServer.totalUsers;
	}
	
}

class clientHandler extends Thread{
	
	private Socket client;
	private ObjectOutputStream out;
	private user user;
	private String name;
	private String channel = "";
	private int messagesSent = 0;
	
	public clientHandler(Socket client , user u) {
		this.client = client;
		this.user = u;
		this.out = u.out;
		this.name = u.name;
	}
	
	public void run() {
		try {
			ObjectInputStream in = new ObjectInputStream(client.getInputStream());
			while(true) {
				IRCRequest r = (IRCRequest) in.readObject();
				if(ChatServer.debug == 1) {
					System.out.println("Recieved IRCRequest from " + name + " -> " +  r.command + ":" + r.information);
				}
				switch(r.command) {
				case "stats":
					r.information = "Number of user on the ChatServer: " + ChatServer.numberOfGuests +
							"\nTotal session connections: " + ChatServer.totalUsers +
							"\nTotal messages sent: " + ChatServer.totalMessages + 
							"\nNumber of messages you sent: " + messagesSent;;
					out.writeObject(r);
					out.flush();
					if(ChatServer.debug == 1) {
						System.out.println("Sent IRCRequest to " + name + " -> " +  r.command + ":" + r.information);
					}
					break;
				case "quit":
					ChatServer.numberOfGuests--;
					out.writeObject(r);
					out.flush();
					if(ChatServer.debug == 1) {
						System.out.println("Sent IRCRequest to " + name + " -> " +  r.command + ":" + r.information);
					}
					return;
				case "nick":
					boolean found = false;
					for(user u: ChatServer.global) {
						if(r.information.trim().equals(u.name.trim())) {
							found = true;
						}
					}
					if(found) {
						r.information = "Username already in use";
						out.writeObject(r);
						out.flush();
					}else {
						user.name = r.information;
						name = r.information;
					}
					if(ChatServer.debug == 1) {
						System.out.println("Sent IRCRequest to " + name + " -> " +  r.command + ":" + r.information);
					}
					break;
				case "leave":
					if(channel.toLowerCase().equals("alpha")) {
						ChatServer.alpha.remove(ChatServer.alpha.indexOf(user));
					}else if(channel.toLowerCase().equals("beta")) {
						ChatServer.beta.remove(ChatServer.beta.indexOf(user));
					}else if(channel.toLowerCase().equals("gamma")) {
						ChatServer.gamma.remove(ChatServer.gamma.indexOf(user));
					}else if(channel.toLowerCase().equals("delta")) {
						ChatServer.delta.remove(ChatServer.delta.indexOf(user));
					};
					channel = "";
					break;
				case "list":
					r.information = ChatServer.alpha.size() + "," + ChatServer.beta.size() + "," + ChatServer.gamma.size() + "," + ChatServer.delta.size(); 
					out.writeObject(r);
					out.flush();
					if(ChatServer.debug == 1) {
						System.out.println("Sent IRCRequest to " + name + " -> " +  r.command + ":" + r.information);
					}
					break;
				case "join":
					if(r.information.toLowerCase().equals("alpha")) {
						if(channel.equals("beta")) {
							ChatServer.alpha.add(ChatServer.beta.remove(ChatServer.beta.indexOf(user)));
						}else if(channel.equals("gamma")){
							ChatServer.alpha.add(ChatServer.gamma.remove(ChatServer.gamma.indexOf(user)));
						}else if(channel.equals("delta")){
							ChatServer.alpha.add(ChatServer.delta.remove(ChatServer.delta.indexOf(user)));
						}else {
							ChatServer.alpha.add(user);
						}
						out.writeObject(r);
						out.flush();
						if(ChatServer.debug == 1) {
							System.out.println("Sent IRCRequest to " + name + " -> " +  r.command + ":" + r.information);
						}
						channel = "alpha";
					}else if(r.information.toLowerCase().equals("beta")) {
						if(channel.equals("alpha")) {
							ChatServer.beta.add(ChatServer.alpha.remove(ChatServer.alpha.indexOf(user)));
						}else if(channel.equals("gamma")){
							ChatServer.beta.add(ChatServer.gamma.remove(ChatServer.gamma.indexOf(user)));
						}else if(channel.equals("delta")){
							ChatServer.beta.add(ChatServer.delta.remove(ChatServer.delta.indexOf(user)));
						}else {
							ChatServer.beta.add(user);
						}
						out.writeObject(r);
						out.flush();
						if(ChatServer.debug == 1) {
							System.out.println("Sent IRCRequest to " + name + " -> " +  r.command + ":" + r.information);
						}
						channel = "beta";
					}else if(r.information.toLowerCase().equals("gamma")) {
						if(channel.equals("alpha")) {
							System.out.println("got here");
							ChatServer.gamma.add(ChatServer.alpha.remove(ChatServer.alpha.indexOf(user)));
						}else if(channel.equals("beta")){
							ChatServer.gamma.add(ChatServer.beta.remove(ChatServer.beta.indexOf(user)));
						}else if(channel.equals("delta")){
							ChatServer.gamma.add(ChatServer.delta.remove(ChatServer.delta.indexOf(user)));
						}else {
							ChatServer.gamma.add(user);
						}
						out.writeObject(r);
						out.flush();
						if(ChatServer.debug == 1) {
							System.out.println("Sent IRCRequest to " + name + " -> " +  r.command + ":" + r.information);
						}
						channel = "gamma";
					}else if(r.information.toLowerCase().equals("delta")) {
						if(channel.equals("alpha")) {
							ChatServer.delta.add(ChatServer.alpha.remove(ChatServer.alpha.indexOf(user)));
						}else if(channel.equals("beta")){
							ChatServer.delta.add(ChatServer.beta.remove(ChatServer.beta.indexOf(user)));
						}else if(channel.equals("gamma")){
							ChatServer.delta.add(ChatServer.gamma.remove(ChatServer.gamma.indexOf(user)));
						}else {
							ChatServer.delta.add(user);
						}
						out.writeObject(r);
						out.flush();
						if(ChatServer.debug == 1) {
							System.out.println("Sent IRCRequest to " + name + " -> " +  r.command + ":" + r.information);
						}
						channel = "delta";
					}
					break;
					
				case "message":
					ChatServer.totalMessages++;
					messagesSent++;
					if(channel.toLowerCase().equals("alpha")) {
						for(user u: ChatServer.alpha) {
							ObjectOutputStream to = u.out;
							if(!to.equals(this.out)) {
								r.information = name + ": " + r.information;
								to.writeObject(r);
								to.flush();
								if(ChatServer.debug == 1) {
									System.out.println("Sent IRCRequest to " + u.name + " -> " +  r.command + ":" + r.information);
								}
							}	
						}
					}else if(channel.toLowerCase().equals("beta")) {
						for(user u: ChatServer.beta) {
							ObjectOutputStream to = u.out;
							if(!to.equals(this.out)) {
								r.information = name + ": " + r.information;
								to.writeObject(r);
								to.flush();
								if(ChatServer.debug == 1) {
									System.out.println("Sent IRCRequest to " + u.name + " -> " +  r.command + ":" + r.information);
								}
							}	
						}
					}else if(channel.toLowerCase().equals("gamma")) {
						for(user u: ChatServer.gamma) {
							ObjectOutputStream to = u.out;
							if(!to.equals(this.out)) {
								r.information = name + ": " + r.information;
								to.writeObject(r);
								to.flush();
								if(ChatServer.debug == 1) {
									System.out.println("Sent IRCRequest to " + u.name + " -> " +  r.command + ":" + r.information);
								}
							}	
						}
					}else if(channel.toLowerCase().equals("delta")) {
						for(user u: ChatServer.delta) {
							ObjectOutputStream to = u.out;
							if(!to.equals(this.out)) {
								r.information = name + ": " + r.information;
								to.writeObject(r);
								to.flush();
								if(ChatServer.debug == 1) {
									System.out.println("Sent IRCRequest to " + u.name + " -> " +  r.command + ":" + r.information);
								}
							}	
						}
					}
					break;
				}
				
			}
		} catch (IOException | ClassNotFoundException e) {
			System.err.println(e);
		}
	}
	
}