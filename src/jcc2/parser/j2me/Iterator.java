/**
 *
 * @author note173 mailto:note173@mail.ru
 */

package jcc2.parser.j2me;

import java.util.*;

public class Iterator
{
    Enumeration en;

    public Iterator (Vector v)
    {
        en = v.elements();
    }

    public boolean hasNext ()
    {
        return en.hasMoreElements();
    }

    public Object next ()
    {
        return en.nextElement();
    }
}
