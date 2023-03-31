
import java.rmi.*;
import java.util.*;

public interface RegistrationServiceInterface extends Remote {
	
	public boolean register(String usr, String pwd) throws RemoteException;
	
}
