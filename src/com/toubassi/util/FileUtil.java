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
 * Created on Aug 25, 2004
 */
package com.toubassi.util;

import java.io.File;

/**
 * @author garrick
 */
public class FileUtil
{
    public static boolean isAncestor(File ancestor, File descendant)
    {
        return isAncestor(ancestor.getPath(), descendant.getPath());
    }

    /**
     * Returns true if ancestor is equal to descendant, or if ancestor
     * represents a parent directory of descendant.  Another way to think
     * of it is, "if I did an rm -rf on ancestor, would descendant go away?".
     * So its not really a "proper" ancestor.  Note the assumption is that
     * ancestor, if it is a directory DOES NOT have a trailing slash.
     */
    public static boolean isAncestor(String ancestor, String descendant)
    {
        if (descendant.startsWith(ancestor)) {
            
            if (ancestor.length() == descendant.length()) {
                return true;
            }
            
            if (descendant.charAt(ancestor.length()) == File.separatorChar) {
                return true;
            }
        }
        
        return false;
    }
    
    public static String getExtension(File file)
    {
        return getExtension(file.getPath());
    }
    
    /**
     * Returns the extension (including the '.') or null if none.
     */
    public static String getExtension(String path)
    {
        int lastDot = path.lastIndexOf('.');
        int lastSlash = path.lastIndexOf(File.separatorChar);
        
        if (lastDot == -1 || lastDot == 0 || lastDot == path.length() - 1) {
            return null;
        }
        if (lastSlash != -1 && lastDot == lastSlash + 1) {
            return null;
        }
        
        return path.substring(lastDot);
    }
    
    public static String getBasename(File file)
    {
        return getBasename(file.getPath());
    }
    
    public static String getBasename(String path)
    {
        int lastDot = path.lastIndexOf('.');
        int lastSlash = path.lastIndexOf(File.separatorChar);
        int firstChar = lastSlash == -1 ? 0 : lastSlash + 1;
        
        if (lastDot == -1 || lastDot == 0 || lastDot == path.length() - 1) {
            return path.substring(firstChar);
        }
        if (lastSlash != -1 && lastDot == lastSlash + 1) {
            return path.substring(firstChar);
        }
        
        return path.substring(firstChar, lastDot);
    }
}
