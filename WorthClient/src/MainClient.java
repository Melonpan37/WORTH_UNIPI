
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class MainClient {

	
	final static int DEF_REGISTRY_PORT = 6000;
	final static int DEF_TCP_PORT = 6001;
	final static int DEF_CHAT_PORT = 6002;
	
	private static void argsError() {
		System.out.println("Se si vuole lanciare WorthClient con argomenti, essi devono essere: \n'numero di porta per connessione TCP col server'\n'numero di porta per localizzazione degli oggetti remoti'\n'numero di porta per la chat UDP'");
	}
	
	public static void main(String[] args) throws Exception{
		
		int tcp_port = -1;
		int chat_port = -1;
		int registry_port = -1;
		
		if(args.length >= 1) {
			if(args.length != 3) {
				argsError();
				return;
			}
			try {
				tcp_port = Integer.parseInt(args[0]);
				registry_port = Integer.parseInt(args[1]);
				chat_port = Integer.parseInt(args[2]);
			}
			catch(Exception e) {
				argsError();
				return;
			}
		}
		
		if(tcp_port == -1 && chat_port == -1 && registry_port == -1) {
			tcp_port = DEF_TCP_PORT;
			chat_port = DEF_CHAT_PORT;
			registry_port = DEF_REGISTRY_PORT;
		}
		else {
			System.out.println("Laciando WorthClient con porte indicate dall'utente");
		}
	
		Scanner sc = new Scanner(System.in);
		
		while(true) {
			CommandClient cc = new CommandClient(tcp_port, registry_port, chat_port);
			cc.start(sc);
		}
		
	}
	
}
