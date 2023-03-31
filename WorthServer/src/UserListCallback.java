
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

public class UserListCallback extends RemoteServer implements UserListCallbackInterface {
	
	//mappatura utenti registrati - status
	private HashMap<String, Boolean> userList;
	//clients registrati al servizio
	private List<UserListRemoteInterface> remoteSet;
	
	//costruttore
	public UserListCallback(HashMap<String, Boolean> userList) {
		super();
		this.remoteSet = new LinkedList<UserListRemoteInterface>();
		this.userList = userList;
	}
	
	//usata dal client per registrarsi al sistema di notifiche
	public synchronized HashMap<String, Boolean> registerForCallback(UserListRemoteInterface clientRemote) throws RemoteException {
		if(!remoteSet.contains(clientRemote))
			remoteSet.add(clientRemote);
		return userList;
	}
	
	//usata dal client per annullare la propria registrazione al sistema di notifiche
	public synchronized void unregisterForCallback(UserListRemoteInterface clientRemote) throws RemoteException {
		if(remoteSet.contains(clientRemote))
			remoteSet.remove(clientRemote);
	}
	
	//invia la notifica di un cambiamento nello stato della mappatura utenti registrati - status
	public synchronized void callback(String username, boolean status) throws RemoteException {
		for(UserListRemoteInterface clientRemote : remoteSet) {
			try{
				clientRemote.notifyUserListChange(username, status);
			}
			//se è impossibile comunicare con un client, questo viene rimosso dalla lista degli utenti registrati al servizio
			catch(Exception e) {
				remoteSet.remove(remoteSet.indexOf(clientRemote));
			}
		}
	}
	
	//aggiunge una nuova entry nella mappatura utente registrato - status
	public synchronized void addUser(String name, boolean status) {
		userList.put(name, status);
	}
	
	//inizializza la mappatura degli utenti registrati, tutti con status offline (viene chiamata al momento dell'avvio del server da RegistrationService)
	public synchronized void update(Set<String> userList) {
		for(String name : userList) {
			this.userList.put(name, false);
		}
	}
	
	//restituisce lo status di un utente
	public synchronized boolean getOnlineStatus(String username) {
		if(userList.containsKey(username)) return userList.get(username);
		return false;
	}
}
