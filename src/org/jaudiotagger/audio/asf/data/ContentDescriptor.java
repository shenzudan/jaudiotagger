/*
 * Entagged Audio Tag library
 * Copyright (c) 2004-2005 Christian Laireiter <liree@web.de>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jaudiotagger.audio.asf.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.jaudiotagger.audio.asf.tag.AsfFieldKey;
import org.jaudiotagger.audio.asf.util.Utils;

/**
 * This class is a wrapper for properties within a
 * {@link org.jaudiotagger.audio.asf.data.ExtendedContentDescription}.<br>
 *
 * @author Christian Laireiter
 */
public final class ContentDescriptor implements Comparable<ContentDescriptor>
{
    /**
     * This constant gives the common id (name) for the "album" field in an asf
     * extended content description.
     */
    public final static String ID_ALBUM = AsfFieldKey.ALBUM.getFieldId();

    /**
     * This constant gives the common id (name) for the "artist" field in an asf
     * extended content description.
     */
    public final static String ID_ARTIST = AsfFieldKey.ARTIST.getFieldId();

    /**
     * This constant gives the common id (name) for the "genre" field in an asf
     * extended content description.
     */
    public final static String ID_GENRE = AsfFieldKey.GENRE.getFieldId();

    /**
     * This constant gives the common id (name) for the "genre Id" field in an
     * asf extended content description.
     */
    public final static String ID_GENREID = AsfFieldKey.GENRE_ID.getFieldId();

    /**
     * This constant gives the common id (name) for the "track number" field in
     * an asf extended content description.
     */
    public final static String ID_TRACKNUMBER = AsfFieldKey.TRACK.getFieldId();

    /**
     * This constant gives the common id (name) for the "year" field in an asf
     * extended content description.
     */
    public final static String ID_YEAR = AsfFieldKey.YEAR.getFieldId();

    /**
     * Constant for the content descriptor-type for binary data.
     */
    public final static int TYPE_BINARY = 1;

    /**
     * Constant for the content descriptor-type for booleans.
     */
    public final static int TYPE_BOOLEAN = 2;

    /**
     * Constant for the content descriptor-type for integers (32-bit). <br>
     */
    public final static int TYPE_DWORD = 3;

    /**
     * Constant for the content descriptor-type for integers (64-bit). <br>
     */
    public final static int TYPE_QWORD = 4;

    /**
     * Constant for the content descriptor-type for Strings.
     */
    public final static int TYPE_STRING = 0;

    /**
     * Constant for the content descriptor-type for integers (16-bit). <br>
     */
    public final static int TYPE_WORD = 5;

    /**
     * The binary representation of the value.
     */
    protected byte[] content = new byte[0];

    /**
     * This field shows the type of the content descriptor. <br>
     *
     * @see #TYPE_BINARY
     * @see #TYPE_BOOLEAN
     * @see #TYPE_DWORD
     * @see #TYPE_QWORD
     * @see #TYPE_STRING
     * @see #TYPE_WORD
     */
    private int descriptorType;

    /**
     * The name of the content descriptor.
     */
    private final String name;

    /**
     * Creates an Instance.
     *
     * @param propName Name of the ContentDescriptor.
     * @param propType Type of the content descriptor. See {@link #descriptorType}
     */
    public ContentDescriptor(String propName, int propType)
    {
        if (propName == null)
        {
            throw new IllegalArgumentException("Arguments must not be null.");
        }
        Utils.checkStringLengthNullSafe(propName);
        this.name = propName;
        this.descriptorType = propType;
    }

    /**
     * (overridden)
     *
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException
    {
        return createCopy();
    }

    /**
     * (overridden)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ContentDescriptor o)
    {
        int result = getName().compareTo(o.getName());
        return result;
    }

    /**
     * This method creates a copy of the current object. <br>
     * All data will be copied, too. <br>
     *
     * @return A new Contentdescriptor containing the same values as the current
     *         one.
     */
    public ContentDescriptor createCopy()
    {
        ContentDescriptor result = new ContentDescriptor(getName(), getType());
        result.content = getRawData();
        return result;
    }

    /**
     * (overridden)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        boolean result = false;
        if (obj instanceof ContentDescriptor)
        {
            if (obj == this)
            {
                result = true;
            }
            else
            {
                ContentDescriptor other = (ContentDescriptor) obj;
                result = other.getName().equals(getName()) && other.descriptorType == this.descriptorType && Arrays.equals(this.content, other.content);
            }
        }
        return result;
    }

    /**
     * Returns the value of the ContentDescriptor as a Boolean. <br>
     * If no Conversion is Possible false is returned. <br>
     * <code>true</code> if first byte of {@link #content}is not zero.
     *
     * @return boolean representation of the current value.
     */
    public boolean getBoolean()
    {
        return content.length > 0 && content[0] != 0;
    }

    /**
     * This method will return a byte array, which can directly be written into
     * an "Extended Content Description"-chunk. <br>
     *
     * @return byte[] with the data, that occurs in asf files.
     */
    public byte[] getBytes()
    {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try
        {
            byte[] nameBytes = getName().getBytes(AsfHeader.ASF_CHARSET);
            // Write the number of bytes the name needs. +2 because of the
            // Zero term character.
            result.write(Utils.getBytes(nameBytes.length + 2, 2));
            // Write the name itself
            result.write(nameBytes);
            // Write zero term character
            result.write(Utils.getBytes(0, 2));
            // Write the type of the current descriptor
            result.write(Utils.getBytes(getType(), 2));
            /*
             * Now the content.
             */
            if (this.getType() == TYPE_STRING)
            {
                // String length +2 for zero termination
                result.write(Utils.getBytes(content.length + 2, 2));
                // Value
                result.write(content);
                // Zero term
                result.write(Utils.getBytes(0, 2));
            }
            else
            {
                result.write(Utils.getBytes(content.length, 2));
                result.write(content);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return result.toByteArray();
    }

    /**
     * Returns the size (in bytes) this descriptor will take when written to 
     * an ASF file.<br>
     * 
     * @return size of the descriptor in an ASF file.
     */
    public int getCurrentAsfSize()
    {
        /*
         * 2 bytes name length, 2 bytes name zero term, 2 bytes type, 2 bytes
         * content length
         */
        int result = 8;
        result += getName().length() * 2;
        result += this.content.length;
        if (TYPE_STRING == this.getType())
        {
            result += 2; // zero term of content string.
        }
        return result;
    }

    /**
     * This method returns the name of the content descriptor.
     *
     * @return Name.
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * This method returns the value of the content descriptor as an integer.
     * <br>
     * Converts the needed amount of byte out of {@link #content}to a number.
     * <br>
     * Only possible if {@link #getType()}equals on of the following: <br>
     * <li>
     *
     * @return integer value.
     * @see #TYPE_BOOLEAN </li>
     *      <li>
     * @see #TYPE_DWORD </li>
     *      <li>
     * @see #TYPE_QWORD </li>
     *      <li>
     * @see #TYPE_WORD </li>
     */
    public long getNumber()
    {
        long result = 0;
        int bytesNeeded = -1;
        switch (getType())
        {
            case TYPE_BOOLEAN:
                bytesNeeded = 1;
                break;
            case TYPE_DWORD:
                bytesNeeded = 4;
                break;
            case TYPE_QWORD:
                bytesNeeded = 8;
                break;
            case TYPE_WORD:
                bytesNeeded = 2;
                break;
            default:
                throw new UnsupportedOperationException("The current type doesn't allow an interpretation as a number.");
        }
        if (bytesNeeded > content.length)
        {
            throw new IllegalStateException("The stored data cannot represent the type of current object.");
        }
        for (int i = 0; i < bytesNeeded; i++)
        {
            result |= (content[i] << (i * 8));
        }
        return result;
    }

    /**
     * This method returns a copy of the content of the descriptor. <br>
     *
     * @return The content in binary representation, as it would be written to
     *         asf file. <br>
     */
    public byte[] getRawData()
    {
        byte[] copy = new byte[this.content.length];
        System.arraycopy(this.content, 0, copy, 0, this.content.length);
        return copy;
    }

    /**
     * Returns the value of the ContentDescriptor as a String. <br>
     *
     * @return String - Representation Value
     */
    public String getString()
    {
        String result = "";
        switch (getType())
        {
            case TYPE_BINARY:
                result = "binary data";
                break;
            case TYPE_BOOLEAN:
                result = String.valueOf(getBoolean());
                break;
            case TYPE_QWORD:
            case TYPE_DWORD:
            case TYPE_WORD:
                result = String.valueOf(getNumber());
                break;
            case TYPE_STRING:
                try
                {
                    result = new String(content, "UTF-16LE");
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;
            default:
                throw new IllegalStateException("Current type is not known.");
        }
        return result;
    }

    /**
     * Returns the type of the content descriptor. <br>
     *
     * @return the value of {@link #descriptorType}
     * @see #TYPE_BINARY
     * @see #TYPE_BOOLEAN
     * @see #TYPE_DWORD
     * @see #TYPE_QWORD
     * @see #TYPE_STRING
     * @see #TYPE_WORD
     */
    public int getType()
    {
        return this.descriptorType;
    }

    /**
     * This method checks whether the name of the current field is one of the
     * commonly specified fields. <br>
     *
     * @return <code>true</code> if a common field.
     * @see AsfFieldKey#isCommonField(String)
     */
    public boolean isCommon()
    {
        // TODO: We have at least to specifiy a list of common fields (somwhere in code)
        return false;
    }

    /**
     * This method checks if the binary data is empty. <br>
     * Disregarding the type of the descriptor its content is stored as a byte
     * array.
     *
     * @return <code>true</code> if no value is set.
     */
    public boolean isEmpty()
    {
        return this.content.length == 0;
    }

    /**
     * Sets the Value of the current content descriptor. <br>
     * Using this method will change {@link #descriptorType}to
     * {@link #TYPE_BINARY}.<br>
     *
     * @param data Value to set.
     * @throws IllegalArgumentException If the byte array is greater that 65535 bytes.
     */
    public void setBinaryValue(byte[] data) throws IllegalArgumentException
    {
        if (data.length > 65535)
        {
            throw new IllegalArgumentException("Too many bytes. 65535 is maximum.");
        }
        this.content = data;
        this.descriptorType = TYPE_BINARY;
    }

    /**
     * Sets the Value of the current content descriptor. <br>
     * Using this method will change {@link #descriptorType}to
     * {@link #TYPE_BOOLEAN}.<br>
     *
     * @param value Value to set.
     */
    public void setBooleanValue(boolean value)
    {
        this.content = new byte[]{value ? (byte) 1 : 0, 0, 0, 0};
        this.descriptorType = TYPE_BOOLEAN;
    }

    /**
     * Sets the Value of the current content descriptor. <br>
     * Using this method will change {@link #descriptorType}to
     * {@link #TYPE_DWORD}.
     *
     * @param value Value to set.
     */
    public void setDWordValue(long value)
    {
        this.content = Utils.getBytes(value, 4);
        this.descriptorType = TYPE_DWORD;
    }

    /**
     * Sets the Value of the current content descriptor. <br>
     * Using this method will change {@link #descriptorType}to
     * {@link #TYPE_QWORD}
     *
     * @param value Value to set.
     */
    public void setQWordValue(long value)
    {
        this.content = Utils.getBytes(value, 8);
        this.descriptorType = TYPE_QWORD;
    }

    /**
     * Sets the Value of the current content descriptor. <br>
     * Using this method will change {@link #descriptorType}to
     * {@link #TYPE_STRING}.
     *
     * @param value Value to set.
     * @throws IllegalArgumentException If byte representation would take more than 65535 Bytes.
     */
    public void setStringValue(String value) throws IllegalArgumentException
    {
        Utils.checkStringLengthNullSafe(value);
        if (value != null)
        {
            this.content = value.getBytes(AsfHeader.ASF_CHARSET);
        }
        else
        {
            this.content = new byte[0];
        }
        this.descriptorType = TYPE_STRING;
    }

    /**
     * Sets the Value of the current content descriptor. <br>
     * Using this method will change {@link #descriptorType}to
     * {@link #TYPE_WORD}
     *
     * @param value Value to set.
     */
    public void setWordValue(int value)
    {
        this.content = Utils.getBytes(value, 2);
        this.descriptorType = TYPE_WORD;
    }

    /**
     * (overridden)
     *
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return getName() + " : " + new String[]{"String: ", "Binary: ", "Boolean: ", "DWORD: ", "QWORD:", "WORD:"}[this.descriptorType] + getString();
    }

    /**
     * Writes this descriptor into the specified output stream.<br>
     * 
     * @param out stream to write into.
     * @return amount of bytes written.
     * @throws IOException on I/O Errors
     */
    public int writeInto(OutputStream out) throws IOException
    {
        final int size = getCurrentAsfSize();
        Utils.writeUINT16(getName().length() * 2 + 2, out);
        out.write(getName().getBytes(AsfHeader.ASF_CHARSET));
        out.write(AsfHeader.ZERO_TERM);
        final int type = getType();
        Utils.writeUINT16(type, out);
        int contentLen = this.content.length;
        if (TYPE_STRING == type)
        {
            contentLen += 2; // Zero Term
        }
        Utils.writeUINT16(contentLen, out);
        out.write(this.content);
        if (TYPE_STRING == type)
        {
            out.write(AsfHeader.ZERO_TERM);
        }
        return size;
    }
}