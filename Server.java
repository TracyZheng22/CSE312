import java.io.BufferedReader;
import java.io.File;
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
import java.util.HashMap;

public class Server {
	static int port = 8000;
	static ServerSocket ssocket;
	
	public static void main(String[] args){
		try {
			ssocket = new ServerSocket(port);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		while(true) {
			try (Socket socket = ssocket.accept()){
				handler(socket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}

	public static void handler(Socket socket) throws IOException {
		PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
		InputStream alt = socket.getInputStream();
		InputStreamReader altReader = new InputStreamReader(alt);
		BufferedReader reader = new BufferedReader(altReader); 
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
		
		boolean new_user = true;
		if(cookies.containsKey("visit")) {
			new_user = false;
		}
		
		//To make it more like a module in case we want to add it to a function later on, we split the
		//requests by section.
		if(head[0].equals("GET")  && head[2].equals("HTTP/1.1")) {
			if(new_user == false && head[1].equals("/") || head[1].equals("/index.html")){
				processFile("index.html", "text/html", socket, writer);
			}else if(new_user == true && head[1].equals("/") || head[1].equals("/index.html")){
				processFile("src/index.html", "text/html", socket, writer);
			}else if(head[1].equals("/feed.html")){
				processFile("feed.html", "text/html", socket, writer);
			}else if(head[1].equals("/Sign-in.html")){
				processFile("Sign-in.html", "text/html", socket, writer);
			}else if(head[1].equals("/feedstyle.css")) {
				processFile("feedstyle.css", "text/css", socket, writer);
			}else if(head[1].equals("/style.css")) {
				processFile("style.css", "text/css", socket, writer);
			}else if(head[1].equals("/landing.css")){
				processFile("landing.css", "text/css", socket, writer);			
			}else{
				//404 Not Found
				print404Text("404 Not Found!", socket, writer);
			}
		}
		//Just to make everything look pretty to read in console
		System.out.println("------------------------------------------------------------------------------");
		socket.close();
	}
	
	public static void processFile(String name, String type, Socket socket, PrintWriter writer) throws IOException {
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
		/*String line;
		if(type == "image/jpg") {
			//https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html
			byte[] image = Files.readAllBytes(f.toPath());
			socket.getOutputStream().write(image);
			return;
		}
		while((line = file.readLine()) != null) {
			writer.println(line);
		}*/
		byte[] bytes = Files.readAllBytes(f.toPath());
		socket.getOutputStream().write(bytes);
		return;
	}
	
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
	
	public static void print404Text(String text, Socket socket, PrintWriter writer) {
		writer.println("HTTP/1.1 404 Not Found");
		writer.println("Content-Type: text/plain; charset=utf-8");
		writer.println("Connection: close");
		writer.println("Content-Length: " + text.length());
		writer.println(); //This is to indicate the end of the headers.
		writer.println(text);
	}
	
	public static void printPlainText(String text, Socket socket, PrintWriter writer) {
		writer.println("HTTP/1.1 200 OK");
		writer.println("Content-Type: text/plain; charset=utf-8");
		writer.println("Connection: close");
		writer.println("Content-Length: " + text.length());
		writer.println(); //This is to indicate the end of the headers.
		writer.println(text);
	}
	
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
	
	public static String injectionDefense(String text) {
		text = text.replaceAll("&", "&amp;");
		text = text.replaceAll("<", "&lt;");
		text = text.replaceAll(">", "&gt;");
		return text;
	}
}
