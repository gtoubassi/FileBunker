/*
 * Created on Sep 15, 2004
 */
package com.toubassi.jface;

import java.util.HashMap;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

public class ControlUtil
{
    private static final HashMap cachedFontMetrics = new HashMap();
    
    public static void convertSizeInCharsToPixels(Control control, Point size)
    {
        FontMetrics fontMetrics = getFontMetrics(control);
        
        if (fontMetrics == null) {
            return;
        }
        
		size.x = fontMetrics.getAverageCharWidth() * size.x;
		size.y = fontMetrics.getHeight() * size.y;
    }

    public static FontMetrics getFontMetrics(Control control)
    {
        FontData[] fontDatas = control.getFont().getFontData();
        
        if (fontDatas.length == 0) {
            return null;
        }
        
		String fontDescription = fontDatas[0].toString();
        
        synchronized (cachedFontMetrics) {
            
            FontMetrics fontMetrics = (FontMetrics)cachedFontMetrics.get(fontDescription);
            
            if (fontMetrics == null) {
        		GC gc = new GC(control);
        		gc.setFont(JFaceResources.getDialogFont());
        		fontMetrics = gc.getFontMetrics();
        		gc.dispose();
        		
        		cachedFontMetrics.put(fontDescription, fontMetrics);
            }
            
            return fontMetrics;
        }
    }
}

