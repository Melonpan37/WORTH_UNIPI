
import java.rmi.*;

public interface UserListRemoteInterface extends Remote {
	public void notifyUserListChange(String username, boolean status) throws RemoteException;
}