package org.luaj.vm2.lib;

import org.luaj.vm2.Buffer;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.Varargs;
import gplx.objects.strings.char_sources.*;

public class Match_state {
	private final Str_find_mgr find_mgr;
	private final Str_char_class_mgr char_class_mgr;
	private final Char_source src;
	private final Char_source pat;
	private final int src_len;
	private final int pat_len;
	private final int[] capture_bgns;
	private final int[] capture_lens;
	private int level;

	public Match_state(Str_find_mgr find_mgr) {
		this.find_mgr = find_mgr;
		this.char_class_mgr = find_mgr.Char_class_mgr();
		this.src = find_mgr.src;
		this.src_len = find_mgr.src_len;
		this.pat = find_mgr.pat;
		this.pat_len = find_mgr.pat_len;
		this.level = 0;
		this.capture_bgns = new int[MAX_CAPTURES];
		this.capture_lens = new int[MAX_CAPTURES];
	}

	public void reset() {
		level = 0;
		find_mgr.reset();
	}

	private void add_s(Buffer lbuf, LuaString new_s, int str_off, int str_end) {
		int l = new_s.length();
		for (int i = 0; i < l; i++) {
			byte b = (byte)new_s.Get_data(i);
			if (b != StringLib.L_ESC) {
				lbuf.append((byte)b);
			} else {
				i++; // skip ESC
				if (i == l) {// handle ESC at EOS; ISSUE#:571; DATE:2019-09-08
					lbuf.append(StringLib.L_ESC_STRING);
					break;
				}
				b = (byte)new_s.Get_data(i);
				if (!Character.isDigit((char)b)) {
					lbuf.append(b);
				} else if (b == '0') {
					lbuf.append(src.Substring(str_off, str_end));
				} else {
					lbuf.append(push_onecapture(false, b - '1', str_off, str_end).strvalue());
				}
			}
		}
	}

	/*
	public void add_value_old(Buffer lbuf, int src_pos, int str_end, LuaValue repl) {
		switch (repl.type()) {
			case LuaValue.TNUMBER:
			case LuaValue.TSTRING:
				add_s(lbuf, repl.strvalue(), src_pos, str_end);
				return;	
			case LuaValue.TFUNCTION:
				Varargs n = push_captures_old(true, src_pos, str_end);
				repl = repl.invoke(n).arg1();
				break;
			case LuaValue.TTABLE:
				// Need to call push_onecapture here for the error checking
				repl = repl.get(push_onecapture_old(0, src_pos, str_end));
				break;

			default:
				LuaValue.error("bad argument: string/function/table expected");
				return;
		}

		if (!repl.toboolean()) { // nil or false? 
			repl = src.Src().substring(src_pos, str_end); // keep original text
		} else if (!repl.isstring()) {
			LuaValue.error("invalid replacement value (a " + repl.typename() + ")");
		}
		lbuf.append(repl.strvalue()); // add result to accumulator
	}
	*/
	public void add_value(Buffer lbuf, int src_pos, int str_end, LuaValue repl) {
		switch (repl.type()) {
			case LuaValue.TNUMBER:
			case LuaValue.TSTRING:
				add_s(lbuf, repl.strvalue(), src_pos, str_end);
				return;	
			case LuaValue.TFUNCTION:
				Varargs n = push_captures(true, src_pos, str_end);
				repl = repl.invoke(n).arg1();
				break;
			case LuaValue.TTABLE:
				// Need to call push_onecapture here for the error checking
				repl = repl.get(push_onecapture(false, 0, src_pos, str_end));
				break;

			default:
				LuaValue.error("bad argument: string/function/table expected");
				return;
		}

		if (!repl.toboolean()) { // nil or false?
			repl = LuaValue.valueOf(src.Substring(src_pos, str_end)); // keep original text
		} else if (!repl.isstring()) {
			LuaValue.error("invalid replacement value (a " + repl.typename() + ")");
		}
		lbuf.append(repl.strvalue()); // add result to accumulator
	}

	/*
	private LuaValue push_onecapture_old(int i, int src_pos, int end) {
		if (i >= this.level) {
			if (i == 0) {
				return src.Src().substring(src_pos, end);
			} else {
				throw new LuaError("invalid capture index");
			}
		} else {
			int l = capture_lens[i];
			if (l == CAP_UNFINISHED) {
				throw new LuaError("unfinished capture");
			}
			if (l == CAP_POSITION) {
				return LuaValue.valueOf(capture_bgns[i] + 1);
			} else {
				int begin = capture_bgns[i];
				return src.Src().substring(begin, begin + l);
			}
		}
	}
	*/
	private LuaValue push_onecapture(boolean register_capture, int i, int src_pos, int end) {
		if (i >= this.level) {
			if (i == 0) {
				return find_mgr.Capture__make__string(register_capture, src_pos, end);
			} else {
				throw new LuaError("invalid capture index");
			}
		} else {
			int capture_len = capture_lens[i];
			if (capture_len == CAP_UNFINISHED) {
				throw new LuaError("unfinished capture");
			}
			int capture_bgn = capture_bgns[i];
			if (capture_len == CAP_POSITION) {
				// assert register_capture is true; refactor code to remove register_capture from Capture__position after next enwiki build
				if (!register_capture) {
					throw new LuaError("LUAJ_XOWA:register capture should always be true");
				}
				// NOTE: +1 to normalize capture to base1; ISSUE#:726; DATE:2020-05-17;
				// REF.LUA: https://www.lua.org/source/5.1/lstrlib.c.html
				//   if (l == CAP_POSITION)
				//     lua_pushinteger(ms->L, ms->capture[i].init - ms->src_init + 1);
				// REF.LUAJ: https://github.com/luaj/luaj/blob/master/src/core/org/luaj/vm2/lib/StringLib.java#L954
				return find_mgr.Capture__position(register_capture, capture_bgn + Str_find_mgr.Base_1);
			} else {
				return find_mgr.Capture__make__string(register_capture, capture_bgn, capture_bgn + capture_len);
			}
		}
	}
	
	/*
	public Varargs push_captures_old(boolean wholeMatch, int src_pos, int end) {
		int nlevels = (this.level == 0 && wholeMatch) ? 1 : this.level;
		switch (nlevels) {
			case 0: return LuaValue.NONE;
			case 1: return push_onecapture_old(0, src_pos, end);
		}
		LuaValue[] v = new LuaValue[nlevels];
		for (int i = 0; i < nlevels; ++i)
			v[i] = push_onecapture_old(i, src_pos, end);
		return LuaValue.varargsOf(v);
	}
	*/
	public Varargs push_captures(boolean wholeMatch, int src_pos, int end) {
		int nlevels = (this.level == 0 && wholeMatch) ? 1 : this.level;		
		if (nlevels == 0) {
			return find_mgr.Captures__make__none();
		}
		else {
			find_mgr.Captures__init(nlevels);
			for (int i = 0; i < nlevels; ++i)
				push_onecapture(true, i, src_pos, end);
		}
		return find_mgr.Captures__make__many();
	}


	private int check_capture(int l) {
		l -= '1'; // NOTE: '1' b/c Lua uses %1 to means captures[0]
		if (l < 0 || l >= level || this.capture_lens[l] == CAP_UNFINISHED) {
			LuaValue.error("invalid capture index");
		}
		return l;
	}

	private int capture_to_close() {
		int level = this.level;
		for (level--; level >= 0; level--)
			if (capture_lens[level] == CAP_UNFINISHED)
				return level;
		LuaValue.error("invalid pat capture");
		return 0;
	}

	private int classend(int pat_pos) {
		switch (pat.Get_data(pat_pos++)) {
			case StringLib.L_ESC:
				if (pat_pos == pat_len) {
					LuaValue.error("malformed pat (ends with %)");
				}
				return pat_pos + 1;	
			case '[':
				if (pat.Get_data(pat_pos) == '^')
					pat_pos++;
				do {
					if (pat_pos == pat_len) {
						LuaValue.error("malformed pat (missing])");
					}
					if (pat.Get_data(pat_pos++) == StringLib.L_ESC && pat_pos != pat_len)
						pat_pos++;
				} while (pat.Get_data(pat_pos) != ']');
				return pat_pos + 1;
			default:
				return pat_pos;
		}
	}

	private boolean matchbracketclass(int cur, int pat_pos, int ep) {
		boolean sig = true;
		if (pat.Get_data(pat_pos + 1) == '^') {
			sig = false;
			pat_pos++;
		}
		while (++pat_pos < ep) {
			if (pat.Get_data(pat_pos) == StringLib.L_ESC) {
				pat_pos++;
				if (char_class_mgr.Match_class(cur, pat.Get_data(pat_pos)))
					return sig;
			}
			else if ((pat.Get_data(pat_pos + 1) == '-') && (pat_pos + 2 < ep)) {
				pat_pos += 2;
				if (pat.Get_data(pat_pos - 2) <= cur && cur <= pat.Get_data(pat_pos))
					return sig;
			}
			else if (pat.Get_data(pat_pos) == cur) return sig;
		}
		return !sig;
	}

	private boolean singlematch(int cur, int pat_pos, int ep) {
		switch (pat.Get_data(pat_pos)) {
			case '.': return true;
			case StringLib.L_ESC: return char_class_mgr.Match_class(cur, pat.Get_data(pat_pos + 1));
			case '[': return matchbracketclass(cur, pat_pos, ep - 1);
			default: return pat.Get_data(pat_pos) == cur;
		}
	}

	private int matchbalance(int src_pos, int pat_pos) {
		if (pat_pos == pat_len || pat_pos + 1 == pat_len) {
			LuaValue.error("unbalanced pat");
		}
		if (src_pos >= src.Len_in_data()) return NULL;	// XOWA: check bounds; EX:string_match('a', '^(.) ?%b()'); DATE:2014-08-13
		if (src.Get_data(src_pos) != pat.Get_data(pat_pos))
			return NULL;
		else {
			int balance_bgn = pat.Get_data(pat_pos);
			int balance_end = pat.Get_data(pat_pos + 1);
			int balance_count = 1;
			while (++src_pos < src_len) {
				if (src.Get_data(src_pos) == balance_end) {
					if (--balance_count == 0)
						return src_pos + 1;
				}
				else if (src.Get_data(src_pos) == balance_bgn)
					balance_count++;
			}
		}
		return NULL;
	}

	private int max_expand(int src_pos, int pat_pos, int ep) {
		int i = 0; // counts maximum expand for item
		while	(   src_pos + i < src_len
				&&	singlematch(src.Get_data(src_pos + i), pat_pos, ep))
			i++;

		// keeps trying to match with the maximum repetitions 
		while (i >= 0) {
			int res = match(src_pos + i, ep + 1);
			if (res != NULL)
				return res;
			i--; // else didn't match; reduce 1 repetition to try again
		}
		return NULL;
	}

	private int min_expand(int src_pos, int pat_pos, int ep) {
		int src_len = src.Len_in_data();	// XOWA: cache string length; DATE: 2014-08-13
		for (;;) {
			int res = match(src_pos, ep + 1);
			if (res != NULL)
				return res;
			else if (src_pos < src_len && singlematch(src.Get_data(src_pos), pat_pos, ep))
				src_pos++; // try with one more repetition
			else
				return NULL;
		}
	}

	private int start_capture(int src_pos, int pat_pos, int what) {
		int res;
		int level = this.level;
		if (level >= MAX_CAPTURES) {
			LuaValue.error("too many captures");
		}
		capture_bgns[level] = src_pos;
		capture_lens[level] = what;
		this.level = level + 1;
		if ((res = match(src_pos, pat_pos)) == NULL) // match failed?
			this.level--; // undo capture
		return res;
	}

	private int end_capture(int src_pos, int pat_pos) {
		int l = capture_to_close();
		int res;
		capture_lens[l] = src_pos - capture_bgns[l]; // close capture
		if ((res = match(src_pos, pat_pos)) == NULL) // match failed?
			capture_lens[l] = CAP_UNFINISHED; // undo capture
		return res;
	}

	private int match_capture(int src_pos, int l) {
		l = check_capture(l);
		int len = capture_lens[l];
		
		if 	((src_len - src_pos) >= len
//			&& LuaString.equals(src, capture_bgns[l], src, src_pos, len))
			&& src.Eq(capture_bgns[l], src, src_pos, len)
			)
			return src_pos + len;
		else
			return NULL;
	}

	// Perform pat matching. If there is a match, returns offset into src
	// where match ends, otherwise returns -1.
	public int match(int src_pos, int pat_pos) {
		while (true) {
			// Check if we are at the end of the pat - 
			// equivalent to the '\0' case in the C version, but our pat
			// string is not NUL-terminated.
			if (pat_pos == pat_len)
				return src_pos;
			switch (pat.Get_data(pat_pos)) {
				case '(': // start capture
					if (++pat_pos < pat_len && pat.Get_data(pat_pos) == ')') // position capture?
						return start_capture(src_pos, pat_pos + 1, CAP_POSITION);
					else
						return start_capture(src_pos, pat_pos, CAP_UNFINISHED);
				case ')': // end capture
					return end_capture(src_pos, pat_pos + 1);
				case StringLib.L_ESC:
					if (pat_pos + 1 == pat_len)
						LuaValue.error("malformed pat (ends with '%')");
					switch (pat.Get_data(pat_pos + 1)) {
						case 'b': // balanced string?
							src_pos = matchbalance(src_pos, pat_pos + 2);
							if (src_pos == NULL) return NULL;
							pat_pos += 4; // NOTE assumes <> are ASCII length %b<> 
							continue;
						case 'f': {// frontier?
							pat_pos += 2;
							if (pat.Get_data(pat_pos) != '[') {
								LuaValue.error("Missing [after %f in pat");
							}
							int ep = classend(pat_pos);
							int previous = (src_pos == 0) ? -1 : src.Get_data(src_pos - 1);
							if (							 matchbracketclass(previous				, pat_pos, ep - 1) ||
								 (src_pos < src.Len_in_data() && 	!matchbracketclass(src.Get_data(src_pos)	, pat_pos, ep - 1)))	// XOWA: (1) added bounds check of "src_pos < src.m_length"; DATE:2014-08-14; (2) fixed by changing from "matchbracketclass" to "!matchbracketclass"; PAGE:en.w:A; DATE:2016-01-28 
								return NULL;
							pat_pos = ep;
							continue;
						}
						default: {
							int c = pat.Get_data(pat_pos + 1);
							if (Character.isDigit((char) c)) {
								src_pos = match_capture(src_pos, c);
								if (src_pos == NULL)
									return NULL;
								return match(src_pos, pat_pos + 2);
							}
						}
					}
				case '$':
					if (pat_pos + 1 == pat_len) // is the `$' the last char in pat?
						return (src_pos == src_len) ? src_pos : NULL; // check end of string
			}
			
			int ep = classend(pat_pos);
			boolean m = src_pos < src_len && singlematch(src.Get_data(src_pos), pat_pos, ep);
			int pc = (ep < pat_len) ? pat.Get_data(ep) : '\0';
			switch (pc) {
				case '?': // optional
					int res;
					if (m && ((res = match(src_pos + 1, ep + 1)) != NULL))
						return res;
					pat_pos = ep + 1;
					continue;
				case '*': // 0 or more repetitions
					return max_expand(src_pos, pat_pos, ep);
				case '+': // 1 or more repetitions
					return (m ? max_expand(src_pos + 1, pat_pos, ep) : NULL);
				case '-': // 0 or more repetitions (minimum)
					return min_expand(src_pos, pat_pos, ep);
				default:
					if (!m)
						return NULL;
					src_pos++;
					pat_pos = ep;
					continue;
			}
		}
	}

	private static final int NULL = -1;
	private static final int MAX_CAPTURES = 32;
	private static final int CAP_UNFINISHED = -1;
	public static final int CAP_POSITION = -2;
}

