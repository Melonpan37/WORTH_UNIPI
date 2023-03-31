
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class Chat extends Thread {
	
	//socket con cui comnunicare con gli altri membri della chat
	private MulticastSocket socket;
	//indirizzo multicast della chat
	private InetAddress address;
	//porta da associare alla socket
	private int chat_port = 7001;
	//lock per accedere in modo safe alla lista dei messaggi della chat
	private ReentrantLock lock;
	//lista dei messaggi della chat
	private List<String> chatHistory;
	
	//nome del progetto di cui Chat è la chat
	private String projectName;
	//username del proprietario del client
	private String userName;
	
	
	//costruttore
	//lancia IOException se si verifica un errore nella connessione alla chat di progetto
	public Chat(String userName, String projectName, String address, int port) {
		try {
			this.userName = userName;
			this.projectName = projectName;
			this.chat_port = port;
			this.socket = new MulticastSocket(chat_port);
			this.address = InetAddress.getByName(address);
			this.lock = new ReentrantLock();
			this.chatHistory = new LinkedList<String>();
		}
		catch(IOException e) {
			System.out.println("Impossibile istanziare una chat di progetto");
			e.printStackTrace();
		}
	}
	
	//ciclo principale del thread
	public void run() {
		try {
			//inizializzazione buffer per contenere il messaggio
			byte[] msg = new byte[128];
			//inizializzazione datagram packet 
			DatagramPacket packet = new DatagramPacket(msg, msg.length);
			//connessione al gruppo
			socket.joinGroup(address);
			System.out.println("In ascolto sulla chat del progetto " + this.projectName);
			while(true) {
				try {
					//attesa bloccante di ricevere un messaggio
					socket.receive(packet);
					
					//messaggio da bytes a String
					String message = new String(packet.getData());
					//memorizzazione del messaggio nella lista dei messaggi
					receive(message);
					
					//reinizializzazione del buffer
					msg = new byte[128];
					packet.setData(msg);
				}
				catch(SocketException e) {
					//il socket è stato chiuso
					if(socket.isClosed()) {
						return;
					}
					e.printStackTrace();
					return;
				}
			}		
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	//chiamata quando si riceve un messaggio per memorizzarlo nella lista dei messaggi
	private void receive(String msg) {
		//richiede accesso alla risorsa critica
		lock.lock();
		//aggiuta del messaggio alla lista dei messaggi
		chatHistory.add(msg);
		//rilascio della risorsa critica
		lock.unlock();
	}
	
	//chiamata esternamente al thread per inviare un messaggio alla chat
	public synchronized void send(String msg) throws IOException, IllegalArgumentException{
		//costruzione del messaggio
		msg = this.userName + ": " + msg;
		//controllo che la lunghezza del messaggio sia corretta
		if(msg.getBytes().length > 128) {
			throw new IllegalArgumentException();
		} 
		//creazione del buffer 
		byte[] toSend = msg.getBytes();
		//crezione del datagram packet contenente il messaggio
		DatagramPacket packet = new DatagramPacket(toSend, toSend.length, address, chat_port);
		//invio del messaggio
		socket.send(packet);
		System.out.println("Messaggio inviato");
	}
	
	//chiamata esternamente al thread per leggere i messaggi inviati e ricevuti
	public String readChat() {
		String res = "";
		//richiesta di accesso alla risorsa critica
		lock.lock();
		
		//lettura della lista messaggi
		while(!chatHistory.isEmpty()) {
			res += chatHistory.remove(0) + "\n";
		}
		
		//rilascio della risorsa critica
		lock.unlock();
		return res;
	}
	
	//chiamata per interrompere il thread
	public void shut() {
		socket.close();
	}
	
	//getter
	public String getProjectName() {
		return this.projectName;
	}
}
