/**
 *
 * @author note173 mailto:note173@mail.ru
 */


package jcc2.io.fs;

import java.io.*;

public class FileSystem
{
    public static final int READ = 1;
    public static final int WRITE = 2;
    
    static FileSystem fs = null;
    
    static String pack;
    
    FileSystem ()
    {
        pack = this.getClass().getName();
        pack = pack.substring(0, pack.lastIndexOf('.')+1);
    }
    
    public static FileSystem GetInstance () throws IOException
    {
        if (fs != null)
        {
            return fs;
        }
        
        boolean bSiemens = false;
        try
        {
            com.siemens.mp.io.File fltest;
            bSiemens = true;
        }
        catch (Exception ex)
        {
            bSiemens = false;
        }

        try
        {
            if (System.getProperty ("microedition.io.file.FileConnection.version") != null)
            {
                return new FsPdap();
            }
            else if (bSiemens)
            {
                Object file = Class.forName ("library.fs." + "FsSiemens1").newInstance ();
                fs = (FileSystem)file;
                return (FileSystem)file;
            }
            else
            {
                throw new IOException ("file system not supported");
            }
        }
        catch (Exception e)
        {
            throw new IOException (e.toString());
        }
    }
    
    public FilePtr Open (String path, int mode) throws IOException
    {
        throw new IOException ("using abstract class");
    }
    
    public int Size (String path) throws IOException
    {
        throw new IOException ("using abstract class");
    }
    
    public String[] List (String path) throws IOException
    {
        throw new IOException ("using abstract class");
    }
}
