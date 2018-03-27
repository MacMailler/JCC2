
package org.pabloid.util;

/**
 *
 * @author P@bloid
 */

public class Preprocessor
{
    static boolean isName(char c)
    {
        return (c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='_';
    }
 }
