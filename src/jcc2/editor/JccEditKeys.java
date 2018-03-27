/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jcc2.editor;

/**
 *
 * @author root
 */
public class JccEditKeys
{
    public static final int QKEY_BACKSPACE = 8;
    public static final int QKEY_DELETE = 127;
    public static final int QKEY_ENTER = -5;
    public static final int QKEY_TAB = 9;

    public static char getQwertyChar (int key)
    {
        if (key >= 97 && key <= 122)
            return (char)('a'+(char)(key-97));
        else if (key >= 65 && key <= 90)
            return (char)('A'+(char)(key-65));
        else if (key >= 48 && key <= 57)
            return (char)('0'+(char)(key-48));
        switch (key)
        {
            case 33:
                return '!';
            case 64:
                return '@';
            case 35:
                return '#';
            case 36:
                return '$';
            case 37:
                return '%';
            case 94:
                return '^';
            case 38:
                return '&';
            case 42:
                return '*';
            case 40:
                return '(';
            case 41:
                return ')';
            case 95:
                return '_';
            case 43:
                return '+';
            case 45:
                return '-';
            case 61:
                return '=';
            case 91:
                return '[';
            case 93:
                return ']';
            case 123:
                return '{';
            case 125:
                return '}';
            case 59:
                return ';';
            case 58:
                return ':';
            case 39:
                return '\'';
            case 34:
                return '\"';
            case 44:
                return ',';
            case 60:
                return '<';
            case 46:
                return '.';
            case 62:
                return '>';
            case 47:
                return '/';
            case 63:
                return '?';
            case 96:
                return '`';
            case 126:
                return '~';
            case 92:
                return '\\';
            case 124:
                return '|';
            case 32:
                return ' ';
            default:
                return (char)-1;
        }
    }

    public static int getQwertyControlChar (int key)
    {
        switch (key)
        {
            case 8:
                return QKEY_BACKSPACE;
            case 127:
                return QKEY_DELETE;
            case -5:
            case 10:
                return QKEY_ENTER;
            case 9:
                return QKEY_TAB;
            default:
                return -1;
        }
    }
}
