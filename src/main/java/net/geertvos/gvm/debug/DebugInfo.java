package net.geertvos.gvm.debug;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import net.geertvos.gvm.core.GVM;
import net.geertvos.gvm.program.GVMFunction;
import net.geertvos.gvm.program.GVMProgram;
import net.geertvos.gvm.streams.RandomAccessByteStream;

public class DebugInfo {

	public static void displayProgram(GVMProgram p) {
		for (Entry<Integer, GVMFunction> entry : p.getFunctions().entrySet()) {
			System.out.println("Function: " + entry.getKey());
			System.out.println("Parameters:");
			System.out.println(entry.getValue().getParameters());
			System.out.println("Locals:");
			System.out.println(entry.getValue().getLocals());
			System.out.println("Bytecode:");
			displayFunction(System.out, entry.getValue().getBytecode());
			System.out.println();
		}
	}

	public static void disassemble(PrintStream out, GVMProgram program) {
		List<String> strings = program.getStringConstants();

		out.println("; program: " + program.getName());
		out.println("; strings: " + strings.size());
		out.println("; functions: " + program.getFunctions().size());
		out.println();

		out.println(".strings");
		for (int i = 0; i < strings.size(); i++) {
			out.printf("  %4d  \"%s\"%n", i, escape(strings.get(i)));
		}
		out.println();

		List<Integer> funcIds = new ArrayList<>(program.getFunctions().keySet());
		Collections.sort(funcIds);

		for (int funcId : funcIds) {
			GVMFunction function = program.getFunction(funcId);
			out.print(".function " + funcId);
			if (function.getDebugName() != null) {
				out.print(" ; " + function.getDebugName());
			}
			if (!function.getParameters().isEmpty()) {
				out.print("(" + String.join(", ", function.getParameters()) + ")");
			}
			out.println();
			if (!function.getLocals().isEmpty()) {
				out.println("  .locals " + String.join(", ", function.getLocals()));
			}
			for (int[] h : function.getExceptionHandlers()) {
				out.println("  .catch " + h[0] + ".." + h[1] + " -> " + h[2]);
			}

			RandomAccessByteStream bc = function.getBytecode();
			bc.seek(0);
			while (bc.getPointerPosition() < bc.size()) {
				int addr = bc.getPointerPosition();
				byte instruction = bc.read();
				out.printf("  %5d  ", addr);
				switch (instruction) {
					case GVM.NEW: {
						String typeName = bc.readString();
						out.println("NEW          " + typeName);
						break;
					}
					case GVM.LDS: {
						int arg = bc.readInt();
						out.println("LDS          " + arg);
						break;
					}
					case GVM.DUP: {
						out.println("DUP");
						break;
					}
					case GVM.LDC_D: {
						int val = bc.readInt();
						String typeName = bc.readString();
						switch (typeName) {
							case "String":
								out.println("LDC          " + resolve(strings, val) + " ; " + typeName);
								break;
							case "Number":
								out.println("LDC          " + val + " ; " + typeName);
								break;
							case "Boolean":
								out.println("LDC          " + (val != 0 ? "true" : "false") + " ; " + typeName);
								break;
							default:
								out.println("LDC          " + val + " ; " + typeName);
								break;
						}
						break;
					}
					case GVM.INVOKE: {
						int arg = bc.readInt();
						out.println("INVOKE       " + arg);
						break;
					}
					case GVM.RETURN:
						out.println("RETURN");
						break;
					case GVM.PUT:
						out.println("PUT");
						break;
					case GVM.GET:
						out.println("GET");
						break;
					case GVM.HALT:
						out.println("HALT");
						break;
					case GVM.ADD:
						out.println("ADD");
						break;
					case GVM.SUB:
						out.println("SUB");
						break;
					case GVM.MULT:
						out.println("MULT");
						break;
					case GVM.DIV:
						out.println("DIV");
						break;
					case GVM.MOD:
						out.println("MOD");
						break;
					case GVM.AND:
						out.println("AND");
						break;
					case GVM.OR:
						out.println("OR");
						break;
					case GVM.NOT:
						out.println("NOT");
						break;
					case GVM.EQL:
						out.println("EQL");
						break;
					case GVM.LT:
						out.println("LT");
						break;
					case GVM.GT:
						out.println("GT");
						break;
					case GVM.JMP: {
						int pc = bc.readInt();
						out.println("JMP          @" + pc);
						break;
					}
					case GVM.CJMP: {
						int pc = bc.readInt();
						out.println("CJMP         @" + pc);
						break;
					}
					case GVM.POP:
						out.println("POP");
						break;
					case GVM.NATIVE:
						out.println("NATIVE");
						break;
					case GVM.THROW:
						out.println("THROW");
						break;
					case GVM.DEBUG: {
						int line = bc.readInt();
						int loc = bc.readInt();
						out.println("DEBUG        line " + line + " ; " + resolve(strings, loc));
						break;
					}
					case GVM.BREAKPOINT:
						out.println("BREAKPOINT");
						break;
					case GVM.FORK:
						out.println("FORK");
						break;
					case GVM.GETDYNAMIC:
						out.println("GETDYNAMIC");
						break;
					default:
						out.println("??? opcode " + instruction);
						break;
				}
			}
			out.println();
		}
	}

	private static String resolve(List<String> strings, int idx) {
		if (idx >= 0 && idx < strings.size()) {
			return "\"" + escape(strings.get(idx)) + "\"";
		}
		return "str#" + idx;
	}

	private static String escape(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
	}

	public static void displayFunction(PrintStream out, RandomAccessByteStream bytecode) {
		bytecode.seek(0);
		while (bytecode.getPointerPosition() < bytecode.size()) {
			byte instruction = bytecode.read();
			switch (instruction) {
				case GVM.NEW: {
					bytecode.readString();
					out.println("NEW");
					break;
				}
				case GVM.LDS: {
					int arg = bytecode.readInt();
					out.println("LDS " + arg);
					break;
				}
				case GVM.DUP:
					out.println("DUP");
					break;
				case GVM.LDC_D: {
					int arg = bytecode.readInt();
					out.println("LDC_D " + arg + " " + bytecode.readString());
					break;
				}
				case GVM.INVOKE: {
					int arg = bytecode.readInt();
					out.println("INVOKE " + arg);
					break;
				}
				case GVM.RETURN:
					out.println("RETURN");
					break;
				case GVM.PUT:
					out.println("PUT");
					break;
				case GVM.GET:
					out.println("GET");
					break;
				case GVM.HALT:
					out.println("HALT");
					break;
				case GVM.ADD:
					out.println("ADD");
					break;
				case GVM.SUB:
					out.println("SUB");
					break;
				case GVM.MULT:
					out.println("MULT");
					break;
				case GVM.DIV:
					out.println("DIV");
					break;
				case GVM.MOD:
					out.println("MOD");
					break;
				case GVM.AND:
					out.println("AND");
					break;
				case GVM.OR:
					out.println("OR");
					break;
				case GVM.NOT:
					out.println("NOT");
					break;
				case GVM.EQL:
					out.println("EQL");
					break;
				case GVM.LT:
					out.println("LT");
					break;
				case GVM.GT:
					out.println("GT");
					break;
				case GVM.JMP: {
					int pc = bytecode.readInt();
					out.println("JMP " + pc);
					break;
				}
				case GVM.CJMP: {
					int pc = bytecode.readInt();
					out.println("CJMP " + pc);
					break;
				}
				case GVM.POP:
					out.println("POP");
					break;
				case GVM.NATIVE:
					out.println("NATIVE");
					break;
				case GVM.THROW:
					out.println("THROW");
					break;
				case GVM.DEBUG: {
					int line = bytecode.readInt();
					int loc = bytecode.readInt();
					out.println("DEBUG " + line + " " + loc);
					break;
				}
				case GVM.BREAKPOINT:
					out.println("BREAKPOINT");
					break;
				case GVM.FORK:
					out.println("FORK");
					break;
				case GVM.GETDYNAMIC:
					out.println("GETDYNAMIC");
					break;
				default:
					break;
			}
		}
	}
}
