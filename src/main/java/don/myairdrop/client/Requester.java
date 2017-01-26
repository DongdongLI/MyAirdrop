package don.myairdrop.client;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import don.myairdrop.model.Node;

public class Requester implements Node, Runnable{

	long FILE_NAME_TEMP;
	
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
		
		FILE_NAME_TEMP = System.currentTimeMillis();
	}
	
	@Override
	public void run() {
		try{
			Thread.currentThread().setName("requester");
			requestSocket = new Socket("localhost", 6666);
			System.out.println("Connected to localhost:6666");
			
			showOptions();
			
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
	
	public void showOptions() {
		System.out.println("====================");
		System.out.println("Start as ");
		System.out.println("====================");
		System.out.println("1 (Greeting)");
		System.out.println("2 (Receiving a file)");
		
		String choice = null;
		while(choice == null || choice.length()!=1 || !Character.isDigit(choice.charAt(0))){
			choice = s.nextLine().trim();
		}
		
		switch (choice){
			case "1":
				System.out.println("============================");
				System.out.println("Greeting");
				System.out.println("============================");
				chatListenThread.start();
				break;
			case "2":
				System.out.println("============================");
				System.out.println("Receiving a file");
				System.out.println("============================");
				receieveFiles();
				break;
			default:
				break;
		}
	}
	
	public void receieveFiles() {
		
		Map<String, Long> files = null;
		try{
			ois = new ObjectInputStream(requestSocket.getInputStream());
			files = (LinkedHashMap<String, Long>)ois.readObject();
			System.out.println(files);
		} catch (ClassNotFoundException e){
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		
		
		byte[] buff = new byte[1000];

		long fileSize=0;
		int tempSize = 0;
		
		try {
			is = requestSocket.getInputStream();
			for(String fileName: files.keySet()){
				
				fileSize = files.get(fileName);
				
				fos = new FileOutputStream(new File(fileName));
				bos = new BufferedOutputStream(fos);
				
				if(tempSize!=0){
					bos.write(buff, 0, tempSize);
					bos.flush();
					tempSize = 0;
				}
				
				int len = is.read(buff);
				int sizeReceieved = len;
				
				
				
				while( len != -1 && sizeReceieved<=fileSize){
					
					bos.write(buff, 0, len);
					bos.flush();
					len = is.read(buff);
					sizeReceieved+=len;
				}
				
				if(sizeReceieved>fileSize)
					tempSize = len;	
			}
		}
		
		catch (IOException e) {
			e.printStackTrace();
		} 
		
		finally {
			try{
				is.close();
				fos.close();
				bos.close();
			} catch(IOException e){
				e.printStackTrace();
			}
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
		new Thread(new Requester()).start();
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
