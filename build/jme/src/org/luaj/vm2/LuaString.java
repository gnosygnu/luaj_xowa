/*******************************************************************************
* Copyright (c) 2009-2011 Luaj.org. All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
******************************************************************************/
package org.luaj.vm2;


import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.luaj.vm2.lib.MathLib;
import org.luaj.vm2.lib.StringLib;

/**
 * Subclass of {@link LuaValue} for representing lua strings. 
 * <p>
 * Because lua string values are more nearly sequences of bytes than 
 * sequences of characters or unicode code points, the {@link LuaString}
 * implementation holds the string value in an internal byte array.  
 * <p>
 * {@link LuaString} values are generally not mutable once constructed, 
 * so multiple {@link LuaString} values can chare a single byte array.
 * <p>
 * Currently {@link LuaString}s are pooled via a centrally managed weak table.
 * To ensure that as many string values as possible take advantage of this, 
 * Constructors are not exposed directly.  As with number, booleans, and nil, 
 * instance construction should be via {@link LuaValue#valueOf(byte[])} or similar API.
 * <p>
 * When Java Strings are used to initialize {@link LuaString} data, the UTF8 encoding is assumed. 
 * The functions 
 * {@link LuaString#lengthAsUtf8(char[]),
 * {@link LuaString#encodeToUtf8(char[], int, byte[], int)}, and 
 * {@link LuaString#decodeAsUtf8(byte[], int, int) 
 * are used to convert back and forth between UTF8 byte arrays and character arrays.
 * 
 * @see LuaValue
 * @see LuaValue#valueOf(String)
 * @see LuaValue#valueOf(byte[])
 */
public class LuaString extends LuaValue {

	/** Size of cache of recent short strings. This is the maximum number of LuaStrings that 
	 * will be retained in the cache of recent short strings.  */
	public static final int RECENT_STRINGS_CACHE_SIZE = 128;

	/** Maximum length of a string to be considered for recent short strings caching. 
	 * This effectively limits the total memory that can be spent on the recent strings cache,
	 * ecause no LuaString whose backing exceeds this length will be put into the cache.  */
	public static final int RECENT_STRINGS_MAX_LENGTH = 32;

	/** The singleton instance representing lua {@code true} */
	public static LuaValue s_metatable;

	/** The bytes for the string */
	public final byte[] m_bytes;
	
	/** The offset into the byte array, 0 means start at the first byte */
	public final int    m_offset;
	
	/** The number of bytes that comprise this string */
	public final int    m_length;
	
	private static class Cache {
		/** Simple cache of recently created strings that are short.  
		 * This is simply a list of strings, indexed by their hash codes modulo the cache size 
		 * that have been recently constructed.  If a string is being constructed frequently 
		 * from different contexts, it will generally may show up as a cache hit and resolve 
		 * to the same value.  */
		public final LuaString recent_short_strings[] = new LuaString[RECENT_STRINGS_CACHE_SIZE];
		
		public LuaString get(LuaString s) {
			final int index = s.hashCode() & (RECENT_STRINGS_CACHE_SIZE - 1);
			final LuaString cached = (LuaString) recent_short_strings[index];
			if (cached != null && s.raweq(cached))
				return cached;
			recent_short_strings[index] = s;
			return s;
		}
		
		static final Cache instance = new Cache();
	}
	
	/**
	 * Get a {@link LuaString} instance whose bytes match 
	 * the supplied Java String using the UTF8 encoding. 
	 * @param string Java String containing characters to encode as UTF8
	 * @return {@link LuaString} with UTF8 bytes corresponding to the supplied String
	 */
	public static LuaString valueOf(String string) {
		char[] c = string.toCharArray();
		byte[] b = new byte[lengthAsUtf8(c)];
		encodeToUtf8(c, c.length, b, 0);
		return valueOf(b, 0, b.length);
	}

	public static LuaString valueOfCopy(byte[] bytes, int off, int len) {
		byte[] rv = new byte[len];
		for (int i = 0; i < len; i++)
			rv[i] = bytes[i + off];
		return valueOf(rv, 0, len);
	}
	// TODO: should this be deprecated or made private?
	/** Construct a {@link LuaString} around a byte array that may be used directly as the backing.
	 * <p>
	 * The array may be used as the backing for this object, so clients must not change contents.
	 * If the supplied value for 'len' is more than half the length of the container, the 
	 * supplied byte array will be used as the backing, otherwise the bytes will be copied to a
	 * new byte array, and cache lookup may be performed.
	 * <p>
	 * @param bytes byte buffer
	 * @param off offset into the byte buffer
	 * @param len length of the byte buffer
	 * @return {@link LuaString} wrapping the byte buffer
	 */
	public static LuaString valueOf(byte[] bytes, int off, int len) {
		if (bytes.length < RECENT_STRINGS_MAX_LENGTH) {
			// Short string.  Reuse the backing and check the cache of recent strings before returning.
			final LuaString s = new LuaString(bytes, off, len);
			return Cache.instance.get( s );
		} else if (len >= bytes.length / 2) {
			// Reuse backing only when more than half the bytes are part of the result.
			return new LuaString(bytes, off, len);
		} else {
			// Short result relative to the source.  Copy only the bytes that are actually to be used.
			final byte[] b = new byte[len];
			System.arraycopy(bytes, off, b, 0, len);
			return valueOf(b, 0, len);  // To possibly use cached version.
		}
	}
	
	/** Construct a {@link LuaString} using the supplied characters as byte values.
	 * <p>
	 * Only the low-order 8-bits of each character are used, the remainder is ignored. 
	 * <p>
	 * This is most useful for constructing byte sequences that do not conform to UTF8. 
	 * @param bytes array of char, whose values are truncated at 8-bits each and put into a byte array. 
	 * @return {@link LuaString} wrapping a copy of the byte buffer 
	 */
	public static LuaString valueOf(char[] bytes) {
		return valueOf(bytes, 0, bytes.length);
	}

	/** Construct a {@link LuaString} using the supplied characters as byte values.
	 * <p>
	 * Only the low-order 8-bits of each character are used, the remainder is ignored. 
	 * <p>
	 * This is most useful for constructing byte sequences that do not conform to UTF8. 
	 * @param bytes array of char, whose values are truncated at 8-bits each and put into a byte array. 
	 * @return {@link LuaString} wrapping a copy of the byte buffer 
	 */
	public static LuaString valueOf(char[] chars, int off, int len) {
		// COMMENTED: does not handle 2+ byte chars; assumes 1 char = 1 byte
		//	byte[] b = new byte[len];
		//	for ( int i=0; i<len; i++ )
		//		b[i] = (byte) chars[i + off];
		//	return valueOf(b, 0, len);
		
		// calc length of bry by looping char[] and summing length of each char
		int bry_len = 0;
		for (int i = 0; i < len; i++) {
			int b_len = LuaString.Utf16_Len_by_char((int)(chars[i + off]));
			if (b_len == 4) 
				++i;	// char is 4 bytes, so has 2-len (surrogate pair); skip next char;
			bry_len += b_len;  
		}
		byte[] bry = new byte[bry_len];

		// set bytes in byte[] by looping char[]
		int bry_idx = 0;
	    int i = 0;
	    while (i < len) {
	      char c = chars[i + off];
	      int b_len = Utf16_Encode_char(c, chars, i + off, bry, bry_idx); // XOWA: changed to "i + off" to get current position; DATE:2016-01-21; DATE:2017-03-23
	      bry_idx += b_len;
	      i += b_len == 4 ? 2 : 1;		// 4 bytes; surrogate pair; skip next char;
	    }
		return valueOf(bry, 0, bry_len);
	}
	

	/** Construct a {@link LuaString} around a byte array without copying the contents.
	 * <p>
	 * The array may be used directly as the backing, so clients must not change contents.
	 * <p>
	 * @param bytes byte buffer
	 * @return {@link LuaString} wrapping the byte buffer
	 */
	public static LuaString valueOf(byte[] bytes) {
		return valueOf(bytes, 0, bytes.length);
	}
	
	/** Construct a {@link LuaString} around a byte array without copying the contents.
	 * <p>
	 * The array is used directly after this is called, so clients must not change contents.
	 * <p>
	 * @param bytes byte buffer
	 * @param offset offset into the byte buffer
	 * @param length length of the byte buffer
	 * @return {@link LuaString} wrapping the byte buffer
	 */
	private LuaString(byte[] bytes, int offset, int length) {
		this.m_bytes = bytes;
		this.m_offset = offset;
		this.m_length = length;
	}

	public boolean isstring() {
		return true; 
	}
		
	public LuaValue getmetatable() { 
		return s_metatable; 
	}
	
	public int type() {
		return LuaValue.TSTRING;
	}

	public String typename() {
		return "string";
	}
	
	public String tojstring() {
		return decodeAsUtf8(m_bytes, m_offset, m_length);
	}

	// get is delegated to the string library
	public LuaValue get(LuaValue key) {
		return s_metatable!=null? gettable(this,key): StringLib.instance.get(key);
	}

	// unary operators
	public LuaValue neg() { double d = scannumber(); return Double.isNaN(d)? super.neg(): valueOf(-d); }

	// basic binary arithmetic
	public LuaValue   add( LuaValue rhs )      { double d = scannumber(); return Double.isNaN(d)? arithmt(ADD,rhs): rhs.add(d); }
	public LuaValue   add( double rhs )        { return valueOf( checkarith() + rhs ); }
	public LuaValue   add( int rhs )           { return valueOf( checkarith() + rhs ); }
	public LuaValue   sub( LuaValue rhs )      { double d = scannumber(); return Double.isNaN(d)? arithmt(SUB,rhs): rhs.subFrom(d); }
	public LuaValue   sub( double rhs )        { return valueOf( checkarith() - rhs ); }
	public LuaValue   sub( int rhs )           { return valueOf( checkarith() - rhs ); }
	public LuaValue   subFrom( double lhs )    { return valueOf( lhs - checkarith() ); }
	public LuaValue   mul( LuaValue rhs )      { double d = scannumber(); return Double.isNaN(d)? arithmt(MUL,rhs): rhs.mul(d); }
	public LuaValue   mul( double rhs )        { return valueOf( checkarith() * rhs ); }
	public LuaValue   mul( int rhs )           { return valueOf( checkarith() * rhs ); }
	public LuaValue   pow( LuaValue rhs )      { double d = scannumber(); return Double.isNaN(d)? arithmt(POW,rhs): rhs.powWith(d); }
	public LuaValue   pow( double rhs )        { return MathLib.dpow(checkarith(),rhs); }
	public LuaValue   pow( int rhs )           { return MathLib.dpow(checkarith(),rhs); }
	public LuaValue   powWith( double lhs )    { return MathLib.dpow(lhs, checkarith()); }
	public LuaValue   powWith( int lhs )       { return MathLib.dpow(lhs, checkarith()); }
	public LuaValue   div( LuaValue rhs )      { double d = scannumber(); return Double.isNaN(d)? arithmt(DIV,rhs): rhs.divInto(d); }
	public LuaValue   div( double rhs )        { return LuaDouble.ddiv(checkarith(),rhs); }
	public LuaValue   div( int rhs )           { return LuaDouble.ddiv(checkarith(),rhs); }
	public LuaValue   divInto( double lhs )    { return LuaDouble.ddiv(lhs, checkarith()); }
	public LuaValue   mod( LuaValue rhs )      { double d = scannumber(); return Double.isNaN(d)? arithmt(MOD,rhs): rhs.modFrom(d); }
	public LuaValue   mod( double rhs )        { return LuaDouble.dmod(checkarith(), rhs); }
	public LuaValue   mod( int rhs )           { return LuaDouble.dmod(checkarith(), rhs); }
	public LuaValue   modFrom( double lhs )    { return LuaDouble.dmod(lhs, checkarith()); }
	
	// relational operators, these only work with other strings
	public LuaValue   lt( LuaValue rhs )         { return rhs.strcmp(this)>0? LuaValue.TRUE: FALSE; }
	public boolean lt_b( LuaValue rhs )       { return rhs.strcmp(this)>0; }
	public boolean lt_b( int rhs )         { typerror("attempt to compare string with number"); return false; }
	public boolean lt_b( double rhs )      { typerror("attempt to compare string with number"); return false; }
	public LuaValue   lteq( LuaValue rhs )       { return rhs.strcmp(this)>=0? LuaValue.TRUE: FALSE; }
	public boolean lteq_b( LuaValue rhs )     { return rhs.strcmp(this)>=0; }
	public boolean lteq_b( int rhs )       { typerror("attempt to compare string with number"); return false; }
	public boolean lteq_b( double rhs )    { typerror("attempt to compare string with number"); return false; }
	public LuaValue   gt( LuaValue rhs )         { return rhs.strcmp(this)<0? LuaValue.TRUE: FALSE; }
	public boolean gt_b( LuaValue rhs )       { return rhs.strcmp(this)<0; }
	public boolean gt_b( int rhs )         { typerror("attempt to compare string with number"); return false; }
	public boolean gt_b( double rhs )      { typerror("attempt to compare string with number"); return false; }
	public LuaValue   gteq( LuaValue rhs )       { return rhs.strcmp(this)<=0? LuaValue.TRUE: FALSE; }
	public boolean gteq_b( LuaValue rhs )     { return rhs.strcmp(this)<=0; }
	public boolean gteq_b( int rhs )       { typerror("attempt to compare string with number"); return false; }
	public boolean gteq_b( double rhs )    { typerror("attempt to compare string with number"); return false; }

	// concatenation
	public LuaValue concat(LuaValue rhs)      { return rhs.concatTo(this); }
	public Buffer   concat(Buffer rhs)        { return rhs.concatTo(this); }
	public LuaValue concatTo(LuaNumber lhs)   { return concatTo(lhs.strvalue()); }
	public LuaValue concatTo(LuaString lhs)   { 
		byte[] b = new byte[lhs.m_length+this.m_length];
		System.arraycopy(lhs.m_bytes, lhs.m_offset, b, 0, lhs.m_length);
		System.arraycopy(this.m_bytes, this.m_offset, b, lhs.m_length, this.m_length);
		return valueOf(b, 0, b.length);
	}

	// string comparison 
	public int strcmp(LuaValue lhs)           { return -lhs.strcmp(this); }
	private static final Utf16_char lhs_tmp = new Utf16_char(), rhs_tmp = new Utf16_char();
	public int strcmp(LuaString rhs) {
		int lhs_idx = 0, rhs_idx = 0;
		while (lhs_idx < m_length && rhs_idx < rhs.m_length) {
			synchronized (lhs_tmp) {
				Utf16_Decode_to_int(    m_bytes,     m_offset + lhs_idx, lhs_tmp);
				Utf16_Decode_to_int(rhs.m_bytes, rhs.m_offset + rhs_idx, rhs_tmp);
				int comp = lhs_tmp.Codepoint - rhs_tmp.Codepoint;
				if (comp != 0)
					return comp;
				lhs_idx += lhs_tmp.Len;
				rhs_idx += rhs_tmp.Len;
			}
		}
		return m_length - rhs.m_length;
	}
	
	/** Check for number in arithmetic, or throw aritherror */
	private double checkarith() { 
		double d = scannumber();
		if ( Double.isNaN(d) ) 
			aritherror();
		return d;
	}
	
	public int checkint() {
		return (int) (long) checkdouble();
	}
	public LuaInteger checkinteger() {
		return valueOf(checkint());
	}
	public long checklong() {
		return (long) checkdouble();
	}
	public double checkdouble() {
		double d = scannumber();
		if ( Double.isNaN(d) )
			argerror("number");
		return d;
	}
	public LuaNumber checknumber() {
		return valueOf(checkdouble());
	}
	public LuaNumber checknumber(String msg) {
		double d = scannumber();
		if ( Double.isNaN(d) )
			error(msg);
		return valueOf(d);
	}

	public boolean isnumber() {
		double d = scannumber();
		return ! Double.isNaN(d);
	}
	
	public boolean isint() {
		double d = scannumber();
		if ( Double.isNaN(d) ) 
			return false;
		int i = (int) d;
		return i == d;
	}

	public boolean islong() { 
		double d = scannumber();
		if ( Double.isNaN(d) ) 
			return false;
		long l = (long) d;
		return l == d;
	}
	
	public byte    tobyte()        { return (byte) toint(); }
	public char    tochar()        { return (char) toint(); }
	public double  todouble()      { double d=scannumber(); return Double.isNaN(d)? 0: d; }
	public float   tofloat()       { return (float) todouble(); }
	public int     toint()         { return (int) tolong(); }
	public long    tolong()        { return (long) todouble(); }
	public short   toshort()       { return (short) toint(); }

	public double optdouble(double defval) {
		return checknumber().checkdouble();
	}
	
	public int optint(int defval) {
		return checknumber().checkint();
	}
	
	public LuaInteger optinteger(LuaInteger defval) { 
		return checknumber().checkinteger();
	}
	
	public long optlong(long defval) {
		return checknumber().checklong();
	}
	
	public LuaNumber optnumber(LuaNumber defval) {
		return checknumber().checknumber();
	}
	
	public LuaString optstring(LuaString defval) {
		return this; 
	}
	
	public LuaValue tostring() {
		return this;
	}

	public String optjstring(String defval) { 
		return tojstring(); 
	}
	
	public LuaString strvalue() {
		return this;
	}
	
	public LuaString substring( int beginIndex, int endIndex ) {
		return valueOf( m_bytes, m_offset + beginIndex, endIndex - beginIndex );
	}

//	private boolean hashcode_made = false;
//	private int hashcode_cache;
	public int hashCode() {
//		if (hashcode_made) return hashcode_cache;		
		int h = m_length;  /* seed */
		int step = (m_length>>5)+1;  /* if string is too long, don't hash all its chars */
		for (int l1=m_length; l1>=step; l1-=step)  /* compute hash */
		    h = h ^ ((h<<5)+(h>>2)+(((int) m_bytes[m_offset+l1-1] ) & 0x0FF ));
//		hashcode_made = true;
//		hashcode_cache = h;
		return h;
	}
	
	// object comparison, used in key comparison
	public boolean equals( Object o ) {
		if ( o instanceof LuaString ) {
			return raweq( (LuaString) o );
		}
		return false;
	}

	// equality w/ metatable processing
	public LuaValue eq( LuaValue val )    { return val.raweq(this)? TRUE: FALSE; }
	public boolean eq_b( LuaValue val )   { return val.raweq(this); }
	
	// equality w/o metatable processing
	public boolean raweq( LuaValue val ) {
		return val.raweq(this);
	}
	
	public boolean raweq( LuaString s ) { 
		if ( this == s )
			return true;
		if ( s.m_length != m_length )
			return false;
		if ( s.m_bytes == m_bytes && s.m_offset == m_offset )
			return true;
		// if (hashcode_made && s.hashcode_made && s.hashCode() != hashCode() )	// XOWA.PERF: skip hash check since full bry check is done below DATE:2014-08-07
		//	return false;
		byte[] comp_bytes = s.m_bytes;
		int comp_offset = s.m_offset;
		for ( int i=0; i<m_length; i++ )
			if ( comp_bytes[comp_offset+i] != m_bytes[m_offset+i] )
				return false;
		return true;
	}

	public static boolean equals( LuaString a, int i, LuaString b, int j, int n ) {
		return equals( a.m_bytes, a.m_offset + i, b.m_bytes, b.m_offset + j, n );
	}
	
	public static boolean equals( byte[] a, int i, byte[] b, int j, int n ) {
		if ( a.length < i + n || b.length < j + n )
			return false;
		while ( --n>=0 ) 
			if ( a[i++]!=b[j++] )
				return false;
		return true;
	}

	public void write(DataOutputStream writer, int i, int len) throws IOException {
		writer.write(m_bytes,m_offset+i,len);
	}
	
	public LuaValue len() {
		return LuaInteger.valueOf(m_length);
	}

	public int length() {
		return m_length;
	}

	public int rawlen() {
		return m_length;
	}

	public int luaByte(int index) {
		return m_bytes[m_offset + index] & 0x0FF;
	}

	public int charAt( int index ) {
		if ( index < 0 || index >= m_length )
			throw new IndexOutOfBoundsException();
		return luaByte( index );
	}
	
	public String checkjstring() { 
		return tojstring(); 
	}

	public LuaString checkstring() {
		return this;
	}
	
	/** Convert value to an input stream.
	 * 
	 * @return {@link InputStream} whose data matches the bytes in this {@link LuaString}
	 */
	public InputStream toInputStream() {
		return new ByteArrayInputStream(m_bytes, m_offset, m_length);
	}
	
	/**
	 * Copy the bytes of the string into the given byte array. 
	 * @param strOffset offset from which to copy
	 * @param bytes destination byte array
	 * @param arrayOffset offset in destination
	 * @param len number of bytes to copy
	 */
	public void copyInto( int strOffset, byte[] bytes, int arrayOffset, int len ) {
		System.arraycopy( m_bytes, m_offset+strOffset, bytes, arrayOffset, len );
	}
	
	/** Java version of strpbrk - find index of any byte that in an accept string.
	 * @param accept {@link LuaString} containing characters to look for.
	 * @return index of first match in the {@code accept} string, or -1 if not found.
	 */
	public int indexOfAny( LuaString accept ) {
		final int ilimit = m_offset + m_length;
		final int jlimit = accept.m_offset + accept.m_length;
		for ( int i = m_offset; i < ilimit; ++i ) {
			for ( int j = accept.m_offset; j < jlimit; ++j ) {
				if ( m_bytes[i] == accept.m_bytes[j] ) {
					return i - m_offset;
				}
			}
		}
		return -1;
	}
	
	/**
	 * Find the index of a byte starting at a point in this string
	 * @param b the byte to look for
	 * @param start the first index in the string
	 * @return index of first match found, or -1 if not found.
	 */
	public int indexOf( byte b, int start ) {
		for ( int i=start; i < m_length; ++i ) {
			if ( m_bytes[m_offset+i] == b )
				return i;
		}
		return -1;
	}
	
	/**
	 * Find the index of a string starting at a point in this string
	 * @param s the string to search for
	 * @param start the first index in the string
	 * @return index of first match found, or -1 if not found.
	 */
	public int indexOf( LuaString s, int start ) {
		final int slen = s.length();
		final int limit =  m_length - slen;
		for ( int i=start; i <= limit; ++i ) {
			if ( equals( m_bytes, m_offset+i, s.m_bytes, s.m_offset, slen ) )
				return i;
		}
		return -1;
	}
	
	/**
	 * Find the last index of a string in this string
	 * @param s the string to search for
	 * @return index of last match found, or -1 if not found.
	 */
	public int lastIndexOf( LuaString s ) {
		final int slen = s.length();
		final int limit =  m_length - slen;
		for ( int i=limit; i >= 0; --i ) {
			if ( equals( m_bytes, m_offset+i, s.m_bytes, s.m_offset, slen ) )
				return i;
		}
		return -1;
	}


	/**
	 * Convert to Java String interpreting as utf8 characters. 
	 * 
	 * @param bytes byte array in UTF8 encoding to convert
	 * @param offset starting index in byte array
	 * @param length number of bytes to convert
	 * @return Java String corresponding to the value of bytes interpreted using UTF8 
	 * @see #lengthAsUtf8(char[])
	 * @see #encodeToUtf8(char[], int, byte[], int)
	 * @see #isValidUtf8()
	 */
	public static String decodeAsUtf8(byte[] bytes, int offset, int length) {
		// COMMENTED: does not handle 3+ byte chars
//		int i,j,n,b;
//		for ( i=offset,j=offset+length,n=0; i<j; ++n ) {
//			switch ( 0xE0 & bytes[i++] ) {
//			case 0xE0: ++i;
//			case 0xC0: ++i;
//			}
//		}
//		char[] chars=new char[n];
//		for ( i=offset,j=offset+length,n=0; i<j; ) {
//			chars[n++] = (char) (
//				((b=bytes[i++])>=0||i>=j)? b:
//				(b<-32||i+1>=j)? (((b&0x3f) << 6) | (bytes[i++]&0x3f)):
//					(((b&0xf) << 12) | ((bytes[i++]&0x3f)<<6) | (bytes[i++]&0x3f)));
//		}
//		return new String(chars);
		return new String(bytes, offset, length, java.nio.charset.Charset.forName("UTF-8"));
	}
	
	/**
	 * Count the number of bytes required to encode the string as UTF-8.
	 * @param chars Array of unicode characters to be encoded as UTF-8
	 * @return count of bytes needed to encode using UTF-8
	 * @see #encodeToUtf8(char[], int, byte[], int)
	 * @see #decodeAsUtf8(byte[], int, int)
	 * @see #isValidUtf8()
	 */
	public static int lengthAsUtf8(char[] chars) {
		// COMMENTED: does not handle 3+ byte chars
//		int i,b;
//		char c;
//		for ( i=b=chars.length; --i>=0; )
//			if ( (c=chars[i]) >=0x80 )
//				b += (c>=0x800)? 2: 1;
//		return b;
		int len = chars.length;
		int rv = 0;
		for (int i = 0; i < len; i++) {
			int c = chars[i];	// XOWA.PERF: inlined function per VisualVM; DATE:2014-08-08
			if		(	(c >    -1)
				 	&& 	(c <   128))	rv += 1;		// 1 <<	7
			else if (	(c <  2048))	rv += 2;		// 1 << 11
			else if	(	(c > 55295)						// 0xD800
					&&	(c < 56320)) {	rv += 4;		// 0xDFFF
										++i;
			}
			else if (	(c < 65536))	rv += 3;		// 1 << 16			
		}
		return rv;
	}
	
	/**
	 * Encode the given Java string as UTF-8 bytes, writing the result to bytes
	 * starting at offset. 
	 * <p>
	 * The string should be measured first with lengthAsUtf8
	 * to make sure the given byte array is large enough.
	 * @param chars Array of unicode characters to be encoded as UTF-8
	 * @param nchars Number of characters in the array to convert.
	 * @param bytes byte array to hold the result
	 * @param off offset into the byte array to start writing
	 * @return number of bytes converted.
	 * @see #lengthAsUtf8(char[])
	 * @see #decodeAsUtf8(byte[], int, int)
	 * @see #isValidUtf8()
	 */
	public static int encodeToUtf8(char[] chars, int nchars, byte[] bytes, int off) {
		// COMMENTED: does not handle 4+ byte chars; already using Encode_by_int, so might as well be consistent
//		char c;
//		int j = off;
//		for ( int i=0; i<nchars; i++ ) {
//			if ( (c = chars[i]) < 0x80 ) {
//				bytes[j++] = (byte) c;
//			} else if ( c < 0x800 ) {
//				bytes[j++] = (byte) (0xC0 | ((c>>6)  & 0x1f));
//				bytes[j++] = (byte) (0x80 | ( c      & 0x3f));				
//			} else {
//				bytes[j++] = (byte) (0xE0 | ((c>>12) & 0x0f));
//				bytes[j++] = (byte) (0x80 | ((c>>6)  & 0x3f));
//				bytes[j++] = (byte) (0x80 | ( c      & 0x3f));				
//			}
//		}
//		return j - off;
		int bry_idx = off;
	    int i = 0;
	    while (i < nchars) {
			char c = chars[i];	// XOWA.PERF: inlined function per VisualVM; DATE:2014-08-08 			
			if (c > -1 && c < 128) {
				bytes[bry_idx++]	= (byte)c;
				++i;
			}
			else if (c < 2048) {
				bytes[bry_idx++] 	= (byte)(0xC0 | (c >>   6));
				bytes[bry_idx++] 	= (byte)(0x80 | (c & 0x3F));
				++i;
			}	
			else if((c > 55295)				// 0xD800
			   && (c < 56320)) {			// 0xDFFF
				if (i >= nchars)
					throw new RuntimeException("incomplete surrogate pair at end of string; char=" + c);
				int nxt_char = chars[i + 1];
				int v = Utf16_Surrogate_merge(c, nxt_char);
				bytes[bry_idx++] 	= (byte)(0xF0 | (v >> 18));
				bytes[bry_idx++] 	= (byte)(0x80 | (v >> 12) & 0x3F);
				bytes[bry_idx++] 	= (byte)(0x80 | (v >>  6) & 0x3F);
				bytes[bry_idx++] 	= (byte)(0x80 | (v        & 0x3F));
				i += 2;
			}
			else {
				bytes[bry_idx++] 	= (byte)(0xE0 | (c >> 12));
				bytes[bry_idx++] 	= (byte)(0x80 | (c >>  6) & 0x3F);
				bytes[bry_idx++] 	= (byte)(0x80 | (c        & 0x3F));
				++i;
			}
	    }
		return nchars;	// NOTE: code returned # of bytes which is wrong; Globals.UTF8Stream.read caches rv as j which is used as index to char[] not byte[]; will throw out of bounds exception if bytes returned
	}

	/** Check that a byte sequence is valid UTF-8
	 * @return true if it is valid UTF-8, otherwise false
	 * @see #lengthAsUtf8(char[])
	 * @see #encodeToUtf8(char[], int, byte[], int)
	 * @see #decodeAsUtf8(byte[], int, int)
	 */
	public boolean isValidUtf8() {
		// NOTE: this should be changed to handle 4+ bytes, but only called by lua <-> java converter which XOWA does not use
		int i,j,n=0;
		for ( i=m_offset,j=m_offset+m_length,n=0; i<j; ++n ) {
			int c = m_bytes[i++];
			if ( c >= 0 ) continue;
			if ( ((c & 0xE0) == 0xC0) 
					&& i<j 
					&& (m_bytes[i++] & 0xC0) == 0x80) continue;
			if ( ((c & 0xF0) == 0xE0) 
					&& i+1<j 
					&& (m_bytes[i++] & 0xC0) == 0x80 
					&& (m_bytes[i++] & 0xC0) == 0x80) continue;
			return false;
		}
		return true;
	}
	
	// --------------------- number conversion -----------------------
	
	/** 
	 * convert to a number using baee 10 or base 16 if it starts with '0x', 
	 * or NIL if it can't be converted
	 * @return IntValue, DoubleValue, or NIL depending on the content of the string.
	 * @see LuaValue#tonumber() 
	 */
	public LuaValue tonumber() {
		double d = scannumber();
		return Double.isNaN(d)? NIL: valueOf(d);
	}
	
	/** 
	 * convert to a number using a supplied base, or NIL if it can't be converted
	 * @param base the base to use, such as 10
	 * @return IntValue, DoubleValue, or NIL depending on the content of the string.
	 * @see LuaValue#tonumber() 
	 */
	public LuaValue tonumber( int base ) {
		double d = scannumber( base );
		return Double.isNaN(d)? NIL: valueOf(d);
	}
	
	/** 
	 * Convert to a number in base 10, or base 16 if the string starts with '0x', 
	 * or return Double.NaN if it cannot be converted to a number.
	 * @return double value if conversion is valid, or Double.NaN if not 
	 */
	public double scannumber() {
		int i = m_offset, j = m_offset + m_length;
		
		// XOWA:trim ws
		int idx = i;
		while (idx < j) {
			switch (m_bytes[idx]) {
				case 9: case 10: case 13: case 32:
					++idx;
					i = idx;
					break;
				default:
					idx = j;
					break;
			}
		}
		idx = j - 1;
		while (idx >= i) {
			switch (m_bytes[idx]) {
				case 9: case 10: case 13: case 32:
					j = idx;
					--idx;
					break;
				default:
					idx = i -1;
					break;
			}
		}
//		while ( i<j && m_bytes[i]==' ' ) ++i;
//		while ( i<j && m_bytes[j-1]==' ' ) --j;
		if (i >= j)
			return Double.NaN;
		if ( 	i + 1 < j
			&& 	m_bytes[i] == '0'
			) {
			byte next_byte = m_bytes[i+1];
			if (next_byte == 'x' || next_byte == 'X') {
				return i + 2 < j	// XOWA: bounds check; DATE:2014-08-26
					? scanlong(16, i+2, j)
					: Double.NaN 
					;
			}
		}
		double l = scanlong(10, i, j);
		return Double.isNaN(l)? scandouble(i,j): l;
	}
	
	/** 
	 * Convert to a number in a base, or return Double.NaN if not a number.
	 * @param base the base to use between 2 and 36
	 * @return double value if conversion is valid, or Double.NaN if not 
	 */
	public double scannumber(int base) {
		if ( base < 2 || base > 36 )
			return Double.NaN;
		int i=m_offset,j=m_offset+m_length;
		while ( i<j && m_bytes[i]==' ' ) ++i;
		while ( i<j && m_bytes[j-1]==' ' ) --j;
		if ( i>=j )
			return Double.NaN;
		return scanlong( base, i, j );
	}
	
	/**
	 * Scan and convert a long value, or return Double.NaN if not found.
	 * @param base the base to use, such as 10
	 * @param start the index to start searching from
	 * @param end the first index beyond the search range
	 * @return double value if conversion is valid, 
	 * or Double.NaN if not
	 */
	private double scanlong( int base, int start, int end ) {
		long x = 0;
		boolean neg = (m_bytes[start] == '-');

		// XOWA:if "-" only, return "nil", not "0"; EX:tonumber('-'); DATE:2017-12-05
		int bgn = neg?start+1:start;
		if (neg && bgn == end) {
			return Double.NaN;
		}
		
		for ( int i=bgn; i<end; i++ ) {
			int digit = m_bytes[i] - (base<=10||(m_bytes[i]>='0'&&m_bytes[i]<='9')? '0':
					m_bytes[i]>='A'&&m_bytes[i]<='Z'? ('A'-10): ('a'-10));
			if ( digit < 0 || digit >= base )
				return Double.NaN;		
			x = x * base + digit;
			if ( x < 0 )
				return Double.NaN; // overflow
		}
		return neg? -x: x;
	}
	
	/**
	 * Scan and convert a double value, or return Double.NaN if not a double.
	 * @param start the index to start searching from
	 * @param end the first index beyond the search range
	 * @return double value if conversion is valid, 
	 * or Double.NaN if not
	 */
	private double scandouble(int start, int end) {
		if ( end>start+64 ) end=start+64;
		for ( int i=start; i<end; i++ ) {
			switch ( m_bytes[i] ) {
			case '-':
			case '+':
			case '.':
			case 'e': case 'E':
			case '0': case '1': case '2': case '3': case '4':
			case '5': case '6': case '7': case '8': case '9':
				break;
			default:
				return Double.NaN;
			}
		}
		char [] c = new char[end-start];
		for ( int i=start; i<end; i++ )
			c[i-start] = (char) m_bytes[i];
		try {
			return Double.parseDouble(new String(c));
		} catch ( Exception e ) {			
			return Double.NaN;
		}
	}

	/**
	 * Print the bytes of the LuaString to a PrintStream as if it were
	 * an ASCII string, quoting and escaping control characters.
	 * @param ps PrintStream to print to.
	 */
	public void printToStream(PrintStream ps) {
		for (int i = 0, n = m_length; i < n; i++) {
			int c = m_bytes[m_offset+i];
			ps.print((char) c);
		}
	}
	public static int Utf16_Len_by_char(int c) {
		if	   ((c >       -1)
			 && (c <      128))	return 1;		// 1 <<  7
		else if (c <     2048)	return 2;		// 1 << 11
		else if((c > 55295)						// 0xD800
			 && (c < 56320))	
			return 4;		// 0xDFFF
		else if (c <   65536)	return 3;		// 1 << 16
		else throw new RuntimeException("UTF-16 int must be between 0 and 2097152; char=" + c);
	}
	public static int Utf16_Len_by_int(int c) {
		if	   ((c >       -1)
			 && (c <      128))	return 1;		// 1 <<  7
		else if (c <     2048)	return 2;		// 1 << 11
		else if (c <   65536)	return 3;		// 1 << 16
		else if (c < 2097152)	return 4;
		else throw new RuntimeException("UTF-16 int must be between 0 and 2097152; char=" + c);
	}
	public static int Utf8_Len_of_char_by_1st_byte(byte b) {// SEE:w:UTF-8
		int i = b & 0xff;	// PATCH.JAVA:need to convert to unsigned byte
		switch (i) {
			case   0: case   1: case   2: case   3: case   4: case   5: case   6: case   7: case   8: case   9: case  10: case  11: case  12: case  13: case  14: case  15: 
			case  16: case  17: case  18: case  19: case  20: case  21: case  22: case  23: case  24: case  25: case  26: case  27: case  28: case  29: case  30: case  31: 
			case  32: case  33: case  34: case  35: case  36: case  37: case  38: case  39: case  40: case  41: case  42: case  43: case  44: case  45: case  46: case  47: 
			case  48: case  49: case  50: case  51: case  52: case  53: case  54: case  55: case  56: case  57: case  58: case  59: case  60: case  61: case  62: case  63: 
			case  64: case  65: case  66: case  67: case  68: case  69: case  70: case  71: case  72: case  73: case  74: case  75: case  76: case  77: case  78: case  79: 
			case  80: case  81: case  82: case  83: case  84: case  85: case  86: case  87: case  88: case  89: case  90: case  91: case  92: case  93: case  94: case  95: 
			case  96: case  97: case  98: case  99: case 100: case 101: case 102: case 103: case 104: case 105: case 106: case 107: case 108: case 109: case 110: case 111: 
			case 112: case 113: case 114: case 115: case 116: case 117: case 118: case 119: case 120: case 121: case 122: case 123: case 124: case 125: case 126: case 127:
			case 128: case 129: case 130: case 131: case 132: case 133: case 134: case 135: case 136: case 137: case 138: case 139: case 140: case 141: case 142: case 143: 
			case 144: case 145: case 146: case 147: case 148: case 149: case 150: case 151: case 152: case 153: case 154: case 155: case 156: case 157: case 158: case 159: 
			case 160: case 161: case 162: case 163: case 164: case 165: case 166: case 167: case 168: case 169: case 170: case 171: case 172: case 173: case 174: case 175: 
			case 176: case 177: case 178: case 179: case 180: case 181: case 182: case 183: case 184: case 185: case 186: case 187: case 188: case 189: case 190: case 191: 
				return 1;
			case 192: case 193: case 194: case 195: case 196: case 197: case 198: case 199: case 200: case 201: case 202: case 203: case 204: case 205: case 206: case 207: 
			case 208: case 209: case 210: case 211: case 212: case 213: case 214: case 215: case 216: case 217: case 218: case 219: case 220: case 221: case 222: case 223: 
				return 2;
			case 224: case 225: case 226: case 227: case 228: case 229: case 230: case 231: case 232: case 233: case 234: case 235: case 236: case 237: case 238: case 239: 
				return 3;
			case 240: case 241: case 242: case 243: case 244: case 245: case 246: case 247:
				return 4;
			default: throw new RuntimeException("invalid initial utf8 byte; byte=" + b);
		}
	}
	static class Utf16_char {
		public int Codepoint;
		public int Len;
	} 
	public static void Utf16_Decode_to_int(byte[] ary, int pos, Utf16_char c) {
		byte b0 = ary[pos];
		if 		((b0 & 0x80) == 0) {
			c.Len = 1;
			c.Codepoint = b0;
		}
		else if ((b0 & 0xE0) == 0xC0) {
			c.Len = 2;
			c.Codepoint = ( b0           & 0x1f) <<  6
				| 	( ary[pos + 1] & 0x3f)
				;			
		}
		else if ((b0 & 0xF0) == 0xE0) {
			c.Len = 3;
			c.Codepoint = ( b0           & 0x0f) << 12
				| 	((ary[pos + 1] & 0x3f) <<  6)
				| 	( ary[pos + 2] & 0x3f)
				;			
		}
		else if ((b0 & 0xF8) == 0xF0) {
			c.Len = 4;
			c.Codepoint = ( b0           & 0x07) << 18
				| 	((ary[pos + 1] & 0x3f) << 12)
				| 	((ary[pos + 2] & 0x3f) <<  6)
				| 	( ary[pos + 3] & 0x3f)
				;			
		}
		else throw new RuntimeException("invalid utf8 byte: byte=" + b0);
	}
	public static int Utf16_Decode_to_int(byte[] ary, int pos) {
		byte b0 = ary[pos];
		if 		((b0 & 0x80) == 0) {
			return  b0;			
		}
		else if ((b0 & 0xE0) == 0xC0) {
			return  ( b0           & 0x1f) <<  6
				| 	( ary[pos + 1] & 0x3f)
				;			
		}
		else if ((b0 & 0xF0) == 0xE0) {
			return  ( b0           & 0x0f) << 12
				| 	((ary[pos + 1] & 0x3f) <<  6)
				| 	( ary[pos + 2] & 0x3f)
				;			
		}
		else if ((b0 & 0xF8) == 0xF0) {
			return  ( b0           & 0x07) << 18
				| 	((ary[pos + 1] & 0x3f) << 12)
				| 	((ary[pos + 2] & 0x3f) <<  6)
				| 	( ary[pos + 3] & 0x3f)
				;			
		}
		else throw new RuntimeException("invalid utf8 byte: byte=" + b0);
	}
	public static int Utf16_Encode_int(int c, byte[] src, int pos) {
		if	   ((c > -1)
			 && (c < 128)) {
			src[  pos]	= (byte)c;
			return 1;
		}
		else if (c < 2048) {
			src[  pos] 	= (byte)(0xC0 | (c >>   6));
			src[++pos] 	= (byte)(0x80 | (c & 0x3F));
			return 2;
		}	
		else if (c < 65536) {
			src[pos] 	= (byte)(0xE0 | (c >> 12));
			src[++pos] 	= (byte)(0x80 | (c >>  6) & 0x3F);
			src[++pos] 	= (byte)(0x80 | (c        & 0x3F));
			return 3;
		}
		else if (c < 2097152) {
			src[pos] 	= (byte)(0xF0 | (c >> 18));
			src[++pos] 	= (byte)(0x80 | (c >> 12) & 0x3F);
			src[++pos] 	= (byte)(0x80 | (c >>  6) & 0x3F);
			src[++pos] 	= (byte)(0x80 | (c        & 0x3F));
			return 4;
		}
		else throw new RuntimeException("UTF-16 int must be between 0 and 2097152; char=" + c);
	}
	public static int Utf16_Encode_char(int c, char[] c_ary, int c_pos, byte[] b_ary, int b_pos) {
		if	   ((c >   -1)
			 && (c < 128)) {
			b_ary[  b_pos]	= (byte)c;
			return 1;
		}
		else if (c < 2048) {
			b_ary[  b_pos] 	= (byte)(0xC0 | (c >>   6));
			b_ary[++b_pos] 	= (byte)(0x80 | (c & 0x3F));
			return 2;
		}	
		else if((c > 55295)				// 0xD800
			 && (c < 56320)) {			// 0xDFFF
			if (c_pos >= c_ary.length)
				throw new RuntimeException("incomplete surrogate pair at end of string; char=" + c);
			int nxt_char = c_ary[c_pos + 1];
			int v = Utf16_Surrogate_merge(c, nxt_char);
			b_ary[b_pos] 	= (byte)(0xF0 | (v >> 18));
			b_ary[++b_pos] 	= (byte)(0x80 | (v >> 12) & 0x3F);
			b_ary[++b_pos] 	= (byte)(0x80 | (v >>  6) & 0x3F);
			b_ary[++b_pos] 	= (byte)(0x80 | (v        & 0x3F));
			return 4;
		}
		else {
			b_ary[b_pos] 	= (byte)(0xE0 | (c >> 12));
			b_ary[++b_pos] 	= (byte)(0x80 | (c >>  6) & 0x3F);
			b_ary[++b_pos] 	= (byte)(0x80 | (c        & 0x3F));
			return 3;
		}
	}
	public static int Utf16_Encode_char(int c, int nxt_char, byte[] b_ary, int b_pos) {
		if	   ((c >   -1)
			 && (c < 128)) {
			b_ary[  b_pos]	= (byte)c;
			return 1;
		}
		else if (c < 2048) {
			b_ary[  b_pos] 	= (byte)(0xC0 | (c >>   6));
			b_ary[++b_pos] 	= (byte)(0x80 | (c & 0x3F));
			return 2;
		}	
		else if((c > 55295)				// 0xD800
			 && (c < 56320)) {			// 0xDFFF
			int v = Utf16_Surrogate_merge(c, nxt_char);
			b_ary[b_pos] 	= (byte)(0xF0 | (v >> 18));
			b_ary[++b_pos] 	= (byte)(0x80 | (v >> 12) & 0x3F);
			b_ary[++b_pos] 	= (byte)(0x80 | (v >>  6) & 0x3F);
			b_ary[++b_pos] 	= (byte)(0x80 | (v        & 0x3F));
			return 4;
		}
		else {
			b_ary[b_pos] 	= (byte)(0xE0 | (c >> 12));
			b_ary[++b_pos] 	= (byte)(0x80 | (c >>  6) & 0x3F);
			b_ary[++b_pos] 	= (byte)(0x80 | (c        & 0x3F));
			return 3;
		}
	}
	public static int Utf16_Encode_char_ascii_skip(int c, char[] c_ary, int c_pos, byte[] b_ary, int b_pos) {
		if (c < 2048) {
			b_ary[  b_pos] 	= (byte)(0xC0 | (c >>   6));
			b_ary[++b_pos] 	= (byte)(0x80 | (c & 0x3F));
			return 2;
		}	
		else if((c > 55295)				// 0xD800
			 && (c < 56320)) {			// 0xDFFF
			if (c_pos >= c_ary.length)
				throw new RuntimeException("incomplete surrogate pair at end of string; char=" + c);
			int nxt_char = c_ary[c_pos + 1];
			int v = Utf16_Surrogate_merge(c, nxt_char);
			b_ary[b_pos] 	= (byte)(0xF0 | (v >> 18));
			b_ary[++b_pos] 	= (byte)(0x80 | (v >> 12) & 0x3F);
			b_ary[++b_pos] 	= (byte)(0x80 | (v >>  6) & 0x3F);
			b_ary[++b_pos] 	= (byte)(0x80 | (v        & 0x3F));
			return 4;
		}
		else {
			b_ary[b_pos] 	= (byte)(0xE0 | (c >> 12));
			b_ary[++b_pos] 	= (byte)(0x80 | (c >>  6) & 0x3F);
			b_ary[++b_pos] 	= (byte)(0x80 | (c        & 0x3F));
			return 3;
		}
	}
	private static int Utf16_Surrogate_merge(int hi, int lo) { // REF: http://perldoc.perl.org/Encode/Unicode.html
		return 0x10000 + (hi - 0xD800) * 0x400 + (lo - 0xDC00);
	}
	public static void Utf16_Surrogate_split(int v, int[] tmp_ary) {
		tmp_ary[0] = ((v - 0x10000) / 0x400 + 0xD800);
		tmp_ary[1] = ((v - 0x10000) % 0x400 + 0xDC00);
	}
}
