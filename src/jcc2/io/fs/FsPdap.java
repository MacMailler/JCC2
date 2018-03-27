/**
 *
 * @author note173 mailto:note173@mail.ru
 */


package jcc2.io.fs;

import java.io.*;
import java.util.*;
import javax.microedition.io.file.*;
import javax.microedition.io.*;

class FilePtrPdap extends FilePtr
{
    FileConnection fconn;
    InputStream is;
    OutputStream os;
    
    public FilePtrPdap (FileSystem fsys)
    {
        super (fsys);
    }
    
    public void Write (byte[] data, int pos, int len) throws IOException
    {
        if (os == null)
            throw new IOException ("Write() : invalid file mode");
        os.write(data, pos, len);
    }
    
    public int Read (byte[] data, int pos, int len) throws IOException
    {
        if (is == null)
            throw new IOException ("Read() : invalid file mode");
        return is.read(data, pos, len);
    }
    
    public int Read (byte[] data) throws IOException
    {
        if (is == null)
            throw new IOException ("Read() : invalid file mode");
        return is.read(data);
    }
    
    public void Close () throws IOException
    {
        if (is != null)
            is.close ();
        else if (os != null)
            os.close();
        fconn.close();
    }
    
    public DataInputStream GetDataInputStream () throws IOException
    {
        if (is == null)
            throw new IOException ("GetDataInputStream(): can't read");
        return new DataInputStream (is);
    }
    
    public DataOutputStream GetDataOutputStream () throws IOException
    {
        if (os == null)
            throw new IOException ("GetDataOutputStream(): can't write");
        return new DataOutputStream (os);
    }
}

public class FsPdap extends FileSystem
{
    FsPdap ()
    {
    }
    
    public FilePtr Open (String path, int mode) throws IOException
    {
        FileConnection fconn = (FileConnection)Connector.open("file://" + path);
        FilePtrPdap file = new FilePtrPdap (this);
        file.mode = mode;
        file.fconn = fconn;
        if (mode == READ)
        {
            if (!fconn.exists ())
            {
                throw new IOException ("file not found");
            }

            file.is = fconn.openInputStream ();
            file.os = null;
        }
        else if (mode == WRITE)
        {
            if (fconn.exists ())
            {
                fconn.delete ();
            }
            fconn.create ();

            file.os = fconn.openOutputStream ();
            file.is = null;
        }
        else
        {
            throw new IOException ("unknown file mode");
        }
        return file;
    }
    
    public int Size (String path) throws IOException
    {
        FileConnection fconn = (FileConnection)Connector.open("file://" + path);
        int size = (int)fconn.fileSize ();
        fconn.close();
        return size;
    }
    
    public String[] List (String path) throws IOException
    {
        Vector vFiles;
        if (path.equals("/"))
        {
            vFiles = new Vector ();
            Enumeration rootEnum = FileSystemRegistry.listRoots();
            while (rootEnum.hasMoreElements())
            {
                String root = (String)rootEnum.nextElement();
                vFiles.addElement (root);
            }
            String[] list = new String[vFiles.size()];
            vFiles.copyInto(list);
            vFiles.removeAllElements();
            vFiles = null;
            return list;
        }
        else if (path.length() > 1)
        {
            vFiles = new Vector ();
            FileConnection fconn = (FileConnection)Connector.open("file://" + path);
            for (Enumeration e = fconn.list("*", false) ; e.hasMoreElements() ;)
            {
                    vFiles.addElement (e.nextElement());
            }
            fconn.close();
            String[] list = new String[vFiles.size()];
            vFiles.copyInto(list);
            vFiles.removeAllElements();
            vFiles = null;
            return list;
        }
        else
        {
            throw new IOException ("List() : invalid path"); 
        }
    }
}
