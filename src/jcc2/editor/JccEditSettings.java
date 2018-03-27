/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jcc2.editor;

/**
 *
 * @author root
 */

import javax.microedition.rms.*;
import javax.microedition.lcdui.*;
import java.io.*;

public class JccEditSettings extends Canvas implements ItemStateListener,
        CommandListener, ItemCommandListener
{
    public static JccEditSettings singleton;

    public boolean bQwerty;
    public boolean bFullscreen;
    public boolean bLineNumbers;
    public boolean bSyntax;
    public int leftSoft, rightSoft;

    String action;
    boolean bSelecting;
    int keySelected;

    Form fSettings;
    ChoiceGroup chSettings;
    StringItem btnSelectLSOFT;
    StringItem btnSelectRSOFT;
    StringItem btnOk;
    Command CMD_SELECT = new Command ("Select", Command.ITEM, 0);
    Command CMD_OK = new Command ("OK", Command.ITEM, 0);

    Display disp;
    JccEdit edit;

    public JccEditSettings ()
    {
        singleton = this;

        bQwerty = false;
        bLineNumbers = false;
        bFullscreen = false;

        setFullScreenMode (true);
        fSettings = new Form ("Settings");

        String[] setts = {"Qwerty keyboard", "Show line numbers", "Full screen", "Syntax highlighting"
        };
        chSettings = new ChoiceGroup ("", Choice.MULTIPLE, setts, null);
        boolean[] vSel = {bQwerty, bLineNumbers, bFullscreen, bSyntax};
        chSettings.setSelectedFlags(vSel);

        fSettings.append(chSettings);
        fSettings.setItemStateListener(this);
        btnSelectLSOFT = new StringItem ("", "Set LSOFT", Item.BUTTON);
        btnSelectRSOFT = new StringItem ("", "Set RSOFT", Item.BUTTON);
        btnOk = new StringItem ("", "OK", Item.BUTTON);
        btnSelectLSOFT.addCommand(CMD_SELECT);
        btnSelectRSOFT.addCommand(CMD_SELECT);
        btnSelectLSOFT.setDefaultCommand(CMD_SELECT);
        btnSelectRSOFT.setDefaultCommand(CMD_SELECT);
        btnOk.addCommand(CMD_OK);
        btnOk.setDefaultCommand(CMD_OK);
        fSettings.append(btnSelectLSOFT);
        fSettings.append(btnSelectRSOFT);
        fSettings.append(btnOk);
        fSettings.setCommandListener(this);
        btnSelectLSOFT.setItemCommandListener(this);
        btnSelectRSOFT.setItemCommandListener(this);
        btnOk.setItemCommandListener(this);
        edit = null;
    }

    public boolean hasSettings ()
    {
        try
        {
            RecordStore rs = RecordStore.openRecordStore("settings2", false);
            if (rs.getNumRecords() == 0)
            {
                rs.closeRecordStore();
                return false;
            }
            rs.closeRecordStore();
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public void saveSettings ()
    {
        try
        {
            RecordStore rs = RecordStore.openRecordStore("settings2", true);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream os = new DataOutputStream (bos);
            os.writeBoolean(bQwerty);
            os.writeBoolean(bLineNumbers);
            os.writeBoolean(bFullscreen);
            os.writeBoolean(bSyntax);
            os.writeInt (leftSoft);
            os.writeInt (rightSoft);
            byte[] data = bos.toByteArray();
            os.close ();
            if (rs.getNumRecords() == 0)
                rs.addRecord(data, 0, data.length);
            else
                rs.setRecord(1, data, 0, data.length);
            rs.closeRecordStore();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void loadSettings ()
    {
        try
        {
            RecordStore rs = RecordStore.openRecordStore("settings2", true);
            if (rs.getNumRecords() == 0)
            {
                rs.closeRecordStore();
                saveSettings ();
            }
            else
            {
                byte[] data = rs.getRecord(1);
                DataInputStream is = new DataInputStream(new ByteArrayInputStream(data));
                bQwerty = is.readBoolean();
                bLineNumbers = is.readBoolean();
                bFullscreen = is.readBoolean();
                bSyntax = is.readBoolean();
                leftSoft = is.readInt();
                rightSoft = is.readInt();
                chSettings.setSelectedFlags(new boolean[]{bQwerty, bLineNumbers, bFullscreen, bSyntax});
                is.close ();
                rs.closeRecordStore();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void show (Display d, JccEdit next)
    {
        disp = d;
        edit = next;
        d.setCurrent (fSettings);
    }

    public void keyPressed (int key)
    {
        if (bSelecting)
        {
            keySelected = key;
            bSelecting = false;
            disp.setCurrent(fSettings);
        }
    }

    public void paint (Graphics g)
    {
        g.setColor (0xffffff);
        g.fillRect (0, 0, getWidth(), getHeight());
        g.setColor (0);
        g.drawString (action, getWidth()/2 - g.getFont().stringWidth(action)/2,
                getHeight()/2, Graphics.LEFT|Graphics.TOP);
    }

    public void itemStateChanged(Item item)
    {
        if (item == chSettings)
        {
            boolean[] bSettings = new boolean[4];
            chSettings.getSelectedFlags(bSettings);
            if (bSettings[0])
                bQwerty = true;
            else
                bQwerty = false;
            if (bSettings[1])
                bLineNumbers = true;
            else
                bLineNumbers = false;
            if (bSettings[2])
                bFullscreen = true;
            else
                bFullscreen = false;
            if (bSettings[3])
                bSyntax = true;
            else
                bSyntax = false;
        }
    }

    public void commandAction(Command c, Displayable d)
    {
    }

    public void commandAction(Command c, Item item)
    {
        if (item == btnSelectLSOFT)
        {
            bSelecting = true;
            keySelected = Integer.MAX_VALUE;
            action = "press LSOFT";
            disp.setCurrent(this);
            new Thread ()
            {
                public void run ()
                {
                    while (keySelected == Integer.MAX_VALUE)
                        yield ();
                    leftSoft = keySelected;
                }
            }.start ();
        }
        else if (item == btnSelectRSOFT)
        {
            bSelecting = true;
            keySelected = Integer.MAX_VALUE;
            action = "press RSOFT";
            disp.setCurrent(this);
            new Thread ()
            {
                public void run ()
                {
                    while (keySelected == Integer.MAX_VALUE)
                        yield ();
                    rightSoft = keySelected;
                }
            }.start ();
        }
        else if (item == btnOk)
        {
            disp.setCurrent(edit);
			saveSettings();
            edit.reinit();
        }
    }
}
