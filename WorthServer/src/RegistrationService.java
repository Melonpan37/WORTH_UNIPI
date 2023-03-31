
import java.rmi.*;
import java.util.*;
import java.rmi.server.RemoteServer;
import java.io.*;

public class RegistrationService extends RemoteServer implements RegistrationServiceInterface {
	
	//mappatura dei nomi utente e password
	private HashMap<String, String> registeredUsers;
	//oggetto remoto per segnalare cambiamenti allo stato degli utenti registrati
	private UserListCallback ulc;
	
	//construttore
	public RegistrationService(UserListCallback ulc) {
		//inizializzazione della mappa username-password
		registeredUsers = new HashMap<String, String>();
		
		this.ulc = ulc;
		
		//recupero delle informazioni utenti da file system
		try(FileInputStream in = new FileInputStream("./res/users.txt")){
			String mapToString = "";
			byte[] buf = new byte[1024];
			//memorizzo il contenuto di users.txt in una stringa
			while(in.read(buf) != -1) {
				mapToString += new String(buf);
			}
			//inizializzo la map nome utente - password 
			this.registeredUsers = new HashMap<String, String>();
			
			//recupero delle informazioni sugli utenti registrati
			StringTokenizer tok = new StringTokenizer(mapToString, "|");
			while(tok.hasMoreTokens()) {
				String username = tok.nextToken();
				if(!tok.hasMoreTokens()) break;
				String password = tok.nextToken();
				registeredUsers.put(username, password);
			}
			//aggiorna la lista utenti remota con tutti gli utenti registrati a WORTH
			ulc.update(registeredUsers.keySet());
		}
		catch(FileNotFoundException e) {
			System.out.println("users.txt non trovato: creazione del file users.txt");
			File file = new File("./res/users.txt");
			try {
				file.createNewFile();
			}
			catch(IOException e1) {
				System.out.println("Impossibile create il file users.txt");
				e1.printStackTrace();
			}
		}
		catch(Exception e) {
			System.out.println("Errore durante la lettura degli utenti da disco");
			e.printStackTrace();
		}
		
	}
	
	//REQUIRES: usr != null and pwd != null
	//THROWS: RemoteException se la callback fallisce
	//MODIFIES: this.registerdUsers
	//EFFECT: se non ci sono altri utenti registrati con l'username usr, registra con successo il nuovo utente, altrimenti restituisce false
	public synchronized boolean register(String usr, String pwd) throws RemoteException {
		if(usr == null || pwd == null) return false;
		if(registeredUsers.containsKey(usr)) return false;
		
		//aggiorno la lista utenti regitrati
		registeredUsers.put(usr, pwd);
		System.out.println("Nuova registrazione "+usr+" "+pwd);
		
		//aggiorno la lista degli utenti online/offline in userListCallback
		ulc.addUser(usr, false);
		//invio la chiamata a tutti gli iscritti al callback degli aggiornamenti in lista utenti
		ulc.callback(usr, false);
		
		//aggiorno la lista degli utenti registrati su disco
		this.saveRegisteredUser(usr, pwd);
		
		//ritorno con successo dalla registrazione
		return true;
	}
	
	//permette a usr di accedere a WORTH 
	public synchronized int login(String usr, String pwd) {
		//se l'utente non è ancora registrato
		if(!registeredUsers.containsKey(usr)) return -2;
		//se la password è errata
		if(registeredUsers.get(usr).compareTo(pwd) != 0) return -1;
		
		//c'è già un' altra istanza del client loggata con questo account
		if(ulc.getOnlineStatus(usr)) return -3;
		
		//aggiorno la lista degli utenti online/offline in userListCallback
		//e chiamo in callback tutti gli iscritti agli aggiornamenti della lista utenti
		try {
			ulc.addUser(usr, true);
			ulc.callback(usr, true);
		}
		catch(RemoteException e) {
			System.out.println("Errore nel callback per segnalazione di nuova login");
		}
		return 0;
		
	}
	
	//metodi getter
	public synchronized Set<String> getUsers() {
		return registeredUsers.keySet();
	}
	
	public synchronized Set<String> getRegisterdUsers() {
		return this.registeredUsers.keySet();
	}
	
	//scrive su disco modifiche relative a nuove registrazioni a WORTH
	public synchronized void saveRegisteredUser(String usr, String pwd) {
		try(OutputStream out = new FileOutputStream("./res/users.txt", true);) {
			String newEntry = usr + "|" + pwd + "|";
			out.write(newEntry.getBytes());
		}
		catch(IOException e) {
			System.out.println("Errore nella scrittura utenti su disco");
			e.printStackTrace();
		}
	}
	

	
}
