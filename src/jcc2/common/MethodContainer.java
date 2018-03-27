
package jcc2.common;

/**
 *
 * @author note173@gmail.com
 */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import jcc2.lib.Library;

public class MethodContainer
{
    String name;
    public Type[] args;
    public Type ret;
    public String spec, fullSpec;

    public Hashtable htLocals;

    public int nextId;
    ByteArrayOutputStream bos;
    public DataOutputStream os;
    public int pos;

    public Library libHost;
    public int id;
    public Hashtable htLabels;

    public MethodContainer (String name, Type ret, Type[] args)
    {
        this.name = name;
        this.args = args;
        this.ret = ret;
        htLocals = new Hashtable ();
        spec = MethodContainer.genSpec(this);
        fullSpec = spec + Type.genSpec(ret);
        nextId = 1;

        bos = new ByteArrayOutputStream ();
        os = new DataOutputStream (bos);
        pos = 0;
        htLabels = new Hashtable ();
    }

    public MethodContainer (String name, Type ret, Type[] args, Library libHost, int id)
    {
        this.name = name;
        this.args = args;
        this.ret = ret;
        htLocals = new Hashtable ();
        spec = MethodContainer.genSpec(this);
        fullSpec = spec + Type.genSpec(ret);
        nextId = 1;

        bos = new ByteArrayOutputStream ();
        os = new DataOutputStream (bos);

        this.libHost = libHost;
        this.id = id;
    }


    public byte[] getCode () throws IOException
    {
        bos.close();
        byte[] data = bos.toByteArray();
        for (Enumeration en = htLabels.elements(); en.hasMoreElements(); )
        {
            Object[] label = (Object[])en.nextElement();
            int target = ((Integer)label[1]).intValue();
            for (Enumeration en2 = ((Vector)label[2]).elements(); en2.hasMoreElements(); )
            {
                int link = ((Integer)en2.nextElement()).intValue();
                int offset = target - link - 2;
                data[link + 0] = (byte)((offset >> 8) & 0xff);
                data[link + 1] = (byte)((offset) & 0xff);
            }
        }
        return data;
    }

    public FieldContainer getLocal (int num)
    {
        return (FieldContainer)htLocals.get(new Integer(num));
    }

    public void addLocal (FieldContainer field)
    {
        field.num = nextId++;
        if (field.type.equals(Type.TYPE_DOUBLE) || field.type.equals(Type.TYPE_LONG))
            nextId++;
        htLocals.put(field.name, new Integer(field.num));
    }

    public static String genSpec (MethodContainer method)
    {
        String sSpec = "";
        sSpec += method.name;
        sSpec += "(";
        for (int i = 0; i < method.args.length; i++)
        {
            sSpec += Type.genSpec(method.args[i]);
        }
        sSpec += ")";
        return sSpec;
    }

    public static String genSpec (String mName, Type[] mArgs)
    {
        String sSpec = "";
        sSpec += mName;
        sSpec += "(";
        for (int i = 0; i < mArgs.length; i++)
        {
            sSpec += Type.genSpec(mArgs[i]);
        }
        sSpec += ")";
        return sSpec;
    }
}
