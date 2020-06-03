package org.luaj.vm2.lib;

import org.junit.*;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Luaj_fxt;
import org.luaj.vm2.Varargs;

import java.util.List;
import java.util.ArrayList;

import gplx.objects.errs.Err_;
import gplx.objects.primitives.Bool_;
import gplx.objects.types.Type_ids_;
import gplx.tests.Gftest_fxt;

import static org.luaj.vm2.lib.Str_char_class_mgr.CLASS_ALPHA;
import static org.luaj.vm2.lib.Str_char_class_mgr.CLASS_DIGIT;
import static org.luaj.vm2.lib.Str_char_class_mgr.CLASS_LOWER;
import static org.luaj.vm2.lib.Str_char_class_mgr.CLASS_UPPER;
import static org.luaj.vm2.lib.Str_char_class_mgr.CLASS_CTRL;
import static org.luaj.vm2.lib.Str_char_class_mgr.CLASS_PUNCT;
import static org.luaj.vm2.lib.Str_char_class_mgr.CLASS_SPACE;
import static org.luaj.vm2.lib.Str_char_class_mgr.CLASS_WORD;
import static org.luaj.vm2.lib.Str_char_class_mgr.CLASS_HEX;
import static org.luaj.vm2.lib.Str_char_class_mgr.CLASS_NULL;
import static org.luaj.vm2.lib.Str_char_class_mgr__fxt.MGR_A7;
import static org.luaj.vm2.lib.Str_char_class_mgr__fxt.MGR_U8;


public class Str_char_class_mgr__tst {
	private final Str_char_class_mgr__fxt fxt = new Str_char_class_mgr__fxt();

	@Test public void Alpha() {
		// a7+u8: UPPERCASE_LETTER
		fxt
		.Init("G", "M", "Z")
		.Test(fxt.Rule(MGR_A7, MGR_U8).Is(CLASS_UPPER, CLASS_ALPHA, CLASS_WORD).Is_not(CLASS_LOWER, CLASS_DIGIT, CLASS_CTRL, CLASS_PUNCT, CLASS_SPACE, CLASS_HEX, CLASS_NULL))
		;

		// a7+u8: LOWERCASE_LETTER
		fxt
		.Init("g", "m", "z")
		.Test(fxt.Rule(MGR_A7, MGR_U8).Is(CLASS_LOWER, CLASS_ALPHA, CLASS_WORD).Is_not(CLASS_UPPER, CLASS_DIGIT, CLASS_CTRL, CLASS_PUNCT, CLASS_SPACE, CLASS_HEX, CLASS_NULL))
		;

		// u8: UPPERCASE_LETTER
		fxt
		.Init("ä", "é", "í")
		.Test(fxt.Rule(MGR_U8).Is(CLASS_LOWER, CLASS_ALPHA, CLASS_WORD).Is_not(CLASS_UPPER, CLASS_DIGIT, CLASS_CTRL, CLASS_PUNCT, CLASS_SPACE, CLASS_HEX, CLASS_NULL))
		.Test(fxt.Rule(MGR_A7).Is_not(CLASS_LOWER, CLASS_ALPHA, CLASS_WORD))
		;
		
		// u8: LOWERCASE_LETTER
		fxt
		.Init("Ä", "É", "Í")
		.Test(fxt.Rule(MGR_U8).Is(CLASS_UPPER, CLASS_ALPHA, CLASS_WORD).Is_not(CLASS_LOWER, CLASS_DIGIT, CLASS_CTRL, CLASS_PUNCT, CLASS_SPACE, CLASS_HEX, CLASS_NULL))
		.Test(fxt.Rule(MGR_A7).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_WORD))
		;
		
		// u8: TITLECASE_LETTER, MODIFIER_LETTER, OTHER_LETTER
		fxt
		.Init("\u01C5", "\u02B1", "\u05D0")
		.Test(fxt.Rule(MGR_U8).Is(CLASS_ALPHA, CLASS_WORD).Is_not(CLASS_DIGIT, CLASS_CTRL, CLASS_PUNCT, CLASS_SPACE, CLASS_HEX, CLASS_NULL))
		.Test(fxt.Rule(MGR_A7).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_WORD))
		;
	}

	@Test public void Digit() {
		// a7+u8
		fxt
		.Init("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
		.Test(fxt.Rule(MGR_A7, MGR_U8).Is(CLASS_DIGIT, CLASS_WORD).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_CTRL, CLASS_PUNCT, CLASS_SPACE, CLASS_NULL))
		;

		// u8: DECIMAL_DIGIT_NUMBER
		fxt
		.Init("٠", "۱", "߂", "३", "৪", "੫", "૬", "୭", "௮", "౯")
		.Test(fxt.Rule(MGR_U8).Is(CLASS_DIGIT, CLASS_WORD).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_CTRL, CLASS_PUNCT, CLASS_SPACE, CLASS_HEX, CLASS_NULL))
		.Test(fxt.Rule(MGR_A7).Is_not(CLASS_DIGIT, CLASS_WORD))
		;

		// LETTER_NUMBER OTHER_NUMBER; Assert disabled ISSUE#:617
		fxt
		.Init("Ⅰ", "²")
		.Test(fxt.Rule(MGR_U8).Is_not(CLASS_DIGIT, CLASS_WORD).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_CTRL, CLASS_PUNCT, CLASS_SPACE, CLASS_HEX, CLASS_NULL))
		.Test(fxt.Rule(MGR_A7).Is_not(CLASS_DIGIT))
		;
	}

	@Test public void Control() {
		// a7+u8
		fxt
		.Init("\u0001", "\u0014", "\u007f")
		.Test(fxt.Rule(MGR_A7, MGR_U8).Is(CLASS_CTRL).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_DIGIT, CLASS_WORD, CLASS_PUNCT, CLASS_SPACE, CLASS_HEX, CLASS_NULL))
		;

		// u8
		fxt
		.Init("\u0080", "\u0090", "\u009f")
		.Test(fxt.Rule(MGR_U8).Is(CLASS_CTRL).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_DIGIT, CLASS_WORD, CLASS_PUNCT, CLASS_SPACE, CLASS_HEX, CLASS_NULL))
		.Test(fxt.Rule(MGR_A7).Is_not(CLASS_CTRL))
		;
	}

	@Test public void Punct() {
		// a7+u8
		fxt
		.Init("#", ".", ";", "?")
		.Test(fxt.Rule(MGR_A7, MGR_U8).Is(CLASS_PUNCT).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_DIGIT, CLASS_WORD, CLASS_SPACE, CLASS_CTRL, CLASS_HEX, CLASS_NULL))
		;		

		// a7: not punctuation
		fxt
		.Init("]", "~")
		.Test(fxt.Rule(MGR_A7).Is_not(CLASS_PUNCT).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_DIGIT, CLASS_WORD, CLASS_SPACE, CLASS_CTRL, CLASS_HEX, CLASS_NULL))
		;		

		// u8: DASH_PUNCTUATION; START_PUNCTUATION; END_PUNCTUATION; OTHER_PUNCTUATION; INITIAL_QUOTE_PUNCTUATION; FINAL_QUOTE_PUNCTUATION
		fxt
		.Init("\u2011", "\u2983", "\uFE3A", "\u0E4F", "\u201C", "\u203A")
		.Test(fxt.Rule(MGR_U8).Is(CLASS_PUNCT).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_DIGIT, CLASS_WORD, CLASS_SPACE, CLASS_CTRL, CLASS_HEX, CLASS_NULL))
		.Test(fxt.Rule(MGR_A7).Is_not(CLASS_PUNCT))
		;
		
		// u8: CONNECTOR_PUNCTATION is considered word and space
		fxt
		.Init("_", "\uFE4F")
		.Test(fxt.Rule(MGR_U8).Is(CLASS_PUNCT, CLASS_WORD).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_DIGIT, CLASS_SPACE, CLASS_CTRL, CLASS_HEX, CLASS_NULL))
		.Test(fxt.Rule(MGR_A7).Is_not(CLASS_PUNCT, CLASS_WORD, CLASS_SPACE))
		;
	}

	@Test public void Space() {
		// a7+u8
		fxt
		.Init("\t", "\n", "\r", "\f", "\u000C")
		.Test(fxt.Rule(MGR_A7, MGR_U8).Is(CLASS_SPACE, CLASS_CTRL).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_DIGIT, CLASS_WORD, CLASS_PUNCT, CLASS_HEX, CLASS_NULL))
		;		

		// a7+u8: space
		fxt
		.Init(" ")
		.Test(fxt.Rule(MGR_A7, MGR_U8).Is(CLASS_SPACE).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_DIGIT, CLASS_WORD, CLASS_PUNCT, CLASS_CTRL, CLASS_HEX, CLASS_NULL))
		;
		
		// u8: SPACE_SEPARATOR; LINE_SEPARATOR 
		fxt
		.Init("\u2000", "\u2028", "\u2029")
		.Test(fxt.Rule(MGR_U8).Is(CLASS_SPACE).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_DIGIT, CLASS_WORD, CLASS_PUNCT, CLASS_CTRL, CLASS_HEX, CLASS_NULL))
		.Test(fxt.Rule(MGR_A7).Is_not(CLASS_SPACE))
		;		
		
		// a7+u8: PARAGRAPH_SEPARATOR
		fxt
		.Init("\u000C")
		.Test(fxt.Rule(MGR_A7, MGR_U8).Is(CLASS_SPACE, CLASS_CTRL).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_DIGIT, CLASS_WORD, CLASS_PUNCT, CLASS_HEX, CLASS_NULL))
		;		

		// u8: CONNECTOR_PUNCTUATION
		fxt
		.Init("\uFE4F")
		.Test(fxt.Rule(MGR_U8).Is(CLASS_PUNCT, CLASS_WORD).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_DIGIT, CLASS_SPACE, CLASS_CTRL, CLASS_HEX, CLASS_NULL))
		.Test(fxt.Rule(MGR_A7).Is_not(CLASS_SPACE, CLASS_PUNCT))
		;		
	}

	@Test public void Hex() {
		// a7+u8
		fxt
		.Init("a", "b", "c", "d", "e", "f", "A", "B", "C", "D", "E", "F", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
		.Test(fxt.Rule(MGR_A7, MGR_U8).Is(CLASS_HEX).Is_not(CLASS_CTRL, CLASS_PUNCT, CLASS_SPACE, CLASS_NULL))
		;

		// u8
		fxt
		.Init("０", "９", "Ａ", "Ｆ", "ａ", "ｆ")
		.Test(fxt.Rule(MGR_U8).Is(CLASS_HEX).Is_not(CLASS_CTRL, CLASS_PUNCT, CLASS_SPACE, CLASS_NULL))
		.Test(fxt.Rule(MGR_A7).Is_not(CLASS_HEX))
		;
	}

	@Test public void Null() {
		// a7+u8
		fxt
		.Init("\u0000")
		.Test(fxt.Rule(MGR_A7, MGR_U8).Is(CLASS_NULL, CLASS_CTRL).Is_not(CLASS_UPPER, CLASS_ALPHA, CLASS_LOWER, CLASS_DIGIT, CLASS_WORD, CLASS_PUNCT, CLASS_SPACE, CLASS_HEX))
		;		
	}
}
class Str_char_class_mgr__fxt {
	private final Str_char_class_mgr mgr_ascii = new Str_char_class_mgr__ascii();
	private final Str_char_class_mgr mgr_unicode = new Str_char_class_mgr__unicode();
	private String[] codepoint_ary; 
	public static int MGR_A7 = 1, MGR_U8 = 2;
	public Str_char_class_mgr__fxt Init(String... ary) {
		this.codepoint_ary = ary;
		return this;
	}
	public Str_char_class_mgr_rule_mok Rule(int... ary) {
		Str_char_class_mgr[] mgr_ary = new Str_char_class_mgr[ary.length]; 
		for (int i = 0; i < ary.length; i++) {
			mgr_ary[i] = ary[i] == MGR_A7 ? mgr_ascii : mgr_unicode;				
		}
		return new Str_char_class_mgr_rule_mok(mgr_ary);
	}
	public Str_char_class_mgr__fxt Test(Str_char_class_mgr_rule_mok... ary) {
		for (Str_char_class_mgr_rule_mok itm : ary)
			itm.Test(codepoint_ary);
		return this;
	}
}
class Str_char_class_mgr_rule_mok {
	private final Str_char_class_mgr[] mgr_ary;
	private int[] match_y, match_n;
	public Str_char_class_mgr_rule_mok(Str_char_class_mgr... ary) {
		this.mgr_ary = ary;
	}
	public Str_char_class_mgr_rule_mok Is(int... ary) {
		this.match_y = ary;
		return this;
	} 
	public Str_char_class_mgr_rule_mok Is_not(int... ary) {
		this.match_n = ary;
		return this;
	}
	public void Test(String[] ary) {
		for (Str_char_class_mgr mgr : mgr_ary) {
			for (String itm : ary) {
				int cp = itm.codePointAt(0);
				if (match_y != null) {
					for (int cls_val : match_y) {
						int cls_rev = cls_val >= 97 ? cls_val - 32 : cls_val + 32; 
						Gftest_fxt.Eq__bool(Bool_.Y, mgr.Match_class(cp, cls_val), "expected match.y: mgr={0} str={1} cp={2} cls={3}", mgr, itm, cp, (char)cls_val);				
						Gftest_fxt.Eq__bool(Bool_.N, mgr.Match_class(cp, cls_rev), "expected match.n: mgr={0} str={1} cp={2} cls={3}", mgr, itm, cp, (char)cls_rev);				
					}
				}
				if (match_n != null) {
					for (int cls_val : match_n) {
						int cls_rev = cls_val >= 97 ? cls_val - 32 : cls_val + 32; 
						Gftest_fxt.Eq__bool(Bool_.N, mgr.Match_class(cp, cls_val), "expected match.n: mgr={0} str={1} cp={2} cls={3}", mgr, itm, cp, (char)cls_val);
						Gftest_fxt.Eq__bool(Bool_.Y, mgr.Match_class(cp, cls_rev), "expected match.y: mgr={0} str={1} cp={2} cls={2}", mgr, itm, cp, (char)cls_rev);
					}
				}
			}
		}
	}
}
