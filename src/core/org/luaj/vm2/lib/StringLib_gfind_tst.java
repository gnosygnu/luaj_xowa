package org.luaj.vm2.lib;

import org.junit.*;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Luaj_fxt;
import org.luaj.vm2.Varargs;

import gplx.objects.brys.Bry_;
import gplx.objects.errs.Err_;
import gplx.objects.strings.String_;
import gplx.objects.types.Type_ids_;
import gplx.tests.Gftest_fxt;

public class StringLib_gfind_tst {
	private final StringLib_fxt fxt = new StringLib_fxt();
	
	@Test public void Gfind__char_class() {
		fxt.Test__gfind("ab cd", "%w+", 0); // PURPOSE:LUAJ_PATTERN_REPLACEMENT; DATE:2019-04-28
	}

	@Test public void Gfind__smoke_1() {
		fxt.Test__gfind("A/B/C", "^[^/]*().*()/[^/]*$", 0); // PURPOSE:LUAJ_PATTERN_REPLACEMENT; DATE:2019-04-28
	}	

	@Test public void Match__offset() {// FIX:need to account for m_offset ISSUE#:520; DATE:2019-07-25
		fxt.Test__match(LuaString.valueOf(Bry_.New_utf08("abcde"), 1, 4), "b", 0, "b"); // Note that string starts from "b"; fails if "a" is returned 
	}	
}
class StringLib_fxt {
	public void Test__gfind(String src, String pat, int expd) {
		Varargs args = Luaj_fxt.New_varargs(src, pat, expd);
		Varargs actl = StringLib.gmatch(args);
		Gftest_fxt.Eq__int(expd, actl.toint(0));
	}
	public void Test__match(LuaString src, String pat, int pos, String expd) {
		Varargs args = Luaj_fxt.New_varargs(src, pat, pos);
		Varargs actl = StringLib.match(args);
		Gftest_fxt.Eq__str(expd, actl.tojstring());
	}
}
