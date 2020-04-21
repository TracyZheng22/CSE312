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
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import javax.net.ssl.*;

/**
 * Main class that beings the server on the specified port and handles incoming requests through
 * a ServerSocket.
 */
public class Server {
	static int port = 8000;
	//static SSLServerSocket ssocket;
	//static SSLServerSocketFactory ssf;
	static ServerSocket ssocket;
	static HashMap<Socket, WebSocket> websockets = new HashMap<Socket, WebSocket>();

	public static void main(String[] args){
		System.out.println("Server is starting on port " + port);

		/*
		ConfigManager.getInstance().loadConfigFile("http.json");
		Config conf = ConfigManager.getInstance().getCurrentConfig();

		ContentHandler ch = new ContentHandler();
		*/
		     
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
						//Append header to key
						key += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
						WebSocket sock = new WebSocket(socket, key);
						//sock.start();
						return;
					}
				}else if(head[1].equals("/") || head[1].equals("/index.html")){
					processFile("index.html", "text/html", socket, writer);
				}else if(head[1].equals("/UserPage.html")){
					processFile("UserPage.html", "text/html", socket, writer);
				}else if(head[1].equals("/UserPage.css")){
					processFile("UserPage.css", "text/css", socket, writer);
				}else if(head[1].equals("/UserPage.js")){
					processFile("UserPage.js", "text/javascript", socket, writer);
				}else if(head[1].equals("/feed.html")){
					processFile("feed.html", "text/html", socket, writer);
				}else if(head[1].equals("/Sign-in.html")){
					processFile("Sign-in.html", "text/html", socket, writer);
				}else if(head[1].equals("/BookFaceLogo.png")){
					processFile("BookFaceLogo.png", "image/png", socket, writer);
				}else if(head[1].equals("/boxconnected.png")){
					processFile("boxconnected.png", "image/png", socket, writer);
				}else if(head[1].equals("/connected.png")){
					processFile("connected.png", "image/png", socket, writer);
				}else if(head[1].equals("/SpongeBob_stock_art.png")){
					processFile("SpongeBob_stock_art.png", "image/png", socket, writer);
				}else if(head[1].equals("/feedstyle.css")) {
					processFile("feedstyle.css", "text/css", socket, writer);
				}else if(head[1].equals("/style.css")) {
					processFile("style.css", "text/css", socket, writer);
				}else if(head[1].equals("/landing.css")){
					processFile("landing.css", "text/css", socket, writer);
				}else if(head[1].equals("/landing.js")){
					processFile("landing.js", "text/javascript", socket, writer);
				}else if(head[1].equals("/sign.js")){
					processFile("sign.js", "text/javascript", socket, writer);
				}else{
					//404 Not Found
					print404Text("404 Not Found!", socket, writer);
				}
			}
			//Just to make everything look pretty to read in console
			System.out.println("------------------------------------------------------------------------------");
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		writer.println("Set-Cookie: visit=true; Max-Age: 10000");
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
	 * Query decoding, currently unused and unmodified for this project
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
		System.out.println("Decoded text: " + text);
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

