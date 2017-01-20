package don.myairdrop.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import javax.xml.ws.handler.MessageContext;

public class Provider {
	
	ServerSocket providerSocket;
	Socket conn = null;
	ObjectOutputStream oos = null;
	ObjectInputStream ois = null;
	Scanner s = new Scanner(System.in);
	String msg;
	
	public Provider() {}
	
	void run() {
		try {
			providerSocket = new ServerSocket(6666, 10);
			System.out.println("Listening on port 6666...");
			
			conn = providerSocket.accept();

			while(conn==null)providerSocket.accept();
			
			System.out.println("Connected. From "+conn.getInetAddress()+":"+conn.getPort());
			
			oos = new ObjectOutputStream(conn.getOutputStream());
			oos.flush();
			ois = new ObjectInputStream(conn.getInputStream());

			//sendMessage("bye");
			new Thread(new SendThread()).start();
			do{
				try{
					msg = (String)ois.readObject();
					System.out.println("From client: "+msg);
					if(msg.toLowerCase().equals("bye"))
						sendMessage("bye");
				}catch(ClassNotFoundException e){
					e.printStackTrace();
				}
			}
			while(!msg.toLowerCase().equals("bye"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try{
				oos.close();
				ois.close();
				providerSocket.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
	}
	
	void sendMessage(String str) {
		try{
			oos.writeObject(str);
			oos.flush();
			//System.out.println("server> "+str);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	static void showMenu() {
		System.out.println("============================");
		System.out.println("============Menu============");
		System.out.println("============================");
		System.out.println("1. Greeting");
		System.out.println("2. Send a file");
	}
	
	public static void main(String[] args) {
		//showMenu();
		Provider provider = new Provider();
		//while(true)
			provider.run();
	}
	
	class SendThread implements Runnable{
		@Override
		public void run() {
			while(true){
				System.out.print("/>");
				sendMessage(s.nextLine());
			}
		}
	}
}
