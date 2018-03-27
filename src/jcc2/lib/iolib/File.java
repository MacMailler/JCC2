package jcc2.lib.iolib;

import java.io.IOException;
import jcc2.common.ClassContainer;

/**
 *
 * @author note173@gmail.com
 */

import jcc2.common.MethodContainer;
import jcc2.common.Type;
import jcc2.io.fs.FileSystem;
import jcc2.io.fs.FilePtr;
import jcc2.lib.RTObject;

public class File extends RTObject
{
    static ClassContainer ctClass = null;
    FilePtr file;

    public File ()
    {
    }

    public static ClassContainer getClassContainer ()
    {
        if (ctClass != null)
            return ctClass;

        ClassContainer container = new ClassContainer ("File");
        container.className = "jcc2/lib/iolib/File";

        container.addStatic("READ", new Integer(FileSystem.READ), Type.TYPE_INT);
        container.addStatic("WRITE", new Integer(FileSystem.WRITE), Type.TYPE_INT);

        Type[] args;
        MethodContainer method;
        int mid = 0;

        args = new Type[0];
        method = new MethodContainer ("openInputStream", new Type(InputStream.getClassContainer(), 0), args, null, mid++);
        container.addMethod(method);

        args = new Type[0];
        method = new MethodContainer ("openOutputStream", new Type(OutputStream.getClassContainer(), 0), args, null, mid++);
        container.addMethod(method);

        ctClass = container;
        return ctClass;
    }

    public Object rtInvoke(int iMethod, Object[] args) throws Exception
    {
        switch (iMethod)
        {
            case 0: //openInputStream
            {
                return openInputStream ();
            }
            case 1: //openOutputStream
            {
                return openOutputStream ();
            }
            default:
                throw new UnsupportedOperationException ("InputStream[" + iMethod + "]");
        }
    }

    public InputStream openInputStream () throws IOException
    {
        InputStream is = new InputStream ();
        is.is = file.GetDataInputStream();
        return is;
    }

    public OutputStream openOutputStream () throws IOException
    {
        OutputStream os = new OutputStream ();
        os.os = file.GetDataOutputStream();
        return os;
    }
}
