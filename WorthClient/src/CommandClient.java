
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.IOException;
import java.lang.IndexOutOfBoundsException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandClient {
	
	private boolean logged;
	private String username;
	
	private final int port;
	private final int registry_port;
	private final int chat_port;
	private final int buf_size = 256;
	
	private boolean run;
	
	private SocketChannel channel;
	private UserListRemoteInterface stub;
	private UserListRemoteInterface ulr;
	private ByteBuffer buf;
	
	private List<Chat> chats;
	
	public CommandClient(int port, int registry_port, int chat_port) {
		this.port = port;
		this.registry_port = registry_port;
		this.chat_port = chat_port;
		buf = ByteBuffer.allocate(buf_size);
		chats = new LinkedList<Chat>();
		run = true;
	}
	
	public void start(Scanner sc) throws IOException {
		
		//connessione al server
		channel = SocketChannel.open(new InetSocketAddress(InetAddress.getLocalHost(), port));
		System.out.println("Stabilita connessione col server");
		System.out.println("Benvenuto in WORTH");
		System.out.println("Per registrarsi usare: Register 'username' 'password'");
		System.out.println("Per accedere usare: Login 'username' 'password'");
		
		Pattern pattern = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
		Matcher matcher;
		
		while(run) {
			String cmd = sc.nextLine();
			StringTokenizer tok = new StringTokenizer(cmd, " ");
			
			List<String> args = new LinkedList<String>();
			while(tok.hasMoreTokens()) {
				args.add(tok.nextToken());
			}
			
			if(!args.isEmpty())
				//controllo che non vengano usati caratteri speciali
				//se è un qualsiasi comando tranne SendChatMsg
				if(!args.get(0).equals("SendChatMsg") && !args.get(0).equals("sendChatMsg") && !args.get(0).equals("sendchatmsg")) {
					//se è il comando AddCard
					if(args.get(0).equals("AddCard") || args.get(0).equals("addCard") || args.get(0).equals("addcard")) {
						if(args.size() < 3) {
							System.out.println("AddCard richiede più argomenti");
							return;
						}
						//controllo che non ci siano caratteri speciali nel nome progetto e nel nome della carta
						matcher = pattern.matcher(args.get(1));
						if(matcher.find()) {
							System.out.println("Non si possono usare caratteri speciale nei nomi progetto");
							return;
						}
						matcher = pattern.matcher(args.get(2));
						if(matcher.find()) {
							System.out.println("Non si possono usare caratteri speciali nel nome della carta");
							return;
						}
						//controllo che non ci siano i caratteri | e $ nella descrizione della carta
						for(int i = 3; i < args.size()-1; i++) {
							if(args.get(i).contains("$") || args.get(i).contains("|")) {
								System.out.println("Non si possono usare i caratteri '$' e '|' nella descrizione di una carta");
								return;
							}
						}
						
					}
					//per qualsiasi altro comando controllo che non ci siano caratteri speciali
					matcher = pattern.matcher(cmd);
					if(matcher.find()) {
						System.out.println("Non si possono usare caratteri speciali per questo comando. Usa solo a-z, A-Z, 0-9");
						continue;
					}
				}
				//esegue il comando
				commandHandler(args, channel);
		}
	}
	
	public void commandHandler(List<String> args, SocketChannel channel) {
		String cmd = args.remove(0);
		switch(cmd){
			case "Login" :
			case "login" :
				if(logged) {
					System.out.println("Sei gia loggato su WORTH");
					return;
				}
				try {
				login(args);
				}
				catch(Exception e) {
					System.out.println("Errore nella fase di login");
					e.printStackTrace();
				}
				break;
			case "Register" :
			case "register" :
				if(logged) {
					System.out.println("Registrazione annullata: sei gia loggato su WORTH");
					return;
				}
				register(args);
				break;
			case "ListUsers" :
			case "listusers" :
			case "listUsers" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				listUsers();
				break;
			case "ListOnlineUsers" :
			case "listOnlineUsers" :
			case "listonlineusers" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				listOnlineUsers();
				break;
			case "Logout" :
			case "logout" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				logout();
				logged = false;
				username = null;
				run = false;
				break;
			case "Quit" :
			case "quit" :
				if(!logged) {
					System.exit(0);
				}
				logout();
				System.exit(0);
			case "CreateProject" :
			case "createproject" :
			case "createProject" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				createProject(args.remove(0));
				break;
			case "ListProjects" :
			case "listProjects" :
			case "listprojects" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				showProjects();
				break;
			case "AddCard" :
			case "addCard" :
			case "addcard" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				try {
					addCard(args);
				}
				catch(IndexOutOfBoundsException e) {
					System.out.println("usa AddCard NomeProgetto NomeCarta DescrizioneCarta");
					return;
				}
				break;
			case "AddMember" :
			case "addMember" :
			case "addmember" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				try {
					addMember(args.get(0), args.get(1));
				}
				catch(IndexOutOfBoundsException e) {
					System.out.println("usa AddMember NomeProgetto NomeUtenteDaAggiungere");
					return;
				}
				break;
			case "MoveCard" :
			case "moveCard" :
			case "movecard" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				try {
					moveCard(args.get(0), args.get(1), args.get(2), args.get(3));
				}
				catch(IndexOutOfBoundsException e) {
					System.out.println("usa MoveCard NomeProgetto NomeCarta DaLista ALista");
					return;
				}
				break;
			case "ShowCard" :
			case "showCard" :
			case "showcard" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				try {
					showCard(args.get(0), args.get(1));
				}
				catch(IndexOutOfBoundsException e) {
					System.out.println("usa ShowCard NomeProgetto NomeCarta");
					return;
				}
				break;
			case "ShowCards" : 
			case "showCards" :
			case "showcards" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				try {
					showCards(args.get(0));
				}
				catch(IndexOutOfBoundsException e) {
					System.out.println("usa ShowCards ProjectName");
					return;
				}
				break;
			case "ShowMembers" :
			case "showMembers" :
			case "showmembers" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				try {
					showMembers(args.get(0));
				}
				catch(IndexOutOfBoundsException e) {
					System.out.println("usa ShowMembers NomeProgetto");
					return;
				}
				break;
			case "GetCartHistory" :
			case "getCardHisotry" :
			case "getcardhistory" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				try {
					getCardHistory(args.get(0), args.get(1));
				}
				catch(IndexOutOfBoundsException e) {
					System.out.println("usa GetCardHistory NomeProgetto NomeCarta");
					return;
				}
				break;
			case "CancelProject" :
			case "cancelProject" :
			case "cancelproject" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				try {
					cancelProject(args.get(0));
				}
				catch(IndexOutOfBoundsException e) {
					System.out.println("usa CancelProject NomeProgetto");
					return;
				}
				break;
			case "JoinChat" :
			case "joinChat" :
			case "joinchat" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				try {
					joinChat(args.get(0));
				}
				catch(IndexOutOfBoundsException e) {
					System.out.println("usa JoinChat NomeProgetto");
					return;
				}
				break;
			case "ReadChat" :
			case "readChat" :
			case "readchat" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				try {
					readChat(args.get(0));
				}
				catch(IndexOutOfBoundsException e) {
					System.out.println("usa ReadChat NomeProgetto");
					return;
				}
				break;
			case "SendChatMsg" :
			case "sendChatMsg" :
			case "sendchatmsg" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				try {
					String pjName = args.remove(0);
					String msg = "";
					while(!args.isEmpty())
						msg += " " + args.remove(0);
					sendChatMsg(pjName, msg);
				}
				catch(IndexOutOfBoundsException e) {
					System.out.println("usa SendChatMsg NomeProgetto Messaggio");
					return;
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				break;
			case "LeaveChat" :
			case "leaveChat" :
			case "leavechat" :
				if(!logged) {
					System.out.println("Prima devi accedere a WORTH");
					return;
				}
				try {
					String pjName = args.remove(0);
					leaveChat(pjName);
				}
				catch(IndexOutOfBoundsException e) {
					System.out.println("usa LeaveChat NomeProgetto");
					return;
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				break;
			
			default :
				System.out.println("Comando inesistente");
				break;
		}
	}
	
	//REQUIRES: args.size() > 2
	//THROWS: IOException, NotBoundException
	//MODIFIES: this.username, this.logged, this.ulr, this.stub
	//EFFECT: tentativo di accedere al sistemo WORTH contattando il server
	public void login(List<String> args) throws IOException, NotBoundException {
		//prendo username e password dagli argomenti del comando
		String[] data = new String[2];
		int i=0;
		//se ci sono errori nel numero di argomenti
		for(i=0; i<2; i++) {
			if(args.isEmpty()) { 
				System.out.println("Errore nel numero di argomenti di Login");
				return;
			}
			data[i] = args.remove(0);
		}
		//costruisco il messaggio da inviare al server
		String msg = "LOGIN|" + data[0] + "|" + data[1] + "|";
		if(msg.getBytes().length >= buf_size) {
			System.out.println("Il messaggio è troppo lungo");
			return;
		}
		buf.clear();
		buf.put(msg.getBytes());
		buf.flip();
		while(buf.hasRemaining())
			channel.write(buf);
		
		//mi preparo alla lettura della risposta
		msg = readFromServer();
		
		//controllo la risposta
		StringTokenizer tok = new StringTokenizer(msg, "|");
		if(tok.hasMoreTokens()) {
			if(tok.nextToken().equals("ERROR")) {
				System.out.println("errore: "+tok.nextToken());
				return;
			}
			System.out.println(tok.nextToken());
		}
		
		//costruisco l'oggetto remoto per l'aggiornamento della lista utenti
		Registry reg = LocateRegistry.getRegistry(registry_port);
		UserListCallbackInterface server = (UserListCallbackInterface) reg.lookup("USER_LIST");
		this.ulr = new UserListRemote(new HashMap<String, Boolean>());
		this.stub = (UserListRemoteInterface) UnicastRemoteObject.exportObject(ulr, 0);
		HashMap<String, Boolean> userList = server.registerForCallback(stub);
		//aggiorno la lista degli utenti online interna a UserListRemote
		((UserListRemote)ulr).updateUserListFromClient(userList);
		
		//svuoto il buffer
		buf.clear();
		
		//aggiorno il valore logged
		logged = true;
		username = data[0];
	}

	
	//REQUIRES: args.size() > 2
	//THROWS: nil
	//MODIFIES: nil
	//EFFECT: registra l'utente al sistema WORTH
	public void register(List<String> args) {
		//prendo username e password dagli argomenti del comando
		String[] data = new String[2];
		int i=0;
		//se ci sono errori nel numero di argomenti
		for(i=0; i<2; i++) {
			if(args.isEmpty()) { 
				System.out.println("Errore nel numero di argomenti di Register");
				return;
			}
			data[i] = args.remove(0);
		}
		
		if(data[0].getBytes().length > 12 || data[1].getBytes().length > 12) {
			System.out.println("Username o password troppo lunghi");
			return;
		}
		
		try {
			Registry registry = LocateRegistry.getRegistry(registry_port);
			RegistrationServiceInterface registrationService = (RegistrationServiceInterface) registry.lookup("REGISTRATION_SERVICE");
		
			
			
			if(registrationService.register(data[0], data[1])) {
				System.out.println("Registrazione avvenuta con successo");
			}
			else {
				System.out.println("Errore nella registrazione");
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	//REQUIRES: nil
	//THROWS: nil
	//MODIFIES: nil
	//EFFECT: stampa in System.out la lista completa degli utenti registrati a WORTH
	public void listUsers() {
		if(!logged) {
			System.out.println("Per vedere la lista degli utenti è necessaria l'autenticazione");
			return;
		}
		System.out.println("Lista degli utenti registrati a WORTH:");
		HashMap<String, Boolean> userList = ((UserListRemote) ulr).getUserList();
		for(Map.Entry<String, Boolean> entry : userList.entrySet()) {
			if(entry.getValue()) System.out.println(entry.getKey() + " -status- online");
			else System.out.println(entry.getKey() + " -status- offline");
		}
	}
	
	//REQUIRES: nil
	//THROWS: nil
	//MODIFIES: nil
	//EFFECT: stampa in System.out la lista completa degli utenti attualmente loggati su WORTH
	public void listOnlineUsers() {
		if(!logged) {
			System.out.println("Per vedere la lista degli utenti online è necessaria l'autenticazione");
			return;
		}
		System.out.println("Lista degli utenti attualmente connessi a WORTH:");
		HashMap<String, Boolean> userList = ((UserListRemote) ulr).getUserList();
		for(Map.Entry<String, Boolean> entry : userList.entrySet()) {
			if(entry.getValue()) System.out.println(entry.getKey());
		}
	}
	
	//REQUIRES: nil
	//THROWS: nil
	//MODIFIES: this.username, this.logged, this.ulr, this.stub
	//EFFECT: disconnette l'utente dal sistema WORTH
	public void logout() {
		//tentativo di rimuovere il proprio username dalla lista utenti remota
		//e di annullare la registrazione al callback dell'oggetto remoto UserListCallback
		try {
			Registry reg = LocateRegistry.getRegistry(registry_port);
			UserListCallbackInterface server = (UserListCallbackInterface) reg.lookup("USER_LIST");
			server.unregisterForCallback(stub);
		}
		//si procede al logout anche se si incontrano problemi
		catch(Exception e) {
			System.out.println("Impossibile annulare la registrazione al sistema di notifiche UserListCallback. La procedura di logout coninuerà comunque. ");
		}
		//invio del messaggio TCP di logout al server
		buf.clear();
		buf.put(new String("LOGOUT|").getBytes());
		buf.flip();
		try {
			while(buf.hasRemaining())
				channel.write(buf);
		}
		//si procede al logout anche se si incontrano problemi
		catch(IOException e) {
			System.out.println("Errore durante la comunicazione col server per il Logout");
			//termino l'esecuzione segnalando l'insuccesso
			System.exit(-1);
		}
	}
	
	//REQUIRES: name != null
	//THROWS: nil
	//MODIFIES: nil
	//EFFECT: comunica al server la volontà di creare un nuovo progetto
	public void createProject(String name) {
		try {
			String msg = "NEW_PROJECT|"+name+"|";
			if(name.getBytes().length > 12) {
				System.out.println("Nome del progetto troppo lungo");
				return;
			}
			buf.clear();
			buf.put(msg.getBytes());
			
			buf.flip();
			while(buf.hasRemaining())
				channel.write(buf);
		
			msg = readFromServer();
			
			StringTokenizer tok = new StringTokenizer(msg, "|");
			if(tok.hasMoreTokens()) {
				if(tok.nextToken().equals("ERROR")) {
					System.out.println(tok.nextToken());
					return;
				}
				else System.out.println(tok.nextToken());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
	}
	
	public void addCard(List<String> args) {
		String project = "";
		String cardName = "";
		String description = "";
		try {
			//recupero i parametri del comando addCard
			project = args.remove(0);
			cardName = args.remove(0);
			while(args.size() > 0)
				if(args.size() == 1)
					description += args.remove(0);
				else 
					description += args.remove(0) + " ";
		}
		//se ho IndexOutOfBoundException
		catch(Exception e) {
			System.out.println("Il numero degli argomenti non è sufficiente per questo comando");
			return;
		}
		
		try {
			//costruzione del messaggio da inviare al server
			String msg = "ADD_CARD|"+this.username+"|"+project+"|"+cardName+"|"+description+"|";
			if(cardName.getBytes().length > 12) {
				System.out.println("Nome della carta troppo lungo");
				return;
			}
			if(description.getBytes().length > 32) {
				System.out.println("Nome della carta troppo lungo");
				return;
			}
			if(msg.getBytes().length >= buf_size) {
				System.out.println("Nome del progetto troppo lungo");
				return;
			}
			
			buf.clear();
			buf.put(msg.getBytes());
			buf.flip();
			
			//invio del messaggio al server
			while(buf.hasRemaining())
				channel.write(buf);
			
			//preparazione a ricevere la risposta dal server
			msg = readFromServer();
			
			StringTokenizer tok = new StringTokenizer(msg, "|");
			if(tok.hasMoreTokens()) {
				if(tok.nextToken().equals("ERROR")) {
					System.out.println(tok.nextToken());
					return;
				}
			}
			System.out.println(msg);
			buf.clear();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addMember(String project, String toAdd) {
		try {
			String msg = "ADD_MEMBER|"+this.username+"|"+project+"|"+toAdd+"|";
			
			if(toAdd.getBytes().length > 12) {
				System.out.println("Il nome dell'utente da aggiungere non è valido (troppo lungo)");
				return;
			}
			
			if(msg.getBytes().length >= buf_size) {
				System.out.println("Nome del progetto troppo lungo");
				return;
			}
			
			buf.clear();
			buf.put(msg.getBytes());
			buf.flip();
			
			while(buf.hasRemaining())
				channel.write(buf);
			
			msg = readFromServer();
			
			StringTokenizer tok = new StringTokenizer(msg, "|");
			if(tok.hasMoreTokens()) {
				if(tok.nextToken().equals("ERROR")) {
					System.out.println(tok.nextToken());
					return;
				}
			}
			System.out.println(tok.nextToken());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void moveCard(String project, String cardName, String from, String to) {
		try {
			String msg = "MOVE_CARD|"+this.username+"|"+project+"|"+cardName+"|"+from+"|"+to+"|";
			if(cardName.getBytes().length >= buf_size) {
				System.out.println("Nome della carta non valido (troppo lungo)");
				return;
			}
			if(msg.getBytes().length >= buf_size) {
				System.out.println("Messaggio troppo lungo");
				return;
			}
			buf.clear();
			buf.put(msg.getBytes());
			buf.flip();
			
			while(buf.hasRemaining())
				channel.write(buf);
			
			msg = readFromServer();
			
			StringTokenizer tok = new StringTokenizer(msg, "|");
			if(tok.hasMoreTokens()) {
				if(tok.nextToken().equals("ERROR")) {
					System.out.println(tok.nextToken());
					return;
				}
			}
			System.out.println(tok.nextToken());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void showCard(String project, String cardName) {
		try {
			String msg = "SHOW_CARD|"+this.username+"|"+project+"|"+cardName+"|";
			if(project.getBytes().length > 12) {
				System.out.println("Nome progetto troppo lungo");
				return;
			}
			if(cardName.getBytes().length > 12) {
				System.out.println("Nome carta troppo lungo");
				return;
			}
			buf.clear();
			buf.put(msg.getBytes());
			buf.flip();
			
			while(buf.hasRemaining())
				channel.write(buf);
			
			msg = readFromServer();
			
			StringTokenizer tok = new StringTokenizer(msg, "|");
			if(tok.hasMoreTokens()) {
				if(tok.nextToken().equals("ERROR")) {
					System.out.println(tok.nextToken());
					return;
				}
			}
			System.out.println(tok.nextToken());
			buf.clear();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void showCards(String project) {
		try {
			String msg = "SHOW_CARDS|"+this.username+"|"+project+"|";
			
			if(project.getBytes().length > 12) {
				System.out.println("Nome progetto troppo lungo");
				return;
			}
			
			buf.clear();
			buf.put(msg.getBytes());
			buf.flip();
			

			while(buf.hasRemaining())
				channel.write(buf);
			
			msg = readFromServer();
			
			StringTokenizer tok = new StringTokenizer(msg, "|");
			if(tok.hasMoreTokens()) {
				if(tok.nextToken().equals("ERROR")) {
					System.out.println(tok.nextToken());
					return;
				}
			}
			System.out.println(tok.nextToken());
			buf.clear();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void showMembers(String project) {
		try {
			String msg = "SHOW_MEMBERS|"+this.username+"|"+project+"|";
			if(msg.getBytes().length >= buf_size) {
				System.out.println("Messaggio troppo lungo");
				return;
			}
			if(project.getBytes().length > 12) {
				System.out.println("Nome progetto troppo lungo");
				return;
			}
			
			buf.clear();
			buf.put(msg.getBytes());
			buf.flip();
			
			while(buf.hasRemaining())
				channel.write(buf);
			
			msg = readFromServer();
			
			StringTokenizer tok = new StringTokenizer(msg, "|");
			if(tok.hasMoreTokens()) {
				if(tok.nextToken().equals("ERROR")) {
					System.out.println(tok.nextToken());
					return;
				}
			}
			System.out.println(tok.nextToken());
			buf.clear();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void getCardHistory(String projectName, String cardName) {
		try {
			String msg = "GET_CARD_HISTORY|"+this.username+"|"+projectName+"|"+cardName+"|";
			if(cardName.getBytes().length > 12) {
				System.out.println("Nome carta troppo lungo");
				return;
			}
			if(msg.getBytes().length >= buf_size) {
				System.out.println("Nome progetto troppo lungo");
				return;
			}
			
			buf.clear();
			buf.put(msg.getBytes());
			buf.flip();
			
			
			while(buf.hasRemaining())
				channel.write(buf);
			
			msg = readFromServer();
			
			StringTokenizer tok = new StringTokenizer(msg, "|");
			if(tok.hasMoreTokens()) {
				if(tok.nextToken().equals("ERROR")) {
					System.out.println("errore:" + tok.nextToken());
					return;
				}
				System.out.println(tok.nextToken());
				return;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void joinChat(String projectName) {
		for(Chat chat : chats) {
			if(chat.getProjectName().equals(projectName)) {
				System.out.println("L'utente è già membro della chat di progetto");
				return;
			}
		}
		try {
			String msg = "GET_CHAT|"+this.username+"|"+projectName+"|";
			if(msg.getBytes().length >= buf_size) {
				System.out.println("Nome progetto troppo lungo");
				return;
			}
			buf.clear();
			buf.put(msg.getBytes());
			buf.flip();
			
			while(buf.hasRemaining())
				channel.write(buf);
			
			msg = readFromServer();
			
			StringTokenizer tok = new StringTokenizer(msg, "|");
			if(tok.hasMoreTokens()) {
				if(tok.nextToken().equals("ERROR")) {
					System.out.println(tok.nextToken());
					return;
				}
				String address = tok.nextToken();
				Chat chat = new Chat(this.username, projectName, address, chat_port);
				chats.add(chat);
				chat.start();
			}
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void readChat(String projectName) {
		for(Chat chat : chats) {
			if(chat.getProjectName().equals(projectName)) {
				System.out.println(chat.readChat());
				return;
			}
		}
		System.out.println("L'utente deve prima entrare nella chat del progetto " + projectName + ".\nUsa JoinChat " + projectName);
	}
	
	public void sendChatMsg(String projectName, String msg) {
		for(Chat chat : chats) {
			if(chat.getProjectName().equals(projectName)) {
				try {
					chat.send(msg);
				}
				catch(IllegalArgumentException e) {
					System.out.println("Il messaggio è troppo lungo");
				}
				catch(IOException e) {
					System.out.println("Problema durante l'invio del messaggio: "+ e.getCause().toString());
				}
				return;
			}
		}
		System.out.println("L'utente deve prima entrare nella chat del progetto " + projectName + ".\nUsa JoinChat " + projectName);
	}
	
	public void leaveChat(String projectName) {
		for(Chat chat : chats) {
			if(chat.getProjectName().equals(projectName)) {
				chat.shut();
				if(chat.isAlive())
					chat.interrupt();
				chats.remove(chats.indexOf(chat));
				System.out.println("Hai lasciato la chat del progetto " + projectName);
				return;
			}
		}
		System.out.println("L'utente non è membro di nessun progetto dal nome " + projectName);
	}
	
	public void showProjects() {
		try {
			String msg = "LIST_PROJECTS|"+this.username+"|";
			
			buf.clear();
			buf.put(msg.getBytes());
			buf.flip();
			
			while(buf.hasRemaining())
				channel.write(buf);
			
			msg = readFromServer();
			
			StringTokenizer tok = new StringTokenizer(msg, "|");
			if(tok.hasMoreTokens()) {
				if(tok.nextToken().equals("ERROR")) {
					System.out.println(tok.nextToken());
					return;
				}
			}
			System.out.println(tok.nextToken());
			buf.clear();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void cancelProject(String projectName) {
		try {
			String msg = "CLOSE_PROJECT|"+this.username+"|"+projectName+"|";
			if(msg.getBytes().length >= buf_size) {
				System.out.println("Nome progetto troppo lungo");
				return;
			}
			buf.clear();
			buf.put(msg.getBytes());
			buf.flip();
			
			while(buf.hasRemaining())
				channel.write(buf);
			
			msg = readFromServer();
			
			StringTokenizer tok = new StringTokenizer(msg, "|");
			if(tok.hasMoreTokens()) {
				if(tok.nextToken().equals("ERROR")) {
					System.out.println(tok.nextToken());
					return;
				}
			}
			System.out.println(tok.nextToken());
			buf.clear();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String readFromServer() throws IOException {
		buf.clear();
		channel.read(buf);
		buf.flip();
		String msg = new String(buf.array(), buf.position(), buf.limit());
		if(msg.contains("$")) {
			msg = msg.substring(0, msg.length()-1);
			msg += readFromServer();
		}
		return msg;
	}
	
}




