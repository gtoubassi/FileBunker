/*

Copyright (c) 2004, Garrick Toubassi

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

*/

/*
 * Created on Aug 15, 2004
 */
package com.toubassi.jface;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author garrick
 */
public class ContextHintDialog extends Dialog
{
    private HashMap controlHints = new HashMap();
    
    // May be null if no hints were specified.
    private Label infoDescription;
    
    private WidgetFocusListener focusListener;
    private Label infoTitleLabel;
    private String infoTitle;
    
    public ContextHintDialog(Shell shell)
    {
        super(shell);
        focusListener = new WidgetFocusListener();
    }
    
    public void setInfoTitle(String infoTitle)
    {
        this.infoTitle = infoTitle;
        if (infoTitleLabel != null) {
            infoTitleLabel.setText(infoTitle);
        }
    }
    
    protected Point getInitialLocation(Point initialSize)
    {
        Point location = super.getInitialLocation(initialSize);
        
        Point parentSize = getParentShell().getSize();
		Point centerPoint = Geometry.centerPoint(getParentShell().getBounds());

		// Start by centering evenly
        location.y = centerPoint.y - initialSize.y / 2;
        if (initialSize.y < parentSize.y) {
            // Optically, if the child is smaller lets shift up a bit
            location.y -= (parentSize.y - initialSize.y)/6;
        }
        
        // Offset this shell from its parent just in case it has the same
        // x and width or y and height.  This is just for aesthetics.
        Point parentLocation = getParentShell().getLocation();
        
        if (parentLocation.x == location.x && initialSize.x == parentSize.x) {
            location.x += 20;
        }
        if (parentLocation.y == location.y && initialSize.y == parentSize.y) {
            location.y += 20;
        }

        return location;
    }
    
    public void setControlHint(Control control, String hint)
    {
        controlHints.put(control, hint);
        control.addFocusListener(focusListener);
    }
    
    public void setDefaultHint(String hint)
    {
        controlHints.put(null, hint);
    }
    
    private String longestControlHint()
    {
        String longest = null;
        Iterator i = controlHints.values().iterator();
        while (i.hasNext()) {
            String hint = (String)i.next();
            if (longest == null || hint.length() > longest.length()) {
                longest = hint;
            }
        }
        return longest;
    }
    
    protected Control createDialogArea(Composite parent)
    {
		Composite contents = new Composite(parent, SWT.NONE);
		
		contents.setLayout(new FormLayout());
		
		Composite infoComposite = new Composite(contents, SWT.NONE);
		FormData infoCompositeFormData = new FormData();
		infoCompositeFormData.left = new FormAttachment(0, 0);
		infoCompositeFormData.right = new FormAttachment(100, 0);
		infoCompositeFormData.top = new FormAttachment(0, 0);
		infoComposite.setLayoutData(infoCompositeFormData);
		infoComposite.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		infoComposite.setLayout(new FormLayout());
		
		infoTitleLabel = new Label(infoComposite, SWT.NONE);
		FontRegistry fontRegistry = JFaceResources.getFontRegistry();
		infoTitleLabel.setFont(fontRegistry.getBold(JFaceResources.DIALOG_FONT));
		infoTitleLabel.setBackground(infoComposite.getBackground());
		
		if (infoTitle != null) {
		    infoTitleLabel.setText(infoTitle);
		}
		FormData infoTitleLabelFormData = new FormData();
		infoTitleLabelFormData.top = new FormAttachment(0, 10);
		infoTitleLabelFormData.left = new FormAttachment(0, 15);
		
		Label topSeparator = new Label(contents, SWT.SEPARATOR | SWT.HORIZONTAL);
		FormData topSeparatorFormData = new FormData();
		topSeparatorFormData.top = new FormAttachment(infoComposite, 0);
		topSeparatorFormData.left = new FormAttachment(0, 0);
		topSeparatorFormData.right = new FormAttachment(100, 0);
		topSeparator.setLayoutData(topSeparatorFormData);

		Composite customContents = new Composite(contents, SWT.NONE);
		FormData customContentsFormData = new FormData();
		customContentsFormData.top = new FormAttachment(topSeparator, 20);
		customContentsFormData.left = new FormAttachment(0, 15);
		customContentsFormData.right = new FormAttachment(100, -15);
		customContents.setLayoutData(customContentsFormData);
		
		createCustomContents(customContents);

		if (controlHints.size() > 0) {		
			infoDescription = new Label(infoComposite, SWT.WRAP);
			infoDescription.setBackground(infoComposite.getBackground());
		}
		else {
		    infoTitleLabelFormData.width = 300;
		    Composite spacer = new Composite(infoComposite, SWT.NONE);
		    spacer.setBackground(infoComposite.getBackground());
		    FormData spacerFormData = new FormData();
		    spacerFormData.top = new FormAttachment(infoTitleLabel);
		    spacerFormData.height = 15;
		    spacer.setLayoutData(spacerFormData);
		}

		infoTitleLabel.setLayoutData(infoTitleLabelFormData);
				
        Label bottomSeparator = new Label(contents, SWT.SEPARATOR | SWT.HORIZONTAL);
        FormData bottomSeparatorFormData = new FormData();
        bottomSeparatorFormData.top = new FormAttachment(customContents, 5, SWT.BOTTOM);
        bottomSeparatorFormData.left = new FormAttachment(0, 0);
        bottomSeparatorFormData.right = new FormAttachment(100, 0);
        bottomSeparator.setLayoutData(bottomSeparatorFormData);

        if (infoDescription != null) {
            Point size = new Point(70, 0);
            ControlUtil.convertSizeInCharsToPixels(infoDescription, size);
            
	        String longestHint = longestControlHint();
	        Point infoSize = new Point(0, 0);
	        if (longestHint != null) {
	    		infoDescription.setText(longestHint);
	    		infoSize = infoDescription.computeSize(size.x, SWT.DEFAULT);
	    		infoDescription.setText("");
	        }
	
			if (controlHints.get(null) != null) {
			    infoDescription.setText((String)controlHints.get(null));
			}
	        
			FormData infoDescriptionFormData = new FormData();
			infoDescriptionFormData.top = new FormAttachment(infoTitleLabel, 10);
			infoDescriptionFormData.left = new FormAttachment(0, 25);
			infoDescriptionFormData.right = new FormAttachment(100, -15);
			infoDescriptionFormData.bottom = new FormAttachment(100, -15);
			infoDescriptionFormData.width = size.x;
			infoDescriptionFormData.height = infoSize.y;
			infoDescription.setLayoutData(infoDescriptionFormData);        
        }
		
		return contents;
    }
    
    protected void createCustomContents(Composite parent)
    {
    }

    class WidgetFocusListener implements FocusListener
    {
        public void focusGained(FocusEvent e)
        {
            String hint = (String)controlHints.get(e.widget);
            if (hint == null) {
                hint = (String)controlHints.get(null);
            }
            if (hint != null && infoDescription != null) {
                infoDescription.setText(hint);
            }
            if (e.widget instanceof Text) {
                Text text = (Text)e.widget;
                text.selectAll();
            }
        }

        public void focusLost(FocusEvent e)
        {            
            // Looks better to keep the text.  infoDescription.setText("");
        }
    };
}
