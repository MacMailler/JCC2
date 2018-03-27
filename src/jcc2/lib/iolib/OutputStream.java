/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jcc2.lib.iolib;

import java.io.IOException;
import jcc2.common.ClassContainer;
import jcc2.common.MethodContainer;
import jcc2.common.Type;
import jcc2.lib.RTObject;

/**
 *
 * @author note173@gmail.com
 */

public class OutputStream extends RTObject
{
    static ClassContainer ctClass = null;
    java.io.DataOutputStream os;

    public OutputStream ()
    {
    }

    public static ClassContainer getClassContainer ()
    {
        if (ctClass != null)
            return ctClass;

        ClassContainer container = new ClassContainer ("OutputStream");
        container.className = "jcc2/lib/iolib/OutputStream";

        Type[] args;
        MethodContainer method;
        int mid = 0;

        args = new Type[0];
        method = new MethodContainer ("flush", Type.TYPE_VOID, args, null, mid++);
        container.addMethod(method);

        args = new Type[1];
        args[0] = Type.TYPE_INT;
        method = new MethodContainer ("write", Type.TYPE_VOID, args, null, mid++);
        container.addMethod(method);

        args = new Type[3];
        args[0] = Type.TYPE_ABYTE;
        args[1] = Type.TYPE_INT;
        args[2] = Type.TYPE_INT;
        method = new MethodContainer ("write", Type.TYPE_VOID, args, null, mid++);
        container.addMethod(method);

        args = new Type[1];
        args[0] = Type.TYPE_INT;
        method = new MethodContainer ("writeByte", Type.TYPE_VOID, args, null, mid++);
        container.addMethod(method);

        args = new Type[1];
        args[0] = Type.TYPE_INT;
        method = new MethodContainer ("writeChar", Type.TYPE_VOID, args, null, mid++);
        container.addMethod(method);

        args = new Type[1];
        args[0] = Type.TYPE_STRING;
        method = new MethodContainer ("writeChars", Type.TYPE_VOID, args, null, mid++);
        container.addMethod(method);

        args = new Type[1];
        args[0] = Type.TYPE_INT;
        method = new MethodContainer ("writeShort", Type.TYPE_VOID, args, null, mid++);
        container.addMethod(method);

        args = new Type[1];
        args[0] = Type.TYPE_INT;
        method = new MethodContainer ("writeInt", Type.TYPE_VOID, args, null, mid++);
        container.addMethod(method);

        args = new Type[1];
        args[0] = Type.TYPE_LONG;
        method = new MethodContainer ("writeLong", Type.TYPE_VOID, args, null, mid++);
        container.addMethod(method);

        args = new Type[1];
        args[0] = Type.TYPE_FLOAT;
        method = new MethodContainer ("writeFloat", Type.TYPE_VOID, args, null, mid++);
        container.addMethod(method);

        args = new Type[1];
        args[0] = Type.TYPE_DOUBLE;
        method = new MethodContainer ("writeDouble", Type.TYPE_VOID, args, null, mid++);
        container.addMethod(method);

        args = new Type[1];
        args[0] = Type.TYPE_STRING;
        method = new MethodContainer ("writeUTF", Type.TYPE_VOID, args, null, mid++);
        container.addMethod(method);

        ctClass = container;
        return ctClass;
    }

    public Object rtInvoke (int iMethod, Object[] args) throws Exception
    {
        switch (iMethod)
        {
            case 0: //flush
            {
                flush ();
                return Type.TYPE_VOID;
            }
            case 1: //write
            {
                write (((int[])(args[0]))[0]);
                return Type.TYPE_VOID;
            }
            case 2: //write
            {
                write ((byte[])(((Object[])(args[0]))[0]), ((int[])(args[1]))[0], ((int[])(args[2]))[0]);
                return Type.TYPE_VOID;
            }
            case 3: //writeByte
            {
                writeByte (((int[])(args[0]))[0]);
                return Type.TYPE_VOID;
            }
            case 4: //writeChar
            {
                writeChar (((int[])(args[0]))[0]);
                return Type.TYPE_VOID;
            }
            case 5: //writeShort
            {
                writeShort (((int[])(args[0]))[0]);
                return Type.TYPE_VOID;
            }
            case 6: //writeInt
            {
                writeInt (((int[])(args[0]))[0]);
                return Type.TYPE_VOID;
            }
            case 7: //writeLong
            {
                writeLong (((long[])(args[0]))[0]);
                return Type.TYPE_VOID;
            }
            case 8: //writeFloat
            {
                writeFloat (((float[])(args[0]))[0]);
                return Type.TYPE_VOID;
            }
            case 9: //writeDouble
            {
                writeDouble (((double[])(args[0]))[0]);
                return Type.TYPE_VOID;
            }
            case 10: //writeUTF
            {
                writeUTF (((String)(args[0])));
                return Type.TYPE_VOID;
            }
            default:
                throw new UnsupportedOperationException ("InputStream[" + iMethod + "]");
        }
    }

    void flush () throws IOException
    {
        os.flush ();
    }

    void write (int v) throws IOException
    {
        os.write (v);
    }

    void write (byte[] data, int offset, int length) throws IOException
    {
        os.write (data, offset, length);
    }

    void writeByte (int v) throws IOException
    {
        os.writeByte (v);
    }

    void writeChar (int v) throws IOException
    {
        os.writeChar (v);
    }

    void writeChars (String v) throws IOException
    {
        os.writeChars (v);
    }

    void writeShort (int v) throws IOException
    {
        os.writeShort (v);
    }

    void writeInt (int v) throws IOException
    {
        os.writeInt (v);
    }

    void writeLong (long v) throws IOException
    {
        os.writeLong (v);
    }

    void writeFloat (float v) throws IOException
    {
        os.writeFloat (v);
    }

    void writeDouble (double v) throws IOException
    {
        os.writeDouble (v);
    }

    void writeUTF (String v) throws IOException
    {
        os.writeUTF (v);
    }
}