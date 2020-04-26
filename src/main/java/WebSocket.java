import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

/**
 * Websocket class that handles websocket connections.
 * 
 */
public class WebSocket{
	Socket socket;
	String key;
	byte[] sha1;
	//Ryan Note: Combined for String.contains()
	static String fileTypes = ".png.jpg.jpeg.mp3.mov.mp4.m4a.m4v.mpg.mpeg.wmv.avi.flv.3gp.3gpp.3g2.3gp2.txt.docx.pdf.csv";
	
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
					ignore = true;
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
				//4=Request
				int type = (payload[0] & 0xFF);
				System.out.println("Type: " + type);
				
				int id_len = (payload[1] & 0xFF);
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
					String filetype = filename.substring(filename.lastIndexOf("."),filename.length());
					System.out.println("filetype: " + filetype);
					
					if(fileTypes.contains(filetype)) {
						//Save the data onto the database.
						
						//TEMPORARY: save locally
						File file = new File(id + filename);
						OutputStream writer = new FileOutputStream(file);
						writer.write(payload);
						writer.flush();
					}
				}
				
				write(payload, line2);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}
	}
	
	public void write(byte[] message, byte[] line2) {
		HashMap<Socket, WebSocket> x = Server.websockets;
		for(Socket s : x.keySet()) {
			System.out.println("Writing to Socket: " + s.getPort());
			try {
				OutputStream out = s.getOutputStream();
				
				//Set up frame with opcode
				out.write(-126);
				
				if(message.length<126) {
					out.write(message.length);
				}else if (message.length == 126) {
					out.write(126);
					out.write(line2);
				}else if (message.length == 127) {
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
	
	public static String injectionDefense(String text) {
		text = text.replaceAll("&", "&amp;");
		text = text.replaceAll("<", "&lt;");
		text = text.replaceAll(">", "&gt;");
		return text;
	}
}
