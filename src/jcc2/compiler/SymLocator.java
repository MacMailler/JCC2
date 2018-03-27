
package jcc2.compiler;

/**
 *
 * @author note173@gmail.com
 */

import java.util.*;
import jcc2.common.*;
import jcc2.lib.Library;

class Namespace
{
    Hashtable htNames;

    public Namespace ()
    {
        htNames = new Hashtable ();
    }

    public FieldContainer getField (String name)
    {
        return (FieldContainer)htNames.get (name);
    }

    public boolean addField (FieldContainer field)
    {
        return htNames.put(field.name, field) == null;
    }
}

public class SymLocator
{
    Hashtable htClasses;
    Hashtable htMethods;
    ClassContainer curClass;
    Vector vNamespaces;
    Hashtable htLibraries;

    int nextLib;

    public SymLocator ()
    {
        htClasses = new Hashtable ();
        htMethods = new Hashtable ();
        htLibraries = new Hashtable ();
        vNamespaces = new Vector ();

        nextLib = 0;
    }

    public void setClass (ClassContainer curClass)
    {
        this.curClass = curClass;
    }

    public void enterBlock ()
    {
        vNamespaces.addElement(new Namespace ());
    }

    public void leaveBlock ()
    {
        vNamespaces.removeElementAt(vNamespaces.size()-1);
    }

    public boolean addField (FieldContainer field)
    {
        return ((Namespace)vNamespaces.elementAt(vNamespaces.size()-1)).addField(field);
    }

    public FieldContainer getField (String name)
    {
        for (int i = vNamespaces.size()-1; i >= 0; i--)
        {
            FieldContainer field = ((Namespace)vNamespaces.elementAt(i)).getField(name);
            if (field != null)
                return field;
        }
        return null;
    }

    public String importLibrary (String name)
    {
        if (htLibraries.get(name) != null)
            return null;

        Class classLibrary = null;
        Library library = null;
        try {
            classLibrary = Class.forName("jcc2.lib." + name + "." + "library");
        } catch (Exception e) {
            return "library '" + name + "' not found";
        }
        try {
            library = (Library)classLibrary.newInstance();
        } catch (Exception e) {
            return "library '" + name + "' initialization failed";
        }

        library.id = nextLib++;
        library.init (true);
        htLibraries.put(name, library);

        for (Enumeration en = library.ctGetAllClasses(); en.hasMoreElements(); )
        {
            ClassContainer libClass = (ClassContainer)en.nextElement();
            if (htClasses.put (libClass.name, libClass) != null)
            {
                //TODO: warning
            }
        }
        for (Enumeration en = library.ctGetAllMethods(); en.hasMoreElements(); )
        {
            MethodContainer libMethod = (MethodContainer)en.nextElement();
            if (htMethods.put (libMethod.spec, libMethod) != null)
            {
                //TODO: warning
            }
            if (curClass.getMethod(libMethod.spec) != null)
            {
                //TODO: warning
            }
        }

        return null;
    }

    public ClassContainer getClass (String name)
    {
        return (ClassContainer)htClasses.get(name);
    }

    MethodContainer getMethod(String sSpec)
    {
        MethodContainer method = curClass.getMethod(sSpec);
        if (method == null)
            method = (MethodContainer)htMethods.get(sSpec);
        return method;
    }

    boolean addMethod(MethodContainer method)
    {
        return curClass.addMethod(method);
    }
}
