package don.myairdrop.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Requester {

	Socket requestSocket;
	ObjectOutputStream oos;
	ObjectInputStream ois;
	String msg;
	Scanner s = new Scanner(System.in);
	public Requester() {}
	
	void run() {
		try{
			requestSocket = new Socket("localhost", 6666);
			System.out.println("Connected to localhost:6666");
			
			oos = new ObjectOutputStream(requestSocket.getOutputStream());
			oos.flush();
			ois = new ObjectInputStream(requestSocket.getInputStream());
			
			new Thread(new SendThread()).start();
			
			do{
				try{
					msg = (String)ois.readObject();
					System.out.println("server> "+msg);
					//sendMessage("Hi my server");
				}
				catch(ClassNotFoundException e){
					e.printStackTrace();
				}
			}while(!msg.toLowerCase().equals("bye"));
		}
		catch(UnknownHostException e){
			e.printStackTrace();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally{
			try{
				oos.close();
				ois.close();
				requestSocket.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
	void sendMessage(String msg){
		try{
			oos.writeObject(msg);
			oos.flush();
			//System.out.println("client> "+msg);
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Requester requestor = new Requester();
		requestor.run();
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
