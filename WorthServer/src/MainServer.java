
import java.rmi.registry.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.nio.file.*;
import java.util.*;
import java.io.File;
import java.io.IOException;

public class MainServer {
	//porta usata per il locateRegistry
	static final int DEF_REGISTRY_PORT = 6000;
	//porta usata per la comunicazione TCP fra clients e server
	static final int DEF_TCP_PORT = 6001;
	
	//per segnalare errori negli argomenti di lancio di WorthServer
	private static void argsError() {
		System.out.println("Se si vuole lanciare WorthServer con argomenti, essi devono essere: \n'numero di porta per connessione TCP col server'\n'numero di porta per localizzazione degli oggetti remoti");
	}
	
	public static void main(String[] args) {
		//contorlli da effettuare se il programma è lanciato con argomenti
		int tcp_port = -1;
		int registry_port = -1;
		
		if(args.length >= 1) {
			if(args.length != 2) {
				argsError();
				return;
			}
			try {
				tcp_port = Integer.parseInt(args[0]);
				registry_port = Integer.parseInt(args[1]);
			}
			catch(Exception e) {
				argsError();
				return;
			}
		}
		
		if(tcp_port == -1 && registry_port == -1) {
			tcp_port = DEF_TCP_PORT;
			registry_port = DEF_REGISTRY_PORT;
		}
		
		
		//controllo se i files di sistema sono presenti (altrimenti li creo)
		File checkFile = new File("./res");
		if(!checkFile.exists()) {
			System.out.println("Alcuni files mancanti. Creazione dei files richiesti in corso.");
			checkFile.mkdir();
		}
		checkFile = new File("./res/users.txt");
		if(!checkFile.exists()) {
			System.out.println("Alcuni files mancanti. Creazione dei files richiesti in corso.");
			try {
				checkFile.createNewFile();
			}
			catch(IOException e) {
				System.out.println("Impossibile creare il file ./res/users.txt");
				e.printStackTrace();
			}
		}
		checkFile = new File("./res/projects");
		if(!checkFile.exists()) {
			System.out.println("Alcuni files mancanti. Creazione dei files richiesti in corso.");
			checkFile.mkdir();
		}
		
		
		//inizializzazione degli oggetti remoti per la lista utenti e il servizio di registrazione a WORTH
		try {
			//creazione del locateRegistry per il recupero degli oggetti remoti 
			LocateRegistry.createRegistry(registry_port);
			Registry registry = LocateRegistry.getRegistry(registry_port);
			
			//creazione e registrazione dell' oggetto remoto per le notifiche riguardanti la lista degli utenti connessi a WORTH
			UserListCallback userListCallback = new UserListCallback(new HashMap<String, Boolean>());
			UserListCallbackInterface userListStub = (UserListCallbackInterface) UnicastRemoteObject.exportObject(userListCallback, 0); //0
			registry.rebind("USER_LIST", userListStub);
			
			System.out.println("User List Callback in funzione");
			
			//creazione e registrazione dell'oggetto remoto per la registrazione a WORTH
			RegistrationService registrationService = new RegistrationService(userListCallback);
			RegistrationServiceInterface stub = (RegistrationServiceInterface) UnicastRemoteObject.exportObject(registrationService, 0);
			registry.rebind("REGISTRATION_SERVICE", stub);
			
			System.out.println("Registration Service in funzione");
			

			//recupero i progetti worth da filesystem
			LinkedList<Project> pjList = new LinkedList<Project>();
			
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("./res/projects"))){
				for(Path file : stream) {
					String currentProjectName = file.getFileName().toString();
					pjList.add(Project.retriveProject(currentProjectName));
				}
			}
			catch(Exception e) {
				System.out.println("Impossibile recuperare le informazioni sui progetti");
				e.printStackTrace();
			}
			
			//INIZIALIZZAZIONE DEL CICLO PRINCIPALE
			CommandServer cs = new CommandServer(tcp_port, registrationService, userListCallback, pjList);
			cs.start();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		
	
	}
}
