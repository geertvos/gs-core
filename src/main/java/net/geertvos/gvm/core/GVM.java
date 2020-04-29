package net.geertvos.gvm.core;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import net.geertvos.gvm.core.Value.TYPE;
import net.geertvos.gvm.gc.MarkAndSweepGarbageCollector;
import net.geertvos.gvm.gc.GarbageCollector;
import net.geertvos.gvm.program.GVMFunction;
import net.geertvos.gvm.program.GVMProgram;
import net.geertvos.gvm.program.NativeMethodWrapper;
import net.geertvos.gvm.streams.RandomAccessByteStream;

/**
 * Geert Virtual Machine main class. 
 * The GVM is a stack based virtual machine that implements a simple instruction set for dynamic object oriented scripting languages. 
 * 
 * It supports the following basic types:
 * - undefined
 * - boolean
 * - number
 * - string
 * - object
 * 
 * @author geertvos
 */
public class GVM {
	
	//The stack for the virtual machine, holds local variables and intermediate results
	private Stack<Value> stack = new Stack<Value>();

	//The start location on the stack of the current frame
	private int framepointer;
	
	//The current identifier of the function
	private int function;

	//The garbage collector, by default a simple depth first search trough the object references on the stack
	private GarbageCollector gc = new MarkAndSweepGarbageCollector();
	
	//The heap contains the objects
	private HashMap<Integer,GVMObject> heap = new HashMap<Integer,GVMObject>();

	//Bytecode of the current function
	private RandomAccessByteStream bytecode;
	
	//Current program
	private GVMProgram program;
	
	public GVM( GVMProgram program )
	{
		this.program = program;
	}
	
	public void run()
	{
		framepointer = 0;
		function = 0;
		stack.clear();
		heap.clear();
		
		//The following bytecode loads function#0 from the program and invokes it without parameters.
		bytecode = new RandomAccessByteStream();
		bytecode.write( NEW );
		bytecode.write( LDC_F );
		bytecode.writeInt(0);
		bytecode.write( INVOKE );
		bytecode.writeInt(0);
		bytecode.write( HALT );
		bytecode.seek(0);

		fetchAndDecode();
	}
	
	/**
	 * The run method executes the supplied program. Once the VM is finished this method returns.
	 * @param program
	 */
	private void fetchAndDecode()
	{
		int instruction = HALT;

		//Start the fetch cycle
		boolean executing = true; 
		
		while( executing )
		{
			//Fetch
			instruction = bytecode.read();
			//Decode
			switch (instruction) {
			case NEW:
			{
				int id = heap.size()+1;
				heap.put(id, new GVMPlainObject());
				stack.push( new Value(id,Value.TYPE.OBJECT));
			}
			break;
			case LDS:
			{
				int pos = bytecode.readInt();
				if(pos >= 0) {
					int arg = framepointer + pos;
					stack.push(stack.get(arg));
				} else {
					stack.push(stack.get(stack.size()+pos-1));
				}
			}
			break;
			case DUP:
			{
				stack.push(stack.get(stack.size()-1));
			}
			break;
			case LDC_N:
			{
				int arg = bytecode.readInt();
				stack.push( new Value(arg, Value.TYPE.NUMBER) );
			}
			break;
			case LDC_S:
			{
				int arg = bytecode.readInt();
				stack.push( new Value(arg, Value.TYPE.STRING) );
			}
			break;
			case LDC_B:
			{
				int arg = bytecode.read();
				stack.push( new Value(arg, Value.TYPE.BOOLEAN) );
			}
			break;
			case LDC_U:
			{
				stack.push( new Value(0, Value.TYPE.UNDEFINED) );
			}
			break;
			case LDC_F:
			{
				int arg = bytecode.readInt();
				stack.push( new Value(arg, Value.TYPE.FUNCTION) );
			}
			break;
			case INVOKE:
				{
					//Pop the function reference
					int argCount = bytecode.readInt();
					Value arg = stack.pop();
					if( arg.getType() != Value.TYPE.FUNCTION ){
						handleException("Calling a non function: "+arg);
						break;
					}

					//Set the current function pointer
					int callingFunction = function;
					function = arg.getValue();
					GVMFunction fstruct = program.getFunction(function);
					//Obtain the number of parameters
					int paramCount = fstruct.getParameters().size() ;
					if(argCount != paramCount) {
						handleException("Argument count for function "+function+" is "+paramCount+", but "+argCount+" provided.");
						break;
					}
					
					//Store them for now
					Value[] params = new Value[paramCount];
					for( int i=paramCount-1;i>=0;i--)
						params[i] = stack.pop();
					
					Value thisval = stack.pop();
					//Push state on the stack
					stack.push( new Value(bytecode.getPointerPosition(), Value.TYPE.NUMBER, "Program counter") );
					stack.push( new Value(framepointer, Value.TYPE.NUMBER, "Frame pointer") );
					stack.push( new Value(callingFunction, Value.TYPE.NUMBER, "Calling function") );
					
					
					//Push this and arguments on the stack
					framepointer = stack.size();
					stack.push(thisval);
					for( int i=0;i<paramCount;i++)
					{
						stack.push(params[i]);
						params[i].setComment("Function parameter "+i);
					}
					for( int i=0;i<fstruct.getLocals().size();i++)
					{
						stack.push(new Value(0,TYPE.UNDEFINED,"Local variable"));
					}					
					bytecode = fstruct.getBytecode();
					bytecode.seek(0);
				}
				break;
			case RETURN:
				{
					//Pop return value
					Value v = stack.pop();
					
					//Pop locals
					int localCount = program.getFunction(function).getLocals().size() ;
					for( int i=0;i<localCount;i++)
					{
						stack.pop();
					}
					//Pop arguments, TODO: issue.. caller can supply different number of arguments.
					int paramCount = program.getFunction(function).getParameters().size() ;
					for( int i=0;i<paramCount;i++) {
						stack.pop();
					}
					stack.pop(); // this
					function = stack.pop().getValue(); //Function pointer
					framepointer = stack.pop().getValue(); //FP
					int pc = stack.pop().getValue(); //PC
					bytecode = program.getFunction(function).getBytecode();
					bytecode.seek(pc);
					stack.push(v);
					gc.collect(heap, stack);
				}
				break;
			case PUT:
				{
					Value toSet = stack.pop();
					Value value = stack.peek();
					toSet.setValue(value.getValue());
					toSet.setType(value.getType());
				}
				break;
			case GET:
				{
					Value reference = stack.pop();	//pop value which must be a reference to object
					String variableName = bytecode.readString();
					if(variableName.equals("ref")) {
						int ref = reference.getValue();
						stack.push(new Value(ref, TYPE.NUMBER));
						break;
					}
					if(reference.getType() == Value.TYPE.STRING) {
						if(variableName.equals("length")) {
							String s = program.getString(reference.getValue());
							stack.push(new Value(s.length(), TYPE.NUMBER));
							break;
						}
						if(variableName.equals("lowercase")) {
							String s = program.getString(reference.getValue()).toLowerCase();
							stack.push(new Value(s.length(), TYPE.NUMBER));
							break;
						}
						handleException("String does not support: "+variableName+" at pc: "+bytecode.getPointerPosition()+" f:"+function);
						break;
					}
					if( reference.getType() != Value.TYPE.OBJECT ){
						handleException("Not a reference to an object: "+reference+" pc: "+bytecode.getPointerPosition()+" f:"+function);
						break;
					}
					GVMObject vo = heap.get(reference.getValue());
					stack.push(vo.getValue(variableName) );
				}
				break;
			case HALT:
				{
					executing = false;
					break;
				}
			case ADD: 
			{
				Value arg1 = stack.pop();
				Value arg2 = stack.pop();
				if( arg1.getType()==Value.TYPE.NUMBER && arg2.getType()==Value.TYPE.NUMBER)
				{
					Value returnValue = new Value(arg1.getValue()+arg2.getValue(),Value.TYPE.NUMBER);
					stack.push(returnValue);
				}
				else if( arg1.getType()==Value.TYPE.NUMBER && arg2.getType()==Value.TYPE.STRING)
				{
					String value = program.getString(arg2.getValue())+arg1.getValue();
					int val = program.addString(value);
					Value returnValue = new Value(val,Value.TYPE.STRING);
					stack.push(returnValue);
				}
				else if( arg1.getType()==Value.TYPE.STRING && arg2.getType()==Value.TYPE.NUMBER)
				{
					String value = arg2.getValue() + program.getString(arg1.getValue());
					int val = program.addString(value);
					Value returnValue = new Value(val,Value.TYPE.STRING);
					stack.push(returnValue);
				}
				else if( arg1.getType()==Value.TYPE.STRING && arg2.getType()==Value.TYPE.STRING)
				{
					int val = program.addString(program.getString(arg2.getValue())+program.getString(arg1.getValue()));
					Value returnValue = new Value(val,Value.TYPE.STRING);
					stack.push(returnValue);
				}
				else if( arg1.getType()==Value.TYPE.STRING && arg2.getType()==Value.TYPE.OBJECT)
				{
					GVMObject o = heap.get(arg2.getValue());
					int val = program.addString(o.toString()+program.getString(arg1.getValue()));
					Value returnValue = new Value(val,Value.TYPE.STRING);
					stack.push(returnValue);
				}
				else if( arg1.getType()==Value.TYPE.OBJECT && arg2.getType()==Value.TYPE.STRING)
				{
					GVMObject o = heap.get(arg1.getValue());
					int val = program.addString(program.getString(arg2.getValue())+o.toString());
					Value returnValue = new Value(val,Value.TYPE.STRING);
					stack.push(returnValue);
				}
				else if( arg1.getType()==Value.TYPE.STRING && arg2.getType()==Value.TYPE.BOOLEAN)
				{
					int val = program.addString((arg2.getValue()==1?"true":"false")+program.getString(arg1.getValue()));
					Value returnValue = new Value(val,Value.TYPE.STRING);
					stack.push(returnValue);
				}
				else if( arg1.getType()==Value.TYPE.BOOLEAN && arg2.getType()==Value.TYPE.STRING)
				{
					int val = program.addString(program.getString(arg2.getValue())+(arg1.getValue()==1?"true":"false"));
					Value returnValue = new Value(val,Value.TYPE.STRING);
					stack.push(returnValue);
				}
				else handleException("Incompatible types cannot be added "+arg1.getType()+" and "+arg2.getType()+"!");
				break;
			}
			case SUB: 
			{
				Value arg2 = stack.pop();
				Value arg1 = stack.pop();
				if( arg1.getType()==Value.TYPE.NUMBER && arg2.getType()==Value.TYPE.NUMBER)
				{
					Value returnValue = new Value(arg1.getValue()-arg2.getValue(),Value.TYPE.NUMBER);
					stack.push(returnValue);
				}
				else handleException("Only numbers can be substracted!");
				break;
			}
			case MULT: 
			{
				Value arg2 = stack.pop();
				Value arg1 = stack.pop();
				if( arg1.getType()==Value.TYPE.NUMBER && arg2.getType()==Value.TYPE.NUMBER)
				{
					Value returnValue = new Value(arg1.getValue()*arg2.getValue(), Value.TYPE.NUMBER);
					stack.push(returnValue);
				}
				else handleException("Only numbers can be multiplied!");
				break;
			}
			case DIV: 
			{
				Value arg2 = stack.pop();
				Value arg1 = stack.pop();
				if( arg1.getType()==Value.TYPE.NUMBER && arg2.getType()==Value.TYPE.NUMBER)
				{
					if( arg2.getValue() == 0 )
					{
						handleException("Division by zero");
						break;
					} else {
						Value returnValue = new Value(arg1.getValue()/arg2.getValue(),Value.TYPE.NUMBER);
						stack.push(returnValue);
					}
				}
				else handleException("Only numbers can be divided!");
				break;
			}
			case AND: 
			{
				Value arg2 = stack.pop();
				Value arg1 = stack.pop();
				if( arg1.getType()==Value.TYPE.BOOLEAN && arg2.getType()==Value.TYPE.BOOLEAN)
				{
					int result = (arg1.getValue()>0 && arg2.getValue()>0)?1:0;
					Value returnValue = new Value(result, Value.TYPE.BOOLEAN);
					stack.push(returnValue);
				}
				else handleException("Only booleans can be ANDed!");
				break;
			}
			case OR: 
			{
				Value arg2 = stack.pop();
				Value arg1 = stack.pop();
				if( arg1.getType()==Value.TYPE.BOOLEAN && arg2.getType()==Value.TYPE.BOOLEAN)
				{
					int result = (arg1.getValue()>0 || arg2.getValue()>0)?1:0;
					Value returnValue = new Value(result, Value.TYPE.BOOLEAN);
					stack.push(returnValue);
				}
				else handleException("Only booleans can be ORed!");
				break;
			}
			case NOT: 
			{
				Value arg1 = stack.pop();
				if( arg1.getType()==Value.TYPE.BOOLEAN )
				{
					int result = arg1.getValue()>0?0:1;
					Value returnValue = new Value(result, Value.TYPE.BOOLEAN);
					stack.push(returnValue);
				}
				else handleException("Only booleans can be NOTed!");
				break;
			}
			case EQL: 
			{
				Value arg2 = stack.pop();
				Value arg1 = stack.pop();
				boolean tv = (arg1.getType() == arg2.getType()) && arg1.getValue() == arg2.getValue();
				Value returnValue = new Value(tv?1:0, Value.TYPE.BOOLEAN);
				stack.push(returnValue);
				break;
			}
			case LT: 
			{
				Value arg2 = stack.pop();
				Value arg1 = stack.pop();
				if( arg1.getType() == Value.TYPE.NUMBER )
				{
					boolean tv = arg1.getType() == arg2.getType() && arg1.getValue() < arg2.getValue();
					Value returnValue = new Value(tv?1:0, Value.TYPE.BOOLEAN);
					stack.push(returnValue);
				} else handleException("Only numbers can be compared");
				break;
			}
			case GT: 
			{
				Value arg2 = stack.pop();
				Value arg1 = stack.pop();
				if( arg1.getType() == Value.TYPE.NUMBER )
				{
					boolean tv = arg1.getType() == arg2.getType() && arg1.getValue() > arg2.getValue();
					Value returnValue = new Value(tv?1:0, Value.TYPE.BOOLEAN);
					stack.push(returnValue);
				} else handleException("Only numbers can be compared");
				break;
			}
			case JMP: 
			{
				int pc = bytecode.readInt();
				bytecode.seek(pc);
				break;
			}
			case CJMP: 
			{
				Value cond = stack.pop();
				int jump = bytecode.readInt();
				if( cond.getType() == Value.TYPE.BOOLEAN  )
				{
					if( cond.getValue() > 0)
					{
						bytecode.seek(jump);
					} 
					break;
				} else handleException("Condition should be a boolean value.");
			}			
			case POP:
			{
				stack.pop();
				break;
			}
			case NATIVE: {
				Value arg = stack.pop();
				if (arg.getType() != Value.TYPE.FUNCTION)
				{
					handleException("Calling a non function: " + arg);
					break;
				}
				NativeMethodWrapper wrapper = program.getNativeWrappers().get( arg.getValue() );
				List<Value> args = new ArrayList<Value>();
				for(int i=0; i <wrapper.argumentCount() ; i++)
					args.add( stack.pop() );
				try {
					Value returnVal = wrapper.invoke(args ,heap, program.getStringConstants() );
					stack.push(returnVal);
				} catch(Exception e) {
					handleException(e.getMessage());
				}
				this.gc.collect(heap, stack);
				break;
			}				
			default:
				break;
			}
		}
		System.out.println("> VM exited normal");

	}
	
	/**
	 * Remove one function call of the stack including local variables etc.
	 * Does not leave return values.
	 */
	private boolean peel()
	{
		//Do not peel the values pushed by the VM itself
		if( framepointer < 4 )
			return false;
		
		//Remove local variables and parameters
		while( stack.size() > framepointer )
			stack.pop();
		
		function = stack.pop().getValue(); //Function pointer
		framepointer = stack.pop().getValue(); //FP
		int pc = stack.pop().getValue(); //PC
		bytecode = program.getFunction(function).getBytecode();
		bytecode.seek(pc);
		return true;
	}
	
	/**
	 * Handle the exception in the current function. When this function has no exception handler attached, the function
	 * is peeled from the stack and the underlying function is checked.
	 * @param message The error message generated by the VM
	 */
	private void handleException( String message )
	{
		//Locate the catch block (if there is one)
		GVMFunction f = program.getFunction(function);
		int catchBlock = f.getExceptionHandler(bytecode.getPointerPosition());
		if( catchBlock > -1 )
		{
			//Catch block located, push error message on the stack
			int index = program.addString(message);
			Value v = new Value(index,Value.TYPE.STRING);
			stack.push(v);
			//And relocate the program counter
			bytecode.seek(catchBlock);
		} else {
			//No catch block found, see if we can peel off the current function from the stack
			if( peel() )
			{
				//We peeled one off, recursively handle the exception.
				handleException(message);
			} else {
				System.err.println("Unhandled exception: "+message);
				System.exit(1);
			}
		}
	}
	
	//Stack manipulation
	public static final byte NEW=1; 		//Create an empty object and put reference on the stack
	public static final byte LDS=2;		//Load value from the stack <pos> and put on top
	public static final byte DUP=29;	//Duplicate the current top of the stack
	public static final byte LDF=3;		//Create a reference to function <ID> on the stack
	public static final byte LDC_N=4;	//Push a number constant on the stack
	public static final byte LDC_S=5;	//Push a string constant on the stack
	public static final byte LDC_B=6;	//Push a boolean constant on the stack
	public static final byte LDC_U=7;	//Push a undefined constant on the stack
	public static final byte LDC_F=26;	//Push a function constant on the stack
	public static final byte INVOKE=8; 	//PUT program counter on stack and set PC to location of function
	public static final byte RETURN=9;	//POP PC from the stack and set PC to old PC, leave return values on the stack
	public static final byte PUT=10;		//Pop variable to set from the stack, then pop the new value from the stack. Copies the values from the latter to the first.
	public static final byte GET=11;		//Pop reference from the stack, load value <ID> from reference and push on stack
	public static final byte HALT=12;	//End machine
	
	//Arithmetic
	public static final byte ADD=14;		//Pop two values and add them
	public static final byte SUB=15;		//Pop two values and sub them
	public static final byte MULT=16;	//Pop two values and mult them
	public static final byte DIV=17;		//Pop two values and div them
	//public static final byte MOD=17;		//Pop two values and mod them
	
	//Logic
	public static final byte AND=18;		//Pop two values and AND them
	public static final byte OR=19;		//Pop two values and OR them
	public static final byte NOT=20;		//Pop value and invert it
	public static final byte EQL=21;		//Pop two values and return true if values are equal
	public static final byte GT=22;		//Pop two values and return true if x>y
	public static final byte LT=23;		//Pop two values and return true if x<y

	//Control flow
	public static final byte CJMP=24;	//Pop value, if true set PC to argument
	public static final byte JMP=25;		//Set PC to argument
	
	//Stack manipulation
	public static final byte POP=27;		//Just pop a value from the stack
	public static final byte NATIVE=28;

}
