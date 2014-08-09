package org.luaj.vm2;
import org.luaj.vm2.lib.StringLib;
import junit.framework.*;
public class Xowa_tst extends TestCase {
	private Xowa_fxt fxt = new Xowa_fxt();
	public void test_tonumber_ws() {
		fxt.Test_tonumber_int("123"						, 123);
		fxt.Test_tonumber_int("\t\n\r 123\t\n\r"		, 123);
		fxt.Test_tonumber_nil("1a");
		fxt.Test_tonumber_nil("1 2");
		fxt.Test_tonumber_nil("");
		fxt.Test_tonumber_nil("\t\n\r \t\n\r");
	}
	public void test_gsub() {
		fxt.Test_gsub("abc", "a", "A", "Abc");	// basic
		fxt.Test_gsub("a#b", "#", "", "ab");	// match() fails when shortening string 
		fxt.Test_gsub("", "%b<>", "A", "");		// balance() fails with out of index when find is blank
	}
	public void test_format() {
		fxt.Test_format("%.1f"	, "1.23", "1.2");	// apply precision; 1 decimal place
		fxt.Test_format("(%.1f)", "1.23", "(1.2)");	// handle substring; format_string should be "%.1f" not "(%.1f)"
		fxt.Test_format("%0.1f"	, "1.23", "1.2");	// handle invalid padding of 0 
		fxt.Test_format("%02.f"	, "1.23", "01");	// handle missing precision 
	}
}
class Xowa_fxt {
	public void Test_tonumber_int(String raw, int expd) {
		LuaString actl_str = LuaString.valueOf(raw);
		LuaInteger actl_int = (LuaInteger)actl_str.tonumber();		
		Assert.assertEquals(expd, actl_int.v);
	}
	public void Test_tonumber_nil(String raw) {
		LuaString actl_str = LuaString.valueOf(raw);
		Assert.assertEquals(LuaValue.NIL, actl_str.tonumber());
	}
	public void Test_gsub(String text, String regx, String repl, String expd) {
		Varargs actl_args = StringLib.gsub_test(LuaValue.varargsOf(new LuaValue[] {LuaValue.valueOf(text), LuaValue.valueOf(regx), LuaValue.valueOf(repl)}));
		Assert.assertEquals(expd, actl_args.checkstring(1).tojstring());
	}
	public void Test_format(String fmt, String val, String expd) {
		Varargs actl_args = StringLib.format_test(LuaValue.varargsOf(new LuaValue[] {LuaValue.valueOf(fmt), LuaValue.valueOf(val)}));
		Assert.assertEquals(expd, actl_args.checkstring(1).tojstring());
	}
}
