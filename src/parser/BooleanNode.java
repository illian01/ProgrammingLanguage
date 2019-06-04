package parser;

public class BooleanNode implements ValueNode{

	public static BooleanNode FALSE_NODE = new BooleanNode(false);
	public static BooleanNode TRUE_NODE = new BooleanNode(true);
	public boolean value;
	
	private BooleanNode(Boolean b) {
		value = b;
	}
	
	public String toString() {
		return value ? "#T" : "#F";
	}
}
