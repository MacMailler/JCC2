/**
 *
 * @author note173 mailto:note173@mail.ru
 */

package jcc2.io.fs;

import java.io.*;
import com.siemens.mp.io.File;

class FilePtrSiemens1 extends FilePtr
{
    File file;
    InputStream is;
    OutputStream os;
    int id;

    public FilePtrSiemens1 (FileSystem fsys)
    {
        super (fsys);
    }

    public void Write (byte[] data, int pos, int len) throws IOException
    {
        file.write(id, data, pos, len);
    }

    public int Read (byte[] data, int pos, int len) throws IOException
    {
        return file.read(id, data, pos, len);
    }

    public int Read (byte[] data) throws IOException
    {
        return file.read(id, data, 0, data.length);
    }

    public void Close () throws IOException
    {
        file.close(id);
    }

    public DataInputStream GetDataInputStream () throws IOException
    {
        return new DataInputStream (new SiemensDataInputStream(this));
    }

    public DataOutputStream GetDataOutputStream () throws IOException
    {
        return new DataOutputStream (new SiemensDataOutputStream(this));
    }
}

class SiemensDataInputStream extends InputStream
{
    FilePtrSiemens1 file;
    public SiemensDataInputStream (FilePtrSiemens1 file)
    {
        this.file = file;
    }

    public int read() throws IOException
    {
        byte[] data = new byte[1];
        file.Read(data);
        return data[0];
    }

    public int read (byte[] data) throws IOException
    {
        return file.Read(data);
    }

    public int read (byte[] data, int off, int len) throws IOException
    {
        return file.Read(data, off, len);
    }
}


class SiemensDataOutputStream extends OutputStream
{
    FilePtrSiemens1 file;
    public SiemensDataOutputStream (FilePtrSiemens1 file)
    {
        this.file = file;
    }

    public void write(int b) throws IOException
    {
        byte[] data = {(byte)b};
        file.Write(data, 0, 1);
    }

    public void write (byte[] data) throws IOException
    {
        file.Write(data, 0, data.length);
    }

    public void write (byte[] data, int off, int len) throws IOException
    {
        file.Write(data, off, len);
    }
}
public class FsSiemens1 extends FileSystem
{
    File file;

    FsSiemens1 ()
    {
        file = new File ();
    }

    public FilePtr Open (String path, int mode) throws IOException
    {
        FilePtrSiemens1 f = new FilePtrSiemens1 (this);
        f.mode = mode;
        if (path.charAt(0) == '/')
            f.id = file.open(replace(path.substring(1),'/',"\\"));
        else
            throw new IOException ("Open() : invalid path " + path);
        f.file = file;
        return f;
    }

    public int Size (String path) throws IOException
    {
        if (path.charAt(0) == '/')
        {
            int fid = file.open(replace(path.substring(1), '/', "\\"));
            int len = file.length(fid);
            file.close(fid);
            return len;
        }
        else
            throw new IOException ("Size() : invalid path " + path);
    }

    public String[] List (String path) throws IOException
    {
        String rPath = replace(path, '/', "\\");
        System.out.println ("List:" + rPath);
        if (rPath.equals("\\"))
        {
            String[] list = {"a:/"};
            return list;
        }
        else if (rPath.length() > 1)
        {
            String[] list;
            if (rPath.charAt(0) == '\\' && rPath.endsWith("\\"))
            {
                //System.out.println ("path=" + rPath);
                //System.out.println("real:" + replace(rPath.substring(1, rPath.length()),'/',"\\"));
                list = File.list(replace(path.substring(1, path.length()),'/',"\\"));
            }
            else
                throw new IOException ("List() : invalid path " + rPath);
            for (int i = 0; i < list.length; i++)
            {
                //System.out.println ("isdir="+rPath.substring(1)+list[i]);
                if (File.isDirectory(rPath.substring(1) + list[i]))
                    if (!((list[i]).endsWith("\\")) && !((list[i]).endsWith("/")))
                        list[i] += "\\";
                list[i] = replace(list[i], '\\', "/");
            }
            return list;
        }
        else
        {
            throw new IOException ("List() : invalid path");
        }
    }

    public static String replace (String s, char what, String to)
    {
        String str = "";
        for (int i = 0; i < s.length(); i++)
        {
            if (s.charAt(i) == what)
                str += to;
            else
                str += s.charAt(i);
        }
        return str;
    }
}
