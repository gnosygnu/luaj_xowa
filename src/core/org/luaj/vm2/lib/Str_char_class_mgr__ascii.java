package org.luaj.vm2.lib;

public class Str_char_class_mgr__ascii extends Str_char_class_mgr {
	@Override public boolean Match_class(int cp, int cls) {
		final int cls_lower = cls < 97 ? cls + 32 : cls;
		int char_data = cp == -1 ? 0 : cp > 128 ? 0 : CHAR_TABLE[cp];	// XOWA: handle cp == -1 else bounds error; note that match passes in -1 deliberately: "int previous = (soffset == 0) ? -1..."; DATE:2014-08-14 

		boolean res;
		switch (cls_lower) {
			case CLASS_ALPHA: res = (char_data & MASK_ALPHA) != 0; break;
			case CLASS_DIGIT: res = (char_data & MASK_DIGIT) != 0; break;
			case CLASS_LOWER: res = (char_data & MASK_LOWERCASE) != 0; break;
			case CLASS_UPPER: res = (char_data & MASK_UPPERCASE) != 0; break;
			case CLASS_CTRL : res = (char_data & MASK_CONTROL) != 0; break;
			case CLASS_PUNCT: res = (char_data & MASK_PUNCT) != 0; break;
			case CLASS_SPACE: res = (char_data & MASK_SPACE) != 0; break;
			case CLASS_WORD : res = (char_data & (MASK_ALPHA | MASK_DIGIT)) != 0; break;
			case CLASS_HEX  : res = (char_data & MASK_HEXDIGIT) != 0; break;
			case CLASS_NULL : res = (cp == 0); break;
			default: return cls == cp;
		}
		return (cls_lower == cls) ? res : !res;
	}
	private static final byte MASK_ALPHA		= 0x01;
	private static final byte MASK_LOWERCASE	= 0x02;
	private static final byte MASK_UPPERCASE	= 0x04;
	private static final byte MASK_DIGIT		= 0x08;
	private static final byte MASK_PUNCT		= 0x10;
	private static final byte MASK_SPACE		= 0x20;
	private static final byte MASK_CONTROL		= 0x40;
	private static final byte MASK_HEXDIGIT		= (byte)0x80;

	// lookup table for quick isalpha, islower, etc
	public static final byte[] CHAR_TABLE = makeCharTable();
	private static byte[] makeCharTable() {
		byte[] rv = new byte[256];
		for ( int i = 0; i < 128; ++i ) {	// XOWA: was "i < 256"; NOTE: either lua C defines isalpha as ASCII or Wikimedia uses English locale lua binaries; DATE:2016-04-17
			final char c = (char) i;
			rv[i] = (byte) ((Character.isDigit(c)     ? MASK_DIGIT     : 0)
							       |(Character.isLowerCase(c) ? MASK_LOWERCASE : 0)
							       |(Character.isUpperCase(c) ? MASK_UPPERCASE : 0)
							       |((c < ' ' || c == 0x7F)   ? MASK_CONTROL   : 0)
							       );
			if  (  (c >= 'a' && c <= 'f')
			    || (c >= 'A' && c <= 'F')
			    || (c >= '0' && c <= '9')
			    ) {
				rv[i] |= MASK_HEXDIGIT;
			}
			if  (	(c >= '!' && c <= '/') 
				||	(c >= ':' && c <= '@')
				) {
				rv[i] |= MASK_PUNCT;
			}
			if  ((rv[i] & (MASK_LOWERCASE | MASK_UPPERCASE)) != 0) {
				rv[i] |= MASK_ALPHA;
			}
		}

		rv[' ']   = MASK_SPACE;
		rv['\r'] |= MASK_SPACE;
		rv['\n'] |= MASK_SPACE;
		rv['\t'] |= MASK_SPACE;
		rv[0x0C] |= MASK_SPACE;  // '\v'
		rv['\f'] |= MASK_SPACE;
		
		return rv;
	};
}
