package org.luaj.vm2.lib;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import junit.framework.*;
public class Xowa_tst extends TestCase {
	private final Xowa_fxt fxt = new Xowa_fxt();
	public void test__tonumber_ws() {
		fxt.Test__tonumber_int("123"					, 123);
		fxt.Test__tonumber_int("\t\n\r 123\t\n\r"		, 123);
		fxt.Test__tonumber_nil("1a");
		fxt.Test__tonumber_nil("1 2");
		fxt.Test__tonumber_nil("");
		fxt.Test__tonumber_nil("\t\n\r \t\n\r");
	}
	public void test__gsub() {
		fxt.Test__gsub("abc", "a", "A", "Abc");			// basic
		fxt.Test__gsub("a#b", "#", "", "ab");			// match() fails when shortening string 
		fxt.Test__gsub("", "%b<>", "A", "");			// balance() fails with out of index when find is blank
		fxt.Test__gsub("abc", "z", "%A", "abc");		// negate
	}
	public void test__find() {
		fxt.Test__find(true , "%a", "a", "A", "z", "Z");
		fxt.Test__find(false, "%a", "1", "@");
		fxt.Test__find(true , "%A", "1", "@");
		fxt.Test__find(false, "%A", "a", "A", "z", "Z");
		fxt.Test__find(true , "%d", "1", "0");
		fxt.Test__find(false, "%d", "a", "@");
		fxt.Test__find(true , "%D", "a", "@");
		fxt.Test__find(false, "%D", "1", "0");
		fxt.Test__find(true , "%l", "a", "z");
		fxt.Test__find(false, "%l", "A", "Z", "1", "@");
		fxt.Test__find(true , "%L", "A", "Z", "1", "@");
		fxt.Test__find(false, "%L", "a", "z");
		fxt.Test__find(true , "%u", "A", "Z");
		fxt.Test__find(false, "%u", "a", "z", "1", "@");
		fxt.Test__find(true , "%U", "a", "z", "1", "@");
		fxt.Test__find(false, "%U", "A", "Z");
		fxt.Test__find(true , "%c", "\1");
		fxt.Test__find(false, "%c", "a", "1", "@");
		fxt.Test__find(true , "%C", "a", "1", "@");
		fxt.Test__find(false, "%C", "\1");
		fxt.Test__find(true , "%p", "@");
		fxt.Test__find(false, "%p", "a", "1");
		fxt.Test__find(true , "%P", "a", "1");
		fxt.Test__find(false, "%P", "@");
		fxt.Test__find(true , "%s", " ", "\t", "\n", "\r");
		fxt.Test__find(false, "%s", "a", "1", "@");
		fxt.Test__find(true , "%S", "a", "1", "@");
		fxt.Test__find(false, "%S", " ", "\t", "\n", "\r");
		fxt.Test__find(true , "%w", "a", "1");
		fxt.Test__find(false, "%w", " ", "@");
		fxt.Test__find(true , "%W", " ", "@");
		fxt.Test__find(false, "%W", "a", "1");
		fxt.Test__find(true , "%x", "0", "9", "a", "f");
		fxt.Test__find(false, "%x", "g", "z");
		fxt.Test__find(true , "%X", "g", "z");
		fxt.Test__find(false, "%X", "0", "9", "a", "f");
		fxt.Test__find(true , "%z", "\0");
		fxt.Test__find(false, "%z", "a");
		fxt.Test__find(true , "%Z", "a");
		fxt.Test__find(false, "%Z", "\0");
	}
	public void test__find__u8() {
		fxt.Test__find(false, "%a", "ß");	// NOTE: ß is not an alpha b/c either lua C defines isalpha as ASCII or Wikimedia uses English locale lua binaries; DATE:2016-04-17 		
	}
	public void test__gsub__u8() {
		fxt.Test__gsub("ß", "ß", "a", "a"); 
	}
	public void test__format() {
		fxt.Test__format("%.1f"	, "1.23", "1.2");		// apply precision; 1 decimal place
		fxt.Test__format("(%.1f)", "1.23", "(1.2)");	// handle substring; format_string should be "%.1f" not "(%.1f)"
		fxt.Test__format("%0.1f"	, "1.23", "1.2");	// handle invalid padding of 0 
		fxt.Test__format("%02.f"	, "1.23", "01");	// handle missing precision 
	}
}
class Xowa_fxt {
	public void Test__tonumber_int(String raw, int expd) {
		LuaString actl_str = LuaString.valueOf(raw);
		LuaInteger actl_int = (LuaInteger)actl_str.tonumber();		
		Assert.assertEquals(expd, actl_int.v);
	}
	public void Test__tonumber_nil(String raw) {
		LuaString actl_str = LuaString.valueOf(raw);
		Assert.assertEquals(LuaValue.NIL, actl_str.tonumber());
	}
	public void Test__find(boolean expd, String pat, String... src_ary) {
		int src_len = src_ary.length;
		for (int i = 0; i < src_len; ++i) {
			String src = src_ary[i];
			Varargs actl_args = StringLib.find__test(LuaValue.varargsOf(new LuaValue[] {LuaValue.valueOf(src), LuaValue.valueOf(pat)}));
			LuaValue actl_arg = actl_args.arg(1);
			Assert.assertEquals(expd, actl_arg != LuaValue.NIL);
		}		
	}
	public void Test__gsub(String text, String regx, String repl, String expd) {
		Varargs actl_args = StringLib.gsub_test(LuaValue.varargsOf(new LuaValue[] {LuaValue.valueOf(text), LuaValue.valueOf(regx), LuaValue.valueOf(repl)}));
		Assert.assertEquals(expd, actl_args.checkstring(1).tojstring());
	}
	public void Test__format(String fmt, String val, String expd) {
		Varargs actl_args = StringLib.format_test(LuaValue.varargsOf(new LuaValue[] {LuaValue.valueOf(fmt), LuaValue.valueOf(val)}));
		Assert.assertEquals(expd, actl_args.checkstring(1).tojstring());
	}
}
