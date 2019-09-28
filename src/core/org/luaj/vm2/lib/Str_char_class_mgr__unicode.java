package org.luaj.vm2.lib;

public class Str_char_class_mgr__unicode extends Str_char_class_mgr {
	@Override public boolean Match_class(int cp, int cls) {
		final int cls_lower = cls < 97 ? cls + 32 : cls;
		boolean res;
		int char_type;
		switch (cls_lower) { 
			case CLASS_ALPHA: // "\\p{L}"; REF:https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/master/jdk/src/share/classes/java/util/regex/Pattern.java#L5635-L5639 
				char_type = Character.getType(cp);
				switch (char_type) {
					case Character.UPPERCASE_LETTER:
					case Character.LOWERCASE_LETTER:
					case Character.TITLECASE_LETTER:
					case Character.MODIFIER_LETTER:
					case Character.OTHER_LETTER:
						res = true;
						break;
					default:
						res = false;
						break;
				}
				break;
			case CLASS_CTRL: // "\\p{Cc}"; REF:https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/master/jdk/src/share/classes/java/util/regex/Pattern.java#L5620
				char_type = Character.getType(cp);
				res = char_type == Character.CONTROL;
				break;
			case CLASS_DIGIT: // "\\p{Nd}"; REF:https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/master/jdk/src/share/classes/java/util/regex/Pattern.java#L5614
				char_type = Character.getType(cp);
				switch (char_type) {
					case Character.DECIMAL_DIGIT_NUMBER:
					case Character.LETTER_NUMBER:
					case Character.OTHER_NUMBER:
						res = true;
						break;
					default:
						res = false;
						break;
				}
				break;
			case CLASS_LOWER: // "\\p{Ll}"; REF:https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/master/jdk/src/share/classes/java/util/regex/Pattern.java#L5607
				char_type = Character.getType(cp);
				res = char_type == Character.LOWERCASE_LETTER;
				break;
			case CLASS_PUNCT: // "\\p{P}"; REF:https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/master/jdk/src/share/classes/java/util/regex/Pattern.java#L5653-L5659
				char_type = Character.getType(cp);
				switch (char_type) {
					case Character.DASH_PUNCTUATION:
					case Character.START_PUNCTUATION:
					case Character.END_PUNCTUATION:
					case Character.CONNECTOR_PUNCTUATION:
					case Character.OTHER_PUNCTUATION:
					case Character.INITIAL_QUOTE_PUNCTUATION:
					case Character.FINAL_QUOTE_PUNCTUATION:
						res = true;
						break;
					default:
						res = false;
						break;
				}
				break;
			case CLASS_SPACE: // "\\s"; REF:https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/master/jdk/src/share/classes/java/util/regex/Pattern.java#L2436-L2438; https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/aa318070b27849f1fe00d14684b2a40f7b29bf79/jdk/src/share/classes/java/util/regex/UnicodeProp.java#L72-L75
				if ((cp >= 0x9 && cp <= 0xd) || cp == 0x85) {
					res = true;
				}
				else {
					char_type = Character.getType(cp);
					switch (char_type) {
						case Character.SPACE_SEPARATOR:
						case Character.LINE_SEPARATOR:
						case Character.PARAGRAPH_SEPARATOR:
						// case Character.CONNECTOR_PUNCTUATION:   // do not include CONNECTOR_PUNCTUATION b/c it includes "_"; ISSUE#:582 DATE:2019-09-28
							res = true;
							break;
						default:
							res = false; 
							break;
					}
				}
				break;
			case CLASS_UPPER: // "\\p{Lu}"; REF:https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/master/jdk/src/share/classes/java/util/regex/Pattern.java#L5606
				char_type = Character.getType(cp);
				res = char_type == Character.UPPERCASE_LETTER;
				break;
			case CLASS_WORD: // "\\w"; REF:https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/master/jdk/src/share/classes/java/util/regex/Pattern.java#L2459; https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/aa318070b27849f1fe00d14684b2a40f7b29bf79/jdk/src/share/classes/java/util/regex/UnicodeProp.java#L187-L194
				if (Character.isAlphabetic(cp)
					|| (cp == 0x200C || cp == 0x200D)) {
					res = true;
				}
				else {
					char_type = Character.getType(cp);
					switch (char_type) {
						case Character.NON_SPACING_MARK:
						case Character.ENCLOSING_MARK:
						case Character.COMBINING_SPACING_MARK:
						case Character.DECIMAL_DIGIT_NUMBER:
						case Character.CONNECTOR_PUNCTUATION:
						case Character.LETTER_NUMBER: // expand word to include LETTER_NUMBER / OTHER_NUMBER since Word should equal Letter + Number; ISSUE#:582 DATE:2019-09-28
						case Character.OTHER_NUMBER:
							res = true;
							break;
						default:
							res = false;
							break;
					}
				}
				break;
			case CLASS_HEX: // "[^0-9A-Fa-f０-９Ａ-Ｆａ-ｆ]"
				res = 
					(  (cp >=    48 && cp <=    57) // 0-9 
					|| (cp >=    65 && cp <=    70) // A-F
					|| (cp >=    97 && cp <=   102) // a-f
					|| (cp >= 65296 && cp <= 65305) // ０-９
					|| (cp >= 65313 && cp <= 65318) // Ａ-Ｆ
					|| (cp >= 65345 && cp <= 65350) // ａ-ｆ
					);
				break;
			case CLASS_NULL: // "\\x00" 
				res = cp == 0; 
				break;
			default: // escaped; EX: "%b" -> "b"
				return cls == cp;
		}
		return (cls_lower == cls) ? res : !res;
	}
/*
== UstringLibrary_APPROXIMATION ==
* This approximates the MediaWiki UstringLibrary.php section below
* Most of the mapping from PHP to Java is based on https://www.regular-expressions.info/unicode.html
* Note that Java has different definitions of what different Unicode categories. Particularly:
  * CLASS_SPACE: should not have CONNECTOR_PUNCTUATION
  * CLASS_DIGIT: should not have LETTER_NUMBER, OTHER_NUMBER
  * CLASS_ALPHA: should have CASED_LETTER?

=== /extensions/Scribunto/engines/LuaCommon/UstringLibrary.php ===
// If you change these, also change lualib/ustring/make-tables.php
// (and run it to regenerate charsets.lua)
'a' => '\p{L}',
'c' => '\p{Cc}',
'd' => '\p{Nd}',
'l' => '\p{Ll}',
'p' => '\p{P}',
's' => '\p{Xps}',
'u' => '\p{Lu}',
'w' => '[\p{L}\p{Nd}]',
'x' => '[0-9A-Fa-f０-９Ａ-Ｆａ-ｆ]',
'z' => '\0',
*/
}
