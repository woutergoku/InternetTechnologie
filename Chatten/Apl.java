import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;


public class Apl {

	private static int SERVER_PORT = 5222;
	private ArrayList<ClientThread> threads = new ArrayList<ClientThread>();
	
	public void serverLoop() {
			
		try {
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			Socket socket = new Socket();
			
			while(true) {
				System.out.println("luistert");
				socket = serverSocket.accept();
				ClientThread ct = new ClientThread(socket);
				threads.add(ct);
				ct.start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}	
	}
	
	public static void main(String[] args) {
		new Apl().serverLoop();
		//System.out.println("Hallo");
	}
	
	public class ClientThread extends Thread {
		 private Socket socket;
		 private OutputStream outputStream;

		 public ClientThread(Socket socket) {
			 this.socket = socket;
		 }

		 public void run() {
			 // 1. Wacht op berichten van de client.
			 // 2. Stuur berichten van de clients door naar de andere
			 // clients. (Broadcast)
			 
			 try {
				InputStream inputStream = new BufferedInputStream(socket.getInputStream());
				outputStream = new BufferedOutputStream(socket.getOutputStream());
				BufferedReader reader = new BufferedReader(
						 new InputStreamReader(inputStream));
				
				PrintWriter print = new PrintWriter(outputStream);
				
				while(true) {
					
					String data = getXMLfromReader(reader);
					System.out.println("---");
					System.out.println(data);
					
					if(data.startsWith("<stream")) {
						print.println("<?xml version='1.0'?>");
						print.println("<stream:stream xmlns:stream='http://etherx.jabber.org/streams' version='1.0' id='psiTest' xmlns='jabber:client' from='localhost' xml:lang='en' xmlns:xml='http://www.w3.org/XML/1998/namespace'>");
						print.println("<stream:features	/>");
						print.flush();
					}
					else if(data.startsWith("<iq")) {
						StringBuilder builder = new StringBuilder();
						builder.append(data);
						String read = "";
						
						while((read = reader.readLine()) != null) {
							String end = read.substring(read.length() - 5, read.length());
							System.out.println("End: " + end);
							if(!end.equals("</iq>")) {
								System.out.println("read: " + read);
								builder.append(read);
							} 
						}
						builder.append(read);
						System.out.println(builder.toString());
						String id = getIdFromXML(builder.toString());
						if(id.equals("auth_1")) {
							print.println("<iq type='result' id='auth_1'>");
							print.println("<query xmlns='jabber:iq:auth'>");
							print.println("<username/><password/><resource/>");
							print.println("</query></iq>");
							print.flush();
						}
						
						System.out.println(builder.substring(builder.length() - 5, builder.length()));
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("Er is een client afgesloten");
			}
		 }
		 
		 public void doorsturen(String s){
			 for(ClientThread c : threads) {
				 if(c != this) {
					 
					 PrintWriter print = new PrintWriter(c.outputStream);
					 
					 if(s.startsWith("<stream")) {
						 print.println("");
						 print.flush();
					 }
					 
				 }

			 }
		 }
		 
		 public Document loadXMLFromString(String xml) throws Exception
		 {
		     DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		     DocumentBuilder builder = factory.newDocumentBuilder();
		     InputSource is = new InputSource(new StringReader(xml));
		     return builder.parse(is);
		 }
		 
		 public String getIdFromXML(String xml) {
			 try {
				Document doc = loadXMLFromString(xml);
				XPath xPath = XPathFactory.newInstance().newXPath();
				
				if(doc != null) {
					Element iq = (Element) xPath.compile("/iq").evaluate(doc, XPathConstants.NODE);
					String auth = (String) iq.getAttribute("id");
					return auth;
				}
			    
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
			 return "";
		 }
		 
		 public String getXMLfromReader(BufferedReader reader) {
			 //StringBuilder builder = new StringBuilder();
			 String builder = "";
			 int character = 0;
			 try {
				//int character = reader.read();
				while((character = reader.read()) != -1) {
					builder += (char)character;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 return builder.toString();
		 }
	}
}


