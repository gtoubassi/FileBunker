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
 * Created on Sep 1, 2004
 */
package com.toubassi.filebunker.ui.restore;

import com.toubassi.filebunker.ui.LabelUtil;
import com.toubassi.jface.ObjectChooserDialog;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

/**
 * @author garrick
 */
public class DateChooser extends Composite
{
    public interface Listener
    {
        public void selectionChanged(DateChooser dateChooser);
    }
    
    // Number of dates to display in the drop down other than the custom date.
    private static final int NumberOfDatesToDisplay = 15;
    
    private static final String OtherLabel = "Other...";

    private Collection dates;
    private Combo dateCombo;
    private Date customDate;
    private int lastSelectionIndex;
    private Listener listener;
    
    public DateChooser(Composite parent)
    {
        super(parent, SWT.NONE);
        createContents();
    }
    
    public void setSelectionListener(Listener listener)
    {
        this.listener = listener;
    }
    
    private void selectionChanged()
    {
        if (listener != null) {
            listener.selectionChanged(this);
        }
        lastSelectionIndex = dateCombo.getSelectionIndex();
    }
    
    protected void createContents()
    {
        setLayout(new FormLayout());

        dateCombo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
        FormData dateComboFormData = new FormData();
        dateComboFormData.top = new FormAttachment(0, 0);
        dateComboFormData.left = new FormAttachment(0, 0);
        dateComboFormData.bottom = new FormAttachment(100, 0);
        dateComboFormData.right = new FormAttachment(100, 0);
        dateCombo.setLayoutData(dateComboFormData);

        dateCombo.setVisibleItemCount(30);

        dateCombo.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                int selectedIndex = dateCombo.getSelectionIndex();
                String selection = dateCombo.getItem(selectedIndex);
                
                if (selection.equals(OtherLabel)) {
                    chooseOther();
                }
                selectionChanged();
            }            
        });
    }
    
    public void setContents(Collection newDates)
    {
        boolean hasPreviousSelection = false;
        Date previousSelectedDate = null;
        Date oldFirst = null;
        
        if (dates != null && !dates.isEmpty()) {
            oldFirst = (Date)dates.iterator().next();
        }
        
        if (oldFirst != null) {
            hasPreviousSelection = true;
            previousSelectedDate = selectedDate();
        }        
        
        dates = newDates;
        
        Date newFirst = null;
        if (dates != null && !dates.isEmpty()) {
            newFirst = (Date)dates.iterator().next();
        }
        
        refresh();        
        
        if (hasPreviousSelection && (oldFirst.equals(newFirst) || previousSelectedDate == null)) {
            setSelectedDate(previousSelectedDate);
        }
        else {
            dateCombo.select(dates.isEmpty() ? 0 : 1);            
        }
    }
    
    private void refresh()
    {
        dateCombo.removeAll();

        dateCombo.add("Show all files that have ever been backed up");
        
        Iterator i = dates.iterator();
        int count = 0;
        while (i.hasNext() && count < NumberOfDatesToDisplay) {
            addDate((Date)i.next());
            count++;            
        }
        
        if (customDate != null) {
            addDate(customDate);
        }
        
        if (i.hasNext()) {
            dateCombo.add(OtherLabel);
        }        

        pack();
        Composite parent = getParent();
        if (parent != null) {
            parent.layout();
        }
    }
    
    private void addDate(Date date)
    {
        String dateString = LabelUtil.shortDateTimeSecondsFormat.format(date);
        dateCombo.add("Show files up to " + dateString);                    
    }
    
    protected void chooseOther()
    {
        String description = "The dates and times of all backup operations you " +
        		"have performed are listed below.  Select one in order to filter " +
        		"the explorer to show files as they existed at that time.  Files " +
        		"that were created and backed up later will not appear.";
        ObjectChooserDialog dialog = new ObjectChooserDialog(getShell(), dates.toArray(), "History", "History of all backups", description);
        dialog.setFormat(LabelUtil.shortDateTimeSecondsFormat);
        
        Date selectedDate = (Date)dialog.run();
        
        if (selectedDate != null) {
            setSelectedDate(selectedDate);
        }
        else {
            // Cancel
            dateCombo.select(lastSelectionIndex);
        }
    }
    
    public int numberOfDates()
    {
        return dates == null ? 0 : dates.size();
    }
    
    public Date selectedDate()
    {
        int selected = dateCombo.getSelectionIndex();
        
        // The first one is the "Show all" choice, represented
        // by null.
        if (selected <= 0) {
            return null;
        }
        
        // If we have a non null customDate, and the last date
        // is selected ("last date" is a tricky notion since "Other..."
        // may or may not be displayed), then it is the customDate.
        if (customDate != null && lastDateIndex() == selected) {
            return customDate;
        }
        
        // Skip the first one which is the "Show all" label
        selected--;

        Iterator iterator = dates.iterator();
        for (int i = 0; i < selected; i++) {
            iterator.next();
        }
        return (Date)iterator.next();
    }
    
    public void setSelectedDate(Date selectedDate)
    {
        if (selectedDate == null) {
            dateCombo.select(0);
            return;
        }
        
        int selectedDateIndex = -1;
        
        // See if the date is already in the list
        Iterator iterator = dates.iterator();
        int count = 0;
        while (iterator.hasNext() && count < NumberOfDatesToDisplay) {
            Date date = (Date)iterator.next();
            
            if (date.equals(selectedDate)) {
                selectedDateIndex = count;
                break;
            }
            count++;            
        }
        
        if (selectedDateIndex == -1 && !selectedDate.equals(customDate)) {
            customDate = selectedDate;
            refresh();
        }
        
        if (selectedDateIndex != -1) {
            lastSelectionIndex = selectedDateIndex + 1;
        }
        else {
            lastSelectionIndex = lastDateIndex();
        }        
        dateCombo.select(lastSelectionIndex);	            
    }
    
    private int lastDateIndex()
    {
        int lastIndex = dateCombo.getItemCount() - 1;
        
        if (dateCombo.getItem(lastIndex).equals(OtherLabel)) {
            return lastIndex - 1;
        }
        else {
            return lastIndex;
        }        
    }
}
