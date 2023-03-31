
import java.rmi.*;
import java.util.HashMap;

public interface UserListCallbackInterface extends Remote {
	public HashMap<String, Boolean> registerForCallback(UserListRemoteInterface clientRemote) throws RemoteException;
	public void unregisterForCallback(UserListRemoteInterface clientRemote) throws RemoteException;
}
