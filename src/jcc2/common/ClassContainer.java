
package jcc2.common;

/**
 *
 * @author note173@gmail.com
 */

import java.util.*;

public class ClassContainer
{
    public String name, className;
    public Hashtable htFields;
    public Hashtable htMethods;
    public Hashtable htStatics;
    public int nextGlobal;
    public int nextMethod;

    public ClassContainer(String name)
    {
        this.name = name;
        htFields = new Hashtable ();
        htMethods = new Hashtable ();
        htStatics = new Hashtable ();
        nextGlobal = 0;
        nextMethod = 0;
    }

    public MethodContainer getMethod (String spec)
    {
        return (MethodContainer)htMethods.get(spec);
    }

    public FieldContainer getField (String name)
    {
        return (FieldContainer)htFields.get(name);
    }

    public boolean addMethod (MethodContainer method)
    {
        boolean exists =  htMethods.put(method.spec, method) == null;
        method.id = nextMethod++;
        return exists;
    }

    public boolean addField (FieldContainer field)
    {
        return htFields.put(field.name, field) == null;
    }

    public Enumeration getFields ()
    {
        return htFields.elements();
    }

    public Enumeration getMethods ()
    {
        return htMethods.elements();
    }

    public void addStatic (String name, Object val, Type type)
    {
        Object[] obj = {val, type};
        htStatics.put(name, obj);
    }

    public Object[] getStatic (String sName)
    {
        return (Object[])htStatics.get(sName);
    }
}
