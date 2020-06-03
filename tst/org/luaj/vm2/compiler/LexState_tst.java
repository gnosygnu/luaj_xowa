package org.luaj.vm2.compiler;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import org.junit.*;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.lib.jse.JsePlatform;

public class LexState_tst {
	private final LexState_fxt fxt = new LexState_fxt();
	@Test   public void Var() {
		fxt.Test__var("â");   // 1-byte
		fxt.Test__var("abz");   // 1-byte
		fxt.Test__var("a¢z");   // 2-byte
		fxt.Test__var("a€z");   // 3-byte
		fxt.Test__var("a𤭢z");  // 4-byte
	}
	@Test  public void Gsub() {
		fxt.Test__gsub("abcdz", "bcd", "n", "anz");
		fxt.Test__gsub("a\\226\\130\\172z", "\\226\\130\\172", "n", "anz");
		// fxt.Test__gsub("a\\226\\130\\172z", "\\226\\130[\\172]", "n", "an172z"); // DATE:2018-09-03: used to fail
	}
	@Test  public void Utf8_bracketed() {
		fxt.Test__gmatch("[[a]]", "([^[]*)(%[%[[^[]*%]%])([^[]*)", ";");
	}
	@Test public void Ascii() {
		fxt.Test__gsub("[a\tc]", "\\009", "b", "[abc]");
	}
}
class LexState_fxt {
	public void Test__gsub(String str, String find, String repl, String expd) {
		Test__script(expd, Concat_w_nl
				( "local str = '" + str  + "';"
				, "return str:gsub('" + find + "', '" + repl + "');"
				));
	}
	public void Test__var(String str) {
		Test__script(str, "return '" + str + "';");
	}
	public void Test__gmatch(String str, String pat, String expd) {
		Test__script(expd, Concat_w_nl
				( "local rv = '';"
				, "for val in string.gmatch('" + str + "', '" + pat + "') do"
				, "   rv = rv .. val .. ';'"
				, "end"
				, "return rv"
				));
	}
	private static String Concat_w_nl(String... ary) {
		StringBuffer sb = new StringBuffer();
		int len = ary.length;
		for (int i = 0; i < len; i++) {
			if (i != 0) sb.append("\n");
			sb.append(ary[i]);
		}
		return sb.toString();
	}
	private static void Test__script(String expd, String script) {
		Object actl = Exec__script(script);
		Assert.assertEquals(expd, actl.toString());
	}
	private static Object Exec__script(String script) {
		ByteArrayInputStream is = new ByteArrayInputStream(script.getBytes(Charset.forName("UTF-8")));
		Globals globals = JsePlatform.standardGlobals();
		LuaValue chunk = globals.load(is, "script", "t", globals);
		return chunk.call();
	}
}