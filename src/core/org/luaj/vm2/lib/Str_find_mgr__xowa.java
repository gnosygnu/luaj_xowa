package org.luaj.vm2.lib;

import org.luaj.vm2.Buffer;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import gplx.objects.strings.char_sources.*;

public class Str_find_mgr__xowa extends Str_find_mgr {
	private int capture_idx = 0;
	public Str_find_mgr__xowa(Char_source src, Char_source pat, int src_bgn, boolean plain, boolean find) {
		super(src, pat, src_bgn, plain, find);
	}
	@Override public Str_char_class_mgr Char_class_mgr() {return char_class_mgr;} private final Str_char_class_mgr char_class_mgr = new Str_char_class_mgr__unicode();
	public int Bgn() {return bgn;} private int bgn = -1;
	public int End() {return end;} private int end = -1;
	public int[] Captures_ary() {return captures_ary;} private int[] captures_ary = null;
	
	@Override protected void reset() {
		capture_idx = 0;
		captures_ary = null;
	}
	@Override protected void Captures__init(int levels) {
		if (levels > 0)
			this.captures_ary = new int[levels * 2];
	}
	@Override protected LuaValue Captures__make__none() {
		return null;
	}
	@Override protected Varargs Captures__make__many() {
		if (capture_idx == 0) { // counterpart to Str_find_mgr__luaj and "return captures_ary == null ? LuaValue.NONE : LuaValue.varargsOf(captures_ary);"
			captures_ary = null;
		}
		return null;
	}
	@Override protected LuaValue Capture__make__string(boolean register_capture, int bgn, int end) {
		if (register_capture) {
			captures_ary[capture_idx++] = bgn;
			captures_ary[capture_idx++] = end;
		}
		return null;		
	}
	@Override protected LuaValue Capture__position(boolean register_capture, int val) {
		if (register_capture) {
			// NOTE: -1 b/c "find_mgr.Capture__position" calls this with a base1 val. ISSUE#:726; DATE:2020-05-17;
			// Specifically:
			// * XOWA uses LUAJ as a shim for Scribunto PHP Regex (since Java Regex cannot support balanced parentheses); See Scrib_pattern_matcher__xowa
			// * So, subtract -1 since Scribunto will need base-0 indexes (remember, it would ordinarily call PHP Regex which is base-0, not Lua Pattern Matching which is base-1)
			int capture_bgn = val - Str_find_mgr.Base_1;
			int capture_end = val < this.src_len ? val : this.src_len;
			captures_ary[capture_idx++] = capture_bgn;
			captures_ary[capture_idx++] = capture_end;
		}
		return null;
	}
	@Override protected void Result__make__bgn_end(int bgn, int end) {
		this.bgn = bgn;
		this.end = end;
	}	
	@Override protected Varargs Result__make__plain(int bgn, int end) {
		this.bgn = bgn;
		this.end = end;
		return null;
	}
	@Override protected Varargs Result__make__find(int bgn, int end) {
		this.bgn = bgn;
		this.end = end;
		return null;
	}
	@Override protected Varargs Result__make__match() {
		return null;
	}
	@Override protected Varargs Result__make__nil() {
		this.bgn = -1;
		this.end = -1;
		return null;
	}
}
