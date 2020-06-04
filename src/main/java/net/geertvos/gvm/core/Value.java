package net.geertvos.gvm.core;


public class Value {

	private Type type;
	private int value;
	private String comment;
	
	public Value( int value , Type type )
	{
		if(type == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}
		this.value = value;
		this.type = type;
	}

	public Value( int value , Type type , String comment )
	{
		this.value = value;
		this.type = type;
		this.comment = comment;
	}

	public Value() {
		this.type = new Undefined();
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	
	/**
	 * For debug purposes
	 */
	public String toString()
	{
		if( comment!=null )
		{
			return value+":"+type+" //"+comment;
		}
		return value+":"+type;
	}

	public void setComment(String string) {
		this.comment = string;
	}
}
