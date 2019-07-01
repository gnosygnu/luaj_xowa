package org.luaj.vm2.lib;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.Varargs;
import gplx.objects.strings.char_sources.*;

public abstract class Str_find_mgr {
	public final Char_source src;
	public final Char_source pat;
	public final int src_len;
	public final int pat_len;
	public int src_bgn;
	public final boolean plain;
	public final boolean find;

	public Str_find_mgr(Char_source src, Char_source pat, int src_bgn, boolean plain, boolean find) {
		this.src = src;
		this.pat = pat;
		this.src_bgn = src_bgn;
		this.src_len = src.Len_in_data();
		this.pat_len = pat.Len_in_data();
		this.plain = plain;
		this.find = find;
	}
	protected abstract void reset();
	protected abstract void       Captures__init(int levels);
	protected abstract LuaValue   Captures__make__none();
	protected abstract Varargs    Captures__make__many();
	protected abstract LuaValue   Capture__make__string(boolean register_capture, int bgn, int end);
	protected abstract LuaValue   Capture__make__int(boolean register_capture, int val);
	protected abstract Varargs    Result__make__plain(int bgn, int end);
	protected abstract Varargs    Result__make__find(int bgn, int end);
	protected abstract Varargs    Result__make__match();
	protected abstract Varargs    Result__make__nil();
	protected void                Result__make__bgn_end(int bgn, int end) {}
	public abstract Str_char_class_mgr Char_class_mgr();
	public Varargs Process(boolean adjust_base1) {
		// adjust_base1 will be false when called by Scrib_pattern_matcher_xowa
		if (adjust_base1) {
			if (src_bgn > 0) {
				// subtract 1 for base 1; also, if src_bgn is > src_len, make it src_len to prevent out-of-bounds
				int src_bgn_base_1 = src_bgn - Base_1;
				src_bgn = src_bgn_base_1 < src_len ? src_bgn_base_1 : src_len;	// XOWA.PERF:Math.min(src_bgn - 1, src.length()); DATE:2014-08-13 
			} 
			else if (src_bgn < 0) {
				// adjust negative number for len; if still negative, make it 0
				int src_bgn_adjusted = src_len + src_bgn;
				src_bgn = 0 > src_bgn_adjusted ? 0 : src_bgn_adjusted;			// XOWA.PERF:Math.max(0, src_len + src_bgn); DATE:2014-08-13
			}
		}

		// find mode and (plain or no special pattern characters)
		if (find && (plain || Char_source_.Index_of_any(pat.Src(), SPECIALS_ARY) == Not_found)) {
			int result = src.Index_of(pat, src_bgn);
			if (result != Not_found) {
				return this.Result__make__plain(result + Base_1, result + pat_len);
			}
		}
		else {
			// if ^ at BOS, enable anchor and skip forward 1
			boolean anchor = false;
			int pat_pos = 0;
			if (pat_len > 0 && pat.Get_data(0) == '^') { // XOWA: check length > 0 else IndexOutOfBoundsException; PAGE:c:File:Nouveauxvoyagese-p378.png; DATE:2017-07-19
				anchor = true;
				pat_pos = 1;
			}
			
			// match
			int src_pos = src_bgn;
			Match_state ms = new Match_state(this);			 
			do {
				ms.reset();
				int res = ms.match(src_pos, pat_pos);
				if (res != Not_found) {
					Varargs r = null;
					if (find) {
						ms.push_captures(false, src_pos, res);
						r = this.Result__make__find(src_pos + Base_1, res);
					}
					else {
						ms.push_captures(true, src_pos, res);
						r = this.Result__make__match();
					}
					this.Result__make__bgn_end(src_pos, res);
					return r;
				}
			}	while (src_pos++ < src_len && !anchor);	// NOTE: src_pos++ will force evaluation one more time at end of string; EX: src_pos = 0; src_len = 1; s_off++ < src_len -> true and src_pos will be 1
		}
		return this.Result__make__nil();
	}
	protected static final LuaString SPECIALS = LuaString.valueOf("^$*+?.([%-");
	protected static final char[] SPECIALS_ARY = SPECIALS.tojstring().toCharArray();
	public static final int Base_1 = 1;
	public static final int Not_found = -1;
}
