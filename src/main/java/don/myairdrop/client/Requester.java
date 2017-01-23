package don.myairdrop.client;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Set;

import don.myairdrop.model.Node;
import don.myairdrop.server.Provider;

public class Requester implements Node {

	Socket requestSocket;
	
	InputStream is;
	
	ObjectOutputStream oos;
	ObjectInputStream ois;
	
	FileOutputStream fos;
	BufferedOutputStream bos;
	
	String msg;
	Scanner s = new Scanner(System.in);
	Thread chatSendThread;
	static Thread chatListenThread;
	
	public Requester() {
		chatSendThread = new Thread(new ChatSendThread());
		chatListenThread = new Thread(new ChatListenThread());
	}
	
	@Override
	public void run() {
		try{
			
			requestSocket = new Socket("localhost", 6666);
			System.out.println("Connected to localhost:6666");
			receieveFile();
		}catch(IOException e){
			e.printStackTrace();
		}
			
	}
	
	@Override
	public void sendMessage(String str) {
		try{
			oos.writeObject(str);
			oos.flush();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void receieveFile() {
		byte[] buff = new byte[1000];
		try {
			is = requestSocket.getInputStream();
			fos = new FileOutputStream("received");
			bos = new BufferedOutputStream(fos);
			
			int len = is.read(buff);
			while( len != 0){
				System.out.println("write "+len+" bytes...");
				bos.write(buff, 0, len);
				bos.flush();
				len = is.read(buff);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean isInterrupted(String name){
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		for(Thread thread: threadSet){
			if(thread.getName().equals(name)){
				return thread.isInterrupted();
			}
		}
		return false;
	}
		
	public static void main(String[] args) {
		Thread.currentThread().setName("RequesterMain");
		Requester requestor = new Requester();
		requestor.run();
		
		new Thread(chatListenThread).run();
	}
	
	public class ChatSendThread implements Runnable{
		@Override
		public void run() {
			
			String temp;
			
			chatSendThread.setName("RequesterSendThread");
			while(!isInterrupted("ProviderListeningThread")){
				System.out.print("/>");
				temp = s.nextLine();
				if(temp.trim().toLowerCase().equals("/exit"))
					break;
				sendMessage(temp);
			}
			System.out.println("INTERRUPTTED!!!!!!!!!!!!");
			return;
		}
	}
	
	public class ChatListenThread implements Runnable {
		@Override
		public void run() {
			Thread.currentThread().setName("RequestorListeningThread");
			
			try{
				oos = new ObjectOutputStream(requestSocket.getOutputStream());
			
				oos.flush();
				ois = new ObjectInputStream(requestSocket.getInputStream());
				
				chatSendThread.start();
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
		
	}
}
