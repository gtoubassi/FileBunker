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

import java.io.IOException;

/**
 * Objects that wish to be archived must implement this interface.
 * When they, in turn, archive other objects, they can choose the
 * manner in which those objects are archived, with corresponding
 * performance tradeoffs.
 * @author garrick
 */
public interface Archivable
{
    /**
     * The most general/robust means of archiving an object.  The
     * concrete class of the object does not need to be known when
     * unarchiving, and multiple references to the same object will
     * result to the same object when unarchiving.
     */
    public static final int PolymorphicReference = 0;
    
    /**
     * The concrete class of the object does not need to be known
     * when unarchiving, but references will not be restored.  Multiple
     * references to the same object will result in multiple distinct
     * objects when unarchiving.
     */
    public static final int PolymorphicValue = 1;

    /**
     * The concrete class of the object needs to be known when unarchiving,
     * but multiple references to the same object will
     * result to the same object when unarchiving.
     */
    public static final int StrictlyTypedReference = 2;

    /**
     * The concrete class of the object needs to be known when unarchiving,
     * and references will not be restored.  Multiple references to the same
     * object will result in multiple distinct objects when unarchiving.
     */
    public static final int StrictlyTypedValue = 3;
    
    
    /**
     * The object is asked to archive its internal state by calling methods
     * on the specified ArchiveOutputStream.  Objects are encouraged to also
     * track versioning info using writeClassVersion.  They should also make
     * sure to call super.archive if necessary.
     */
    public void archive(ArchiveOutputStream output) throws IOException;
    
    /**
     * The object is asked to unarchive itself by calling methods on the
     * specified ArchiveInputStream.  They should also make sure to call
     * super.unarchive if necessary.
     */
    public void unarchive(ArchiveInputStream input) throws IOException;
}
