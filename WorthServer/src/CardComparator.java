
import java.util.*;

//oggetto usato per determinare un ordinamento su Card
public class CardComparator implements Comparator<Card> {
	
	
	//REQUIRES: o1 != null and o2 != null
	//THROWS: NullPointerException se 01 == null or o2 == null
	//MODIFIES: nil
	//EFFECT: restituisce l'ordinamento delle due card in base all'ordinamento lessicografico su Card.name
	public int compare(Card o1, Card o2) {
		if(o1 == null || o2 == null) throw new NullPointerException();
		return o1.getName().compareTo(o2.getName());
	}
	
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
	
}
