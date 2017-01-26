package don.myairdrop.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import don.myairdrop.model.Node;

public class Provider implements Node, Runnable {
	
	ServerSocket providerSocket;
	static Socket conn = null;
	
	static OutputStream os = null;
	
	static ObjectOutputStream oos = null;
	static ObjectInputStream ois = null;
	
	static FileInputStream fis = null;
	static BufferedInputStream bis = null;
	
	static Scanner s = new Scanner(System.in);
	String msg;
	
	Thread chatterSendThread;
	static Thread chatterListenThread;
	
	public Provider() {
		chatterSendThread = new Thread(new ChatterSenderThread());
		chatterListenThread = new Thread(new ChatListenerThread());
	}
	
	@Override
	public void run() {
				
		Thread.currentThread().setName("provider");
		try {
				providerSocket = new ServerSocket(6666, 10);
				System.out.println("Listening on port 6666...");
				
				conn = providerSocket.accept();
	
				while(conn==null){
					conn = providerSocket.accept();
				}
			
				System.out.println("Connected. From "+conn.getInetAddress()+":"+conn.getPort());
		
				showMenu();
				
			}
			catch (IOException e){
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
	
	public static void sendFile(List<String> paths){
		/*
		 * Send the list of 
		 * 		file name and 
		 * 		the size of them 
		 * to the client
		 * 
		 * */
		Map<String, Long> files = new LinkedHashMap<String, Long>();
		File f;
		for(String path: paths){
			String fileName = path.substring(path.lastIndexOf("/")+1, path.length());
			f = new File(path);
			files.put(fileName, f.length() );
		}
		
		try{
			oos = new ObjectOutputStream(conn.getOutputStream());
			oos.flush();

			oos.writeObject(files);
			oos.flush();
			
		} catch(IOException e){
			e.printStackTrace();
		}
		
		
		try{
			for(String path: paths){
				File file = new File(path);
				byte[] buff = new byte[1000];
				fis = new FileInputStream(file);
				bis = new BufferedInputStream(fis);
				os = conn.getOutputStream();
				
				int len = bis.read(buff, 0, buff.length);
				while(len != -1){
					os.write(buff, 0, len);
					os.flush();
					len = bis.read(buff, 0, buff.length);
				}
			}	
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try{
				fis.close();
				bis.close();
				os.close();
			} catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
	static void showMenu() {
		System.out.println("============================");
		System.out.println("1. Greeting");
		System.out.println("2. Send a file");
		System.out.println("============================");
		
		String choice = null;
		while(choice == null || choice.length()!=1 || !Character.isDigit(choice.charAt(0))){
			choice = s.nextLine().trim();
		}
		
		switch (choice){
			case "1":
				System.out.println("============================");
				System.out.println("Greeting");
				System.out.println("============================");
				chatterListenThread.start();
				break;
			case "2":
				System.out.println("============================");
				System.out.println("Send a file");
				System.out.println("============================");
				List<String> paths = new ArrayList<String>();
				paths.add("C:/Users/dli/DeskTop/pdf.pdf");
				paths.add("C:/Users/dli/DeskTop/car_rental.pdf");
				sendFile(paths);
				break;
			default:
				break;
		}
	}
	
	private void killProcess(String name){
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		for(Thread thread: threadSet){
			if(thread.getName().equals(name)){
				thread.interrupt();
			}
		}
	}
	
	public static void main(String[] args) {
		Thread.currentThread().setName("ProviderMain");
		new Thread(new Provider()).start();
	}
	
	public class ChatterSenderThread implements Runnable{
		@Override
		public void run() {
			chatterSendThread.setName("ProviderSendThread");
			String temp;
			while(true){
				System.out.print("/>");
				temp = s.nextLine();
				if(temp.trim().toLowerCase().equals("/exit")){
					killProcess("ProviderListeningThread");
					break;
				}
				sendMessage(temp);
			}
			showMenu();
			return;
		}
	}
	
	public class ChatListenerThread implements Runnable{
		@Override
		public void run() {
			Thread.currentThread().setName("ProviderListeningThread");
			try{
				oos = new ObjectOutputStream(conn.getOutputStream());
				oos.flush();
				ois = new ObjectInputStream(conn.getInputStream());

				
				chatterSendThread.start();
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
				while(!Thread.currentThread().isInterrupted()); //while(!msg.toLowerCase().equals("bye"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			finally{
				try{
					oos.close();
					ois.close();
					providerSocket.close();
					System.out.println("ProviderListeningThread is down...");
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
		
	}
}
