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
 * Created on Aug 4, 2004
 */
package com.toubassi.filebunker.ui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.eclipse.swt.graphics.Image;

/**
 * @author garrick
 */
public class ImageCache
{
    private static final Object NotFoundMarker = new Object();
    private static ImageCache shared;

    private HashMap packageToImageMap = new HashMap();
    
    public static ImageCache sharedCache()
    {
        if (shared == null) {
            shared = new ImageCache();
        }
        return shared;
    }
    
    public Image get(Object object, String imageName)
    {
        return get(object.getClass(), imageName);
    }

    public Image get(Class cls, String imageName)
    {
        String packageName = cls.getPackage().getName();
        HashMap images = (HashMap)packageToImageMap.get(packageName);
        
        if (images == null) {
            images = new HashMap();
            packageToImageMap.put(packageName, images);
        }

        Object image = images.get(imageName);
        
        if (image == null) {
            
            InputStream imageStream = cls.getResourceAsStream(imageName);
            
            if (imageStream == null) {
                // For development, try from working directory (in the source)
                String relativePath = packageName.replace('.', '/') + "/" + imageName;
                try {
                    imageStream = new FileInputStream(relativePath);
                }
                catch (FileNotFoundException e) {
                    image = NotFoundMarker;
                }
            }
            
            if (imageStream != null) {
                image = new Image(null, imageStream);
                try {
                    imageStream.close();
                }
                catch (IOException e) {
                    //swallow
                }
            }
            
            images.put(imageName, image);
        }
        
        return image == NotFoundMarker ? null : (Image)image;
    }
}


