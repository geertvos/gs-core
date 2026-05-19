# gs-core
The GS Core project implements the GVM VM and bytecode language. The GVM is an object oriented, functional and stack based VM, loosely based on Javascript. 

A full high level implementation can be found in this project: https://github.com/geertvos/gs-lang

# type system

The type system of the GVM is completely pluggable and allows the language developer to extend the build in types with easy. Each type can indicate what operations are supported. This allows the GVM to perform runtime type checking. 

Undefined, Boolean, Object and Function are built in types. For more details check out the GScript language implementation that uses the pluggable type system to add numbers and strings. 

# Instruction set

| Instruction | Arguments | Description  |
|-----|---|---|
| NEW | [string] | Push a reference to a new object of the given type on the stack |
| LDS | [int] | Peek a value from the stack at pos ARG and push on the stack |
| LDG | [int] | Load a value from the stack at pos ARG without using the framepointer |
| DUP | | Duplicate the current top of the stack |
| LDC_D | [int] [string] | Create a new value with type defined by string with value int. |
| INVOKE | [int] | Call the function that is on the stack. Argument supplies number of arguments. |
| RETURN | | Return from a function. |
| PUT | | Pop variable to set from the stack, then pop the new value from the stack. Copies the values from the latter to the first. |
| GET | | Pop reference from the stack, pop reference to variable from stack, load value from reference and push on stack. |
| GETDYNAMIC | | Pop variable name from the stack. Load value from current scope, checking parent scopes recursively. If not found, create new undefined in current scope. |
| HALT | | Stop the VM. |
| ADD | | Pop two values from the stack and push the result of the addition |
| SUB | | Pop two values from the stack and push the result of the subtraction |
| MULT | | Pop two values from the stack and push the result of the multiplication |
| DIV | | Pop two values from the stack and push the result of the division |
| MOD | | Pop two values from the stack and push the result of the modulus |
| AND | | Pop two values from the stack and push the result of the and |
| OR | | Pop two values from the stack and push the result of the or |
| NOT | | Pop the value from the stack and push the result of the not |
| EQL | | Pop two values from the stack and push the result of the equality |
| GT | | Pop two values from the stack and push the result of the greater than |
| LT | | Pop two values from the stack and push the result of the less than |
| CJMP | [int] | Conditional jump. Pop value from the stack. If true, move pc to ARG. |
| JMP | [int] | Move pc to ARG |
| POP | | Pop a value from the stack |
| NATIVE | | Pop the function from the stack and treat as native function |
| THROW | | Pop the exception from the stack and throw it. |
| FORK | | Fork the current thread. Push boolean on the stack to identify new and existing thread |
| DEBUG | [int] [int] | Set the current debug line number and source location for debugging purposes. |
| BREAKPOINT | | Pause execution and allow inspection of heap and stack. |




