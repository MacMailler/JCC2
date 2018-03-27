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

public class InputStream extends RTObject
{
    static ClassContainer ctClass = null;
    java.io.DataInputStream is;

    public InputStream ()
    {
    }

    public static ClassContainer getClassContainer ()
    {
        if (ctClass != null)
            return ctClass;

        ClassContainer container = new ClassContainer ("InputStream");
        container.className = "jcc2/lib/iolib/InputStream";

        Type[] args;
        MethodContainer method;
        int mid = 0;

        args = new Type[0];
        method = new MethodContainer ("read", Type.TYPE_INT, args, null, mid++);
        container.addMethod(method);

        args = new Type[0];
        method = new MethodContainer ("readChar", Type.TYPE_CHAR, args, null, mid++);
        container.addMethod(method);

        args = new Type[0];
        method = new MethodContainer ("readShort", Type.TYPE_SHORT, args, null, mid++);
        container.addMethod(method);
        
        args = new Type[0];
        method = new MethodContainer ("readInt", Type.TYPE_INT, args, null, mid++);
        container.addMethod(method);

        args = new Type[0];
        method = new MethodContainer ("readLong", Type.TYPE_LONG, args, null, mid++);
        container.addMethod(method);
        
        args = new Type[0];
        method = new MethodContainer ("readFloat", Type.TYPE_FLOAT, args, null, mid++);
        container.addMethod(method);
        
        args = new Type[0];
        method = new MethodContainer ("readDouble", Type.TYPE_DOUBLE, args, null, mid++);
        container.addMethod(method);
        
        args = new Type[0];
        method = new MethodContainer ("readUTF", Type.TYPE_STRING, args, null, mid++);
        container.addMethod(method);

        ctClass = container;
        return ctClass;
    }

    public Object rtInvoke (int iMethod, Object[] args) throws Exception
    {
        switch (iMethod)
        {
            case 0: //read
            {
                int[] ret = {read()};
                return ret;
            }
            case 1: //readChar
            {
                char[] ret = {readChar()};
                return ret;
            }
            case 2: //readShort
            {
                short[] ret = {readShort()};
                return ret;
            }
            case 3: //readInt
            {
                int[] ret = {readInt()};
                return ret;
            }
            case 4: //readLong
            {
                long[] ret = {readLong()};
                return ret;
            }
            case 5: //readFloat
            {
                float[] ret = {readFloat()};
                return ret;
            }
            case 6: //readDouble
            {
                double[] ret = {readDouble()};
                return ret;
            }
            case 7: //readUTF
            {
                String ret = readUTF();
                return ret;
            }
            default:
                throw new UnsupportedOperationException ("InputStream[" + iMethod + "]");
        }
    }

    int read () throws IOException
    {
        return is.read();
    }

    char readChar () throws IOException
    {
        return is.readChar();
    }

    short readShort () throws IOException
    {
        return is.readShort();
    }

    int readInt () throws IOException
    {
        return is.readInt();
    }

    long readLong () throws IOException
    {
        return is.readLong();
    }

    float readFloat () throws IOException
    {
        return is.readFloat();
    }

    double readDouble () throws IOException
    {
        return is.readDouble();
    }

    String readUTF () throws IOException
    {
        return is.readUTF();
    }
}