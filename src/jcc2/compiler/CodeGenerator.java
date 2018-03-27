
package jcc2.compiler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import jcc2.common.*;
import jcc2.lib.Library;

/**
 *
 * @author note173@gmail.com
 */

public abstract class CodeGenerator
{
    //abstract public void debugPrintString ();

    abstract public void setInitMethod ();
    abstract public void setCurrentMethod (MethodContainer method);
    abstract public void link (DataOutputStream os, ClassContainer mainClass, SymLocator locator) throws IOException;
    abstract public void loadThis () throws IOException;
    abstract public void loadLibraryInstance (Library library) throws IOException;

    abstract public Object reserveLabel () throws IOException;
    abstract public void markLabel (Object label) throws IOException;
    abstract public void nop () throws IOException;
    abstract public void fakeModeOn ();
    abstract public void fakeModeOff ();

    abstract public void setLocal (MethodContainer method, FieldContainer field) throws IOException;
    abstract public void setGlobal (FieldContainer field) throws IOException;
    abstract public void getLocal (MethodContainer method, FieldContainer field) throws IOException;
    abstract public void getGlobal (FieldContainer field) throws IOException;

    abstract public void setMember (ClassContainer cont, FieldContainer field) throws IOException;
    abstract public void getMember (ClassContainer cont, FieldContainer field) throws IOException;

    abstract public void call (MethodContainer method) throws IOException;
    abstract public void callMember (ClassContainer cont, MethodContainer method) throws IOException;

    abstract public void retVoid () throws IOException;
    abstract public void ret (Type type) throws IOException;

    abstract public void newArray (Type type) throws IOException;
    abstract public void multianewArray (Type type, int dims) throws IOException;
    abstract public void arraylen () throws IOException;

    abstract public void iaload () throws IOException;
    abstract public void iastore () throws IOException;
    abstract public void saload () throws IOException;
    abstract public void sastore () throws IOException;
    abstract public void caload () throws IOException;
    abstract public void castore () throws IOException;
    abstract public void laload () throws IOException;
    abstract public void lastore () throws IOException;
    abstract public void baload () throws IOException;
    abstract public void bastore () throws IOException;
    abstract public void aaload () throws IOException;
    abstract public void aastore () throws IOException;
    abstract public void faload () throws IOException;
    abstract public void fastore () throws IOException;
    abstract public void daload () throws IOException;
    abstract public void dastore () throws IOException;

    abstract public void dup () throws IOException;
    abstract public void dup_x1() throws IOException;
    abstract public void dup2 () throws IOException;
    abstract public void dup_x2 () throws IOException;
    abstract public void dup2_x1 () throws IOException;
    abstract public void dup2_x2 () throws IOException;
    abstract public void swap () throws IOException;

    abstract public void pushLong (long val) throws IOException;
    abstract public void pushInt (int val) throws IOException;
    abstract public void pushString (String val) throws IOException;
    abstract public void pushFloat (float val) throws IOException;
    abstract public void pushDouble (double val) throws IOException;
    abstract public void pushNull () throws IOException;

    abstract public void pop () throws IOException;
    abstract public void pop2 () throws IOException;

    abstract public void jmp (Object label) throws IOException;
    abstract public void ifeq (Object label) throws IOException;
    abstract public void ifne (Object label) throws IOException;
    abstract public void iflt (Object label) throws IOException;
    abstract public void ifle (Object label) throws IOException;
    abstract public void ifgt (Object label) throws IOException;
    abstract public void ifge (Object label) throws IOException;

    abstract public void iand () throws IOException;
    abstract public void ior () throws IOException;
    abstract public void ixor () throws IOException;
    abstract public void land () throws IOException;
    abstract public void lor () throws IOException;
    abstract public void lxor () throws IOException;

    abstract public void ieq () throws IOException;
    abstract public void leq () throws IOException;
    abstract public void aeq () throws IOException;
    abstract public void seq () throws IOException;
    abstract public void deq () throws IOException;
    abstract public void feq () throws IOException;

    abstract public void igreater () throws IOException;
    abstract public void iless () throws IOException;
    abstract public void igeq () throws IOException;
    abstract public void ileq () throws IOException;

    abstract public void lgreater () throws IOException;
    abstract public void lless () throws IOException;
    abstract public void lgeq () throws IOException;
    abstract public void lleq () throws IOException;

    abstract public void fgreater () throws IOException;
    abstract public void fless () throws IOException;
    abstract public void fgeq () throws IOException;
    abstract public void fleq () throws IOException;

    abstract public void dgreater () throws IOException;
    abstract public void dless () throws IOException;
    abstract public void dgeq () throws IOException;
    abstract public void dleq () throws IOException;

    abstract public void ishl () throws IOException;
    abstract public void ishr () throws IOException;
    abstract public void iushr () throws IOException;
    abstract public void lshl () throws IOException;
    abstract public void lshr () throws IOException;
    abstract public void lushr () throws IOException;

    abstract public void iadd () throws IOException;
    abstract public void isub () throws IOException;
    abstract public void idiv () throws IOException;
    abstract public void imul () throws IOException;
    abstract public void irem () throws IOException;

    abstract public void ladd () throws IOException;
    abstract public void lsub () throws IOException;
    abstract public void ldiv () throws IOException;
    abstract public void lmul () throws IOException;
    abstract public void lrem () throws IOException;

    abstract public void fadd () throws IOException;
    abstract public void fsub () throws IOException;
    abstract public void fdiv () throws IOException;
    abstract public void fmul () throws IOException;

    abstract public void dadd () throws IOException;
    abstract public void dsub () throws IOException;
    abstract public void ddiv () throws IOException;
    abstract public void dmul () throws IOException;

    abstract public void i2b () throws IOException;
    abstract public void i2c () throws IOException;
    abstract public void i2s () throws IOException;
    abstract public void i2l () throws IOException;
    abstract public void i2f () throws IOException;
    abstract public void i2d () throws IOException;
    abstract public void l2i () throws IOException;
    abstract public void l2f () throws IOException;
    abstract public void l2d () throws IOException;
    abstract public void f2d () throws IOException;
    abstract public void f2i () throws IOException;
    abstract public void f2l () throws IOException;
    abstract public void d2i () throws IOException;
    abstract public void d2f () throws IOException;
    abstract public void d2l () throws IOException;

    abstract public void intToStr () throws IOException;
    abstract public void longToStr () throws IOException;
    abstract public void floatToStr () throws IOException;
    abstract public void doubleToStr () throws IOException;
    abstract public void concatStr () throws IOException;

    abstract public void ineg () throws IOException;
    abstract public void lneg () throws IOException;
    abstract public void fneg () throws IOException;
    abstract public void dneg () throws IOException;

    abstract public void iinc () throws IOException;
    abstract public void linc () throws IOException;

    abstract public void instance_of (Type type) throws IOException;

    abstract public void strFromByteArray () throws IOException;
    abstract public void strToByteArray () throws IOException;
    abstract public void strSubStr1 () throws IOException;
    abstract public void strSubStr2 () throws IOException;
    abstract public void strCharAt () throws IOException;
    abstract public void strIndexOf1 () throws IOException;
    abstract public void strIndexOf2 () throws IOException;
    abstract public void strIndexOf3 () throws IOException;
    abstract public void strIndexOf4 () throws IOException;
    abstract public void strLastIndexOf1 () throws IOException;
    abstract public void strLastIndexOf2 () throws IOException;
    abstract public void strLength () throws IOException;
    abstract public void strReplace () throws IOException;
    abstract public void strStartsWith () throws IOException;
    abstract public void strEndsWith () throws IOException;
    abstract public void strTrim () throws IOException;

    abstract public void objectEquals () throws IOException;
}
