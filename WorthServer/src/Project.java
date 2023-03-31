
import java.util.*;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Project {
	
	//nome del progetto
	private String name;
	//lista dei membri di progetto
	private Set<String> members;
	
	//lista delle carte in todo
	private Set<Card> todo;
	//lista delle carte in inprogress
	private Set<Card> inprogress;
	//lista delle carte in toberevised
	private Set<Card> toberevised;
	//lista delle carte in don
	private Set<Card> done;
	
	//indirizzo multicast associato alla chat di progetto (in formato stringa)
	private String chatAddress;
	
	//REQUIRES: name != null and user != null
	//THROWS: IOException se non si può scrivere su filesystem
	//		  NullPointerException se name == null or user == null 
	//MODIFIES: this
	//EFFECT: inizializza un nuovo progetto e scrive su filesystem le informazioni sul progetto
	public Project(String name, String user) throws IOException, NullPointerException {
		if(name == null || user == null) throw new NullPointerException();
		
		//assegnamento dei parametri
		this.name = name;
		
		//inizializzazione lista membri
		this.members = new TreeSet<String>();
		//aggiunta dell'utente creatore del progetto
		members.add(user);
		
		//inizializzazione delle liste
		this.todo = new TreeSet<Card>(new CardComparator());
		this.inprogress = new TreeSet<Card>(new CardComparator());
		this.toberevised = new TreeSet<Card>(new CardComparator());
		this.done = new TreeSet<Card>(new CardComparator());
		
		//recupera l'indirizzo multicast per la chat
		this.chatAddress = MulticastAddressSpace.getAddress();
		
		//creazione della directory di progetto
		new File("./res/projects/"+this.name).mkdir();
		
		//tentativo di scrivere su filesystem la lista dei membri del progetto
		try(FileOutputStream out = new FileOutputStream("./res/projects/" + this.name + "/members", true)){
			//scrittura degli utenti registrati al progetto su filesystem
			String toWrite = user + "|";
			out.write(toWrite.getBytes());
		}
		catch(Exception e) {
			System.out.println("Errore nella scrittura del nuovo progetto su FileSystem");
			throw e;
		}
	}
	
	//costruttore privato chiamato dal metodo statico retriveProject all'avvio del server
	private Project(String name, Set<String> members, Set<Card> todo, Set<Card> inprogress, Set<Card> toberevised, Set<Card> done) {
		this.name = name;
		this.members = members;
		
		this.todo = todo;
		this.inprogress = inprogress;
		this.toberevised = toberevised;
		this.done = done;
		
		this.chatAddress = MulticastAddressSpace.getAddress();
	}
	
	//getter semplici
	public String getName() {
		return this.name;
	}
	
	//REQUIRES: nil (il client garantisce che non saranno inviate stringhe nulle o vuote)
	//THROWS: IOException se la scrittura della nuova carta su filesystem è impossibile
	//		  FileNotFoundException se la cartella del progetto ha subito una modifica esterna che rende i dati inconsistenti
	//MODIFIES: this.todo
	//EFFECTS: restituisce -1 se l'utente non è membro, -2 se esiste già una carta con lo stesso nome, 0 se l'operazione va a buon fine e la carta viene aggiunta a todo
	public synchronized int addCard(String userName, String cardname, String desc) throws IOException, FileNotFoundException {
		if(!members.contains(userName)) return -1;
		
		//creazione nuova carta
		Card newCard = new Card(cardname, desc);
		newCard.addToHistory("todo");
		//controllo l'esistenza di una carta con lo stesso nome nel progetto
		if(inprogress.contains(newCard) || toberevised.contains(newCard) || done.contains(newCard)) return -2;
		//aggiungo la carta in todo
		if(!todo.add(newCard)) return -2;
		//scrivo la nuova carta su filesystem
		try(FileOutputStream out = new FileOutputStream("./res/projects/"+this.name+"/"+cardname+".txt")){
			String toWrite = cardname+"|"+desc+"|"+"todo"+"|";
			out.write(toWrite.getBytes());
		}
		catch(Exception e) {
			System.out.println("Impossibile scrivere la nuova carta su filesystem");
			throw e;
		}
		return 0;
	}
	
	//REQUIRES: nil (il client garantisce che non saranno inviate stringhe nulle o vuote)
	//THROWS: IOException se la scrittura della nuova carta su filesystem è impossibile
	//		  FileNotFoundException se la cartella del progetto ha subito una modifica esterna che rende i dati inconsistenti
	//MODIFIES: this.todo, this.inprogress, this.toberevised, this.done
	//EFFECT: restituisce -1 se l'utente non è membro del progetto, -2 se lo spostamento non è consentito, 
	//		  -3 se la carta non è nella lista di partenza, -4 se lista di partenza non esiste (nome errato), 
	//		  0 se va a buon fine, e quindi lo spostamento è registrato nella history della card e la modifica è scritta su filesystem
	public synchronized int moveCard(String userName, String cardName, String from, String to) throws FileNotFoundException, IOException{
		if(!members.contains(userName)) return -1;
		
		Card card = null;
		switch(from){ 
			//spostamento carta da lista todo 
			case "todo" : {
				//cerco la carta in todo
				for(Card cards : todo) {
					//se la trovo
					if(cards.getName().equals(cardName)) {
						card = cards;
						
						//spostamento in lista inprogress
						if(to.equals("inprogress")) {
							//scrivo il cambiamento alla history della carta su filesystem
							try(FileOutputStream out = new FileOutputStream("./res/projects/"+this.name+"/"+cardName+".txt", true)){
								out.write("inprogress|".getBytes());
							}
							
							todo.remove(card);
							card.addToHistory("inprogress");
							inprogress.add(card);
							return 0;
						}
						//spostamento in una lista non valida
						return -2;
					}
				}
				//carta non trovata 
				return -3;
			}
			//spostamento dalla lista inprogress
			case "inprogress" :{
				//cerco la carta in inprogress
				for(Card cards : inprogress) {
					//se la trovo
					if(cards.getName().equals(cardName)) {
						card = cards;
						
						//spostamento nella lista toberevised
						if(to.equals("toberevised")) {
							//scrivo il cambiamento alla history della carta su filesystem
							try(FileOutputStream out = new FileOutputStream("./res/projects/"+this.name+"/"+cardName+".txt", true)){
								out.write("toberevised|".getBytes());
							}
							
							inprogress.remove(card);
							card.addToHistory("toberevised");
							toberevised.add(card);
							return 0;
						}
						//spostamento nella lista done
						if(to.equals("done")) {
							//scrivo il cambiamento alla history della carta su filesystem
							try(FileOutputStream out = new FileOutputStream("./res/projects/"+this.name+"/"+cardName+".txt", true)){
								out.write("done".getBytes());
							}
							
							inprogress.remove(card);
							card.addToHistory("done");
							done.add(card);
							return 0;
						}
						//spostamento in una lista non valida
						return -2;
						
					}
				}
				//carta non trovata 
				return -3;
			}
			//spostamento dalla lista toberevised
			case "toberevised" :{
				//cerco la carta
				for(Card cards : this.toberevised) {
					//se la trovo
					if(cards.getName().equals(cardName)) {
						card = cards;
						
						//spostamento in lista inprogress
						if(to.equals("inprogress")) {
							//scrivo il cambiamento alla history della carta su filesystem
							try(FileOutputStream out = new FileOutputStream("./res/projects/"+this.name+"/"+cardName+".txt", true)){
								out.write("inprogress|".getBytes());
							}
							
							toberevised.remove(card);
							card.addToHistory("inprogress");
							inprogress.add(card);
							return 0;
						}
						//spostamento in lista done
						if(to.equals("done")) {
							//scrivo il cambiamento alla history della carta su filesystem
							try(FileOutputStream out = new FileOutputStream("./res/projects/"+this.name+"/"+cardName+".txt", true)){
								out.write("done|".getBytes());
							}
							
							toberevised.remove(card);
							card.addToHistory("done");
							done.add(card);
							
							return 0;
						}
						//spostamento in una lista non valida
						return -2;
					}
				}
				//carta non trovata in toberevised
				return -3;
			}
		}
		//spostamento da una lista non valida
		return -4;
	}
	
	//REQUIRES: nil
	//THROWS: nil
	//MODIFIES: nil
	//EFFECT: se l'utente non è membro del progetto restituisce una stringa contenente il messaggio di errore
	//		  se nel progetto non esiste nessuna carta con tale nome restituisce la stringa contenente il messaggio di errore
	//		  altrimenti restituisce uno per riga: la lista che contiene la card, il nome della card, la descrizione della card
	public synchronized String showCard(String userName, String cardName) {
		if(userName == null || cardName == null) return "Errore nel nome utente o nome carta";
		
		//controllo che l'utente abbia i permessi per effettuare l'operazione
		if(!members.contains(userName)) return "Solo i membri del progetto possono vedere le carte";
		
		//creazione di una carta temporanea per la ricerca nelle liste
		Card toFind = new Card(cardName, "desc");
		
		//ricerca in lista todo
		if(todo.contains(toFind)) {
			for(Card cards : todo) {
				if(cards.getName().equals(cardName)){
					return "toDo:\n"+cards.toString();
				}
			}
		}
		//ricerca in lista inprogress
		if(inprogress.contains(toFind)) {
			for(Card cards : inprogress) {
				if(cards.getName().equals(cardName)){
					return "inProgress:\n"+cards.toString();
				}
			}
		}
		//ricerca in lista toberevised
		if(toberevised.contains(toFind)) {
			for(Card cards : toberevised) {
				if(cards.getName().equals(cardName)){
					return "toBeRevised:\n"+cards.toString();
				}
			}
		}
		//ricerca in lista done
		if(done.contains(toFind)) {
			for(Card cards : done) {
				if(cards.getName().equals(cardName)){
					return "done:\n"+cards.toString();
				}
			}
		}
		//la carta non è in nessuna lista
		return "Nel progetto non esiste nessuna carta con questo nome";
	}
	
	//REQUIRES: nil
	//THROWS: nil
	//MODIFIES: nil
	//EFFECT: se l'utente non è membro del progetto restituisce la stringa contenente il messaggio di errore
	//		  altrimenti restituisce l'interezza delle liste e del loro contenuto come stringa
	public synchronized String showCards(String userName) {
		if(!members.contains(userName)) return "Solo i membri del progetto possono vedere le carte";
		
		//creazione della stringa risultato
		String cardList = "";
		
		//aggiunta delle varie card alla stringa risultato
		cardList += "List toDo:\n";
		for(Card cards : todo) {
			cardList += cards.getName() + "\n";
		}
		cardList += "List inProgress:\n";
		for(Card cards : inprogress) {
			cardList += cards.getName() + "\n";
		}
		cardList += "List toBeRevised:\n";
		for(Card cards : toberevised) {
			cardList += cards.getName() + "\n";
		}
		cardList += "List done:\n";
		for(Card cards : done) {
			cardList += cards.getName() + "\n";
		}
		
		return cardList;
	}
	
	//REQUIRES: nil
	//THROWS: nil
	//MODIFIES: nil
	//EFFECT: se l'utente non è membro del progetto restituisce la stringa contenente il messaggio di errore
	//		  altrimenti restituisce l'interezza della lista dei membri, uno per riga, come stringa
	public synchronized String showMembers(String userName) {
		if(!members.contains(userName)) return "Solo i membri del progetto possono vedere le carte";
		
		String membersList = "";
		for(String member : this.members) {
			membersList += member + "\n";
		}
		return membersList;
	}
	
	//REQUIRES: nil
	//THROWS: nil
	//MODIFIES: nil
	//EFFECT: restituisce true se l'utente è membro del progetto, altrimenti false
	public synchronized boolean isMember(String userName) {
		return this.members.contains(userName);
	}
	
	//REQUIRES: nil
	//THROWS: IOException se la scrittura della nuovo utente su filesystem è impossibile
	//		  FileNotFoundException se la cartella del progetto ha subito una modifica esterna che rende i dati inconsistenti
	//MODIFIES: this.members
	//EFFECT: restituisce -1 se l'utente richiedente l'aggiunta non è membro, -2 se l'utente da aggiungere è già membro, 0 se l'utente viene aggiunto alla lista
	//		  con successo e la scrittura del cambiamento su filesystem va a buon fine
	public synchronized int addMember(String userName, String toAdd) throws FileNotFoundException, IOException {
		//controllo che l'utente sia membro
		if(!members.contains(userName)) return -1;
		//controllo che l'utente da aggiungere non sia già membro
		if(members.contains(toAdd)) return -2;
		
		//tentativo di scrivere su filesystem la modifica alla lista membri
		try(FileOutputStream out = new FileOutputStream("./res/projects/" +this.name+"/members", true)){
			String toWrite = toAdd + "|";
			out.write(toWrite.getBytes());
		}
		
		//aggiunta dell'utente alla lista membri
		members.add(toAdd);
		return 0;
	}
	
	//REQUIRES: nil
	//THROWS: nil
	//MODIFIES: nil
	//EFFECT: se l'utente non è membro del progetto restituisce la stringa contenente il messaggio di errore
	//		  se la card non esiste resituisce la stringa contenente il messaggio di errore
	//		  altrimenti restituisce la history della card sotto forma di stringa
	public String getCardHistory(String userName, String cardName) {
		//controllo se l'utente è membro del progetto
		if(!members.contains(userName)) return "Solo i membri del progetto possono accedere a questa informazione";
		
		//creazione della carta temporanea per la ricerca nelle liste
		Card card = new Card(cardName, "");
		//ricerca nelle liste
		if(todo.contains(card)) {
			for(Card cards : this.todo) {
				if(cards.getName().equals(cardName)) {
					return cards.getHistory().toString();
				}
			}
		}
		if(inprogress.contains(card)) {
			for(Card cards : this.inprogress) {
				if(cards.getName().equals(cardName)) {
					return cards.getHistory().toString();
				}
			}
		}
		if(toberevised.contains(card)) {
			for(Card cards : this.toberevised) {
				if(cards.getName().equals(cardName)) {
					return cards.getHistory().toString();
				}
			}
		}
		if(done.contains(card)) {
			for(Card cards : this.done) {
				if(cards.getName().equals(cardName)) {
					return cards.getHistory().toString();
				}
			}
		}
		//la carta non è nel progetto
		return "Non esiste nessuna carta dal nome " + cardName + " nel progetto " + this.name;
	}
	
	//REQUIRES: nil
	//THROWS: nil
	//MODIFIES: nil
	//EFFECT: restituisce null se l'utente non è membro del progetto
	//		  altrimenti restituisce l'indirizzo multicast associato alla chat del progetto come stringa
	public String getChatAddress(String userName) {
		if(!this.members.contains(userName)) return null;
		return this.chatAddress.toString() + "|";
	}
	
	//REQUIRES: todo.isEmpty() == true and inprogress.isEmpty() == true and toberevised.isEmpty() == true
	//THROWS: nil
	//MODIFIES: this
	//EFFECT: restituisce -1 se l'utente non è membro, -2 se il progetto ha ancora delle card da spostare in done, 0 se la chiusura va a buon fine
	//		  e la cancellazione dei dati del progetto da filesystem è andata a buon fine
	public int closeProject(String userName) {
		//controllo se l'utente è membro
		if(!this.members.contains(userName)) return -1;
		//controllo che le carte esistenti siano tutte in done
		if(todo.isEmpty() && inprogress.isEmpty() && toberevised.isEmpty()) {
				//cancello il progetto da filesystem
				recoursiveDirectoryDeletion(new File("./res/projects/"+this.name));
				return 0;
		}
		//cancellazione del progetto fallita a causa di un errore sulle operazioni di cancellazione da filesystem
		else return -2;
	}
	
	
	//metodo provato per la cancellazione ricorsiva dei files e delle directory di un progetto
	private void recoursiveDirectoryDeletion(File file) {
		//se è una cartella
		if(file.isDirectory()) {
			File[] fileList = file.listFiles();
			//se contiene elementi
			if(fileList != null) {
				//per ogni elemento contenuto
				for(File files : fileList) {
					recoursiveDirectoryDeletion(files);
				}
			}
		}
		//cancellazione
		if(!file.delete()) {
			System.out.println("Errore durante la cancellazione di un file del progetto "+this.name);
		}
	}
	
	//REQUIRES: name è il nome di una directory nella cartella ./res/projects ed il suo contenuto è integro
	//THROWS: IOException se la lettura da filesystem è impossibile
	//MODIFIES: this
	//EFFECT: restituisce un progetto WORTH costruito con le informazioni recuperate da filesystem 
	public static Project retriveProject(String name) throws IOException {
		//creazione delle liste
		Set<String> _members = new TreeSet<String>();
		Set<Card> _todo = new TreeSet<Card>(new CardComparator());
		Set<Card> _inprogress = new TreeSet<Card>(new CardComparator());
		Set<Card> _toberevised = new TreeSet<Card>(new CardComparator());
		Set<Card> _done = new TreeSet<Card>(new CardComparator());
		
		//recupero delle informazioni di progetto da filesystem
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("./res/projects/"+name))){
			for(Path file : stream) {
				//trovato il file members
				if(file.getFileName().toString().equals("members")) {
					try(FileInputStream in = new FileInputStream(file.toString())){
						//recupero dei nomi utente contenuti nel file members
						String membersString = new String(in.readAllBytes());
						StringTokenizer tok = new StringTokenizer(membersString, "|");
						while(tok.hasMoreTokens()) {
							//aggiunta dei membri alla lista _members
							_members.add(tok.nextToken());
						}
					}
				}
				//trovata una card
				else try(FileInputStream in = new FileInputStream(file.toString())){
					//recupero delle informazioni sulla card
					String cardString = new String(in.readAllBytes());
					StringTokenizer tok = new StringTokenizer(cardString, "|");
					String cardName = tok.nextToken();
					String desc = tok.nextToken();
					Card card = new Card(cardName, desc);
					while(tok.hasMoreTokens()) {
						card.addToHistory(tok.nextToken());
					}
					//la lista in cui la card sarà contenuta è l'ultimo elemento della sua history
					int last = card.getHistory().size()-1;
					//aggiunta della card nella lista corretta
					switch(card.getHistory().get(last)) {
						case "todo" : {
							_todo.add(card);
							break;
						}
						case "inprogress" : {
							_inprogress.add(card);
							break;
						}
						case "toberevised" : {
							_toberevised.add(card);
							break;
						}
						case "done" : {
							_done.add(card);
							break;
						}
					}
				}
			}
		}
		//creazione effettiva del progetto
		return new Project(name, _members, _todo, _inprogress, _toberevised, _done);
	}
	
	public String toString() {
		String res = "";
		res += "nome: " + this.name + "\n" + "membri :\n";
		for(String member : this.members) {
			res += member + "\n";
		}
		res += "toDo:\n";
		for(Card card : todo) {
			res += card.toString() + "\n";
		}
		res += "inProgress:\n";
		for(Card card : inprogress) {
			res += card.toString() + "\n";
		}
		res += "toBeRevised:\n";
		for(Card card : toberevised) {
			res += card.toString() + "\n";
		}
		res += "done:\n";
		for(Card card : done) {
			res += card.toString() + "\n";
		}
		return res;
	}
	
}
