import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bson.types.Binary;

/**
 * Websocket class that handles websocket connections.
 * 
 */
public class WebSocket{
	Socket socket;
	String key;
	byte[] sha1;
	//Ryan Note: Combined for String.contains()
	static String fileTypes = ".png.jpg.jpeg.gif.mp3.mov.mp4.m4a.m4v.mpg.mpeg.wmv.avi.flv.3gp.3gpp.3g2.3gp2.txt.docx.pdf.csv";
	
	public WebSocket(Socket s, String k) {
		socket = s;
		key = k;
		sha1 = sha1(key);
		protocolSwitchResponse();
		Server.websockets.put(socket, this);
		run();
	}
	
	public void run() {
		while(true) {
			//Keep socket open and communicate using frames
			//https://tools.ietf.org/html/rfc6455#section-5.2
			try {
				InputStream in = socket.getInputStream();
				
				boolean ignore = false;
				
				byte[] line = new byte[2];
				in.read(line);
				
				if(line[0] == -126) {
					//This is cause java only has signed :(
					System.out.println("WebSocket Recieved");
				} else if(line[0] == -127){
					System.out.println("Type Identifier");
				}else {
					System.out.println("FAILED!: " + line[0]);
				}                           
				
				byte maskbit = getBit(line[1], 7); //Should be one from client
				System.out.println("Mask bit:" + maskbit);
				long payload_len = ((line[1]<<1) & 0xff)>>1;
				byte[] line2 = null;
				if(payload_len == 126) {
					//<65536
					line2 = new byte[2];
					in.read(line2);
					payload_len = ((0xff & line2[0]) << 8) | (0xff & line2[1]);	
				}else if (payload_len == 127) {
					//>=65536 bytes
					line2 = new byte[8];
					in.read(line2);
					payload_len = processLong(line2, 0);
				}
				
				System.out.println("Payload Length: " + payload_len);
				
				byte[] masking_key = new byte[4];
				in.read(masking_key);
				System.out.println("Masking_key: " + Integer.toBinaryString(masking_key[0])
						+ Integer.toBinaryString(masking_key[1])
						+ Integer.toBinaryString(masking_key[2])
						+ Integer.toBinaryString(masking_key[3]));
				
				//Deal with payload and masking key
				//From lecture, we read 4 bytes at a time and use the mask and XOR to find the original message
				byte[] payload = new byte[(int)payload_len];
				in.read(payload);
				
				if(ignore) {
					continue;
				}
				
				if(maskbit == 1) {
					for (int i = 0; i < (int)payload_len; i++) {
					    payload[i] = (byte) (payload[i] ^ masking_key[i % 4]);
					}
				}
				
				String msg = new String(payload);
				System.out.println("Payload: " + msg);
				
				//Get type (1 byte)
				//0=Post message
				//1=Post file
				//2=Like
				//3=Comment
				//4=Request Initial
				//5=Request Specific
				int type = (payload[0] & 0xFF);
				System.out.println("Type: " + type);
				
				int id_len = payload[1];
				System.out.println("id_len: " + id_len);
				
				String id = new String(Arrays.copyOfRange(payload, 2, id_len+2));
				System.out.println("id: " + id);
				
				payload = Arrays.copyOfRange(payload, id_len+2, payload.length);
				
				if(type == 0) {
					System.out.println("Post Message!");
					//Convert payload to string
					String message = new String(payload);
					message = injectionDefense(message);
					System.out.println("Message: " + message);
					
					Server.dbHandler.write(id, type, message, 0, null, line2, null);
					
					//Send back to sender for immediate view
					write(type, id.getBytes(), message.getBytes(), line2, null);
				}else if(type == 1) {
					System.out.println("Post File!");
					int file_len = (payload[0] & 0xFF);
					System.out.println("file_len: " + file_len);
					
					String filename = new String(Arrays.copyOfRange(payload, 1, file_len+1));
					System.out.println("filename: " + filename);
					
					payload = Arrays.copyOfRange(payload, file_len+1, payload.length);
					
					//Ryan Notes:
					//Check for allowed types, as front-end is not safe. (Don't use form accept and assume they follow through.)
					//For example, they could write and upload a javascript file.
					String filetype = filename.substring(filename.lastIndexOf("."), filename.length());
					System.out.println("filetype: " + filetype);
					
					if(fileTypes.contains(filetype)) {
						//Save the data onto the database.
						Server.dbHandler.write(id, 1, null, 0, payload, line2, filename);
						write(type, id.getBytes(), payload, line2, filename.getBytes());
						
						//TEMPORARY: save locally
						/*File file = new File(id + filename);
						OutputStream writer = new FileOutputStream(file);
						writer.write(payload);
						writer.flush();*/
					}
				}else if(type == 4) {
					System.out.println("Initial Request! " + id);
					ArrayList<Document> docs =  Server.dbHandler.getPosts(id, 0, 10);
					for(Document doc : docs) {
						int t = doc.getInteger("type");
						byte[] n = ((String) doc.get("name")).getBytes();
						byte[] fn = null;
						byte[] m = null;
						if(t == 0) {
							m = ((String) doc.get("message")).getBytes();
							System.out.println("Sending Message " + m);
						}else if(t == 1) {
							fn = ((String) doc.get("filename")).getBytes();
							Binary b = (Binary) doc.get("file");
							m = b.getData();
						}
						Binary temp = (Binary) doc.get("line2");
						byte[] l2 = null;
						if(temp != null) {
							l2 = temp.getData();
						}
						write(t,n,m,l2,fn);
					}
				}
			} catch (IOException e) {				
				//Clean up socket list.
				Server.websockets.remove(socket); 
				return;
			}
		}
	}
	
	public void write(int t, byte[] n, byte[] m, byte[] line2, byte[] fn) {
		//Package the data with metadata
		byte[] send = null;
		if(fn != null && fn.length != 0) {
			send = new byte[3+n.length+fn.length+m.length];
			send[0] = (byte) t;
			send[1] = (byte) n.length;
			for(int i=0; i<n.length; i++) {
				send[i+2] = n[i];
			}
			send[n.length+2]=(byte) fn.length;
			for(int i=0; i<fn.length; i++) {
				send[i+n.length+3] = fn[i];
			}
			for(int i=0; i<m.length; i++) {
				send[i+3+fn.length+n.length] = m[i];
			}
		}else {
			send = new byte[2+n.length+m.length];
			send[0] = (byte) t;
			send[1] = (byte) n.length;
			for(int i=0; i<n.length; i++) {
				send[i+2] = n[i];
			}
			for(int i=0; i<m.length; i++) {
				send[i+2+n.length] = m[i];
			}
		}
		
		write(socket, send, line2);
	}
	
	public void write(byte[] message, byte[] line2) {
		HashMap<Socket, WebSocket> x = Server.websockets;
		ArrayList<Socket> cleanup = new ArrayList<Socket>();
		for(Socket s : x.keySet()) {
			System.out.println("Writing to Socket: " + s.getPort());
			try {
				OutputStream out = s.getOutputStream();
				
				//Set up frame with opcode
				out.write(-126);
				
				if(message.length<126) {
					out.write(message.length);
				}else if (message.length < 65536) {
					out.write(126);
					out.write(line2);
				}else if (message.length >= 65536) {
					out.write(127);
					out.write(line2);
				}
				
				//Send message
				out.write(message);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				cleanup.add(s);
			}
		}
		for(Socket s : cleanup) {
			Server.websockets.remove(s); 
		}
	}
	
	public void write(Socket socket, byte[] message, byte[] line2) {
		System.out.println("Writing to Socket: " + socket.getPort());
		try {
			OutputStream out = socket.getOutputStream();
			
			//Set up frame with opcode
			out.write(-126);
			
			if(message.length<126) {
				out.write(message.length);
			}else if (message.length < 65536) {
				out.write(126);
				out.write(line2);
			}else if (message.length >= 65536) {
				out.write(127);
				out.write(line2);
			}
			
			//Send message
			out.write(message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			Server.websockets.remove(socket);
		}
	}
	
	public byte getBit(int b, int position)
	{
	   return (byte) ((b >> position) & 1);
	}
	
	public void protocolSwitchResponse() {
		//https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_a_WebSocket_server_in_Java
		byte[] response;
		try {
			response = ("HTTP/1.1 101 Switching Protocols\r\n"
					+ "Connection: Upgrade\r\n"
					+ "Upgrade: websocket\r\n"
					+ "Sec-WebSocket-Accept: "
					+ Base64.getEncoder().encodeToString(sha1)
					+ "\r\n\r\n").getBytes("UTF-8");
			socket.getOutputStream().write(response, 0, response.length);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public byte[] sha1(String k){
		//https://docs.oracle.com/javase/7/docs/api/java/security/MessageDigest.html
		try {
			MessageDigest hasher = MessageDigest.getInstance("SHA-1");
			byte[] hash = hasher.digest(k.getBytes("utf8"));
			return hash;
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Takes a raw byte array with an offset, and formats it as a long integer
	 * @param buffer
	 * @param offset
	 * @return
	 */
	public long processLong(byte[] buffer, int offset) {
        return (((long)buffer[offset + 0] << 56) +
                ((long)(buffer[offset + 1] & 255) << 48) +
                ((long)(buffer[offset + 2] & 255) << 40) +
                ((long)(buffer[offset + 3] & 255) << 32) +
                ((long)(buffer[offset + 4] & 255) << 24) +
                ((buffer[offset + 5] & 255) << 16) +
                ((buffer[offset + 6] & 255) <<  8) +
                ((buffer[offset + 7] & 255) <<  0));
    }
	
	/**
	 * First line of defense against injection attacks.
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
