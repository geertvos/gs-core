package net.geertvos.gvm.core;


public class Value {

	private Type type;
	private int value;
	private String comment;
	
	public Value( int value , Type type )
	{
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

	public TYPE getType() {
		return type;
	}

	public void setType(TYPE type) {
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
