
package jcc2.common;

import java.io.IOException;
import java.io.OutputStream;
import jcc2.runtime.RuntimeConst;

/**
 *
 * @author note173@gmail.com
 */

public class Type
{
    public static final int K_PRIMITIVE = 1;
    public static final int K_OBJECT = 2;

    public static final int T_INT = 3;
    public static final int T_LONG = 4;
    public static final int T_SHORT = 5;
    public static final int T_BYTE = 6;
    public static final int T_CHAR = 7;
    public static final int T_FLOAT = 8;
    public static final int T_DOUBLE = 9;
    public static final int T_BOOL = 10;
    public static final int T_STRING = 11;
    public static final int T_VOID = 12;

    public static final int T_NULL = 20;

    public static Type TYPE_INT = new Type (T_INT, 0);
    public static Type TYPE_LONG = new Type (T_LONG, 0);
    public static Type TYPE_SHORT = new Type (T_SHORT, 0);
    public static Type TYPE_BYTE = new Type (T_BYTE, 0);
    public static Type TYPE_CHAR = new Type (T_CHAR, 0);
    public static Type TYPE_FLOAT = new Type (T_FLOAT, 0);
    public static Type TYPE_DOUBLE = new Type (T_DOUBLE, 0);
    public static Type TYPE_BOOL = new Type (T_BOOL, 0);
    public static Type TYPE_STRING = new Type (T_STRING, 0);
    public static Type TYPE_VOID = new Type (T_VOID, 0);

    public static Type TYPE_AINT = new Type (T_INT, 1);
    public static Type TYPE_ALONG = new Type (T_LONG, 1);
    public static Type TYPE_ASHORT = new Type (T_SHORT, 1);
    public static Type TYPE_ABYTE = new Type (T_BYTE, 1);
    public static Type TYPE_ACHAR = new Type (T_CHAR, 1);
    public static Type TYPE_AFLOAT = new Type (T_FLOAT, 1);
    public static Type TYPE_ADOUBLE = new Type (T_DOUBLE, 1);
    public static Type TYPE_ABOOL = new Type (T_BOOL, 1);
    public static Type TYPE_ASTRING = new Type (T_STRING, 1);

    public static Type TYPE_NULL = new Type (null, 0);

    public int kind;
    public int primitive;
    public int arrayDepth;
    public ClassContainer object;

    public Type (int primitive, int arrayDepth)
    {
        if (arrayDepth == 0)
        {
            if (primitive == T_STRING)
            {
                this.primitive = primitive;
                kind = K_OBJECT;
                this.arrayDepth = 0;
                object = null;
            }
            else
            {
                this.primitive = primitive;
                kind = K_PRIMITIVE;
                this.arrayDepth = 0;
                object = null;
            }
        }
        else
        {
            kind = K_OBJECT;
            this.arrayDepth = arrayDepth;
            object = null;
            this.primitive = primitive;
        }
    }

    public Type (ClassContainer object, int arrayDepth)
    {
        if (arrayDepth == 0)
        {
            this.object = object;
            kind = K_OBJECT;
            this.arrayDepth = 0;
            this.primitive = -1;
        }
        else
        {
            this.object = object;
            kind = K_OBJECT;
            this.arrayDepth = arrayDepth;
            primitive = -1;
        }
    }

    public Type arrayType ()
    {
        if (arrayDepth == 0)
            return null;
        if (primitive != -1 && object == null)
        {
            return new Type (primitive, arrayDepth-1);
        }
        else
        {
            return new Type (object, arrayDepth-1);
        }
    }

    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + this.kind;
        hash = 67 * hash + this.primitive;
        hash = 67 * hash + this.arrayDepth;
        hash = 67 * hash + (this.object != null ? this.object.hashCode() : 0);
        return hash;
    }

    public boolean equals (Object obj)
    {
        if (obj == null || !(obj instanceof Type))
            return false;
        Type cmp = (Type)obj;
        if (cmp.kind == this.kind && cmp.primitive == this.primitive &&
                (this.arrayDepth == cmp.arrayDepth) &&
                ((this.object != null && cmp.object != null && cmp.object.equals(this.object)) ||
                 (this.object == null && cmp.object == null))
                )
        {
            return true;
        }
        return false;
    }

    public static String genSpec (Type type)
    {
        String sSpec = "";
        Type t = type;
        if (t.arrayDepth > 0)
        {
            for (int i = 0; i < t.arrayDepth; i++)
                sSpec += "[";
        }
        
        if (t.primitive != -1)
        {
            if (t.primitive == T_INT)
                sSpec += "I";
            else if (t.primitive == T_SHORT)
                sSpec += "S";
            else if (t.primitive == T_LONG)
                sSpec += "W";
            else if (t.primitive == T_CHAR)
                sSpec += "C";
            else if (t.primitive == T_BYTE)
                sSpec += "B";
            else if (t.primitive == T_BOOL)
                sSpec += "Z";
            else if (t.primitive == T_FLOAT)
                sSpec += "F";
            else if (t.primitive == T_DOUBLE)
                sSpec += "D";
            else if (t.primitive == T_STRING)
                sSpec += "Ljava/lang/String;";
            else if (t.primitive == T_VOID)
                sSpec += "V";
            else
                sSpec += "<Unknown>";
        }
        else
        {
            sSpec += "L";
            sSpec += t.object.className;
            sSpec += ";";
        }
        return sSpec;
    }

    public static void genBinDesc (Type type, OutputStream os) throws IOException
    {
        Type t = type;
        os.write (t.arrayDepth);

        if (t.primitive != -1)
        {
            os.write (t.primitive);
        }
        else
        {
            os.write (-1);
            os.write (t.object.className.length());
            for (int i = 0; i < t.object.className.length(); i++)
            {
                os.write (t.object.className.charAt(i));
            }
        }
    }
}
