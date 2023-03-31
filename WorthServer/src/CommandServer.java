
import java.util.*;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

public class CommandServer {

	final static int buffer_size = 256;
	
	private RegistrationService registrationService;
	private UserListCallback userListCallback;
	private final int port;
	private List<Project> projectList;
	private ByteBuffer buf;
	
	public CommandServer(int port, RegistrationService rs, UserListCallback ulc, List<Project> projectList) {
		this.registrationService = rs;
		this.userListCallback = ulc;
		this.port = port;
		this.projectList = projectList;
		this.buf = ByteBuffer.allocate(buffer_size);
	}
	
	//ciclo principale
	public void start() {
		
		//creazione della ServerSocketChannel non bloccante
		try(ServerSocketChannel ssc = ServerSocketChannel.open()){
			ssc.socket().bind(new InetSocketAddress(port));
			ssc.configureBlocking(false);
			
			Selector selector = Selector.open();
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			
			System.out.println("WORTH server è in funzione in attesa di connessioni.");
			
			while(true) {
				if(selector.select() == 0) continue;
				
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> it = keys.iterator();
				
				while(it.hasNext()) {
					SelectionKey key = it.next();
					it.remove();
					
					try {
						//nuova connessione
						if(key.isValid() && key.isAcceptable()) {
							ServerSocketChannel rssc = (ServerSocketChannel) key.channel();
							SocketChannel clientChannel = rssc.accept();
							clientChannel.configureBlocking(false);
							
							System.out.println("Accettata connessione dal client " + clientChannel.getRemoteAddress().toString());
							//il primo attachment '-1' indica che il client non è ancora loggato, il secondo '' è l'attachment per il messaggio di risposta eventuale
							String[] attachments = {"-1", ""};
							//registro l'intenzione all'ascolto del client
							clientChannel.register(selector, SelectionKey.OP_READ, attachments);
						}
						//se il client è intenzionato a comunicare col server
						else if(key.isValid() && key.isReadable()) {
							//se non è ancora loggato, lo costringo a loggarsi
							if(((String[])key.attachment())[0].equals("-1")) login(key, selector);
							//altrimenti eseguo il comando da lui espresso
							else readCommandInput(key, selector);
						}
						//se il client è in ascolto
						else if(key.isValid() && key.isWritable()) {
							SocketChannel clientChannel = (SocketChannel) key.channel();
							String[] attachments = (String[]) key.attachment();
							
							String id = attachments[0];
							
							//controllo se il messaggio di risposta è più lungo di 256 bytes, se si lo taglio
							boolean cut = false;
							String msg = attachments[1];
							if(attachments[1].getBytes().length >= 256){
								byte[] bytes = attachments[1].getBytes();
								//parte del messaggio da inviare subito
								msg = new String(bytes, 0, 254) + "$";
								//parte del messaggio da inviare in seguito
								attachments[1] = new String(bytes, 254, bytes.length-254);
								cut = true;
							}
							
							//invio il messaggio
							buf.clear();
							buf.put(msg.getBytes());
							buf.flip();
							clientChannel.write(buf);
							System.out.println("Risposta inviata al client "+ id);
							
							//se il messaggio è intero posso registrare il canale nuovamente per l'ascolto
							if(!cut) clientChannel.register(selector, SelectionKey.OP_READ, attachments);
							//altrimenti registro il canale per un una ulteriore scrittura
							else clientChannel.register(selector, SelectionKey.OP_WRITE, attachments);
							
						}
					}
					//se un client si disconnette senza effettuare il logout
					catch(IOException e) {
						System.out.println("Disconnessione improvvisa dal client " + ((String[])key.attachment())[0]);
						//recupero il suo id
						String id = ((String[])key.attachment())[0];
						//lo registro come offline nella userListCallback (non annullo la sua registrazione al callback, se ne occuperà userListCallback quando incontra l'eccezione)
						userListCallback.addUser(id, false);
						userListCallback.callback(id, false);
						//chiudo il canale associato e cancello la chiave
						key.channel().close();
						key.cancel();
					}
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void login(SelectionKey key, Selector sel) throws IOException {
		//ottengo il canale e gli attachment relativi alla chiave
		SocketChannel clientChannel = (SocketChannel) key.channel();
		String[] attachment = (String[]) key.attachment();
		
		//creo il buffer per leggere dal canale e leggo
		buf.clear();
		clientChannel.read(buf);
		buf.flip();
		
		//recupero l'username e la password dalla lettura del buffer
		String msg = new String(buf.array(), buf.position(), buf.limit());
		StringTokenizer tok = new StringTokenizer(msg, "|");
		tok.nextToken();
		String username = tok.nextToken();
		String password = tok.nextToken();
		
		//effettuo il login
		int codice = registrationService.login(username, password);
		if(codice == -3)
			attachment[1] = "ERROR|C'è già un altro client loggato con questo account. Sarai mica un ladro?";
		else if(codice == -2)
			attachment[1] = "ERROR|Utente non registrato|";
		else if(codice == -1)
			attachment[1] = "ERROR|Password errata|";
		//login effettuato con successo
		else {
			attachment[0] = username;
			attachment[1] = "SUCCESS|Login effettuato con successo|";
		}
		//registro il canale per la risposta
		clientChannel.register(sel, SelectionKey.OP_WRITE, attachment);
	}
	
	//legge il messaggio del client ed esegue l'operazione richiesta
	public void readCommandInput(SelectionKey key, Selector sel) throws IOException {
		//recupero la socket associata al client
		SocketChannel clientChannel = (SocketChannel) key.channel();
		//recupero gli attachment
		String[] attachment = (String[]) key.attachment();
		
		String id = attachment[0];
		
		//leggo il messaggio inviato dal client
		buf.clear();
		clientChannel.read(buf);
		buf.flip();
		
		
		
		//estrapolo il comando e gli eventuali argomenti dal messaggio
		String msg = new String(buf.array(), buf.position(), buf.limit());
		StringTokenizer tok = new StringTokenizer(msg, "|");
		
		String response = "";
		
		//se il messaggio è vuoto (non dovrebbe accadere)
		if(!tok.hasMoreTokens()) {
			System.out.println("Errore");
			response = "ERROR|";
			return;
		}
		String command = tok.nextToken();
		LinkedList<String> args = new LinkedList<String>();
		//creo la lista degli argomenti (se ce ne sono)
		while(tok.hasMoreTokens())
			args.add(tok.nextToken());
			
		//eseguo il comando richiesto
		switch(command) {
			case "LOGOUT" : {
				System.out.println("Disconnessione dal client " + id);
				userListCallback.addUser(id, false);
				userListCallback.callback(id, false);
				key.channel().close();
				key.cancel();
				return;
			}
			case "NEW_PROJECT" : {
				if(args.isEmpty()) {
					response = "ERROR|Richiede un parametro|";
					break;
				}
				String projectName = args.remove();
				System.out.println(id + " sta creando il progetto " + projectName);
				response = createProject(projectName, id);
				break;
			}
			case "LIST_PROJECTS" :{
				if(args.isEmpty()) {
					response = "ERROR|Richiede un parametro|";
					break;
				}
				String userName = args.remove();
				response = this.listProjects(userName);
				break;
			}
			case "ADD_MEMBER" : {
				//recupero i parametri
				String[] arguments = new String[3];
				boolean ok = true;
				for(int i = 0; i<3; i++) {
					//troppi pochi parametri
					if(args.isEmpty()) {
						ok = false;
						response = "ERROR|Troppi pochi parametri per il comando AddMember|";
						break;
					}
					arguments[i] = args.remove(0);
				}
				//troppi pochi parametri
				if(!ok) break;
				//aggiunta della carta al progetto
				response = this.addMember(arguments[0], arguments[1], arguments[2]);
				break;
			}
			case "ADD_CARD" : {
				//recupero i parametri
				String[] arguments = new String[4];
				boolean ok = true;
				for(int i = 0; i<4; i++) {
					//troppi pochi parametri
					if(args.isEmpty()) {
						ok = false;
						response = "ERROR|Troppi pochi parametri per il comando AddCard|";
						break;
					}
					arguments[i] = args.remove(0);
				}
				//troppi pochi parametri
				if(!ok) break;
				//aggiunta della carta al progetto
				response = this.addCard(arguments[0], arguments[1], arguments[2], arguments[3]);
				//se c'è già un'altra carta con lo stesso nome
				break;
			}
			case "MOVE_CARD" : {
				//recupero i parametri
				String[] arguments = new String[5];
				boolean ok = true;
				for(int i = 0; i<5; i++) {
					//troppi pochi parametri
					if(args.isEmpty()) {
						ok = false;
						response = "ERROR|Troppi pochi parametri per il comando MoveCard|";
						break;
					}
					//recupero il parametro
					arguments[i] = args.remove(0);
				}
				//troppi pochi parametri
				if(!ok) break;
				//spostamento della carta
				
				response = this.moveCard(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);
				break;
			}
			case "SHOW_CARD" : {
				//recuper i parametri
				String arguments[] = new String[3];
				boolean ok = true;
				for(int i = 0; i<3; i++) {
					//se troppi pochi parametri
					if(args.isEmpty()) {
						ok = false;
						response = "ERROR|Troppi pochi parametri per il comando ShowCard|";
						break;
					}
					//recupero il parametro
					arguments[i] = args.remove(0);
				}
				//se troppi pochi parametri
				if(!ok) break;
				//recupero le informazioni richieste e le inserisco nel messaggio di risposta
				response = showCard(arguments[0], arguments[1], arguments[2]);
				break;
			}
			case "SHOW_CARDS" : {
				String arguments[] = new String[2];
				boolean ok = true;
				for(int i = 0; i<2; i++) {
					if(args.isEmpty()) {
						ok = false;
						response = "ERROR|Troppi pochi parametri per il comando ShowCards|";
						break;
					}
					arguments[i] = args.remove(0);
				}
				if(!ok) break;
				response = showCards(arguments[0], arguments[1]);
				break;
			}
			case "SHOW_MEMBERS" : {
				String arguments[] = new String[2];
				boolean ok = true;
				for(int i = 0; i<2; i++) {
					if(args.isEmpty()) {
						ok = false;
						response = "ERROR|Troppi pochi parametri per il comando ShowCards|";
						break;
					}
					arguments[i] = args.remove(0);
				}
				if(!ok) break;
				response = showMembers(arguments[0], arguments[1]);
				break;
			}
			case "GET_CARD_HISTORY" : {
				String arguments[] = new String[3];
				boolean ok = true;
				for(int i = 0; i<3; i++) {
					if(args.isEmpty()) {
						ok = false;
						response = "ERROR|Troppi pochi parametri per il comando GetCardHistory|";
						break;
					}
					arguments[i] = args.remove(0);
				}
				if(!ok) break;
				response = getCardHistory(arguments[0], arguments[1], arguments[2]);
				break;
			}
			case "GET_CHAT" : {
				String arguments[] = new String[2];
				boolean ok = true;
				for(int i = 0; i<2; i++) {
					if(args.isEmpty()) {
						ok = false;
						response = "ERROR|Troppi pochi parametri per il comando JoinChat|";
						break;
					}
					arguments[i] = args.remove(0);
				}
				if(!ok) break;
				response = joinChat(arguments[0], arguments[1]);
				break;
			}
			case "CLOSE_PROJECT" : {
				String arguments[] = new String[2];
				boolean ok = true;
				for(int i = 0; i<2; i++) {
					if(args.isEmpty()) {
						ok = false;
						response = "ERROR|Troppi pochi parametri per il comando CloseProject|";
						break;
					}
					arguments[i] = args.remove(0);
				}
				if(!ok) break;
				response = closeProject(arguments[0], arguments[1]);
				break;
			}
		}
		
		//preparo il server ad inviare la risposta
		attachment[1] = response;
		clientChannel.register(sel, SelectionKey.OP_WRITE, attachment);
		
	}
	
	//crea un nuovo progetto WORTH
	public String createProject(String projectName, String username) {
		//controllo che non esista già un progetto con lo stesso nome
		for(Project project : projectList)
			if(project.getName().equals(projectName)) 
				return "ERROR|Esiste già un progetto con lo stesso nome|";
		
		//creo il progetto
		try {
			Project pj = new Project(projectName, username);
			projectList.add(pj); 
		}
		catch(IOException e) {
			//errore di scrittura su filesystem
			return "ERROR|Errore fatale server|";
		}
		
		return "SUCCESS|Progetto creato con successo, buon divertimento|";
	}
	
	//aggiunge un nuovo membro ad un progetto
	public String addMember(String userName, String projectName, String toAdd) {
		//controllo che l'utente da aggiungere sia un utente registrato
		if(!this.registrationService.getRegisterdUsers().contains(toAdd)) return "ERROR|L'utente " + toAdd + " non è iscritto a WORTH|";
		
		//cerco il progetto
		int index = -1, i = 0;
		for(Project pj : projectList) {
			if(pj.getName().equals(projectName)) {
				index = i;
				break;
			}
			i++;
		}
		if(index == -1) return "ERROR|Il progetto non esiste|";
		int code;
		//tento di aggiungere il nuovo membro
		try {
			if((code = projectList.get(index).addMember(userName, toAdd)) == 0) return "SUCCESS|"+toAdd+" è stato aggiunto!|";
			if(code == -1) return "ERROR|L'utente non è membro del progetto|";
			else return "ERROR|" + toAdd + " fa già parte del progetto " + projectName + "|";
		}
		catch(Exception e) {
			System.out.println("Errore nella scrittura su FileSystem");
			e.printStackTrace();
			return "ERROR|Errore fatale server|";
		}
	}
	
	//aggiunge una carta ad un progetto
	public String addCard(String userName, String projectName, String cardName, String description) {
		//cerco il progetto
		int index = -1, i = 0;
		for(Project pj : projectList) {
			if(pj.getName().equals(projectName)) {
				index = i;
				break;
			}
			i++;
		}
		if(index == -1) return "ERROR|Il progetto non esiste|";
		int code;
		//tenta di aggiungere la carta al progetto
		try {
			if((code = projectList.get(index).addCard(userName, cardName, description)) == 0) return "SUCCESS|Carta aggiunta con successo|";
			if(code == -1) return "ERROR|L'utente non è membro del progetto|";
			else return "ERROR|Esiste già una carta di nome " + cardName +" nel progetto" + projectName +"|";
		}
		catch(Exception e) {
			System.out.println("Errore nella scrittura su filesystem");
			e.printStackTrace();
			return "ERROR|Errore fatale server|";
		}
	}
	
	//sposta una carta da una lista ad un altra nello stesso progetto
	public String moveCard(String userName, String projectName, String cardName, String from, String to) {
		//cerca il progetto
		int index= -1, i = 0;
		for(Project pj : projectList) {
			if(pj.getName().equals(projectName)) {
				index = i;
				break;
			}
			i++;
		}
		if(index == -1) return "ERROR|Il progetto non esiste|";
		int code;
		//tenta di spostare la carta
		try {
			if((code = projectList.get(index).moveCard(userName, cardName, from, to)) == 0) return "SUCCESS|Spostamento eseguito con successo|";
			if(code == -1) return "ERROR|L'utente non è membro del progetto|";
			if(code == -2) return "ERROR|Lo spostamento da " + from + " a " + to + "non è uno spostamento ammissibile|";
			if(code == -3) return "ERROR|La carta non è presente nella lista di partenza|";
			else return "ERROR|La lista di partenza non è valida. Le lista valide sono todo, inprogress, toberevised e done|";
		}
		catch(Exception e) {
			System.out.println("Errore nella scrittura su FileSystem");
			e.printStackTrace();
			return "ERROR|Errore fatale server|";
		}
	}
	
	
	//mostra tutte le carte del progetto nelle rispettive liste
	public String showCards(String userName, String projectName) {
		//cerca il progetto
		int index= -1, i = 0;
		for(Project pj : projectList) {
			if(pj.getName().equals(projectName)) {
				index = i;
				break;
			}
			i++;
		}
		if(index == -1) return "ERROR|Non è stato trovato nessun progetto con nome " + projectName + "|";
		return "SUCCESS|" + projectList.get(index).showCards(userName) + "|";
	}
	
	//mostra una carta e la sua descrizione 
	public String showCard(String userName, String projectName, String cardName) {
		//cerca il progetto
		int index= -1, i = 0;
		for(Project pj : projectList) {
			if(pj.getName().equals(projectName)) {
				index = i;
				break;
			}
			i++;
		}
		if(index == -1) return "ERROR|Non è stato trovato nessun progetto con nome " + projectName + "|";
		return "SUCCESS|" + projectList.get(index).showCard(userName, cardName) + "|";
	}
	
	//mostra tutti i membri di un progetto
	public String showMembers(String userName, String projectName) {
		//cerca il progetto
		int index= -1, i = 0;
		for(Project pj : projectList) {
			if(pj.getName().equals(projectName)) {
				index = i;
				break;
			}
			i++;
		}
		if(index == -1) return "ERROR|Non è stato trovato nessun progetto con nome " + projectName + "|";
		return "SUCCESS|" + projectList.get(index).showMembers(userName) + "|";
	}
	
	//mostra la history di una card
	public String getCardHistory(String userName, String projectName, String cardName) {
		//cerca il progetto
		int index= -1, i = 0;
		for(Project pj : projectList) {
			if(pj.getName().equals(projectName)) {
				index = i;
				break;
			}
			i++;
		}
		if(index == -1) return "ERROR|Non è stato trovato nessun progetto con nome " + projectName + "|";
		return "SUCCESS|" + projectList.get(index).getCardHistory(userName, cardName) + "|";
	}
	
	//restituisce l'indirizzo multicast per entrare nella chat di un progetto (come stringa)
	public String joinChat(String userName, String projectName) {
		//cerca il progetto
		int index= -1, i = 0;
		for(Project pj : projectList) {
			if(pj.getName().equals(projectName)) {
				index = i;
				break;
			}
			i++;
		}
		if(index == -1) return "ERROR|Non è stato trovato nessun progetto con nome " + projectName + "|";
		String res = projectList.get(index).getChatAddress(userName) ;
		if(res != null) return "SUCCESS|" + res + "|";
		return "ERROR|L'utente non è autorizzato ad operare con il progetto" + projectName + "|";
	}
	
	//mostra la lista dei progetti dei quali userName è membro
	public String listProjects(String userName) {
		//itera sulla lista progetti
		String res = "";
		for(Project pj : this.projectList) {
			//se userName è membro allora aggiungo il nome del progetto alla stringa risultato
			if(pj.isMember(userName)) {
				res += pj.getName() + "\n";
			}
		}
		if(res.equals("")) res = "L'utente non è membro di nessun progetto";
		return "SUCCESS|" + res + "|";
	}
	
	//chiude un progetto e ne cancella i files relativi su filesystem
	public String closeProject(String userName, String projectName) {
		//cerca il progetto
		int index= -1, i = 0;
		for(Project pj : projectList) {
			if(pj.getName().equals(projectName)) {
				index = i;
				break;
			}
			i++;
		}
		if(index == -1) return "ERROR|Non è stato trovato nessun progetto con nome " + projectName + "|";
		int code;
		if((code = projectList.get(index).closeProject(userName)) == 0) {
			projectList.remove(index);
			return "SUCCESS|Il progetto " + projectName + " è finito, congratulazioni!|";
		}
		if(code == -1) return "ERROR|L'utente non fa parte del progetto|";
		return "ERROR|il progetto non è ancora finito. Tutte le carte devono essere nella lista Done|";
	}
	
}
