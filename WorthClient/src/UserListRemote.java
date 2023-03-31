
import java.io.IOException;
import java.rmi.*;
import java.util.*;
import java.rmi.server.*;

public class UserListRemote extends RemoteObject implements UserListRemoteInterface {
	
	private HashMap<String, Boolean> userList;
	
	public UserListRemote(HashMap<String, Boolean> userList) {
		super();
		this.userList = userList;
	}
	
	public synchronized void notifyUserListChange(String username, boolean status) throws RemoteException {
		this.userList.put(username, status);
	}
	
	public synchronized HashMap<String, Boolean> getUserList(){
		return this.userList;
	}
	
	public synchronized void updateUserListFromClient(HashMap<String, Boolean> map) {
		for(Map.Entry<String, Boolean> entry : map.entrySet()) {
			userList.put(entry.getKey(), entry.getValue());
		}
	}
	
}
