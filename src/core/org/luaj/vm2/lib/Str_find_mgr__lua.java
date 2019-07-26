package org.luaj.vm2.lib;

import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.StringLib.MatchState;

import gplx.objects.strings.char_sources.Char_source_;

class Str_find_mgr__lua extends Str_find_mgr {
	private LuaValue[] captures_ary;
	private int captures_idx;

	public Str_find_mgr__lua(LuaString src, LuaString pat, int src_bgn, boolean plain, boolean find) {
		super(src, pat, src_bgn, plain, find);
	}
	@Override public Str_char_class_mgr Char_class_mgr() {return char_class_mgr;} private final Str_char_class_mgr char_class_mgr = new Str_char_class_mgr__ascii();
	@Override protected void reset() {
		this.captures_ary = null;
		this.captures_idx = 0;
	}
	@Override public void Captures__init(int levels) {		
		this.captures_ary = new LuaValue[levels];		
	}
	@Override public LuaValue Captures__make__none() {
		return LuaValue.NONE;
	}
	@Override protected Varargs Captures__make__many() {
		return captures_ary == null ? LuaValue.NONE : LuaValue.varargsOf(captures_ary);
	}
	@Override protected LuaValue Capture__make__string(boolean register_capture, int bgn, int end) {
		// NOTE:cannot use Substring b/c Java will "fix" malformed bytes which will break things like "æ".Substring(0, 1); ISSUE#:504; DATE:2019-07-22
		// LuaValue rv = LuaString.valueOf(src.Substring(bgn, end));
		LuaString src_as_lstr = (LuaString)src;
		LuaString rv = LuaString.valueOfCopy(src_as_lstr.m_bytes, src_as_lstr.m_offset + bgn, end - bgn); // NOTE:must account for m_offset; ISSUE#:520; DATE:2019-07-25
		if (register_capture)
			captures_ary[captures_idx++] = rv;
		return rv;		
	}
	@Override protected LuaValue Capture__make__int(boolean register_capture, int val) {
		LuaValue rv = LuaValue.valueOf(val);
		if (register_capture)
			captures_ary[captures_idx++] = rv;
		return rv;		
	}	
	@Override protected void Result__make__bgn_end(int bgn, int end) {}
	@Override protected Varargs Result__make__plain(int bgn, int end) {
		return LuaValue.varargsOf(LuaValue.valueOf(bgn), LuaValue.valueOf(end));
	}
	@Override protected Varargs Result__make__find(int bgn, int end) {
		Varargs capt = (captures_ary == null) ? LuaValue.NONE : LuaValue.varargsOf(captures_ary);
		return LuaValue.varargsOf(LuaValue.valueOf(bgn), LuaValue.valueOf(end), capt);
	}
	@Override protected Varargs Result__make__match() {
		return captures_ary == null ? LuaValue.NONE : LuaValue.varargsOf(captures_ary);
	}
	@Override protected Varargs Result__make__nil() {
		return LuaValue.NIL;
	}
	public static Varargs Run(Varargs args, boolean find) {
		Str_find_mgr__lua mgr = new Str_find_mgr__lua(args.checkstring(1), args.checkstring(2), args.optint(3, 1), args.arg(4).toboolean(), find);
		return mgr.Process(true);
	}
}
