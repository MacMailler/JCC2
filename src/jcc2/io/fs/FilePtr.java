/**
 *
 * @author note173 mailto:note173@mail.ru
 */

package jcc2.io.fs;

import java.io.*;

public class FilePtr
{
    FileSystem fs;
    int mode;
    
    FilePtr (FileSystem fsys)
    {
        fs = fsys;
    }
    
    public void Write (byte[] data, int pos, int len) throws IOException
    {
        throw new IOException ("using abstract class");
    }
    
    public int Read (byte[] data, int pos, int len) throws IOException
    {
        throw new IOException ("using abstract class");
    }
    
    public int Read (byte[] data) throws IOException
    {
        throw new IOException ("using abstract class");
    }
    
    public void Close () throws IOException
    {
        throw new IOException ("using abstract class");
    }
    
    public DataInputStream GetDataInputStream () throws IOException
    {
        throw new IOException ("using abstract class");
    }
    
    public DataOutputStream GetDataOutputStream () throws IOException
    {
        throw new IOException ("using abstract class");
    }
    
    public void Print (Object o) throws IOException
    {
        byte[] data = o.toString().getBytes();
        Write (data, 0, data.length);
    }
}
