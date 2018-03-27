package org.pabloid.util;

import javax.microedition.lcdui.*;

public class Highlighter
{
    static String[][] vKeywords={{"bool","break","byte","char","else",
                        "false","float","if","int","new","null",
                        "return","short","string","true","use",
                        "void","while","for","import"},
                        {"Float","Integer","Object","String",
                          "Canvas","Graphics","Image","Sprite",
                          "SocketConnection","ServerSocketConnection","HttpConnection","InputStream",
                          "OutputStream","Player","System","Thread","Vector"},
                          {"0","1","2","3","4","5","6","7","8","9"}};
    static int[] colors={0x0000ff,0xff0000,0x800080};

    public static Image highlight(String text, Font font)
    {
        int w=font.stringWidth(text);
        if(text.equals(""))
        {
            w=1;
        }
        Image img=Image.createImage(w,font.getHeight());
        Graphics g=img.getGraphics();
        g.setFont(font);
        g.drawString(text,0,0,20);
        int i0=0;
        for(int j=0;j<vKeywords.length;j++)
        {
        g.setColor(colors[j]);
        i0=0;
        for(int i=0;i<vKeywords[j].length;i++)
        {
            for(;i0<text.length();i0++)
            {
            i0=text.indexOf(vKeywords[j][i],i0);
            if(i0<0)
                break;
            char c=(i0+vKeywords[j][i].length()>text.length())?text.charAt(i0+vKeywords[j][i].length()):' ';
            char c1=(i0>0)?text.charAt(i0-1):' ';
            if(Preprocessor.isName(c)||Preprocessor.isName(c1)&&!(Character.isDigit(c1)||Character.isDigit(c)))
                continue;
            int color=g.getColor();
            g.setColor(0xffffff);
            g.fillRect(font.stringWidth(text.substring(0,i0)),0,font.stringWidth(vKeywords[j][i]),w);
            g.setColor(color);
            g.drawSubstring(text,i0,vKeywords[j][i].length(),font.stringWidth(text.substring(0,i0)),0,20);
            }
        }
        }
        g.setColor(0xff8000);
        for(i0=0;i0<text.length();i0++)
            {
            i0=text.indexOf('"',i0);
            if(i0<0)
                break;
            int i1=text.indexOf('"',i0+1);
            if (i1<0)
                i1=text.length()-1;
            int color=g.getColor();
            g.setColor(0xffffff);
            g.fillRect(font.stringWidth(text.substring(0,i0)),0,font.stringWidth(text.substring(i0,i1+1)),w);
            g.setColor(color);
            g.drawSubstring(text,i0,i1-i0+1,font.stringWidth(text.substring(0,i0)),0,20);
            i0=i1;
            }
        g.setColor(0x008000);
        for(i0=0;i0<text.length();i0++)
            {
            i0=text.indexOf("/*",i0);
            if(i0<0)
                break;
            int i1=text.indexOf("*/",i0+2);
            if(i1<0)
                i1=text.length()-2;
            int color=g.getColor();
            g.setColor(0xffffff);
            g.fillRect(font.stringWidth(text.substring(0,i0)),0,font.stringWidth(text.substring(i0,i1+2)),w);
            g.setColor(color);
            g.drawSubstring(text,i0,i1-i0+2,font.stringWidth(text.substring(0,i0)),0,20);
            i0=i1;
            }
        i0=text.indexOf("//");
        if (i0>=0)
        {
            int color=g.getColor();
            g.setColor(0xffffff);
            g.fillRect(font.stringWidth(text.substring(0,i0)),0,font.stringWidth(text.substring(i0)),w);
            g.setColor(color);
            g.drawSubstring(text,i0,text.length()-i0,font.stringWidth(text.substring(0,i0)),0,20);
        }
        return img;
    }
}
