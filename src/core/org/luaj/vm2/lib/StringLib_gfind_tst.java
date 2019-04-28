package org.luaj.vm2.lib;

import org.junit.*;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Luaj_fxt;
import org.luaj.vm2.Varargs;

import gplx.objects.errs.Err_;
import gplx.objects.types.Type_ids_;
import gplx.tests.Gftest_fxt;

public class StringLib_gfind_tst {
	private final StringLib_fxt fxt = new StringLib_fxt();
	
	@Test public void Char_class() {
		fxt.Test__gfind("ab cd", "%w+", 0); // PURPOSE:LUAJ_PATTERN_REPLACEMENT; DATE:2019-04-28
	}

	@Test public void Smoke_1() {
		fxt.Test__gfind("A/B/C", "^[^/]*().*()/[^/]*$", 0); // PURPOSE:LUAJ_PATTERN_REPLACEMENT; DATE:2019-04-28
	}	
}
class StringLib_fxt {
	public void Test__gfind(String src, String pat, int expd) {
		Varargs args = Luaj_fxt.New_varargs(src, pat, expd);
		Varargs actl = StringLib.gmatch(args);
		Gftest_fxt.Eq__int(expd, actl.toint(0));
	}
}
