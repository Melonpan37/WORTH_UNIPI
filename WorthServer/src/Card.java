
import java.util.LinkedList;
import java.util.List;


public class Card {
	
	//nome della card (massimo 12 bytes)
	private final String name;
	//descrizione della card (massimo 12 bytes)
	private final String description;
	//lista delle lista nelle quali la card è stata
	private List<String> history;
	
	//REQUIRES: name != null and description != null and name.equals("") == false
	//THROWS: NullPointerException se name == null or description == null
	//MODIFIES: this
	//EFFECTS: inizializza una nuova card con nome diverso dalla stringa vuota e descrizione
	public Card(String name, String description) {
		if(name == null || description == null) throw new NullPointerException();
		if(name.equals("")) throw new IllegalArgumentException();
		this.name = name;
		this.description = description;
		this.history = new LinkedList<String>();
	}
	
	//METODI GETTER SEMPLICI
	public String getName() {
		return this.name;
	}
	public String getDescription() {
		return this.description;
	}
	public List<String> getHistory() {
		return this.history;
		}
	
	//REQUIRES: listName != null
	//THROWS: NullPointerException se listName == null
	//MODIFIES: nil
	//EFFECT: aggiunge lo spostamento nella nuova list alla cardHistory
	public void addToHistory(String listName) {
		if(listName == null) throw new NullPointerException();
		this.history.add(listName);
	}
	
	
	
	//REQUIRES: anotherObject != null
	//THROWS: NullPointerException se anotherObject == null
	//MODFIES: nil
	//EFFECTS: ritorna true sse: 
	//				anotherObject è la stringa tale che anotherObject.equals(this.name)
	//				oppure anotherObject è la card tale che anotherObject.getName.equals(this.name)
	//		   		altrimenti ritorna false
	public boolean equals(Object anotherObject) {
		if(anotherObject == null) throw new NullPointerException();
		if(anotherObject.getClass() == String.class) {
			return ((String)anotherObject).equals(this.name);
		}
		if(anotherObject.getClass() == Card.class) {
			return ((Card)anotherObject).getName().equals(this.name);
		}
		return false;
	}
	
	//Override del metodo tostring
	//EFFECT: restituisce una stringa contenente il nome della card e in una newline la sua descrizione
	public String toString() {
		return name + ":\n" + description;
	}
	


	
	
}
