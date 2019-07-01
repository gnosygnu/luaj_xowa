package org.luaj.vm2.lib;

public abstract class Str_char_class_mgr {
	public abstract boolean Match_class(int codepoint, int character_class);
		
	public static final int
	  CLASS_ALPHA   = (int)'a'
	, CLASS_DIGIT   = (int)'d'
	, CLASS_LOWER   = (int)'l'
	, CLASS_UPPER   = (int)'u'
	, CLASS_CTRL    = (int)'c'
	, CLASS_PUNCT   = (int)'p'
	, CLASS_SPACE   = (int)'s'
	, CLASS_WORD    = (int)'w'
	, CLASS_HEX     = (int)'x'
	, CLASS_NULL    = (int)'z'
	;
}
