
package jcc2.compiler;

/**
 *
 * @author note173@gmail.com
 */

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import jcc2.common.*;
import jcc2.lib.Library;
import jcc2.runtime.RuntimeConst;

public class JccCodeGenerator extends CodeGenerator implements RuntimeConst
{
    boolean bFakeMode;
    Vector vFakeModeStack;

    ByteArrayOutputStream bosHeader;
    DataOutputStream osHeader;
    DataOutputStream osCur;

    MethodContainer initMethod;
    MethodContainer curMethod;

    Hashtable htLongConst;
    Hashtable htFloatConst;
    Hashtable htDoubleConst;
    Hashtable htStringConst;
    int nextConst;

    int nextLabel;

    public JccCodeGenerator ()
    {
        nextLabel = 0;
        bFakeMode = false;
        vFakeModeStack = new Vector ();

        htLongConst = new Hashtable ();
        htFloatConst = new Hashtable ();
        htDoubleConst = new Hashtable ();
        htStringConst = new Hashtable ();
        nextConst = 0;

        bosHeader = new ByteArrayOutputStream ();
        osHeader = new DataOutputStream (bosHeader);
        initMethod = new MethodContainer ("<init>", Type.TYPE_VOID, new Type[0]);
        setInitMethod();
    }

    public void writeByte (int val) throws IOException
    {
        if (bFakeMode)
            return;
        
        osCur.write(val);
        if (curMethod != null)
            curMethod.pos++;
    }

    public void opcode (int val) throws IOException
    {
        if (val >= Byte.MAX_VALUE)
        {
            writeByte (-1);
            writeShort (val);
        }
        else
        {
            writeByte (val);
        }
    }

    public void writeShort (int val) throws IOException
    {
        writeByte((val>>8)&0xff);
        writeByte(val&0xff);
    }

    public void writeInt (int val) throws IOException
    {
        writeByte((val>>24)&0xff);
        writeByte((val>>16)&0xff);
        writeByte((val>>8)&0xff);
        writeByte(val&0xff);
    }

    public void setInitMethod()
    {
        curMethod = initMethod;
        osCur = curMethod.os;
    }

    public void setCurrentMethod(MethodContainer method)
    {
        curMethod = method;
        osCur = curMethod.os;
    }

    public void link(DataOutputStream os, ClassContainer mainClass, SymLocator locator) throws IOException
    {
        int curPos = 0;

        //magic
        osHeader.writeShort (0xCCCF);
        curPos += 2;
        osHeader.writeInt (Compiler.VERSION);
        curPos += 4;

        //libraries
        String[] vLibs = new String[locator.htLibraries.size()];
        osHeader.write (vLibs.length);
        curPos += 1;
        for (Enumeration en = locator.htLibraries.elements(); en.hasMoreElements(); )
        {
            Library library = (Library)en.nextElement();
            vLibs[library.id] = library.getName();
        }
        for (int i = 0; i < vLibs.length; i++)
        {
            osHeader.writeShort (vLibs[i].length());
            curPos += 2;
            for (int j = 0; j < vLibs[i].length(); j++)
                osHeader.write (vLibs[i].charAt(j));
            curPos += vLibs[i].length();
        }

        //constant pool
        Object[] vConst = new Object[htLongConst.size() + htFloatConst.size() + htStringConst.size() + htDoubleConst.size()];
        for (Enumeration en = htLongConst.elements(); en.hasMoreElements(); )
        {
            Object[] desc = (Object[])en.nextElement();
            vConst[((Integer)desc[0]).intValue()] = desc[1];
        }
        for (Enumeration en = htFloatConst.elements(); en.hasMoreElements(); )
        {
            Object[] desc = (Object[])en.nextElement();
            vConst[((Integer)desc[0]).intValue()] = desc[1];
        }
        for (Enumeration en = htDoubleConst.elements(); en.hasMoreElements(); )
        {
            Object[] desc = (Object[])en.nextElement();
            vConst[((Integer)desc[0]).intValue()] = desc[1];
        }
        for (Enumeration en = htStringConst.elements(); en.hasMoreElements(); )
        {
            Object[] desc = (Object[])en.nextElement();
            vConst[((Integer)desc[0]).intValue()] = desc[1];
        }
        osHeader.writeInt (vConst.length);
        curPos += 4;
        for (int i = 0; i < vConst.length; i++)
        {
            if (vConst[i] instanceof Long)
            {
                osHeader.write (1);
                curPos += 1;
                osHeader.writeLong (((Long)vConst[i]).longValue());
                curPos += 8;
            }
            else if (vConst[i] instanceof Float)
            {
                osHeader.write (2);
                curPos += 1;
                osHeader.writeFloat (((Float)vConst[i]).floatValue());
                curPos += 4;
            }
            else if (vConst[i] instanceof Double)
            {
                osHeader.write (3);
                curPos += 1;
                osHeader.writeDouble (((Double)vConst[i]).doubleValue());
                curPos += 4;
            }
            else if (vConst[i] instanceof String)
            {
                osHeader.write (4);
                curPos += 1;
                osHeader.writeShort (((String)vConst[i]).length());
                curPos += 2;
                for (int j = 0; j < ((String)vConst[i]).length(); j++)
                    osHeader.write (((String)vConst[i]).charAt(j));
                curPos += ((String)vConst[i]).length();
            }
        }

        //globals
        osHeader.writeInt(mainClass.htFields.size());
        curPos += 4;
//        Type[] vFields = new Type[mainClass.htFields.size()];
//        for (Enumeration en = mainClass.getFields(); en.hasMoreElements(); )
//        {
//            FieldContainer field = (FieldContainer)en.nextElement();
//            vFields[field.num] = field.type;
//        }
//        for (int i = 0; i < vFields.length; i++)
//        {
//            Type type = vFields[i];
//            Type.genBinDesc(type, osHeader);
//        }

        //methods
        int[][] methodPointers = new int[mainClass.htMethods.size()][2];
        osHeader.writeInt(mainClass.htMethods.size());
        curPos += 4;
        for (Enumeration en = mainClass.htMethods.elements(); en.hasMoreElements(); )
        {
            MethodContainer method = (MethodContainer)en.nextElement();
            osHeader.writeShort (method.id);
            curPos += 2;
            methodPointers[method.id][0] = curPos;
            osHeader.writeInt (0);
            curPos += 4;
            osHeader.writeShort(method.nextId);
            curPos += 2;
            osHeader.writeShort (method.fullSpec.length());
            curPos += 2;
            for (int i = 0; i < method.fullSpec.length(); i++)
                osHeader.write(method.fullSpec.charAt(i));
            curPos += method.fullSpec.length();
        }

        for (Enumeration en = mainClass.htMethods.elements(); en.hasMoreElements(); )
        {
            MethodContainer method = (MethodContainer)en.nextElement();
            methodPointers[method.id][1] = curPos;
            byte[] codeMethod = method.getCode();
            osHeader.write(codeMethod);
            curPos += codeMethod.length;
        }

        osHeader.close ();
        byte[] dataCode = bosHeader.toByteArray();
        for (int i = 0; i < methodPointers.length; i++)
        {
            int pos = methodPointers[i][0];
            int rep = methodPointers[i][1];
            dataCode[pos + 0] = (byte)((rep >> 24)&0xff);
            dataCode[pos + 1] = (byte)((rep >> 16)&0xff);
            dataCode[pos + 2] = (byte)((rep >> 8)&0xff);
            dataCode[pos + 3] = (byte)((rep >> 0)&0xff);
        }
        

        os.write(dataCode);
    }

    public void loadThis() throws IOException
    {
        opcode (LIBRARY);
        writeByte (-1);
    }

    public void loadLibraryInstance(Library library) throws IOException
    {
        opcode (LIBRARY);
        writeByte (library.id);
    }

    public Object reserveLabel() throws IOException
    {
        Integer label = new Integer(nextLabel++);
        Object[] context = {label, new Integer(0), new Vector ()};
        curMethod.htLabels.put(label, context);
        return label;
    }

    public void markLabel(Object label) throws IOException
    {
        Object[] context = (Object[])curMethod.htLabels.get(label);
        context[1] = new Integer(curMethod.pos);
    }

    public void nop() throws IOException
    {
        opcode (NOP);
    }

    public void fakeModeOn()
    {
        vFakeModeStack.addElement (new Boolean(bFakeMode));
        bFakeMode = true;
    }

    public void fakeModeOff()
    {
        bFakeMode = ((Boolean)vFakeModeStack.elementAt(vFakeModeStack.size()-1)).booleanValue();
        vFakeModeStack.removeElementAt(vFakeModeStack.size()-1);
    }

    public void setLocal(MethodContainer method, FieldContainer field) throws IOException
    {
        Type type = field.type;
        if (type.equals(Type.TYPE_BOOL) || type.equals(Type.TYPE_BYTE) ||
                type.equals(Type.TYPE_CHAR) || type.equals(Type.TYPE_SHORT) ||
                type.equals(Type.TYPE_INT))
        {
            opcode (ISTORE);
            writeShort (field.num);
        }
        else if (type.equals(Type.TYPE_LONG))
        {
            opcode (LSTORE);
            writeShort (field.num);
        }
        else if (type.equals(Type.TYPE_FLOAT))
        {
            opcode (FSTORE);
            writeShort (field.num);
        }
        else if (type.equals(Type.TYPE_DOUBLE))
        {
            opcode (DSTORE);
            writeShort (field.num);
        }
        else
        {
            opcode (ASTORE);
            writeShort (field.num);
        }
    }

    public void setGlobal(FieldContainer field) throws IOException
    {
        opcode (SETGLOBAL);
        writeShort (field.num);
        
        /*Type type = field.type;
        if (type.equals(Type.TYPE_BOOL) || type.equals(Type.TYPE_BYTE) ||
                type.equals(Type.TYPE_CHAR) || type.equals(Type.TYPE_SHORT) ||
                type.equals(Type.TYPE_INT))
        {
            opcode (ISTORE);
            writeByte (-1);
            writeShort (field.num);
        }
        else if (type.equals(Type.TYPE_LONG))
        {
            opcode (LSTORE);
            writeByte (-1);
            writeShort (field.num);
        }
        else if (type.equals(Type.TYPE_FLOAT))
        {
            opcode (FSTORE);
            writeByte (-1);
            writeShort (field.num);
        }
        else if (type.equals(Type.TYPE_DOUBLE))
        {
            opcode (DSTORE);
            writeByte (-1);
            writeShort (field.num);
        }
        else
        {
            opcode (ASTORE);
            writeByte (-1);
            writeShort (field.num);
        }*/
    }

    public void getLocal(MethodContainer method, FieldContainer field) throws IOException
    {
        Type type = field.type;
        if (type.equals(Type.TYPE_BOOL) || type.equals(Type.TYPE_BYTE) ||
                type.equals(Type.TYPE_CHAR) || type.equals(Type.TYPE_SHORT) ||
                type.equals(Type.TYPE_INT))
        {
            opcode (ILOAD);
            writeShort (field.num);
        }
        else if (type.equals(Type.TYPE_LONG))
        {
            opcode (LLOAD);
            writeShort (field.num);
        }
        else if (type.equals(Type.TYPE_FLOAT))
        {
            opcode (FLOAD);
            writeShort (field.num);
        }
        else if (type.equals(Type.TYPE_DOUBLE))
        {
            opcode (DLOAD);
            writeShort (field.num);
        }
        else
        {
            opcode (ALOAD);
            writeShort (field.num);
        }
    }

    public void getGlobal(FieldContainer field) throws IOException
    {
        opcode (GETGLOBAL);
        writeShort (field.num);

        /*Type type = field.type;
        if (type.equals(Type.TYPE_BOOL) || type.equals(Type.TYPE_BYTE) ||
                type.equals(Type.TYPE_CHAR) || type.equals(Type.TYPE_SHORT) ||
                type.equals(Type.TYPE_INT))
        {
            opcode (ILOAD);
            writeByte (-1);
            writeShort (field.num);
        }
        else if (type.equals(Type.TYPE_LONG))
        {
            opcode (LLOAD);
            writeByte (-1);
            writeShort (field.num);
        }
        else if (type.equals(Type.TYPE_FLOAT))
        {
            opcode (FLOAD);
            writeByte (-1);
            writeShort (field.num);
        }
        else if (type.equals(Type.TYPE_DOUBLE))
        {
            opcode (DLOAD);
            writeByte (-1);
            writeShort (field.num);
        }
        else
        {
            opcode (ALOAD);
            writeByte (-1);
            writeShort (field.num);
        }*/
    }

    public void setMember(ClassContainer cont, FieldContainer field) throws IOException
    {
        throw new RuntimeException("Not supported yet.");
    }

    public void getMember(ClassContainer cont, FieldContainer field) throws IOException
    {
        throw new RuntimeException("Not supported yet.");
    }

    public void call(MethodContainer method) throws IOException
    {
        if (method.libHost != null)
        {
            opcode (INVOKE_LIB);
            writeShort(method.id);
        }
        else
        {
            opcode (INVOKE);
            writeShort(method.id);
        }
        writeByte (method.args.length);
    }

    public void callMember(ClassContainer cont, MethodContainer method) throws IOException
    {
        opcode (INVOKE_MEMBER);
        writeShort (method.id);
        writeByte (method.args.length);
    }

    public void retVoid() throws IOException
    {
        opcode (RET_V);
    }

    public void ret(Type type) throws IOException
    {
        opcode (RET);
    }

    public void newArray(Type type) throws IOException
    {
        if (type.kind == Type.K_OBJECT)
            opcode (ANEWARRAY);
        else
        {
            opcode (NEWARRAY);
            if (type.equals(Type.TYPE_BOOL))
                writeByte (T_BYTE);
            else if(type.equals(Type.TYPE_BYTE))
                writeByte (T_BYTE);
            else if(type.equals(Type.TYPE_CHAR))
                writeByte (T_CHAR);
            else if (type.equals(Type.TYPE_DOUBLE))
                writeByte (T_DOUBLE);
            else if (type.equals(Type.TYPE_FLOAT))
                writeByte (T_FLOAT);
            else if (type.equals(Type.TYPE_INT))
                writeByte (T_INT);
            else if (type.equals(Type.TYPE_LONG))
                writeByte (T_LONG);
            else if (type.equals(Type.TYPE_SHORT))
                writeByte (T_SHORT);
        }
    }

    public void multianewArray(Type type, int dims) throws IOException
    {
        opcode (MULTIANEWARRAY);
        writeByte (dims);
    }

    public void arraylen() throws IOException
    {
        opcode (ARRAYLENGTH);
    }

    public void iaload() throws IOException
    {
        opcode (IALOAD);
    }

    public void iastore() throws IOException
    {
        opcode (IASTORE);
    }

    public void saload() throws IOException
    {
        opcode (SALOAD);
    }

    public void sastore() throws IOException
    {
        opcode (SASTORE);
    }

    public void caload() throws IOException
    {
        opcode (CALOAD);
    }

    public void castore() throws IOException
    {
        opcode (CASTORE);
    }

    public void laload() throws IOException
    {
        opcode (LALOAD);
    }

    public void lastore() throws IOException
    {
        opcode (LASTORE);
    }

    public void baload() throws IOException
    {
        opcode (BALOAD);
    }

    public void bastore() throws IOException
    {
        opcode (BASTORE);
    }

    public void aaload() throws IOException
    {
        opcode (AALOAD);
    }

    public void aastore() throws IOException
    {
        opcode (AASTORE);
    }

    public void faload() throws IOException
    {
        opcode (FALOAD);
    }

    public void fastore() throws IOException
    {
        opcode (FASTORE);
    }

    public void daload() throws IOException
    {
        opcode (DALOAD);
    }

    public void dastore() throws IOException
    {
        opcode (DASTORE);
    }

    public void dup() throws IOException
    {
        opcode (DUP);
    }

    public void dup_x1() throws IOException
    {
        opcode (DUP_X1);
    }

    public void dup2() throws IOException
    {
        opcode (DUP2);
    }

    public void dup_x2() throws IOException
    {
        opcode (DUP_X2);
    }

    public void dup2_x1() throws IOException
    {
        opcode (DUP2_X1);
    }

    public void dup2_x2() throws IOException
    {
        opcode (DUP2_X2);
    }

    public void swap() throws IOException
    {
        opcode (SWAP);
    }

    public void pushLong(long val) throws IOException
    {
        opcode (LDC);
        Object[] valConst = (Object[])htLongConst.get(new Long(val));
        int id;
        if (valConst == null)
        {
            id = nextConst++;
            valConst = new Object[2];
            valConst[0] = new Integer(id);
            valConst[1] = new Long (val);
            htLongConst.put(valConst[1], valConst);
        }
        else
        {
            id = ((Integer)valConst[0]).intValue();
        }
        writeShort (id);
    }

    public void pushInt(int val) throws IOException
    {
        opcode (ICONST);
        writeInt (val);
    }

    public void pushString(String val) throws IOException
    {
        opcode (LDC);
        Object[] valConst = (Object[])htStringConst.get(val);
        int id;
        if (valConst == null)
        {
            id = nextConst++;
            valConst = new Object[2];
            valConst[0] = new Integer(id);
            valConst[1] = val;
            htStringConst.put(valConst[1], valConst);
        }
        else
        {
            id = ((Integer)valConst[0]).intValue();
        }
        writeShort (id);
    }

    public void pushFloat(float val) throws IOException
    {
        opcode (LDC);
        Object[] valConst = (Object[])htFloatConst.get(new Float(val));
        int id;
        if (valConst == null)
        {
            id = nextConst++;
            valConst = new Object[2];
            valConst[0] = new Integer(id);
            valConst[1] = new Float (val);
            htLongConst.put(valConst[1], valConst);
        }
        else
        {
            id = ((Integer)valConst[0]).intValue();
        }
        writeShort (id);
    }

    public void pushDouble(double val) throws IOException
    {
        opcode (LDC);
        Object[] valConst = (Object[])htLongConst.get(new Double(val));
        int id;
        if (valConst == null)
        {
            id = nextConst++;
            valConst = new Object[2];
            valConst[0] = new Integer(id);
            valConst[1] = new Double (val);
            htLongConst.put(valConst[1], valConst);
        }
        else
        {
            id = ((Integer)valConst[0]).intValue();
        }
        writeShort (id);
    }

    public void pushNull() throws IOException
    {
        opcode (ACONST_NULL);
    }

    public void pop() throws IOException
    {
        opcode (POP);
    }

    public void pop2() throws IOException
    {
        opcode (POP2);
    }

    public void jmp(Object label) throws IOException
    {
        Object[] context = (Object[])curMethod.htLabels.get(label);
        opcode (GOTO);
        ((Vector)(context[2])).addElement(new Integer(curMethod.pos));
        writeShort (0);
    }

    public void ifeq(Object label) throws IOException
    {
        Object[] context = (Object[])curMethod.htLabels.get(label);
        opcode (IFEQ);
        ((Vector)(context[2])).addElement(new Integer(curMethod.pos));
        writeShort (0);
    }

    public void ifne(Object label) throws IOException
    {
        Object[] context = (Object[])curMethod.htLabels.get(label);
        opcode (IFNE);
        ((Vector)(context[2])).addElement(new Integer(curMethod.pos));
        writeShort (0);
    }

    public void iflt(Object label) throws IOException
    {
        Object[] context = (Object[])curMethod.htLabels.get(label);
        opcode (IFLT);
        ((Vector)(context[2])).addElement(new Integer(curMethod.pos));
        writeShort (0);
    }

    public void ifle(Object label) throws IOException
    {
        Object[] context = (Object[])curMethod.htLabels.get(label);
        opcode (IFLE);
        ((Vector)(context[2])).addElement(new Integer(curMethod.pos));
        writeShort (0);
    }

    public void ifgt(Object label) throws IOException
    {
        Object[] context = (Object[])curMethod.htLabels.get(label);
        opcode (IFGT);
        ((Vector)(context[2])).addElement(new Integer(curMethod.pos));
        writeShort (0);
    }

    public void ifge(Object label) throws IOException
    {
        Object[] context = (Object[])curMethod.htLabels.get(label);
        opcode (IFGE);
        ((Vector)(context[2])).addElement(new Integer(curMethod.pos));
        writeShort (0);
    }

    public void iand() throws IOException
    {
        opcode (IAND);
    }

    public void ior() throws IOException
    {
        opcode (IOR);
    }

    public void ixor() throws IOException
    {
        opcode (IXOR);
    }

    public void land() throws IOException
    {
        opcode (LAND);
    }

    public void lor() throws IOException
    {
        opcode (LOR);
    }

    public void lxor() throws IOException
    {
        opcode (LXOR);
    }

    public void ieq() throws IOException
    {
        opcode (IEQ);
    }

    public void leq() throws IOException
    {
        opcode (LEQ);
    }

    public void aeq() throws IOException
    {
        opcode (AEQ);
    }

    public void seq() throws IOException
    {
        opcode (IEQ);
    }

    public void deq() throws IOException
    {
        opcode (DEQ);
    }

    public void feq() throws IOException
    {
        opcode (FEQ);
    }

    public void igreater() throws IOException
    {
        opcode (IGT);
    }

    public void iless() throws IOException
    {
        opcode (ILT);
    }

    public void igeq() throws IOException
    {
        opcode (IGE);
    }

    public void ileq() throws IOException
    {
        opcode (ILE);
    }

    public void lgreater() throws IOException
    {
        opcode (LGT);
    }

    public void lless() throws IOException
    {
        opcode (LLT);
    }

    public void lgeq() throws IOException
    {
        opcode (LGE);
    }

    public void lleq() throws IOException
    {
        opcode (LLE);
    }

    public void fgreater() throws IOException
    {
        opcode (FGT);
    }

    public void fless() throws IOException
    {
        opcode (FLT);
    }

    public void fgeq() throws IOException
    {
        opcode (FGE);
    }

    public void fleq() throws IOException
    {
        opcode (FLE);
    }

    public void dgreater() throws IOException
    {
        opcode (DGT);
    }

    public void dless() throws IOException
    {
        opcode (DLT);
    }

    public void dgeq() throws IOException
    {
        opcode (DGE);
    }

    public void dleq() throws IOException
    {
        opcode (DLE);
    }

    public void ishl() throws IOException
    {
        opcode (ISHL);
    }

    public void ishr() throws IOException
    {
        opcode (ISHR);
    }

    public void iushr() throws IOException
    {
        opcode (IUSHR);
    }

    public void lshl() throws IOException
    {
        opcode (LSHL);
    }

    public void lshr() throws IOException
    {
        opcode (ISHR);
    }

    public void lushr() throws IOException
    {
        opcode (IUSHR);
    }

    public void iadd() throws IOException
    {
        opcode (IADD);
    }

    public void isub() throws IOException
    {
        opcode (ISUB);
    }

    public void idiv() throws IOException
    {
        opcode (IDIV);
    }

    public void imul() throws IOException
    {
        opcode (IMUL);
    }

    public void irem() throws IOException
    {
        opcode (IREM);
    }

    public void ladd() throws IOException
    {
        opcode (LADD);
    }

    public void lsub() throws IOException
    {
        opcode (LSUB);
    }

    public void ldiv() throws IOException
    {
        opcode (LDIV);
    }

    public void lmul() throws IOException
    {
        opcode (LMUL);
    }

    public void lrem() throws IOException
    {
        opcode (LREM);
    }

    public void fadd() throws IOException
    {
        opcode (FADD);
    }

    public void fsub() throws IOException
    {
        opcode (FSUB);
    }

    public void fdiv() throws IOException
    {
        opcode (FDIV);
    }

    public void fmul() throws IOException
    {
        opcode (FMUL);
    }

    public void dadd() throws IOException
    {
        opcode (DADD);
    }

    public void dsub() throws IOException
    {
        opcode (DSUB);
    }

    public void ddiv() throws IOException
    {
        opcode (DDIV);
    }

    public void dmul() throws IOException
    {
        opcode (DMUL);
    }

    public void i2b() throws IOException
    {
        opcode (I2B);
    }

    public void i2c() throws IOException
    {
        opcode (I2C);
    }

    public void i2s() throws IOException
    {
        opcode (I2S);
    }

    public void i2l() throws IOException
    {
        opcode (I2L);
    }

    public void i2f() throws IOException
    {
        opcode (I2F);
    }

    public void i2d() throws IOException
    {
        opcode (I2D);
    }

    public void l2i() throws IOException
    {
        opcode (L2I);
    }

    public void l2f() throws IOException
    {
        opcode (L2F);
    }

    public void l2d() throws IOException
    {
        opcode (L2D);
    }

    public void f2d() throws IOException
    {
        opcode (F2D);
    }

    public void f2i() throws IOException
    {
        opcode (F2I);
    }

    public void f2l() throws IOException
    {
        opcode (F2L);
    }

    public void d2i() throws IOException
    {
        opcode (D2I);
    }

    public void d2f() throws IOException
    {
        opcode (D2F);
    }

    public void d2l() throws IOException
    {
        opcode (D2L);
    }

    public void intToStr() throws IOException
    {
        opcode (ITOS);
    }

    public void longToStr() throws IOException
    {
        opcode (LTOS);
    }

    public void floatToStr() throws IOException
    {
        opcode (FTOS);
    }

    public void doubleToStr() throws IOException
    {
        opcode (DTOS);
    }

    public void concatStr() throws IOException
    {
        opcode (CONCATSTR);
    }

    public void ineg() throws IOException
    {
        opcode (INEG);
    }

    public void lneg() throws IOException
    {
        opcode (LNEG);
    }

    public void fneg() throws IOException
    {
        opcode (FNEG);
    }

    public void dneg() throws IOException
    {
        opcode (DNEG);
    }

    public void iinc() throws IOException
    {
        opcode (IINC);
    }

    public void linc() throws IOException
    {
        opcode (LINC);
    }

    public void instance_of(Type type) throws IOException
    {
        opcode (INSTANCEOF);
        String sSpec = Type.genSpec(type);
        writeShort (sSpec.length());
        for (int i = 0; i < sSpec.length(); i++)
            writeByte (sSpec.charAt(i));
    }

    public void strFromByteArray() throws IOException
    {
        opcode (ABYTE_TO_STR);
    }

    public void strToByteArray() throws IOException
    {
        opcode (STR_TO_ABYTE);
    }

    public void strSubStr1() throws IOException
    {
        opcode (STR_SUBSTR1);
    }

    public void strSubStr2() throws IOException
    {
        opcode (STR_SUBSTR2);
    }

    public void strCharAt() throws IOException
    {
        opcode (STR_CHARAT);
    }

    public void strIndexOf1() throws IOException
    {
        opcode (STR_INDEXOF1);
    }

    public void strIndexOf2() throws IOException
    {
        opcode (STR_INDEXOF2);
    }

    public void strIndexOf3() throws IOException
    {
        opcode (STR_INDEXOF3);
    }

    public void strIndexOf4() throws IOException
    {
        opcode (STR_INDEXOF4);
    }

    public void strLastIndexOf1() throws IOException
    {
        opcode (STR_LASTINDEXOF1);
    }

    public void strLastIndexOf2() throws IOException
    {
        opcode (STR_LASTINDEXOF2);
    }

    public void strLength() throws IOException
    {
        opcode (STR_LENGTH);
    }

    public void strReplace() throws IOException
    {
        opcode (STR_REPLACE);
    }

    public void strStartsWith() throws IOException
    {
        opcode (STR_STARTSWITH);
    }

    public void strEndsWith() throws IOException
    {
        opcode (STR_ENDSWITH);
    }

    public void strTrim() throws IOException
    {
        opcode (STR_TRIM);
    }

    public void objectEquals() throws IOException
    {
        opcode (OBJ_EQUALS);
    }
}
