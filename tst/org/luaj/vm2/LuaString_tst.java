package org.luaj.vm2;

import java.nio.charset.Charset;

import org.junit.*;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import gplx.objects.errs.Err_;
import gplx.objects.types.Type_ids_;
import gplx.tests.Gftest_fxt;

public class LuaString_tst {
	private final LuaString_fxt fxt = new LuaString_fxt();

	@Test public void Index_of() {
		fxt.Test__Index_of("abc", "b", 0, 1);  // basic
		fxt.Test__Index_of("ab", "bc", 0, -1); // out-of-bounds
		fxt.Test__Index_of("a¢e", "¢", 0, 1);  // check UTF-8 strings still match at byte-level
		fxt.Test__Index_of(fxt.Make__LuaString("cbcde", 1, 3), "c", 0, 1); // m_offset check; fails if picks up first c
		fxt.Test__Index_of(fxt.Make__LuaString("abcde", 1, 3), "d", 0, 2); // m_length check; fails if -1
	}

	@Test public void Substring() {
		fxt.Test__Substring("abc", 1, 2, "b");  // basic
		fxt.Test__Substring(fxt.Make__LuaString("abcde", 1, 3), 1, 2, "c");  // m_offset check; fails if "b"
	}
}
class LuaString_fxt {
	public LuaString Make__LuaString(String src, int bgn, int len) {
		byte[] src_bry = Charset.forName("UTF-8").encode(src).array();
		return LuaString.valueOf(src_bry, bgn, len);
	}
	public void Test__Index_of(String src_str, String find_str, int bgn, int expd) {Test__Index_of(LuaString.valueOf(src_str), find_str, bgn, expd);}
	public void Test__Index_of(LuaString src, String find_str, int bgn, int expd) {
		LuaString find = LuaString.valueOf(find_str);
		int actl = src.Index_of(find, bgn);
		Gftest_fxt.Eq__int(expd, actl);
	}
	public void Test__Substring(String src, int bgn, int end, String expd) {Test__Substring(LuaString.valueOf(src), bgn, end, expd);}
	public void Test__Substring(LuaString src, int bgn, int end, String expd) {
		String actl = src.Substring(bgn, end);
		Gftest_fxt.Eq__str(expd, actl);
	}
}