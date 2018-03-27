package jcc2.application;

/**
 *
 * @author note173@gmail.com
 */

import javax.microedition.midlet.*;

import jcc2.io.fs.*;
import jcc2.parser.*;
import jcc2.compiler.*;
import jcc2.runtime.*;

import javax.microedition.lcdui.*;
import org.pabloid.util.*;
import java.io.*;

//import java.util.Vector;
import jcc2.io.fs.*;

import jcc2.editor.*;

/*public class Main extends MIDlet
{
    public Main ()
    {
        try
        {
            FileSystem fs = FileSystem.GetInstance();
            int size = fs.Size("/" + fs.List("/")[0] + "test.jc2");
            FilePtr f = fs.Open("/" + fs.List("/")[0] + "test.jc2", FileSystem.READ);
            Jcc2Parser parser = new Jcc2Parser (f.GetDataInputStream());
            ASTCompilationUnit unit = parser.CompilationUnit();
            //unit.dump("");
            Compiler compiler = new Compiler (unit);
            JccRuntime runtime = new JccRuntime (compiler.code);
            runtime.start("main()V", new Object[0]);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void startApp ()
    {
    }

    public void pauseApp ()
    {
    }

    public void destroyApp (boolean unconditional)
    {
        notifyDestroyed ();
    }
}*/

public class Main extends MIDlet implements CommandListener
{
    public static Main singleton;

    volatile public Display display;
    public Form f = new Form ("out");
    Command CMD_RETURN = new Command ("Return", Command.SCREEN, 1);

    public JccEdit tbSource;
    public static final int MAX_SOURCE_CODE = 1024*10;
    public TextBox tbText = new TextBox  ("text", "", 4096, TextField.ANY);
    Command CMD_OK = new Command ("OK", Command.OK, 1);
    Command CMD_COMPILE = new Command ("Compile", Command.SCREEN, 1);
    Command CMD_COMPILE_JASMIN = new Command ("Compile to Jasmin", Command.SCREEN, 1);
    Command CMD_RUN = new Command ("Run", Command.SCREEN, 2);
    Command CMD_EDIT = new Command ("Edit selection", Command.SCREEN, 2);
    Command CMD_LOAD_TEST = new Command ("Load test", Command.SCREEN, 2);
    Command CMD_EXIT = new Command ("Exit", Command.EXIT, 1);
    Command CMD_SAVE_SRC = new Command ("Save", Command.SCREEN, 3);
    Command CMD_LOAD_SRC = new Command ("Load", Command.SCREEN, 4);
    Command CMD_SAVE_BYTECODE = new Command ("Save bytecode", Command.SCREEN, 5);
    Command CMD_EDITOR_SETTINGS = new Command ("Editor settings", Command.SCREEN, 6);

    List lSamples = new List ("Samples", List.IMPLICIT);

    List lsFs;
    Command CMD_SAVE = new Command ("Save", Command.SCREEN, 0);
    Command CMD_LOAD = new Command ("Load", Command.SCREEN, 0);
    Command CMD_CANCEL = new Command ("Cancel", Command.CANCEL, 0);
    TextBox tbFileName;
    String curPath;
    int mode;

    FileSystem fs;

    String[][] vSamples = {{"New project","test.jc2"}};


    public JccRuntime vm;
    public byte[] bytecode;
    boolean bCanRun;
    int smode;

    Form waitForm = new Form ("Wait...");
    public String sEntryPoint = "main";

    public Main ()
    {
        singleton = this;
        display = Display.getDisplay(this);
        f = new Form ("out");
        display.setCurrent(f);

        if (false)
        {
        try
        {
            InputStream is = new Object().getClass().getResourceAsStream("/test.jc2");
            Jcc2Parser parser = new Jcc2Parser (is);
            parser.enable_tracing();
            ASTCompilationUnit unit = parser.CompilationUnit();
            unit.dump("");
            Compiler compiler = new Compiler(unit, false);
            bytecode = compiler.code;
            is.close ();
            new Thread () {
                public void run ()
                {
                    try
                    {
                        vm = new JccRuntime(bytecode);
                        vm.start("main()V", new Object[0]);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        f.append(e.toString());
                        display.setCurrent(f);
                    }
                }
            }.start ();


            if (true)
                return;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return;
        }
        }

        tbText.addCommand (CMD_OK);
        tbText.addCommand (CMD_CANCEL);
        tbText.setCommandListener(this);

        tbSource = new JccEdit (display,"Jcc2 v"+getAppProperty("MIDlet-Version"));
        tbSource.addCommand(CMD_COMPILE);
        tbSource.addCommand(CMD_RUN);
        tbSource.addCommand(CMD_EDIT);
        tbSource.addCommand(CMD_LOAD_TEST);
        tbSource.addCommand(CMD_SAVE_SRC);
        tbSource.addCommand(CMD_LOAD_SRC);
        tbSource.addCommand(CMD_COMPILE_JASMIN);
        tbSource.addCommand(CMD_SAVE_BYTECODE);
        tbSource.addCommand(CMD_EDITOR_SETTINGS);
        tbSource.addCommand(CMD_EXIT);
        tbSource.setCommandListener(this);

        lSamples.addCommand(CMD_RETURN);
        lSamples.setCommandListener(this);
        for (int i = 0; i < vSamples.length; i++)
        {
            lSamples.append(vSamples[i][0], null);
        }

        try{
        tbSource.setString(loadTxt("/test.jc2"));
        } catch (Exception e) {}

        f.addCommand(CMD_RETURN);
        f.setCommandListener(this);

        lsFs = new List ("list", List.IMPLICIT);
        lsFs.setCommandListener(this);

        tbFileName = new TextBox ("Filename", "", 256, TextField.ANY);
        tbFileName.addCommand(CMD_SAVE);
        tbFileName.setCommandListener(this);

        try{
        fs = FileSystem.GetInstance();
        } catch (Exception e)
        {
            e.printStackTrace();
            display.setCurrent(new Alert("Error", "FS error: " + e.toString(), null, null), tbSource);
        }

        bytecode = null;
        tbSource.currentMode = JccEdit.STATE_NORMAL;
    }

    public void commandAction (Command c, Displayable d)
    {
        if (d == lsFs)
        {
            if (c == List.SELECT_COMMAND)
            {
                boolean bRefresh = true;
                int idx = lsFs.getSelectedIndex();
                if (idx >= 0)
                {
                    String sf = lsFs.getString(idx);
                    if (sf.equals("../"))
                    {
                        curPath = curPath.substring(0, curPath.lastIndexOf('/'));
                        if (curPath.length() == 0)
                            curPath = "/";
                        else
                            curPath = curPath.substring(0, curPath.lastIndexOf('/')) + "/";
                    }
                    else if (sf.endsWith("/"))
                    {
                        curPath += lsFs.getString(idx);
                    }
                    else
                    {
                        if (mode == 1) //load
                        {
                            curPath += lsFs.getString(idx);
                            try
                            {
                                new Thread ()
                                {
                                    public void run()
                                    {
                                        String trace = "tr5_";
                                        try
                                        {
                                            trace += "2";
                                            int size = fs.Size(curPath);
                                            trace += "3";
                                            FilePtr f = fs.Open(curPath,
                                                    FileSystem.READ);
                                            trace += "4";
                                            byte[] curCode = new byte[size];
                                            trace += "5";
                                            f.Read(curCode, 0, curCode.length);
                                            trace += "6";
                                            f.Close();
                                            trace += "7";
                                            if (curCode.length >= MAX_SOURCE_CODE)
                                            {
                                                throw new Exception ("file lenght >= " + MAX_SOURCE_CODE);
                                            }
                                            tbSource.setString (new String(curCode));
                                            trace += "8";
                                            display.setCurrent(tbSource);
                                        }
                                        catch (Exception e)
                                        {
                                            System.out.println ("load:"+curPath);
                                            e.printStackTrace();
                                            display.setCurrent(new Alert ("error", "can't open file [5-1]:" + e.toString(), null, null), tbSource);
                                        }
                                    }
                                }.start();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                                display.setCurrent(new Alert ("error", "can't open file[8]:" + e.toString(), null, null), tbSource);
                            }
                            bRefresh = false;
                        }
                        else
                        {
                            display.setCurrent(tbFileName);
                            bRefresh = false;
                        }
                    }

                    if (bRefresh)
                    {
                        new Thread()
                        {
                            public void run()
                            {
                                String trace = "tr2_";
                                try
                                {
                                    lsFs.deleteAll();
                                    lsFs.setTitle("Wait...");
                                    trace += "1";
                                    String[] vs = fs.List(curPath);
                                    trace += "2";
                                    lsFs.setTitle("Files");
                                    lsFs.append("../", null);
                                    for (int i = 0; i < vs.length; i++)
                                    {
                                        lsFs.append(vs[i], null);
                                    }
                                    trace += "3";
                                }
                                catch (Exception e)
                                {
                                    System.out.println ("1: " + e.toString());
                                    e.printStackTrace();
                                    display.setCurrent(new Alert ("error", "can't open file [4] "+ trace + "::" + e.toString(), null, null), tbSource);
                                }
                            }
                        }.start ();
                    }
                }
            }
            else if (c == CMD_SAVE)
            {
                tbFileName.setString(".jcc");
                display.setCurrent(tbFileName);
            }
            else if (c == CMD_SAVE_BYTECODE)
            {
                tbFileName.setString(".jcb");
                display.setCurrent(tbFileName);
            }
            else if (c == CMD_LOAD)
            {
                int idx = lsFs.getSelectedIndex();
                if (idx >= 0 && !lsFs.getString(idx).endsWith("/"))
                {
                    new Thread ()
                    {
                        public void run()
                        {
                            String trace = "1";
                            try
                            {
                                trace += "2";
                                int size = fs.Size(curPath + lsFs.getString(lsFs.getSelectedIndex()));
                                trace += "3";
                                FilePtr f = fs.Open(curPath + lsFs.getString(lsFs.getSelectedIndex()),
                                        FileSystem.READ);
                                trace += "4";
                                byte[] curCode = new byte[size];
                                trace += "5";
                                f.Read(curCode, 0, curCode.length);
                                trace += "6";
                                f.Close();
                                trace += "7";
                                if (curCode.length >= MAX_SOURCE_CODE)
                                {
                                    throw new Exception ("file lenght >= " + MAX_SOURCE_CODE);
                                }
                                tbSource.setString (new String(curCode));
                                trace += "8";
                                display.setCurrent(tbSource);
                            }
                            catch (Exception e)
                            {
                                System.out.println ("load:"+curPath);
                                e.printStackTrace();
                                display.setCurrent(new Alert ("error", "can't open file [5]:"  + e.toString(), null, null), tbSource);
                            }
                        }
                    }.start();
                }
            }
        }
        else if (d == tbFileName)
        {
            if (c == CMD_SAVE)
            {
                System.out.println ("save to " + curPath + tbFileName.getString());
                new Thread ()
                {
                    public void run()
                    {
                        try
                        {
                            FileSystem fs = FileSystem.GetInstance();
                            FilePtr f = fs.Open(curPath + tbFileName.getString(), FileSystem.WRITE);
                            byte[] curCode = null;
                            if (smode == 1)
                                curCode = tbSource.getString().getBytes();
                            else if (smode == 2)
                                curCode = bytecode;
                            f.Write(curCode, 0, curCode.length);
                            f.Close();
                            display.setCurrent(tbSource);
                        }
                        catch (Exception e)
                        {
                            System.out.println ("save:"+curPath + tbFileName.getString());
                            e.printStackTrace();
                            display.setCurrent(new Alert ("error", "can't open file:" + e.toString(), null, null), tbSource);
                        }
                    }
                }.start();
            }
            else if (c == CMD_SAVE_BYTECODE)
            {

            }
            else if (c == CMD_CANCEL)
            {
                display.setCurrent(tbSource);
            }
        }
        else if (d == tbText)
        {
            if (c == CMD_OK)
            {
                display.setCurrent(tbSource);
                tbSource.insertString(tbText.getString());
            }
            else if (c == CMD_CANCEL)
            {
                display.setCurrent(tbSource);
            }
        }
        else if (c == CMD_COMPILE)
        {
            bCanRun = true;
            bytecode = null;
            try
            {
		long time=System.currentTimeMillis();
                f.deleteAll();
                display.setCurrent(f);
                tbSource.setTitle("parsing...");
                f.append("parsing...\n");
                //InputStream is = new Object().getClass().getResourceAsStream("/test.jcc");
                Jcc2Parser parser = new Jcc2Parser (new ByteArrayInputStream(tbSource.getString().getBytes()));
                tbSource.setTitle("done parsing");
                f.append("done parsing\n");
                tbSource.setTitle("compiling...");
                f.append("compiling...\n");
//                parser.enable_tracing();
                ASTCompilationUnit unit = parser.CompilationUnit();
//                unit.dump("");
                Compiler compiler = new Compiler(unit, false);
                bytecode = compiler.code;
                tbSource.setTitle("done");
                f.append("done\n");
                tbSource.setTitle("time : "+(System.currentTimeMillis()-time)+"ms.");
                new Thread() {

                    public void run() {
                        try {
                            sleep(2000);
                        } catch (Exception e) {
                        } finally {
                            tbSource.setTitle("Jcc v" + getAppProperty("MIDlet-Version"));
                        }
                    }
                }.start();
                //is.close ();
            }
            catch (Exception e)
            {
                f.append(e.toString());
                e.printStackTrace();
                display.setCurrent(f);
                tbSource.setTitle("Jcc v"+getAppProperty("MIDlet-Version"));
            }
        }
        else if (c == CMD_COMPILE_JASMIN)
        {
            bCanRun = false;
            bytecode = null;
            try
            {
                long time=System.currentTimeMillis();
                f.deleteAll();
                display.setCurrent(f);
                tbSource.setTitle("parsing...");
                f.append("parsing...\n");
                //InputStream is = new Object().getClass().getResourceAsStream("/test.jcc");
                Jcc2Parser parser = new Jcc2Parser (new ByteArrayInputStream(tbSource.getString().getBytes()));
                tbSource.setTitle("done parsing");
                f.append("done parsing\n");
                tbSource.setTitle("compiling...");
                f.append("compiling...\n");
//                parser.enable_tracing();
                ASTCompilationUnit unit = parser.CompilationUnit();
//                unit.dump("");
                Compiler compiler = new Compiler(unit, true);
                bytecode = compiler.code;
                tbSource.setTitle("done");
                f.append("done\n");
                tbSource.setTitle("time : "+(System.currentTimeMillis()-time)+"ms.");
                new Thread() {

                    public void run() {
                        try {
                            sleep(2000);
                        } catch (Exception e) {
                        } finally {
                            tbSource.setTitle("Jcc v" + getAppProperty("MIDlet-Version"));
                        }
                    }
                }.start();
                //is.close ();
            }
            catch (Exception e)
            {
                f.append(e.toString());
                e.printStackTrace();
                display.setCurrent(f);
                tbSource.setTitle("Jcc v"+getAppProperty("MIDlet-Version"));
            }
        }
        else if (c == CMD_RUN)
        {
            display.setCurrent(f);
            if (bytecode != null && bCanRun)
            {
                new Thread () {
                    public void run ()
                    {
                        try
                        {
                            f.deleteAll();
                            tbSource.setTitle("Jcc v"+getAppProperty("MIDlet-Version"));
//                            f.append("running...\n");
                            vm = new JccRuntime(bytecode);
                            vm.start("main()V", new Object[0]);
                            display.setCurrent(f);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            f.append(e.toString());
                            display.setCurrent(f);
                        }
                    }
                }.start ();
            }
            else if (!bCanRun)
            {
                f.append("Compiled to Jasmin assemble");
            }
            else
            {
                f.append("No bytecode\n");
            }
        }
        else if (c == CMD_LOAD_TEST)
        {
            display.setCurrent(lSamples);
        }
        else if (c == List.SELECT_COMMAND)
        {
            int ind = lSamples.getSelectedIndex();
            if (ind >= 0)
            {
                try
                {
                    String s = loadTxt("/"+vSamples[ind][1]);
                    tbSource.setString(s);
                    display.setCurrent(tbSource);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    display.setCurrent(new Alert ("error", "can't open file:" + e.toString(), null, null), tbSource);
                }
            }
        }
        else if (c == CMD_RETURN)
        {
            //bytecode = null;
            if (vm != null)
            {
                vm.stop();
            }
            f.deleteAll();
            display.setCurrent(tbSource);
        }
        else if (c == CMD_EDITOR_SETTINGS)
        {
            tbSource.showSettings();
        }
        else if (c == CMD_EXIT)
        {
            destroyApp (false);
        }
        else if (c == CMD_SAVE_SRC)
        {
            saveSource ();
        }
        else if (c == CMD_LOAD_SRC)
        {
            loadSource ();
        }
        else if (c == CMD_SAVE_BYTECODE)
        {
            saveBytecode ();
        }
        else if (c == CMD_EDIT)
        {
            String s = tbSource.getSelectedString();
            tbText.setString(s);
            display.setCurrent(tbText);
        }
    }

    private String loadTxt (String src) throws IOException
    {
        InputStream is = new Object().getClass().getResourceAsStream(src);
        String s = "";
        if (is != null)
        {
            int ch=0;
            while (ch != -1)
            {
                ch = is.read();
                if (ch != -1)
                    s += (char)ch;
                else
                    break;
            }
            is.close ();
        }
        return s;
    }

    public void startApp ()
    {
    }

    public void pauseApp ()
    {
    }

    public void destroyApp (boolean unconditional)
    {
        notifyDestroyed ();
    }

    public void loadSource ()
    {
        lsFs.removeCommand(CMD_SAVE);
        lsFs.addCommand(CMD_LOAD);
        lsFs.setTitle("Files");
        curPath = "/";
        mode = 1;
        display.setCurrent(lsFs);
        new Thread()
        {
            public void run()
            {
                try
                {
                    String[] vs = fs.List(curPath);
                    lsFs.deleteAll();
                    lsFs.append("../", null);
                    for (int i = 0; i < vs.length; i++)
                    {
                        lsFs.append(vs[i], null);
                    }
                    lsFs.setTitle("Files");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    display.setCurrent(new Alert ("error", "can't list dir:" + e.toString(), null, null), tbSource);
                }
            }
        }.start ();
    }

    public void saveSource ()
    {
        smode = 1;
        mode = 2;
        lsFs.removeCommand(CMD_LOAD);
        lsFs.removeCommand(CMD_SAVE_BYTECODE);
        lsFs.addCommand(CMD_SAVE);
        lsFs.setTitle("Files");
        curPath = "/";
        display.setCurrent(lsFs);
        new Thread()
        {
            public void run()
            {
                try
                {
                    String[] vs = fs.List(curPath);
                    lsFs.deleteAll();
                    lsFs.append("../", null);
                    for (int i = 0; i < vs.length; i++)
                    {
                        lsFs.append(vs[i], null);
                    }
                    lsFs.setTitle("Files");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    display.setCurrent(new Alert ("error", "can't list dir:" + e.toString(), null, null), tbSource);
                }
            }
        }.start ();
    }

    public void saveBytecode ()
    {
        smode = 2;
        mode = 2;
        lsFs.removeCommand(CMD_LOAD);
        lsFs.removeCommand(CMD_SAVE);
        lsFs.addCommand(CMD_SAVE_BYTECODE);
        lsFs.setTitle("Files");
        curPath = "/";
        display.setCurrent(lsFs);
        new Thread()
        {
            public void run()
            {
                try
                {
                    String[] vs = fs.List(curPath);
                    lsFs.deleteAll();
                    lsFs.append("../", null);
                    for (int i = 0; i < vs.length; i++)
                    {
                        lsFs.append(vs[i], null);
                    }
                    lsFs.setTitle("Files");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    display.setCurrent(new Alert ("error", "can't list dir:" + e.toString(), null, null), tbSource);
                }
            }
        }.start ();
    }
}
