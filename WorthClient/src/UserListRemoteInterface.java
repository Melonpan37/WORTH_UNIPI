
import java.rmi.*;
import java.util.*;

public interface UserListRemoteInterface extends Remote {
	public void notifyUserListChange(String username, boolean status) throws RemoteException;
}
