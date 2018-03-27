package jcc2.lib.examplelib;

import java.util.Enumeration;
import java.util.Hashtable;
import jcc2.common.ClassContainer;
import jcc2.common.MethodContainer;
import jcc2.common.Type;
import jcc2.lib.Library;

/**
 *
 * @author note173@gmail.com
 */

public class library extends Library
{
    // стандартный код
    public static library singleton;
    Hashtable htClasses;
    Hashtable htMethods;

    // здесь можно размещать переменные, которые потребуются во время выполнения
    // инициализация этих переменных помещается в init в блок else

    // стандартный код
    public static library getSingleton ()
    {
        if (singleton == null)
            singleton = new library ();
        return singleton;
    }

    // стандартный код
    public library ()
    {
        init (false);
    }

    // инициализация
    // при команде use Имя ищется библиотека с этим именем
    // и выполняется метод init(true)
    // во время выполнения вызывается init(false)
    public void init(boolean compileTime)
    {
        if (compileTime)
        {
            // инициализация переменных
            htClasses = new Hashtable ();
            htMethods = new Hashtable ();

            ClassContainer classContainer;

            // удобно использовать одни переменные для всех методов
            Type[] args;
            MethodContainer method;
            int mid = 0;

            args = new Type[2];
            args[0] = Type.TYPE_INT;
            args[1] = Type.TYPE_INT;
            // параметры:
            // 1. имя метода
            // 2. тип возвращаемого значения
            // 3. список типов аргументов
            // 4. библиотека, которой принадлежит метод
            // 5. номер метода (см. rtInvoke)
            method = new MethodContainer ("add", Type.TYPE_INT, args, this, mid++);
            htMethods.put(method.spec, method);
        }
        else
        {
            singleton = this;
        }
    }

    // возвращает имя класса в формате jvm
    public String getDesc ()
    {
        return "jcc2/lib/examplelib/library";
    }

    // имя библиотеки
    public String getName ()
    {
        return "examplelib";
    }

    // стандартный код
    public ClassContainer ctGetClass(String name)
    {
        return (ClassContainer)htClasses.get(name);
    }

    // стандартный код
    public MethodContainer ctGetMethod(String desc)
    {
        return (MethodContainer)htMethods.get(desc);
    }

    // вызов метода во время выполнения в виртуальной машине
    // id - номер метода (см. init)
    // args - переданные аргументы
    // функция должна вернуть значение, или Type.TYPE_VOID
    public Object rtInvokeMethod(int id, Object[] args) throws Exception
    {
        switch (id)
        {
            case 0: //add(int,int)
            {
                // в таком виде хранятся переменные
                // как ни странно, дает прирост в скорости
                int a = ((int[])args[0])[0];
                int b = ((int[])args[1])[0];
                int[] sum = { add(a, b) };
                return sum;
            }
            default:
                throw new Exception ("method not found");
        }
    }

    // стандартный код
    public Enumeration ctGetAllClasses()
    {
        return htClasses.elements();
    }

    // стандартный код
    public Enumeration ctGetAllMethods()
    {
        return htMethods.elements();
    }

    int add (int a, int b)
    {
        return a+b;
    }
}