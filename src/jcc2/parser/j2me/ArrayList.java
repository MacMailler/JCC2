package jcc2.parser.j2me;

/**
 *
 * @author note173
 */

import java.util.Vector;

public class ArrayList
{
    Vector v;

    public ArrayList ()
    {
        v = new Vector ();
    }

    public void clear ()
    {
        v.removeAllElements();
    }

    public void add (Object value)
    {
        v.addElement(value);
    }

    public Object remove (int position)
    {
        Object value = v.elementAt(position);
        v.removeElementAt(position);
        return value;
    }

    public Object get (int position)
    {
        return v.elementAt(position);
    }

    public int size ()
    {
        return v.size();
    }

    public Iterator iterator ()
    {
        return new Iterator (v);
    }
}
