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
 * Created on Aug 23, 2004
 */
package com.toubassi.util;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author garrick
 */
public class MultiIterator implements Iterator
{
    public ArrayList iterators = new ArrayList();
    
    public MultiIterator()
    {        
    }
    
    public MultiIterator(Iterator iterator1, Iterator iterator2)
    {
        add(iterator1);
        add(iterator2);
    }
    
    public void add(Iterator iterator)
    {
        iterators.add(iterator);
    }
    
    public void add(Object object)
    {
        iterators.add(new SingleObjectIterator(object));
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }
    
    private Iterator current()
    {
        while (iterators.size() > 0) {
            Iterator iterator = (Iterator)iterators.get(0);
            
            if (iterator.hasNext()) {
                return iterator;
            }
            iterators.remove(0);
        }
        return null;
    }

    public boolean hasNext()
    {
        Iterator current = current();
        return (current != null && current.hasNext());
    }

    public Object next()
    {
        Iterator current = current();
        if (current == null) {
            return null;
        }
        return current.next();
    }
    
    public static void main(String args[])
    {
        ArrayList a1 = new ArrayList();
        a1.add("1");
        
        ArrayList a2 = new ArrayList();
        a2.add("3");
        a2.add("4");
        
        ArrayList a3 = new ArrayList();
        a3.add("6");
        a3.add("7");
        a3.add("8");
        a3.add("9");
        
        MultiIterator i = new MultiIterator();
        i.add(a1.iterator());
        i.add("2");
        i.add(a2.iterator());
        i.add("5");
        i.add(a3.iterator());
        i.add("10");
        
        while (i.hasNext()) {
            System.out.println(i.next());
        }
    }
}

class SingleObjectIterator implements Iterator
{
    private Object object;
    
    public SingleObjectIterator(Object object)
    {
        this.object = object;
    }
    
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext()
    {
        return object != null;
    }

    public Object next()
    {
        Object returnValue = object;
        object = null;
        return returnValue;
    }    
}
