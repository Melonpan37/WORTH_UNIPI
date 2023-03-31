
import java.rmi.*;

public interface RegistrationServiceInterface extends Remote {
	
	public boolean register(String usr, String pwd) throws RemoteException;
	
}
