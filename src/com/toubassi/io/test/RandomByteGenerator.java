/*
 * Created on Sep 16, 2004
 */
package com.toubassi.io.test;

import com.toubassi.util.Arguments;

import java.io.OutputStream;
import java.util.Random;

import javax.mail.internet.MimeUtility;

/**
 * @author garrick
 */
public class RandomByteGenerator
{
    public static void main(String args[]) throws Exception
    {
        Arguments arguments = new Arguments(args);
        
        int size = arguments.flagInt("size");
        String format = arguments.flagString("format", "ascii");
        
        if (!format.equals("ascii") && !format.equals("binary")) {
            System.err.println("-format must be ascii or binary.");
            System.exit(1);
        }
        
        OutputStream out = System.out;
        
        if (format.equals("ascii")) {
            out = MimeUtility.encode(out, "base64");
        }
        
        Random random = new Random();
        byte buffer[] = new byte[1024];
        
        while (size > 0) {
            random.nextBytes(buffer);
            if (size > buffer.length) {
                out.write(buffer);
                size -= buffer.length;
            }
            else {
                out.write(buffer, 0, size);
                size = 0;
            }
        }
    }

}
