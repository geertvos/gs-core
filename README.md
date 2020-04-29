# GS Core
The GS Core project implements the GVM VM and bytecode language. The GVM is an object oriented, functional and stack based VM, loosely based on Javascript. 

A full high level implementatino can be found in this project: https://github.com/geertvos/gs-lang

# Instruction set

| Instruction | Arguments | Description  |
|-----|---|---|
| NEW |  | Push a reference to a new object on the stack |
| LDS | [int] | Peek a value from the stack at pos ARG and push on the stack   |
| DUP | | Duplicate the current top of the stack   |
| LDF | [int] |  NOT USED?  |
| LDC_N | [int]  |  Push a Number constant on the stack  |
| LDC_S | [int]  |  Push a String constant on the stack  |
| LDC_B | [int]  |  Push a Boolean constant on the stack  |
| LDC_U |  |  Push an Undefined constant on the stack  |
| LDC_F | [int]  |  Push an Function constant on the stack  |
| INVOKE | [int]  | Call the function that is on the stack. Argument supplies number of arguments. |
| RETURN | | Return from a function. |
| PUT |  | Pop variable to set from the stack, then pop the new value from the stack. Copies the values from the latter to the first. |
| GET | [string] | Pop reference from the stack, load value named ARG from reference and push on stack. |
| HALT | | Stop the VM. |
| ADD | | Pop two values from the stack and push the result of the addition |
| SUB | | Pop two values from the stack and push the result of the substraction |
| MULT | | Pop two values from the stack and push the result of the multiplication |
| DIV | | Pop two values from the stack and push the result of the division |
| MOD | | Pop two values from the stack and push the result of the modulus |
| AND | | Pop two values from the stack and push the result of the and |
| OR | | Pop two values from the stack and push the result of the or |
| NOT | | Pop the value from the stack and push the result of the not |
| EQL | | Pop two values from the stack and push the result of the equality |
| GT | | Pop two values from the stack and push the result of the greater than |
| LT | | Pop two values from the stack and push the result of the less than |
| CJMP | | Conditional jump. Pops value form the stack. If true, move pc to ARG |
| JMP | | Move pc to ARG |
| POP | | Pop a value from the stack |
| NATIVE | | Pop the function from the stack and treat as native function |




