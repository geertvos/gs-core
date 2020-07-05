package net.geertvos.gvm.program;

import java.util.ArrayList;
import java.util.List;

import net.geertvos.gvm.streams.RandomAccessByteStream;

/**
 * A GVMFunction represents a compiled function inside a program.
 * @author geertvos
 *
 */
public class GVMFunction {

	private RandomAccessByteStream bytecode;
	private final List<String> parameters;
	private final List<String> locals  = new ArrayList<String>();
	private final List<ExceptionHandler> exceptionHandlers = new ArrayList<GVMFunction.ExceptionHandler>();
	private int index;
	
	public GVMFunction( RandomAccessByteStream code , List<String> parameters  )
	{
		this.bytecode = code;
		this.parameters = parameters;
	}

	public RandomAccessByteStream getBytecode() {
		return bytecode;
	}

	public void setBytecode(RandomAccessByteStream bytecode) {
		this.bytecode = bytecode;
	}
	
	public int getExceptionHandler( int programCounter )
	{
		int catchBlock = -1;
		int distance = Integer.MAX_VALUE;
		for( ExceptionHandler e : exceptionHandlers )
		{
			int ld = programCounter - e.trystart;
			if( ld > 0 && ld < distance && e.tryend >= programCounter)
			{
				distance = ld;
				catchBlock = e.catchstart;
			}
		}
		return catchBlock;
	}
	
	public void registerLocalVariable( String name )
	{
		if( !locals.contains(name))
		{
			locals.add(name);
		}
	}
	
	public List<String> getLocals()
	{
		return locals;
	}
	
	public void registerCatchBlock( int start , int end , int startOfCatch )
	{
		exceptionHandlers.add( new ExceptionHandler(start, end, startOfCatch));
	}
	
	/**
	 * Structure to keep track of the locations of catch blocks
	 * @author geertvos
	 *
	 */
	private class ExceptionHandler
	{
		private int trystart;
		private int tryend;
		private int catchstart;
		
		public ExceptionHandler( int trystart,int tryend, int catchstart )
		{
			this.trystart = trystart;
			this.tryend = tryend;
			this.catchstart = catchstart;
		}
	}
	
	public List<String> getParameters() {
		return parameters;
	}

	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
}
