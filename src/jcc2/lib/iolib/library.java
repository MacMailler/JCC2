package jcc2.lib.iolib;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import jcc2.common.ClassContainer;
import jcc2.common.MethodContainer;
import jcc2.common.Type;
import jcc2.lib.Library;
import jcc2.io.fs.FileSystem;
import jcc2.io.fs.FilePtr;

/**
 *
 * @author note173@gmail.com
 */

public class library extends Library
{
    public static library singleton;

    Hashtable htClasses;
    Hashtable htMethods;

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

            ClassContainer classContainer;
            classContainer = File.getClassContainer();
            htClasses.put(classContainer.name, classContainer);
            classContainer = InputStream.getClassContainer();
            htClasses.put(classContainer.name, classContainer);
            classContainer = OutputStream.getClassContainer();
            htClasses.put(classContainer.name, classContainer);

            Type[] args;
            MethodContainer method;
            int mid = 0;

            args = new Type[2];
            args[0] = Type.TYPE_STRING;
            args[1] = Type.TYPE_INT;
            method = new MethodContainer ("fileOpen", new Type(File.getClassContainer(), 0), args, this, mid++);
            htMethods.put(method.spec, method);

            
        }
        else
        {
            singleton = this;
        }
    }

    public String getDesc ()
    {
        return "jcc2/lib/iolib/library";
    }

    public String getName ()
    {
        return "iolib";
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
            case 0: //fileOpen
            {
                return fileOpen ((String)args[0], ((int[])args[1])[0]);
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

    public File fileOpen (String path, int mode) throws IOException
    {
        File file = new File ();
        file.file = FileSystem.GetInstance().Open(path, mode);
        return file;
    }
}

