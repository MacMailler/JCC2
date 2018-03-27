package jcc2.lib.stdlib;

import java.util.Enumeration;
import java.util.Hashtable;
import jcc2.common.ClassContainer;
import jcc2.common.MethodContainer;
import jcc2.common.Type;
import jcc2.compiler.JccException;
import jcc2.lib.Library;

/**
 *
 * @author note173@gmail.com
 */

public class library extends Library
{
    public static library singleton;

    Hashtable htClasses;
    Hashtable htMethods;

    private String lastError;

    public static library getSingleton ()
    {
        if (singleton == null)
            singleton = new library ();
        return singleton;
    }

    public library ()
    {
        init (false);
    }

    public void init(boolean compileTime)
    {
        if (compileTime)
        {
            htClasses = new Hashtable ();
            htMethods = new Hashtable ();

            Type[] args;
            MethodContainer method;
            int mid = 0;

            args = new Type[1];
            args[0] = Type.TYPE_STRING;
            method = new MethodContainer ("print", Type.TYPE_VOID, args, this, mid++);
            htMethods.put(method.spec, method);

            args = new Type[1];
            args[0] = Type.TYPE_STRING;
            method = new MethodContainer ("setLastError", Type.TYPE_VOID, args, this, mid++);
            htMethods.put(method.spec, method);

            args = new Type[0];
            method = new MethodContainer ("getLastError", Type.TYPE_STRING, args, this, mid++);
            htMethods.put(method.spec, method);
        }
        else
        {
            singleton = this;
        }
    }

    public String getDesc ()
    {
        return "jcc2/lib/stdlib/library";
    }

    public String getName ()
    {
        return "stdlib";
    }

    public ClassContainer ctGetClass(String name)
    {
        return (ClassContainer)htClasses.get(name);
    }

    public MethodContainer ctGetMethod(String desc)
    {
        return (MethodContainer)htMethods.get(desc);
    }

    public Object rtInvokeMethod(int id, Object[] args) throws Exception
    {
        switch (id)
        {
            case 0: //print
            {
                print ((String)args[0]);
                return Type.TYPE_VOID;
            }
            case 1: //setLastError
            {
                setLastError ((String)args[0]);
                return Type.TYPE_VOID;
            }
            case 2: //getLastError
            {
                return getLastError ();
            }
            default:
                throw new Exception ("method not found");
        }
    }

    public Enumeration ctGetAllClasses()
    {
        return htClasses.elements();
    }

    public Enumeration ctGetAllMethods()
    {
        return htMethods.elements();
    }

    public void print (String s)
    {
        System.out.print(s);
        try
        {
            jcc2.application.Main.singleton.f.append(s);
        }
        catch (Exception e)
        {
        }
    }

    public void setLastError(String message)
    {
        lastError = message;
    }

    public String getLastError()
    {
        return lastError;
    }
}
