
package jcc2.runtime;

import java.io.*;
import java.util.Hashtable;
import java.util.Vector;
import jcc2.common.Type;
import jcc2.compiler.JccException;
import jcc2.lib.Library;
import jcc2.lib.RTObject;

/**
 *
 * @author note173@gmail.com
 */

public class JccRuntime implements RuntimeConst, Runnable
{
    public static final int VERSION = 1;

    byte[] code;
    int IP;

    Object[] vConst;
    Library[] vLibs;
    Object[] vGlobals;
    int[] vMethodEntries;
    int[] vnMethodLocals;
    Hashtable htEntries;
    Vector vStack;
    Vector vCallStack;

    boolean bRun;

    public JccRuntime (byte[] data) throws JccException, IOException
    {
        code = data;
        ByteArrayInputStream bis = new ByteArrayInputStream (data);
        DataInputStream is = new DataInputStream (bis);

        short magic = is.readShort ();
        if (magic != (short)0xCCCF)
            throw new JccException ("incorrect magic");

        if (is.readInt() != VERSION)
            throw new JccException ("incorrect version");

        int nLibs = is.read();
        vLibs = new Library[nLibs];
        for (int i = 0; i < nLibs; i++)
        {
            int len = is.readShort();
            String sLib = "";
            for (int j = 0; j < len; j++)
                sLib += (char)is.read ();
            
            Class classLibrary = null;
            Library library = null;
            try {
                classLibrary = Class.forName("jcc2.lib." + sLib + "." + "library");
            } catch (Exception e) {
                throw new JccException ("can't load library '" + sLib + "'");
            }
            try {
                library = (Library)classLibrary.newInstance();
            } catch (Exception e) {
                throw new JccException ("can't initialize library '" + sLib + "'");
            }
            library.init(false);
            vLibs[i] = library;
        }

        int nConst = is.readInt();
        vConst = new Object[nConst];
        for (int i = 0; i < vConst.length; i++)
        {
            int iType = is.read ();
            if (iType == 1)
            {
                vConst[i] = new long[1];
                ((long[])vConst[i])[0] = is.readLong();
            }
            else if (iType == 2)
            {
                vConst[i] = new float[1];
                ((float[])vConst[i])[0] = is.readFloat();
            }
            else if (iType == 3)
            {
                vConst[i] = new double[1];
                ((double[])vConst[i])[0] = is.readDouble();
            }
            else if (iType == 4)
            {
                short len = is.readShort();
                String sConst = "";
                for (short j = 0; j < len; j++)
                    sConst += (char)is.read();
                vConst[i] = sConst;
            }
            else
                throw new RuntimeException ("constant");
        }

        int nGlobals = is.readInt ();
        vGlobals = new Object[nGlobals];

        int nMethods = is.readInt ();
        vMethodEntries = new int[nMethods];
        vnMethodLocals = new int[nMethods];
        htEntries = new Hashtable ();
        for (int i = 0; i < nMethods; i++)
        {
            int id = is.readShort ();
            vMethodEntries[id] = is.readInt ();
            vnMethodLocals[id] = is.readShort ();
            String sDesc = "";
            int len = is.readShort ();
            for (int j = 0; j < len; j++)
                sDesc += (char)is.read();
            htEntries.put (sDesc, new Integer(id));
        }

        is.close ();
    }

    public void start (String entry, Object[] args) throws JccException
    {
        Integer iEntry = (Integer)htEntries.get(entry);
        if (iEntry == null)
            throw new JccException ("invalid entry");
        IP = -1;
        vCallStack = new Vector ();
        vStack = new Vector ();
        invoke (iEntry.intValue(), args);
        bRun = true;
        new Thread(this).start();
    }

    public void stop ()
    {
        bRun = false;
    }

    public void invoke (int iMethod, Object[] args)
    {
        pushInt (IP);
        IP = vMethodEntries[iMethod];
        Object[] vLocals = new Object[vnMethodLocals[iMethod]];
        for (int i = 0; i < args.length; i++)
            vLocals[i+1] = args[i];
        vCallStack.addElement(vLocals);
    }

    public void invokeLib (Library library, int iMethod, Object[] args)
    {
        try {
            Object ret = library.rtInvokeMethod(iMethod, args);
            if (!(ret instanceof Type))
                push (ret);
        } catch (Exception e)
        {
            jcc2.lib.stdlib.library.singleton.setLastError (e.getMessage());
            e.printStackTrace();
        }
    }

    public void invokeObject (RTObject object, int iMethod, Object[] args)
    {
        try {
            Object ret = object.rtInvoke(iMethod, args);
            if (!(ret instanceof Type))
                push (ret);
        } catch (Exception e)
        {
            jcc2.lib.stdlib.library.singleton.setLastError (e.getMessage());
            e.printStackTrace();
        }
    }

    public int readByte ()
    {
        return code[IP++];
    }

    public int readInt ()
    {
        return ((((int)(code[IP++]) & 0xff) << 24) |
                (((int)(code[IP++]) & 0xff) << 16) |
                (((int)(code[IP++]) & 0xff) << 8) |
                ((int)(code[IP++]) & 0xff));
    }

    public int readShort ()
    {
        return (short)((((short)code[IP++]) << 8) | (((short)code[IP++]) & 0xff));
    }

    public int readOpcode ()
    {
        int opcode = code[IP++];
        if (opcode < 0)
            opcode = readShort ();
        return opcode;
    }

    public void push (Object val)
    {
        vStack.addElement(val);
    }

    public void pushInt (int val)
    {
        int[] v = {val};
        push (v);
    }

    public Object pop ()
    {
        Object val = vStack.elementAt(vStack.size()-1);
        vStack.removeElementAt(vStack.size()-1);
        return val;
    }

    public int popInt ()
    {
        return ((int[])pop())[0];
    }

    public void run ()
    {
        while (bRun)
        {
            if (IP == -1)
                return;
            int opcode = readOpcode ();
            switch (opcode)
            {
                case NOP:
                    continue;
                case ISTORE:
                case LSTORE:
                case FSTORE:
                case DSTORE:
                case ASTORE:
                {
                    int iLocal = readShort ();
                    ((Object[])vCallStack.elementAt(vCallStack.size()-1))[iLocal] = pop ();
                    break;
                }
                case ILOAD:
                case LLOAD:
                case FLOAD:
                case DLOAD:
                case ALOAD:
                {
                    int iLocal = readShort ();
                    push (((Object[])vCallStack.elementAt(vCallStack.size()-1))[iLocal]);
                    break;
                }
                case INVOKE:
                {
                    int iMethod = readShort ();
                    int nArgs = readByte ();
                    Object[] args = new Object[nArgs];
                    for (int i = 0; i < nArgs; i++)
                        args[nArgs-i-1] = pop();
                    pop (); // null
                    invoke (iMethod, args);
                    break;
                }
                case INVOKE_LIB:
                {
                    int iMethod = readShort ();
                    int nArgs = readByte ();
                    Object[] args = new Object[nArgs];
                    for (int i = 0; i < nArgs; i++)
                        args[nArgs-i-1] = pop();
                    Library library = (Library)pop ();
                    invokeLib (library, iMethod, args);
                    break;
                }
                case INVOKE_MEMBER:
                {
                    int iMethod = readShort ();
                    int nArgs = readByte ();
                    Object[] args = new Object[nArgs];
                    for (int i = 0; i < nArgs; i++)
                        args[nArgs-i-1] = pop();
                    RTObject rtObject = (RTObject)pop ();
                    invokeObject (rtObject, iMethod, args);
                    break;
                }
                case RET_V:
                {
                    vCallStack.removeElementAt(vCallStack.size()-1);
                    IP = ((int[])pop())[0];
                    break;
                }
                case RET:
                {
                    vCallStack.removeElementAt(vCallStack.size()-1);
                    Object val = pop ();
                    IP = ((int[])pop())[0];
                    push (val);
                    break;
                }
                case ANEWARRAY:
                {
                    int size = popInt ();
                    Object val = new Object[0][size];
                    push (val);
                    break;
                }
                case NEWARRAY:
                {
                    int type = readByte ();
                    int size = popInt ();
                    Object[] array = new Object[1];
                    if (type == T_INT)
                        array[0] = new int[size];
                    else if (type == T_BYTE)
                        array[0] = new byte[size];
                    else if (type == T_CHAR)
                        array[0] = new char[size];
                    else if (type == T_SHORT)
                        array[0] = new short[size];
                    else if (type == T_LONG)
                        array[0] = new long[size];
                    else if (type == T_FLOAT)
                        array[0] = new float[size];
                    else if (type == T_DOUBLE)
                        array[0] = new double[size];
                    push (array);
                    break;
                }
                case MULTIANEWARRAY:
                {
                    int dims = readByte ();
                    int[] lens = new int[dims];
                    for (int i = 0; i < dims; i++)
                        lens[i] = popInt();
                    Object[] array = {allocArray (dims, lens)};
                    push (array);
                    break;
                }
                case ARRAYLENGTH:
                {
                    Object val = ((Object[])pop ())[0];
                    if (val instanceof int[])
                        pushInt (((int[])val).length);
                    else if (val instanceof byte[])
                        pushInt (((byte[])val).length);
                    else if (val instanceof char[])
                        pushInt (((char[])val).length);
                    else if (val instanceof short[])
                        pushInt (((short[])val).length);
                    else if (val instanceof long[])
                        pushInt (((long[])val).length);
                    else if (val instanceof float[])
                        pushInt (((float[])val).length);
                    else if (val instanceof double[])
                        pushInt (((double[])val).length);
                    else
                        pushInt(((Object[])val).length);
                }
                case IALOAD:
                {
                    int index = popInt ();
                    Object array = ((Object[])pop())[0];
                    pushInt(((int[])array)[index]);
                    break;
                }
                case BALOAD:
                {
                    int index = popInt ();
                    Object array = ((Object[])pop())[0];
                    pushInt(((byte[])array)[index]);
                    break;
                }
                case CALOAD:
                {
                    int index = popInt ();
                    Object array = ((Object[])pop())[0];
                    pushInt(((char[])array)[index]);
                    break;
                }
                case SALOAD:
                {
                    int index = popInt ();
                    Object array = ((Object[])pop())[0];
                    pushInt(((short[])array)[index]);
                    break;
                }
                case LALOAD:
                {
                    int index = popInt ();
                    Object array = ((Object[])pop())[0];
                    long[] val = {((long[])array)[index]};
                    push (val);
                    break;
                }
                case FALOAD:
                {
                    int index = popInt ();
                    Object array = ((Object[])pop())[0];
                    float[] val = {((float[])array)[index]};
                    push (val);
                    break;
                }
                case DALOAD:
                {
                    int index = popInt ();
                    Object array = ((Object[])pop())[0];
                    double[] val = {((double[])array)[index]};
                    push (val);
                    break;
                }
                case AALOAD:
                {
                    int index = popInt ();
                    Object array = ((Object[])pop())[0];
                    Object[] array2 = (Object[])array;
                    push (array2[index]);
                    break;
                }
                case IASTORE:
                {
                    int val = popInt ();
                    int index = popInt ();
                    int[] array = (int[])(((Object[])pop ())[0]);
                    array[index] = val;
                    break;
                }
                case BASTORE:
                {
                    byte val = (byte)popInt();
                    int index = popInt ();
                    byte[] array = (byte[])(((Object[])pop ())[0]);
                    array[index] = val;
                    break;
                }
                case CASTORE:
                {
                    char val = (char)popInt();
                    int index = popInt ();
                    char[] array = (char[])(((Object[])pop ())[0]);
                    array[index] = val;
                    break;
                }
                case SASTORE:
                {
                    short val = (short)popInt();
                    int index = popInt ();
                    short[] array = (short[])(((Object[])pop ())[0]);
                    array[index] = val;
                }
                case LASTORE:
                {
                    long val = ((long[])pop())[0];
                    int index = popInt ();
                    long[] array = (long[])(((Object[])pop ())[0]);
                    array[index] = val;
                    break;
                }
                case FASTORE:
                {
                    float val = ((float[])pop())[0];
                    int index = popInt ();
                    float[] array = (float[])(((Object[])pop ())[0]);
                    array[index] = val;
                    break;
                }
                case DASTORE:
                {
                    double val = ((double[])pop())[0];
                    int index = popInt ();
                    double[] array = (double[])(((Object[])pop ())[0]);
                    array[index] = val;
                    break;
                }
                case AASTORE:
                {
                    Object val = pop();
                    int index = popInt ();
                    Object[] array = (Object[])(((Object[])pop ())[0]);
                    array[index] = val;
                    break;
                }
                case DUP:
                {
                    Object val = pop ();
                    push (val);
                    push (val);
                    break;
                }
                case DUP2:
                {
                    Object val1 = pop ();
                    if (val1 instanceof long[] || val1 instanceof double[])
                    {
                        push (val1);
                        push (val1);
                    }
                    else
                    {
                        Object val2 = pop ();
                        push (val2);
                        push (val1);
                        push (val2);
                        push (val1);
                    }
                    break;
                }
                case DUP2_X1:
                {
                    Object val1 = pop ();
                    Object val2 = pop ();
                    if (val1 instanceof long[] || val1 instanceof double[])
                    {
                        push (val1);
                        push (val2);
                        push (val1);
                    }
                    else
                    {
                        Object val3 = pop ();
                        push (val2);
                        push (val1);
                        push (val3);
                        push (val2);
                        push (val1);
                    }
                    break;
                }
                case DUP_X1:
                {
                    Object val1 = pop ();
                    Object val2 = pop ();
                    push (val1);
                    push (val2);
                    push (val1);
                    break;
                }
                case DUP_X2:
                {
                    Object val1 = pop ();
                    Object val2 = pop ();
                    if (val2 instanceof long[] || val2 instanceof double[])
                    {
                        push (val1);
                        push (val2);
                        push (val1);
                    }
                    else
                    {
                        Object val3 = pop ();
                        push (val1);
                        push (val3);
                        push (val2);
                        push (val1);
                    }
                    break;
                }
                case DUP2_X2:
                {
                    Object val1 = pop ();
                    Object val2 = pop ();
                    Object val3 = pop ();
                    if (val1 instanceof long[] || val1 instanceof double[])
                    {
                        push (val1);
                        push (val3);
                        push (val2);
                        push (val1);
                    }
                    else
                    {
                        Object val4 = pop ();
                        push (val2);
                        push (val1);
                        push (val4);
                        push (val3);
                        push (val2);
                        push (val1);
                    }
                    break;
                }
                case SWAP:
                {
                    Object val1 = pop ();
                    Object val2 = pop ();
                    push (val1);
                    push (val2);
                    break;
                }
                case LDC:
                {
                    int iConst = readShort ();
                    push (vConst[iConst]);
                    break;
                }
                case ICONST:
                {
                    int val = readInt ();
                    pushInt (val);
                    break;
                }
                case ACONST_NULL:
                {
                    push (null);
                    break;
                }
                case POP:
                case POP2:
                {
                    pop ();
                    break;
                }
                case IAND:
                {
                    int val2 = popInt ();
                    int val1 = popInt ();
                    pushInt (val1 & val2);
                    break;
                }
                case IOR:
                {
                    int val2 = popInt ();
                    int val1 = popInt ();
                    pushInt (val1 | val2);
                    break;
                }
                case IXOR:
                {
                    int val2 = popInt ();
                    int val1 = popInt ();
                    pushInt (val1 ^ val2);
                    break;
                }
                case LAND:
                {
                    long val2 = ((long[])pop())[0];
                    long val1 = ((long[])pop())[0];
                    long[] val = {val1 & val2};
                    push (val);
                    break;
                }
                case LOR:
                {
                    long val2 = ((long[])pop())[0];
                    long val1 = ((long[])pop())[0];
                    long[] val = {val1 | val2};
                    push (val);
                    break;
                }
                case LXOR:
                {
                    long val2 = ((long[])pop())[0];
                    long val1 = ((long[])pop())[0];
                    long[] val = {val1 ^ val2};
                    push (val);
                    break;
                }
                case IEQ:
                {
                    int val2 = popInt ();
                    int val1 = popInt ();
                    pushInt ((val1==val2)?1:0);
                    break;
                }
                case LEQ:
                {
                    long val2 = ((long[])pop())[0];
                    long val1 = ((long[])pop())[0];
                    pushInt ((val1==val2)?1:0);
                    break;
                }
                case AEQ:
                {
                    pushInt ((pop()==pop())?1:0);
                    break;
                }
                case FEQ:
                {
                    float val2 = ((float[])pop())[0];
                    float val1 = ((float[])pop())[0];
                    pushInt ((val1==val2)?1:0);
                    break;
                }
                case DEQ:
                {
                    double val2 = ((double[])pop())[0];
                    double val1 = ((double[])pop())[0];
                    pushInt ((val1==val2)?1:0);
                    break;
                }
                case IGT:
                {
                    int val2 = popInt ();
                    int val1 = popInt ();
                    pushInt ((val1>val2)?1:0);
                    break;
                }
                case ILT:
                {
                    int val2 = popInt ();
                    int val1 = popInt ();
                    pushInt ((val1<val2)?1:0);
                    break;
                }
                case IGE:
                {
                    int val2 = popInt ();
                    int val1 = popInt ();
                    pushInt ((val1>=val2)?1:0);
                    break;
                }
                case ILE:
                {
                    int val2 = popInt ();
                    int val1 = popInt ();
                    pushInt ((val1<=val2)?1:0);
                    break;
                }
                case LGT:
                {
                    long val2 = ((long[])pop())[0];
                    long val1 = ((long[])pop())[0];
                    pushInt ((val1>val2)?1:0);
                    break;
                }
                case LLT:
                {
                    long val2 = ((long[])pop())[0];
                    long val1 = ((long[])pop())[0];
                    pushInt ((val1<val2)?1:0);
                    break;
                }
                case LGE:
                {
                    long val2 = ((long[])pop())[0];
                    long val1 = ((long[])pop())[0];
                    pushInt ((val1>=val2)?1:0);
                    break;
                }
                case LLE:
                {
                    long val2 = ((long[])pop())[0];
                    long val1 = ((long[])pop())[0];
                    pushInt ((val1<=val2)?1:0);
                    break;
                }
                case FGT:
                {
                    float val2 = ((float[])pop())[0];
                    float val1 = ((float[])pop())[0];
                    pushInt ((val1>val2)?1:0);
                    break;
                }
                case FLT:
                {
                    float val2 = ((float[])pop())[0];
                    float val1 = ((float[])pop())[0];
                    pushInt ((val1<val2)?1:0);
                    break;
                }
                case FGE:
                {
                    float val2 = ((float[])pop())[0];
                    float val1 = ((float[])pop())[0];
                    pushInt ((val1>=val2)?1:0);
                    break;
                }
                case FLE:
                {
                    float val2 = ((float[])pop())[0];
                    float val1 = ((float[])pop())[0];
                    pushInt ((val1<=val2)?1:0);
                    break;
                }
                case DGT:
                {
                    double val2 = ((double[])pop())[0];
                    double val1 = ((double[])pop())[0];
                    pushInt ((val1>val2)?1:0);
                    break;
                }
                case DLT:
                {
                    double val2 = ((double[])pop())[0];
                    double val1 = ((double[])pop())[0];
                    pushInt ((val1<val2)?1:0);
                    break;
                }
                case DGE:
                {
                    double val2 = ((double[])pop())[0];
                    double val1 = ((double[])pop())[0];
                    pushInt ((val1>=val2)?1:0);
                    break;
                }
                case DLE:
                {
                    double val2 = ((double[])pop())[0];
                    double val1 = ((double[])pop())[0];
                    pushInt ((val1<=val2)?1:0);
                    break;
                }
                case ISHL:
                {
                    int frac = popInt ();
                    int val = popInt ();
                    pushInt (val << frac);
                }
                case ISHR:
                {
                    int frac = popInt ();
                    int val = popInt ();
                    pushInt (val >> frac);
                }
                case IUSHR:
                {
                    int frac = popInt ();
                    int val = popInt ();
                    pushInt (val >>> frac);
                }
                case LSHL:
                {
                    int frac = popInt ();
                    long val = ((long[])pop())[0];
                    long[] newVal = {val << frac};
                    push (newVal);
                }
                case LSHR:
                {
                    int frac = popInt ();
                    long val = ((long[])pop())[0];
                    long[] newVal = {val >> frac};
                    push (newVal);
                }
                case LUSHR:
                {
                    int frac = popInt ();
                    long val = ((long[])pop())[0];
                    long[] newVal = {val >>> frac};
                    push (newVal);
                    break;
                }
                case IADD:
                {
                    int val2 = popInt ();
                    int val1 = popInt ();
                    pushInt (val1 + val2);
                    break;
                }
                case ISUB:
                {
                    int val2 = popInt ();
                    int val1 = popInt ();
                    pushInt (val1 - val2);
                    break;
                }
                case IMUL:
                {
                    int val2 = popInt ();
                    int val1 = popInt ();
                    pushInt (val1 * val2);
                    break;
                }
                case IDIV:
                {
                    int val2 = popInt ();
                    int val1 = popInt ();
                    pushInt (val1 / val2);
                    break;
                }
                case IREM:
                {
                    int val2 = popInt ();
                    int val1 = popInt ();
                    pushInt (val1 % val2);
                    break;
                }
                case LADD:
                {
                    long val2 = ((long[])pop())[0];
                    long val1 = ((long[])pop())[0];
                    long[] val = {val1 + val2};
                    push (val);
                    break;
                }
                case LSUB:
                {
                    long val2 = ((long[])pop())[0];
                    long val1 = ((long[])pop())[0];
                    long[] val = {val1 - val2};
                    push (val);
                    break;
                }
                case LMUL:
                {
                    long val2 = ((long[])pop())[0];
                    long val1 = ((long[])pop())[0];
                    long[] val = {val1 * val2};
                    push (val);
                    break;
                }
                case LDIV:
                {
                    long val2 = ((long[])pop())[0];
                    long val1 = ((long[])pop())[0];
                    long[] val = {val1 / val2};
                    push (val);
                    break;
                }
                case LREM:
                {
                    long val2 = ((long[])pop())[0];
                    long val1 = ((long[])pop())[0];
                    long[] val = {val1 % val2};
                    push (val);
                    break;
                }
                case FADD:
                {
                    float val2 = ((float[])pop())[0];
                    float val1 = ((float[])pop())[0];
                    float[] val = {val1 + val2};
                    push (val);
                    break;
                }
                case FSUB:
                {
                    float val2 = ((float[])pop())[0];
                    float val1 = ((float[])pop())[0];
                    float[] val = {val1 - val2};
                    push (val);
                    break;
                }
                case FMUL:
                {
                    float val2 = ((float[])pop())[0];
                    float val1 = ((float[])pop())[0];
                    float[] val = {val1 * val2};
                    push (val);
                    break;
                }
                case FDIV:
                {
                    float val2 = ((float[])pop())[0];
                    float val1 = ((float[])pop())[0];
                    float[] val = {val1 / val2};
                    push (val);
                    break;
                }
                case DADD:
                {
                    double val2 = ((double[])pop())[0];
                    double val1 = ((double[])pop())[0];
                    double[] val = {val1 + val2};
                    push (val);
                    break;
                }
                case DSUB:
                {
                    double val2 = ((double[])pop())[0];
                    double val1 = ((double[])pop())[0];
                    double[] val = {val1 - val2};
                    push (val);
                    break;
                }
                case DMUL:
                {
                    double val2 = ((double[])pop())[0];
                    double val1 = ((double[])pop())[0];
                    double[] val = {val1 * val2};
                    push (val);
                    break;
                }
                case DDIV:
                {
                    double val2 = ((double[])pop())[0];
                    double val1 = ((double[])pop())[0];
                    double[] val = {val1 / val2};
                    push (val);
                    break;
                }
                case I2B:
                {
                    pushInt((byte)popInt());
                    break;
                }
                case I2C:
                {
                    pushInt((char)popInt());
                    break;
                }
                case I2S:
                {
                    pushInt((short)popInt());
                    break;
                }
                case I2L:
                {
                    long[] val = {(long)popInt()};
                    push (val);
                    break;
                }
                case I2F:
                {
                    float[] val = {(float)popInt()};
                    push (val);
                    break;
                }
                case I2D:
                {
                    double[] val = {(double)popInt()};
                    push (val);
                    break;
                }
                case L2I:
                {
                    int[] val = {(int)(((long[])pop())[0])};
                    push (val);
                    break;
                }
                case L2F:
                {
                    float[] val = {(float)(((long[])pop())[0])};
                    push (val);
                    break;
                }
                case L2D:
                {
                    double[] val = {(double)(((long[])pop())[0])};
                    push (val);
                    break;
                }
                case F2D:
                {
                    double[] val = {(double)(((float[])pop())[0])};
                    push (val);
                    break;
                }
                case F2I:
                {
                    int[] val = {(int)(((float[])pop())[0])};
                    push (val);
                    break;
                }
                case F2L:
                {
                    long[] val = {(long)(((float[])pop())[0])};
                    push (val);
                    break;
                }
                case D2I:
                {
                    int[] val = {(int)(((double[])pop())[0])};
                    push (val);
                    break;
                }
                case D2F:
                {
                    float[] val = {(float)(((double[])pop())[0])};
                    push (val);
                    break;
                }
                case D2L:
                {
                    long[] val = {(long)(((double[])pop())[0])};
                    push (val);
                    break;
                }
                case ITOS:
                {
                    push (String.valueOf(popInt()));
                    break;
                }
                case LTOS:
                {
                    push (String.valueOf(((long[])pop())[0]));
                    break;
                }
                case FTOS:
                {
                    push (String.valueOf(((float[])pop())[0]));
                    break;
                }
                case DTOS:
                {
                    push (String.valueOf(((double[])pop())[0]));
                    break;
                }
                case CONCATSTR:
                {
                    String s2 = (String)pop();
                    String s1 = (String)pop();
                    push (s1 + s2);
                    break;
                }
                case INEG:
                {
                    pushInt (-popInt());
                    break;
                }
                case LNEG:
                {
                    long[] val = {-((long[])pop())[0]};
                    push (val);
                }
                case FNEG:
                {
                    float[] val = {-((float[])pop())[0]};
                    push (val);
                }
                case DNEG:
                {
                    double[] val = {-((double[])pop())[0]};
                    push (val);
                }
                case IINC:
                {
                    pushInt (popInt()+1);
                    break;
                }
                case LINC:
                {
                    long[] val = {((long[])pop())[0]+1};
                    push (val);
                    break;
                }
                case INSTANCEOF:
                {
                    int len = readShort ();
                    String sSpec = "";
                    for (int i = 0; i < len; i++)
                        sSpec += (char)readByte();
                    String sClass = pop().getClass().getName();
                    pushInt ((sSpec.equals(sClass))?1:0);
                    break;
                }
                case LIBRARY:
                {
                    int iLib = readByte ();
                    if (iLib == -1)
                        push (null);
                    else
                        push (vLibs[iLib]);
                    break;
                }
                case GOTO:
                {
                    int offset = readShort ();
                    IP += offset;
                    break;
                }
                case IFEQ:
                {
                    int offset = readShort ();
                    if (popInt() == 0)
                        IP += offset;
                    break;
                }
                case IFNE:
                {
                    int offset = readShort ();
                    if (popInt() != 0)
                        IP += offset;
                    break;
                }
                case IFGT:
                {
                    int offset = readShort ();
                    if (popInt() > 0)
                        IP += offset;
                    break;
                }
                case IFLT:
                {
                    int offset = readShort ();
                    if (popInt() < 0)
                        IP += offset;
                    break;
                }
                case IFGE:
                {
                    int offset = readShort ();
                    if (popInt() >= 0)
                        IP += offset;
                    break;
                }
                case IFLE:
                {
                    int offset = readShort ();
                    if (popInt() <= 0)
                        IP += offset;
                    break;
                }
                case SETGLOBAL:
                {
                    int iGlobal = readShort ();
                    vGlobals[iGlobal] = pop ();
                    pop ();
                    break;
                }
                case GETGLOBAL:
                {
                    int iGlobal = readShort ();
                    push (vGlobals[iGlobal]);
                    break;
                }
                case ABYTE_TO_STR:
                {
                    byte[] array = (byte[])(((Object[])pop())[0]);
                    push (new String (array));
                    break;
                }
                case STR_TO_ABYTE:
                {
                    String s = (String)pop();
                    Object[] array = {(Object)s.getBytes()};
                    push (array);
                    break;
                }
                case STR_SUBSTR1:
                {
                    int i1 = popInt ();
                    String s = (String)pop();
                    push (s.substring(i1));
                    break;
                }
                case STR_SUBSTR2:
                {
                    int i2 = popInt ();
                    int i1 = popInt ();
                    String s = (String)pop();
                    push (s.substring(i1, i2));
                    break;
                }
                case STR_CHARAT:
                {
                    int i1 = popInt ();
                    String s = (String)pop();
                    pushInt(s.charAt(i1));
                    break;
                }
                case STR_INDEXOF1:
                {
                    int c = popInt ();
                    String s = (String)pop();
                    pushInt (s.indexOf(c));
                    break;
                }
                case STR_INDEXOF2:
                {
                    int ind = popInt ();
                    int c = popInt ();
                    String s = (String)pop();
                    pushInt (s.indexOf(c, ind));
                    break;
                }
                case STR_INDEXOF3:
                {
                    String sInd = (String)pop();
                    String s = (String)pop();
                    pushInt (s.indexOf(sInd));
                    break;
                }
                case STR_INDEXOF4:
                {
                    int nInd = popInt ();
                    String sInd = (String)pop();
                    String s = (String)pop();
                    pushInt (s.indexOf(sInd, nInd));
                    break;
                }
                case STR_LASTINDEXOF1:
                {
                    int c = popInt ();
                    String s = (String)pop();
                    pushInt (s.lastIndexOf(c));
                    break;
                }
                case STR_LASTINDEXOF2:
                {
                    int ind = popInt ();
                    int c = popInt ();
                    String s = (String)pop();
                    pushInt (s.lastIndexOf(c, ind));
                    break;
                }
                case STR_LENGTH:
                {
                    String s = (String)pop();
                    pushInt (s.length());
                    break;
                }
                case STR_REPLACE:
                {
                    char c2 = (char)popInt();
                    char c1 = (char)popInt();
                    String s = (String)pop();
                    push (s.replace(c1, c2));
                    break;
                }
                case STR_STARTSWITH:
                {
                    String sFind = (String)pop();
                    String s = (String)pop();
                    pushInt((s.startsWith(sFind))?1:0);
                    break;
                }
                case STR_ENDSWITH:
                {
                    String sFind = (String)pop();
                    String s = (String)pop();
                    pushInt((s.endsWith(sFind))?1:0);
                    break;
                }
                case OBJ_EQUALS:
                {
                    Object obj1 = pop();
                    Object obj2 = pop();
                    pushInt((obj1.equals(obj2))?1:0);
                    break;
                }
                default:
                    throw new RuntimeException("opcode: " + opcode);
            }
        }
    }

    Object allocArray (int dims, int[] lens)
    {
        Object[] array = new Object[lens[lens.length - dims]];
        for (int i = 0; i < lens[lens.length - dims]; i++)
            array[i] = allocArray (dims-1, lens);
        return array;
    }
}
