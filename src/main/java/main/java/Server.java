package main.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.*;

import org.bson.Document;
import org.bson.types.Binary;

/**
 * Main class that beings the server on the specified port and handles incoming requests through
 * a ServerSocket.
 * 
 */
public class Server {
	static int port = 8000;
	//static SSLServerSocket ssocket;
	//static SSLServerSocketFactory ssf;
	static ServerSocket ssocket;
	static ContentHandler dbHandler;
	static HashMap<Socket, WebSocket> websockets = new HashMap<Socket, WebSocket>();

	public static void main(String[] args){
		System.out.println("Server is starting on port " + port);
		
		//ConfigManager.getInstance().loadConfigFile("http.json");
		//Config conf = ConfigManager.getInstance().getCurrentConfig();
		dbHandler = new ContentHandler();
		     
		/*try {
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream("keystore.jks"), "changeit".toCharArray());
			
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, "changeit".toCharArray());

			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509"); 
			tmf.init(ks);

			SSLContext sc = SSLContext.getInstance("TLS"); 
			TrustManager[] trustManagers = tmf.getTrustManagers(); 
			sc.init(kmf.getKeyManagers(), trustManagers, null); 

			ssf = sc.getServerSocketFactory(); 
		} catch (KeyStoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		try {
			//ssocket = (SSLServerSocket) ssf.createServerSocket(port);
			ssocket = new ServerSocket(port);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		while(true) {
			try{
				//SSLSocket socket = (SSLSocket) ssocket.accept();
				Socket socket = ssocket.accept();
				PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
				InputStream alt = socket.getInputStream();
				InputStreamReader altReader = new InputStreamReader(alt);
				BufferedReader reader = new BufferedReader(altReader); 
				//This part below directly from oracle documentation
				Thread s = new ServerBox(socket, writer, alt, altReader, reader);
				s.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}

	public static ArrayList<WebSocket> findFriend(String friend, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
		ArrayList<WebSocket> ret = new ArrayList<WebSocket>();
		byte[] token = ServerBox.hash(friend, salt);
        token = ServerBox.hash(new String(token), salt);
		for(WebSocket s : websockets.values()){
			if(Arrays.equals(s.token, token)) {
				ret.add(s);
			}
		}
		return ret;
	}
}

/**
 * Subclass that takes each accepted request in it's own thread, so that the server can accept
 * multiple requests at once. 
 *
 */
class ServerBox extends Thread{
	static int port = 8000;
	Socket socket;
	PrintWriter writer;
	InputStream alt;
	InputStreamReader altReader;
	BufferedReader reader;
	
	/**
	 * Sets initial parameters for the ServerBox
	 * 
	 * @param s socket
	 * @param w writer
	 * @param a alt
	 * @param ar altReader
	 * @param r reader
	 */
	public ServerBox(Socket s, PrintWriter w, InputStream a, InputStreamReader ar, BufferedReader r) {
		socket = s;
		writer = w;
		alt = a;
		altReader = ar;
		reader = r;
	}
	
	@Override
	public void run() {
		try {
			//Assume all traffic is HTTPS
			
			//Initialize HTTP Handler
			
			//This holds the head of the HTML request, since it is the first line of the request
			String t = reader.readLine();
			if(t == null) {
				socket.close();
				return;
			}
			System.out.println(t);
			String head[] = t.split(" ", 3);
			
			//Next we have to find the host
			t = reader.readLine();
			if(t == null) {
				socket.close();
				return;
			}
			System.out.println(t);
			String host[] = t.split(": ", 2);
			
			//Get rest of headers
			HashMap<String, String> headers = new HashMap<String, String>();
			//Note how this stops after the last header, right after reading the new line.
			while((t = reader.readLine()) != null && t.contains(": ")) { 
				System.out.println(t);
				String[] temp = t.split(": ", 2);
				headers.put(temp[0], temp[1]);
			}
			
			HashMap<String, String> cookies = new HashMap<String, String>();
			//Find and process Cookies
			if(headers.containsKey("Cookie")) {
				String[] pairs = headers.get("Cookie").split("; ");
				for(String pair : pairs) {
					String[] temp = pair.split("=");
					cookies.put(temp[0], temp[1]);
				}
			}
		
			//To make it more like a module in case we want to add it to a function later on, we split the
			//requests by section.
			if(head[0].equals("GET")  && head[2].equals("HTTP/1.1")) {
				if(head[1].equals("/websocket")){
					//HW6: start web socket
					if(headers.get("Connection").equals("Upgrade") && headers.get("Upgrade").equals("websocket")) {
						//Take this key
						String key = headers.get("Sec-WebSocket-Key");
						
						//TODO: Check token for websocket!
						//Token check for websocket auth:
						
						//Append header to key
						key += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
						WebSocket sock = new WebSocket(socket, key);
						//sock.start();
						return;
					}
				}else if(head[1].equals("/") || head[1].equals("/index.html")){
					processFile("index.html", "text/html", socket, writer);
				} else {
					autoProcessFile(head[1], socket, writer);
				}
			}else if(head[0].equals("POST") && head[2].equals("HTTP/1.1")) {
				
				String type = headers.get("Content-Type");
				
				//Find the length
				int length = Integer.parseInt(headers.get("Content-Length"));
		
				//Read content length and then decode the input
				String input = "";
				for(int i=0; i<length; i++) {
					input += (char) reader.read();
				}
				
				if(type.contains("urlencoded")) {
					input = decodeQuery(input);
					//Process headers
					String[] temp = input.split("&");
					for(String header : temp) {
						String[] pair = header.split("=", 2);
						headers.put(pair[0], pair[1]);
					}
				}
				
				if(head[1].equals("/register")) {
					//Grab the URI encoded registration information.
					String username = headers.get("username");
					String password = headers.get("password");
					
					//Check if username is too long or short
					if(username.length()<6 || username.length()>20) {
						//Send reply with error message
						printPlainText("Failed, invalid username length! Please select a different username.", socket, writer);
						return;
					}
					
					//Check if username exists
					if(Server.dbHandler.userExists(username)) {
						//Send reply with error message
						printPlainText("Failed, username exists! Please select a different username.", socket, writer);
						return;
					}
					
					if(injectionDefense(username) != username) {
						//Send reply with error message
						printPlainText("Failed, injection prevention. Please use a different username!", socket, writer);
						return;
					}
					
					//Salt and hash password
					SecureRandom random = new SecureRandom();
					byte[] salt = new byte[16];
					random.nextBytes(salt);
					byte[] salted_hash = hash(password, salt);
					Server.dbHandler.secureWrite(username, salted_hash, salt);
					
					printPlainText("Successfuly Registerd! Please login from the login page!", socket, writer);
				}else if(head[1].equals("/login")) {
					//Grab the URI encoded registration information.
					String username = headers.get("username");
					String password = headers.get("password");
					
					if(!Server.dbHandler.userExists(username)) {
						//Send reply with error message
						printPlainText("Failed, incorrect username! Try again!", socket, writer);
						return;
					}
					
					Document document = Server.dbHandler.getCredentials(username);
					
					System.out.println("Login Attempt: " + document.getString("username"));
					
					byte[] salt = ((Binary) document.get("salt")).getData();
					
					//Salt and hash password
					byte[] salted_hash = hash(password, salt);
					
					//Get stored password
					byte[] stored_hash = ((Binary) document.get("password")).getData();
					
					//Verify
					boolean verify = Arrays.equals(salted_hash, stored_hash);
					
					if(verify) {
						System.out.println("Login successful!");
						
						//Generate token
                        byte[] token = hash(username, salt);
                        token = hash(new String(token), salt);
						//Send templated userpage.
						sendUserPage(username, token, writer); //TODO: add token here
					} else {
						//Unauthorized! Send 401.
						print401Text("Failed, incorrect password! Try again!", socket, writer);
						return;
					}
				}
			}
			//Just to make everything look pretty to read in console
			System.out.println("------------------------------------------------------------------------------");
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendUserPage(String username, byte[] token, PrintWriter writer) throws IOException {
		System.out.println("Sending " + username + " UserPage");
		File template_file = new File("UserPage.html");
		byte[] bytes = Files.readAllBytes(template_file.toPath());
		String template = new String(bytes);
		
		//TODO: More customization
		template = template.replace("<title>UserPage</title>", "<title>" + username + "</title>");
		template = template.replace("<h1 class=\"Username\" id = \"NameOfUser\">#####USERNAME#####</h1>",
				"<h1 class=\"Username\" id = \"NameOfUser\">" + username + "</h1>");
		template = template.replace("<a class=\"hidden\" id=\"session\">##############</a>", 
				"<a class=\"hidden\" id=\"session\">"+ Base64.getEncoder().encodeToString(token) +"</a>");
		
		System.out.println(template);
		
		bytes = template.getBytes();
	
		long length = bytes.length;
		
		//TODO: add token to headers
		writer.println("HTTP/1.1 200 OK");
		String type = "text/html";
		writer.println("Content-Type: " + type + "; charset=utf-8");
		writer.println("Connection: close");
		writer.println("Content-Length: " + length);
		writer.println("Set-Cookie: session="+new String(token));
		writer.println();
		socket.getOutputStream().write(bytes);
	}
	
	public static byte[] hash(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
		//Salting and Hashing using PBKDF2 with hmac and SHA256
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		byte[] hash = factory.generateSecret(new PBEKeySpec(password.toCharArray(), salt, 100000, 128)).getEncoded();
		return hash;
	}

	/**
	 * Processes a single file on the server through an HTTP response.
	 * 
	 * @param name filename
	 * @param type Currently supported file types: text/* image/*
	 * @param socket 
	 * @param writer
	 * @throws IOException
	 */
	public static void processFile(String name, String type, Socket socket, PrintWriter writer) throws IOException{
		//FileReader documentation from
		//https://docs.oracle.com/javase/8/docs/api/java/io/FileReader.html
		File f = new File(name);
		BufferedReader file = new BufferedReader(new FileReader(f));
		long length = f.length();
		writer.println("HTTP/1.1 200 OK");
		writer.println("Content-Type: " + type + "; charset=utf-8");
		writer.println("Connection: close");
		writer.println("Content-Length: " + length);
		writer.println();
		String line;
		if(type.contains("image")) {
			//https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html
			byte[] image = Files.readAllBytes(f.toPath());
			socket.getOutputStream().write(image);
			return;
		}
		while((line = file.readLine()) != null) {
			writer.println(line);
		}
		byte[] bytes = Files.readAllBytes(f.toPath());
		socket.getOutputStream().write(bytes);
	}

	/**
	 * Processes a single file on the server through an HTTP response.
	 * 
	 * @param name filename
	 * @param socket 
	 * @param writer
	 * @throws IOException
	 */
	public static void autoProcessFile(String name, Socket socket, PrintWriter writer) throws IOException{
		//FileReader documentation from
		//https://docs.oracle.com/javase/8/docs/api/java/io/FileReader.html
		File f = new File(name.substring(1));
		if(!f.exists()) {
			//404 Not Found
			print404Text("404 Not Found!", socket, writer);
		}
		String type = Files.probeContentType(f.toPath());
		if(name.contains("js")) {
			type = "text/javascript";
		}
		BufferedReader file = new BufferedReader(new FileReader(f));
		long length = f.length();
		writer.println("HTTP/1.1 200 OK");
		writer.println("Content-Type: " + type + "; charset=utf-8");
		writer.println("Connection: close");
		writer.println("Content-Length: " + length);
		writer.println();
		String line;
		if(type.contains("image")) {
			//https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html
			byte[] image = Files.readAllBytes(f.toPath());
			socket.getOutputStream().write(image);
			return;
		}
		while((line = file.readLine()) != null) {
			writer.println(line);
		}
		byte[] bytes = Files.readAllBytes(f.toPath());
		socket.getOutputStream().write(bytes);
		return;
	}
	
	
	/**
	 * Processes queries, currently unused and unmodified for this project.
	 * 
	 * @param pairs
	 * @param socket
	 * @param writer
	 * @throws IOException
	 */
	public static void processQuery(HashMap<String, String> pairs, Socket socket, PrintWriter writer) throws IOException {
		//Probably adding more later, but this satisfies the requirement.
		if(pairs.containsKey("print")) {
			String text = pairs.get("print");
			text = decodeQuery(text);
			writer.println("HTTP/1.1 200 OK");
			writer.println("Content-Type: text/plain; charset=utf-8");
			writer.println("Connection: close");
			writer.println("Content-Length: " + text.length());
			writer.println(); //This is to indicate the end of the headers.
			writer.println(text);
		}
	}

	/**
	 * Produces 404 Not Found http response.
	 * 
	 * @param text
	 * @param socket
	 * @param writer
	 */
	public static void print404Text(String text, Socket socket, PrintWriter writer) {
		writer.println("HTTP/1.1 404 Not Found");
		writer.println("Content-Type: text/plain; charset=utf-8");
		writer.println("Connection: close");
		writer.println("Content-Length: " + text.length());
		writer.println(); //This is to indicate the end of the headers.
		writer.println(text);
	}

	/**
	 * Response to an unauthorized access to private information.
	 * 
	 * @param text
	 * @param socket
	 * @param writer
	 */
	public static void print401Text(String text, Socket socket, PrintWriter writer) {
		writer.println("HTTP/1.1 401 Unauthorized");
		writer.println("Content-Type: text/plain; charset=utf-8");
		writer.println("Connection: close");
		writer.println("Content-Length: " + text.length());
		writer.println(); //This is to indicate the end of the headers.
		writer.println(text);
	}
	
	/**
	 * Sends a plaintext response
	 * 
	 * @param text plaintext message
	 * @param socket 
	 * @param writer
	 */
	public static void printPlainText(String text, Socket socket, PrintWriter writer) {
		writer.println("HTTP/1.1 200 OK");
		writer.println("Content-Type: text/plain; charset=utf-8");
		writer.println("Connection: close");
		writer.println("Content-Length: " + text.length());
		writer.println(); //This is to indicate the end of the headers.
		writer.println(text);
	}

	/**
	 * Query decoding
	 * 
	 * @param text
	 * @return decoded text
	 */
	public static String decodeQuery(String text) {
		try {
			text = URLDecoder.decode(text, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println("Decoded text: " + text);
		return text;
	}

	
	/**
	 * First line of defense against injection attacks, currently unmodified and
	 * likely to be used.
	 * 
	 * @param text
	 * @return
	 */
	public static String injectionDefense(String text) {
		text = text.replaceAll("&", "&amp;");
		text = text.replaceAll("<", "&lt;");
		text = text.replaceAll(">", "&gt;");
		return text;
	}
}

