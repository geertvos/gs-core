package net.geertvos.gvm.core;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.geertvos.gvm.bridge.NativeMethodWrapper;
import net.geertvos.gvm.bridge.NativeObjectWrapper;
import net.geertvos.gvm.core.Value.TYPE;
import net.geertvos.gvm.gc.MarkAndSweepGarbageCollector;
import net.geertvos.gvm.gc.GarbageCollector;
import net.geertvos.gvm.program.GVMFunction;
import net.geertvos.gvm.program.GVMProgram;
import net.geertvos.gvm.program.GVMHeap;
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
 * @author Geert Vos
 */
public class GVM {
	
	//The garbage collector, by default a simple depth first search trough the object references on the stack
	private GarbageCollector gc = new MarkAndSweepGarbageCollector();
	
	//The heap contains the objects
	private final GVMHeap heap = new GVMHeap();
	
	//Current program
	private GVMProgram program;

	private List<GVMThread> threads = new LinkedList<GVMThread>();
	
	public GVM( GVMProgram program )
	{
		this.program = program;
	}
	
	public void run()
	{
		GVMThread main = new GVMThread(program, heap);
		threads.add(main);
		heap.clear();
		
		//The following bytecode loads function#0 from the program and invokes it without parameters.
		RandomAccessByteStream bytecode = new RandomAccessByteStream();
		bytecode.write( NEW );
		bytecode.write( LDC_F );
		bytecode.writeInt(0);
		bytecode.write( INVOKE );
		bytecode.writeInt(0);
		bytecode.write( HALT );
		bytecode.seek(0);

		main.setBytecode(bytecode.clone());
		fetchAndDecodeAll();
		System.out.println("> VM exited normal");
	}
	
	/**
	 * The run method executes the supplied program. Once the VM is finished this method returns.
	 * @param program
	 */
	private void fetchAndDecodeAll()
	{
		while( !threads.isEmpty() )
		{
			Iterator<GVMThread> threadIterator = threads.iterator();
			while(threadIterator.hasNext()) {
				GVMThread thread = threadIterator.next();
				boolean continues = fetchAndDecode(thread);
				if(!continues) {
					threads.remove(thread);
				}
			}
		}
	}
	
	
	public void inject(GVMThread thread)
	{
		if(threads.isEmpty()) {
			//VM was already finished
			threads.add(thread);
			fetchAndDecodeAll();
		} else {
			//TODO: Race condition here
			threads.add(thread);
		}
		thread.awaitFinished();
	}


	private boolean fetchAndDecode(GVMThread thread) {
		int instruction= GVM.HALT;
		//Fetch
		instruction = thread.getBytecode().read();
		//Decode
		switch (instruction) {
		case NEW:
		{
			int id = heap.addObject(new GVMPlainObject());
			thread.getStack().push( new Value(id,Value.TYPE.OBJECT));
		}
		break;
		case LDS:
		{
			int pos = thread.getBytecode().readInt();
			if(pos >= 0) {
				int arg = thread.getFramepointer() + pos;
				thread.getStack().push(thread.getStack().get(arg));
			} else {
				thread.getStack().push(thread.getStack().get(thread.getStack().size()+pos-1));
			}
		}
		break;
		case DUP:
		{
			thread.getStack().push(thread.getStack().get(thread.getStack().size()-1));
		}
		break;
		case LDC_N:
		{
			int arg = thread.getBytecode().readInt();
			thread.getStack().push( new Value(arg, Value.TYPE.NUMBER) );
		}
		break;
		case LDC_S:
		{
			int arg = thread.getBytecode().readInt();
			thread.getStack().push( new Value(arg, Value.TYPE.STRING) );
		}
		break;
		case LDC_B:
		{
			int arg = thread.getBytecode().read();
			thread.getStack().push( new Value(arg, Value.TYPE.BOOLEAN) );
		}
		break;
		case LDC_U:
		{
			thread.getStack().push( new Value(0, Value.TYPE.UNDEFINED) );
		}
		break;
		case LDC_F:
		{
			int arg = thread.getBytecode().readInt();
			thread.getStack().push( new Value(arg, Value.TYPE.FUNCTION) );
		}
		break;
		case LDC_D:
		{
			int arg = thread.getBytecode().readInt();
			String typeName = thread.getBytecode().readString();
			Type type = program.getType(type);
			thread.getStack().push( new Value(arg, Value.TYPE.FUNCTION) );
		}
		break;
		case INVOKE:
			{
				//Pop the function reference
				int argCount = thread.getBytecode().readInt();
				Value calleeFunction = thread.getStack().pop();
				if( calleeFunction.getType() != Value.TYPE.FUNCTION ){
					thread.handleException( "Calling a non function: "+calleeFunction);
					break;
				}

				//Set the current function pointer
				int callerFunction = thread.getFunctionPointer();
				thread.setFunctionPointer(calleeFunction.getValue());
				GVMFunction functionDescription = program.getFunction(calleeFunction.getValue());
				//Obtain the number of parameters
				int paramCount = functionDescription.getParameters().size() ;
				if(argCount != paramCount) {
					thread.handleException( "Argument count for function "+calleeFunction.getValue()+" is "+paramCount+", but "+argCount+" provided.");
					break;
				}
				
				//Store them for now
				Value[] params = new Value[paramCount];
				for( int i=paramCount-1; i >= 0; i--)
					params[i] = thread.getStack().pop();
				
				Value thisval = thread.getStack().peek();;
				//Push state on the stack
				//TODO: create a method for this in the thread class
				thread.getCallStack().push(new StackFrame(thread.getBytecode().getPointerPosition(), thread.getFramepointer(), callerFunction, thread.getDebugLineNumber(), thread.getLocation(), thisval));
				thread.setFramepointer(thread.getStack().size()-1);
				for( int i=0;i<paramCount;i++)
				{
					thread.getStack().push(params[i]);
					params[i].setComment("Function parameter "+i);
				}
				for( int i=0;i<functionDescription.getLocals().size();i++)
				{
					thread.getStack().push(new Value(0,TYPE.UNDEFINED,"Local variable"));
				}					
				thread.setBytecode(functionDescription.getBytecode().clone());
				thread.getBytecode().seek(0);
			}
			break;
		case RETURN:
			{
				//Pop return value
				Value v = thread.getStack().pop();
				
				//Pop locals
				GVMFunction function = program.getFunction(thread.getFunctionPointer());
				int localCount = function.getLocals().size() ;
				for( int i=0;i<localCount;i++)
				{
					thread.getStack().pop();
				}
				//Pop arguments, TODO: issue.. caller can supply different number of arguments. We need to support variable arguments...
				int paramCount = function.getParameters().size() ;
				for( int i=0;i<paramCount;i++) {
					thread.getStack().pop();
				}
				thread.getStack().pop(); // this
				StackFrame frame = thread.getCallStack().pop();
				thread.setDebugLineNumber(frame.getLineNumber());
				thread.setFunctionPointer(frame.getCallingFunction());
				thread.setFramepointer(frame.getFramePointer());
				thread.setLocation(frame.getLocation());
				int pc = frame.getProgramCounter(); //PC
				thread.setBytecode(program.getFunction(thread.getFunctionPointer()).getBytecode().clone());
				thread.getBytecode().seek(pc);
				thread.getStack().push(v);
				gc.collect(heap, threads);
			}
			break;
		case PUT:
			{
				Value toSet = thread.getStack().pop();
				Value value = thread.getStack().peek();
				toSet.setValue(value.getValue());
				toSet.setType(value.getType());
			}
			break;
		case GET:
			{
				Value reference = thread.getStack().pop();	//pop value which must be a reference to object
				String variableName = thread.getBytecode().readString();
				//TODO: Move these to the language implementation 
				if(variableName.equals("ref")) {
					int ref = reference.getValue();
					thread.getStack().push(new Value(ref, TYPE.NUMBER));
					break;
				}
				if(reference.getType() == Value.TYPE.STRING) {
					if(variableName.equals("length")) {
						String s = program.getString(reference.getValue());
						thread.getStack().push(new Value(s.length(), TYPE.NUMBER));
						break;
					}
					if(variableName.equals("bytes")) {
						String s = program.getString(reference.getValue());
						int index = heap.addObject(new NativeObjectWrapper(s.getBytes(), heap, program));
						thread.getStack().push(new Value(index, TYPE.OBJECT));
						break;
					}
					if(variableName.equals("lowercase")) {
						String lowercased = program.getString(reference.getValue()).toLowerCase();
						int ref = program.addString(lowercased);
						thread.getStack().push(new Value(ref, Value.TYPE.STRING, "lowercase"));
						break;
					}
					thread.handleException( "String does not support: "+variableName+" at pc: "+thread.getBytecode().getPointerPosition()+" f:"+thread.getFunctionPointer());
					break;
				}
				if( reference.getType() != Value.TYPE.OBJECT ){
					thread.handleException( "Not a reference to an object: "+reference+" pc: "+thread.getBytecode().getPointerPosition()+" f:"+thread.getFunctionPointer());
					break;
				}
				GVMObject vo = heap.getObject(reference.getValue());
				thread.getStack().push(vo.getValue(variableName));
			}
			break;
		case GETDYNAMIC:
		{
			String variableName = thread.getBytecode().readString();
			Value theValue = null;
			for(StackFrame frame : thread.getCallStack()) {
				Value scope = frame.getScope();
				GVMObject object = heap.getObject(scope.getValue());
				if(object.getValue(variableName).getType() != TYPE.UNDEFINED) {
					theValue = object.getValue(variableName);
					break;
				}
			}
			if(theValue == null) {
				GVMObject vo = heap.getObject(thread.getCallStack().peek().getScope().getValue());
				theValue = vo.getValue(variableName);
			}
			thread.getStack().push(theValue);
			break;
		}
		case HALT:
			{
				thread.markThreadFinished();
				return false;
			}
		case ADD: 
		{
			Value arg1 = thread.getStack().pop();
			Value arg2 = thread.getStack().pop();
			if( arg1.getType()==Value.TYPE.NUMBER && arg2.getType()==Value.TYPE.NUMBER)
			{
				Value returnValue = new Value(arg1.getValue()+arg2.getValue(),Value.TYPE.NUMBER);
				thread.getStack().push(returnValue);
			}
			else if( arg1.getType()==Value.TYPE.NUMBER && arg2.getType()==Value.TYPE.STRING)
			{
				String value = program.getString(arg2.getValue())+arg1.getValue();
				int val = program.addString(value);
				Value returnValue = new Value(val,Value.TYPE.STRING);
				thread.getStack().push(returnValue);
			}
			else if( arg1.getType()==Value.TYPE.STRING && arg2.getType()==Value.TYPE.NUMBER)
			{
				String value = arg2.getValue() + program.getString(arg1.getValue());
				int val = program.addString(value);
				Value returnValue = new Value(val,Value.TYPE.STRING);
				thread.getStack().push(returnValue);
			}
			else if( arg1.getType()==Value.TYPE.STRING && arg2.getType()==Value.TYPE.STRING)
			{
				int val = program.addString(program.getString(arg2.getValue())+program.getString(arg1.getValue()));
				Value returnValue = new Value(val,Value.TYPE.STRING);
				thread.getStack().push(returnValue);
			}
			else if( arg1.getType()==Value.TYPE.STRING && arg2.getType()==Value.TYPE.OBJECT)
			{
				GVMObject o = heap.getObject(arg2.getValue());
				int val = program.addString(o.toString()+program.getString(arg1.getValue()));
				Value returnValue = new Value(val,Value.TYPE.STRING);
				thread.getStack().push(returnValue);
			}
			else if( arg1.getType()==Value.TYPE.OBJECT && arg2.getType()==Value.TYPE.STRING)
			{
				GVMObject o = heap.getObject(arg1.getValue());
				int val = program.addString(program.getString(arg2.getValue())+o.toString());
				Value returnValue = new Value(val,Value.TYPE.STRING);
				thread.getStack().push(returnValue);
			}
			else if( arg1.getType()==Value.TYPE.STRING && arg2.getType()==Value.TYPE.BOOLEAN)
			{
				int val = program.addString((arg2.getValue()==1?"true":"false")+program.getString(arg1.getValue()));
				Value returnValue = new Value(val,Value.TYPE.STRING);
				thread.getStack().push(returnValue);
			}
			else if( arg1.getType()==Value.TYPE.BOOLEAN && arg2.getType()==Value.TYPE.STRING)
			{
				int val = program.addString(program.getString(arg2.getValue())+(arg1.getValue()==1?"true":"false"));
				Value returnValue = new Value(val,Value.TYPE.STRING);
				thread.getStack().push(returnValue);
			}
			else thread.handleException( "Incompatible types cannot be added "+arg1.getType()+" and "+arg2.getType()+"!");
			break;
		}
		case SUB: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if( arg1.getType()==Value.TYPE.NUMBER && arg2.getType()==Value.TYPE.NUMBER)
			{
				Value returnValue = new Value(arg1.getValue()-arg2.getValue(),Value.TYPE.NUMBER);
				thread.getStack().push(returnValue);
			}
			else thread.handleException( "Only numbers can be substracted!");
			break;
		}
		case MULT: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if( arg1.getType()==Value.TYPE.NUMBER && arg2.getType()==Value.TYPE.NUMBER)
			{
				Value returnValue = new Value(arg1.getValue()*arg2.getValue(), Value.TYPE.NUMBER);
				thread.getStack().push(returnValue);
			}
			else thread.handleException( "Only numbers can be multiplied!");
			break;
		}
		case DIV: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if( arg1.getType()==Value.TYPE.NUMBER && arg2.getType()==Value.TYPE.NUMBER)
			{
				if( arg2.getValue() == 0 )
				{
					thread.handleException( "Division by zero");
					break;
				} else {
					Value returnValue = new Value(arg1.getValue()/arg2.getValue(),Value.TYPE.NUMBER);
					thread.getStack().push(returnValue);
				}
			}
			else thread.handleException( "Only numbers can be divided!");
			break;
		}
		case MOD: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if( arg1.getType()==Value.TYPE.NUMBER && arg2.getType()==Value.TYPE.NUMBER)
			{
				if( arg2.getValue() == 0 )
				{
					thread.handleException( "Division by zero");
					break;
				} else {
					Value returnValue = new Value(arg1.getValue()%arg2.getValue(),Value.TYPE.NUMBER);
					thread.getStack().push(returnValue);
				}
			}
			else thread.handleException( "Modulus only accepts numbers as arguments!");
			break;
		}
		case AND: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if( arg1.getType()==Value.TYPE.BOOLEAN && arg2.getType()==Value.TYPE.BOOLEAN)
			{
				int result = (arg1.getValue()>0 && arg2.getValue()>0)?1:0;
				Value returnValue = new Value(result, Value.TYPE.BOOLEAN);
				thread.getStack().push(returnValue);
			}
			else thread.handleException( "Only booleans can be ANDed!");
			break;
		}
		case OR: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if( arg1.getType()==Value.TYPE.BOOLEAN && arg2.getType()==Value.TYPE.BOOLEAN)
			{
				int result = (arg1.getValue()>0 || arg2.getValue()>0)?1:0;
				Value returnValue = new Value(result, Value.TYPE.BOOLEAN);
				thread.getStack().push(returnValue);
			}
			else thread.handleException( "Only booleans can be ORed!");
			break;
		}
		case NOT: 
		{
			Value arg1 = thread.getStack().pop();
			if( arg1.getType()==Value.TYPE.BOOLEAN )
			{
				int result = arg1.getValue()>0?0:1;
				Value returnValue = new Value(result, Value.TYPE.BOOLEAN);
				thread.getStack().push(returnValue);
			}
			else thread.handleException( "Only booleans can be NOTed!");
			break;
		}
		case EQL: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			boolean tv = (arg1.getType() == arg2.getType()) && arg1.getValue() == arg2.getValue();
			Value returnValue = new Value(tv?1:0, Value.TYPE.BOOLEAN);
			thread.getStack().push(returnValue);
			break;
		}
		case LT: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if( arg1.getType() == Value.TYPE.NUMBER )
			{
				boolean tv = arg1.getType() == arg2.getType() && arg1.getValue() < arg2.getValue();
				Value returnValue = new Value(tv?1:0, Value.TYPE.BOOLEAN);
				thread.getStack().push(returnValue);
			} else thread.handleException( "Only numbers can be compared");
			break;
		}
		case GT: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if( arg1.getType() == Value.TYPE.NUMBER )
			{
				boolean tv = arg1.getType() == arg2.getType() && arg1.getValue() > arg2.getValue();
				Value returnValue = new Value(tv?1:0, Value.TYPE.BOOLEAN);
				thread.getStack().push(returnValue);
			} else thread.handleException( "Only numbers can be compared");
			break;
		}
		case JMP: 
		{
			int pc = thread.getBytecode().readInt();
			thread.getBytecode().seek(pc);
			break;
		}
		case CJMP: 
		{
			Value cond = thread.getStack().pop();
			int jump = thread.getBytecode().readInt();
			if( cond.getType() == Value.TYPE.BOOLEAN  )
			{
				if( cond.getValue() > 0)
				{
					thread.getBytecode().seek(jump);
				} 
				break;
			} else thread.handleException( "Condition should be a boolean value.");
		}			
		case POP:
		{
			thread.getStack().pop();
			break;
		}
		case NATIVE: {
			Value arg = thread.getStack().pop();
			if (arg.getType() != Value.TYPE.FUNCTION)
			{
				thread.handleException( "Calling a non function: " + arg);
				break;
			}
			NativeMethodWrapper wrapper = program.getNativeWrappers().get( arg.getValue() );
			List<Value> args = new ArrayList<Value>();
			for(int i=0; i <wrapper.argumentCount() ; i++)
				args.add( thread.getStack().pop() );
			
			try {
				Value returnVal = wrapper.invoke(args ,heap, program);
				thread.getStack().push(returnVal);
			} catch(Exception e) {
				thread.handleException( e.getMessage());
			}
			this.gc.collect(heap, threads);
			break;
		}	
		case THROW: {
			Value arg = thread.getStack().pop();
			if (arg.getType() != Value.TYPE.STRING)
			{
				thread.handleException( "Exception thrown must be a String, not: " + arg.getType());
				break;
			}
			String message = program.getString(arg.getValue());
			thread.handleException(message);
			break;
		}
		case DEBUG: {
			//TODO: Allow language developer to inject arbitrary debug info that will be mapped to an object.
			int line = thread.getBytecode().readInt();
			thread.setDebugLineNumber(line);
			thread.setLocation(thread.getBytecode().readInt());
			break;
		}
		case BREAKPOINT: {
			System.out.println("Breakpoint current line: "+thread.getDebugLineNumber());
			break;
		}
		default:
			break;
		}
		return true;
	}
	
	//Stack manipulation
	public static final byte NEW=1;     //Create an empty object and put reference on the stack
	public static final byte LDS=2;		//Load value from the stack <pos> and put on top
	public static final byte LDG=34;	//Load value from the stack <pos> and put on top, without using the framepointer.
	public static final byte DUP=29;	//Duplicate the current top of the stack
	//public static final byte LDF=3;		//Create a reference to function <ID> on the stack
	public static final byte LDC_N=4;	//Push a number constant on the stack
	public static final byte LDC_S=5;	//Push a string constant on the stack
	public static final byte LDC_B=6;	//Push a boolean constant on the stack
	public static final byte LDC_U=7;	//Push a undefined constant on the stack
	public static final byte LDC_F=26;	//Push a function constant on the stack
	public static final byte INVOKE=8; 	//PUT program counter on stack and set PC to location of function
	public static final byte RETURN=9;	//POP PC from the stack and set PC to old PC, leave return values on the stack
	public static final byte PUT=10;		//Pop variable to set from the stack, then pop the new value from the stack. Copies the values from the latter to the first.
	public static final byte GET=11;		//Pop reference from the stack, load value <ID> from reference and push on stack
	public static final byte GETDYNAMIC = 35; //Get a field from the current scope. If it does not exists, check parent scope.. etc.. until nothing found. Then a new field is created in the current scope.
	public static final byte HALT=12;	//End machine
	
	//Arithmetic
	public static final byte ADD=14;		//Pop two values and add them
	public static final byte SUB=15;		//Pop two values and sub them
	public static final byte MULT=16;	//Pop two values and mult them
	public static final byte DIV=17;		//Pop two values and div them
	public static final byte MOD=30;		//Pop two values and mod them
	
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
	public static final byte THROW=31;		//Pop value from the stack and throw as Exception
	
	//Stack manipulation
	public static final byte POP=27;		//Just pop a value from the stack
	public static final byte NATIVE=28;

	public static final byte DEBUG=32;      //Tell the VM about the code that is being executed. For deubgging purposes.
	public static final byte BREAKPOINT=33; //Tell the VM to pause and allow for inspection of heap and stack.

}
