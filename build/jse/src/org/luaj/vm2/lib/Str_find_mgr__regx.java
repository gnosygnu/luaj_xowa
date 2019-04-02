package org.luaj.vm2.lib;

import org.luaj.vm2.LuaString;

public class Str_find_mgr__regx extends Str_find_mgr {
	public Str_find_mgr__regx(String src, String pat, int src_bgn, boolean plain, boolean find) {
		super(LuaString.valueOf(src), LuaString.valueOf(pat), src_bgn, plain, find, true);
	}
	public int Bgn() {return bgn;} private int bgn;
	public int End() {return end;} private int end;
	public int[] Capture_ints() {return capture_ints;} private int[] capture_ints;
	
	@Override protected void Process_plain(int bgn, int end) {
		this.bgn = bgn - 1;
		this.end = end;
	}
	@Override protected void Process_pat_many(int bgn, int end, MatchState o) {
		this.bgn = bgn - 1;
		this.end = end;
		this.capture_ints = o.Capture_ints();
	}
	@Override protected void Process_pat_one(MatchState o) {
		this.capture_ints = o.Capture_ints();
		this.bgn = capture_ints[0] - 1; 
		this.end = capture_ints[1]; 
	}
	@Override protected void Process_nil() {
		this.bgn = -1;
		this.end = -1;
	}
}
