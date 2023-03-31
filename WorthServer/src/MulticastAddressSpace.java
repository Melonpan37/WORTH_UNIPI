
//classe astratta per l'assegnamento degli indirizzi multicast per la chat di un progetto
public abstract class MulticastAddressSpace {
	
	//attuale indirizzo multicast non utilizzato
	private static int[] address = {239, 0, 0, 0};
	
	//REQUIRES: nill
	//THROWS: nil
	//MODIFIES: this.address
	//EFFECT: restituisce un indirizzo multicast non in uso per la chat di un progetto, poi incrementa this.address
	public static String getAddress() {
		String res = Integer.toString(address[0]) + "." + Integer.toString(address[1]) + "." + Integer.toString(address[2]) + "." + Integer.toString(address[3]);
		address[3]++;
		if(address[3] > 255) {
			address[3] = 0;
			address[2]++;
			if(address[2] > 255) {
				address[2] = 0;
				address[1]++;
				if(address[1] > 255) {
					address[0] = 239;
				}
			}
		}
		return res;
	}
	
}
