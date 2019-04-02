package org.luaj.vm2.lib;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.Varargs;

public abstract class Str_find_mgr {
	private final LuaString src;
	private final LuaString pat;
	private int src_bgn;
	private boolean plain;
	private final boolean find;
	private final boolean push_idxs;
	public Str_find_mgr(LuaString src, LuaString pat, int src_bgn, boolean plain, boolean find, boolean push_idxs) {
		this.src = src;
		this.pat = pat;
		this.src_bgn = src_bgn;
		this.plain = plain;
		this.find = find;
		this.push_idxs = push_idxs;
	}
	protected abstract void Process_plain(int bgn, int end);
	protected abstract void Process_pat_many(int bgn, int end, MatchState o);
	protected abstract void Process_pat_one(MatchState o);
	protected abstract void Process_nil();
	public void Process() {
		int src_len = src.m_length;
		/*
		*/
		if (src_bgn > 0) {
			int min_lhs = src_bgn - 1;
			src_bgn = min_lhs < src_len ? min_lhs : src_len;	// XOWA.PERF:Math.min(src_bgn - 1, src.length()); DATE:2014-08-13 
		} else if (src_bgn < 0) {
			int max_rhs = src_len + src_bgn;
			src_bgn = 0 > max_rhs ? 0 : max_rhs;			    // XOWA.PERF:Math.max(0, src_len + src_bgn); DATE:2014-08-13
		}
		/*
		if (src_bgn < 0)
			src_bgn = 0;
		else if (src_bgn > src_len)
			src_bgn = src_len;
		 */
		int pat_len = pat.length(); 
		plain = find && (plain || pat.indexOfAny(SPECIALS) == -1);
		if (plain) {
			int result = src.indexOf(pat, src_bgn);
			if (result != -1) {
				this.Process_plain(result + 1, result + pat_len);
				return;
			}
		} else {
			boolean anchor = false;
			int pat_pos = 0;
			MatchState ms = new MatchState(src, src_len, pat, pat_len, push_idxs);			 
			if (pat_len > 0 && pat.luaByte(0) == '^') { // XOWA: check length > 0 else IndexOutOfBoundsException; PAGE:c:File:Nouveauxvoyagese-p378.png; DATE:2017-07-19
				anchor = true;
				pat_pos = 1;
			}
			int str_pos = src_bgn;
			do {
				int res;
				ms.reset();
				if ((res = ms.match(str_pos, pat_pos)) != -1) {
					if (find) {
						this.Process_pat_many(str_pos + 1, res, ms.push_captures2(false, str_pos, res));
					} else {
						this.Process_pat_one(ms.push_captures2(true, str_pos, res));
					}
					return;
				}
			} while (str_pos++ < src_len && !anchor);	// NOTE: str_pos++ will force evaluation one more time at end of string; EX: str_pos = 0; src_len = 1; s_off++ < src_len -> true and str_pos will be 1
		}
		this.Process_nil();
	}

	private static final LuaString SPECIALS = LuaString.valueOf("^$*+?.([%-");
}
class Str_find_mgr__lua extends Str_find_mgr {
	private Varargs result;
	Str_find_mgr__lua(Varargs args, boolean find) {
		super(args.checkstring(1), args.checkstring(2), args.optint(3, 1), args.arg(4).toboolean(), find, false);
	}
	public Varargs Result() {return result;}
	
	@Override protected void Process_plain(int bgn, int end) {
		result = LuaValue.varargsOf(LuaValue.valueOf(bgn), LuaValue.valueOf(end));
	}
	@Override protected void Process_pat_many(int bgn, int end, MatchState ms) {
		result = LuaValue.varargsOf(LuaValue.valueOf(bgn), LuaValue.valueOf(end), LuaValue.varargsOf(ms.Capture_vals()));
	}
	@Override protected void Process_pat_one(MatchState ms) {
		LuaValue[] rv = ms.Capture_vals();
		result = rv == null ? LuaValue.NONE : LuaValue.varargsOf(rv);
	}
	@Override protected void Process_nil() {
		result = LuaValue.NIL;
	}
	public static Varargs Run(Varargs args, boolean find) {
		Str_find_mgr__lua mgr = new Str_find_mgr__lua(args, false);
		mgr.Process();
		return mgr.Result(); 
	}
}
