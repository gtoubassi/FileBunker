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
package com.toubassi.archive;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

/**
 * Tracks statistics for an ArchiveOutputStream.  Specifically the total
 * bytes stored for each class, both with and without descendant objects,
 * as well as the number of objects archived is recorded.  See
 * setKeepStatistics dumpStatistics in ArchiveOutputStream.
 * @author garrick
 */
public class ArchiveStatistics
{
    private ArchiveOutputStream output;
    private HashMap statistics = new HashMap();
    private Stack stack = new Stack();
    
    public ArchiveStatistics(ArchiveOutputStream output)
    {
        this.output = output;
    }
    
    public void begin(String category)
    {
        Statistic statistic = new Statistic(category);
        statistic.begin(output.size());
        stack.push(statistic);        
    }
    
    public void end()
    {
        Statistic statistic = (Statistic)stack.pop();
        statistic.end(output.size());

        if (stack.size() > 0) {
            Statistic ancestorStatistic = (Statistic)stack.peek();
            ancestorStatistic.addDescendantSize(statistic.totalSize());
        }
        
        Statistic accruingStatistic = (Statistic)statistics.get(statistic.category());
        if (accruingStatistic == null) {
            statistics.put(statistic.category(), statistic);
        }
        else {
            accruingStatistic.addFrame(statistic);
        }
    }
    
    public void dumpStatistics()
    {
        Iterator i = statistics.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry)i.next();
            Statistic statistic = (Statistic)entry.getValue();
            System.out.println(statistic);
        }        
    }
}

class Statistic
{
    private String category;
    private int numInvocations = 1;
    private int startSize;
    private int endSize;
    private int descendantsSize;
    
    public Statistic(String category)
    {
        this.category = category;
    }
    
    public void addFrame(Statistic other)
    {
        numInvocations += other.numInvocations;
        endSize += (other.endSize - other.startSize);
        descendantsSize += other.descendantsSize;
    }
    
    public String category()
    {
        return category;
    }
    
    public void begin(int startSize)
    {
        this.startSize = startSize;
    }
    
    public void end(int endSize)
    {
        this.endSize = endSize;
    }
    
    public void addDescendantSize(int size)
    {
        descendantsSize += size;
    }
    
    public int totalSize()
    {
        return endSize - startSize;
    }
    
    public int descendantsSize()
    {
        return descendantsSize;
    }
    
    public String toString()
    {
        int totalSizePer = Math.round(totalSize() / (float)numInvocations);
        int size = totalSize() - descendantsSize();
        int sizePer = Math.round(size / (float)numInvocations);
        return category + " " + numInvocations + " " + totalSize() + "(" + totalSizePer + " per) " + size + " (" + sizePer + " per)";
    }
}
