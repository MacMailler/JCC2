package jcc2.lib;

import jcc2.common.ClassContainer;

/**
 *
 * @author note173@gmail.com
 */

public abstract class RTObject
{
    abstract public Object rtInvoke (int iMethod, Object[] args) throws Exception;
}
