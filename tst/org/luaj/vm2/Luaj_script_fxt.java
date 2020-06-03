package org.luaj.vm2;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.luaj.vm2.lib.jse.JsePlatform;

import gplx.objects.errs.Err_;
import gplx.tests.Gftest_fxt;

public class Luaj_script_fxt {
	private String script;
	private Globals globals;
	public void Clear() {
		this.globals = JsePlatform.standardGlobals();		
	}
	public Luaj_script_fxt Init__arg(String key, String val) {
		globals.set(key, val);
		return this;
	}
	public Luaj_script_fxt Init__script(String... lines) {
		this.script = "";
		int len = lines.length;
		for (int i = 0; i < len; i++) {
			if (i != 0) script += "\n";
			script += lines[i];
		}
		return this;
	}
	public void Test(String expd) {
		Prototype prototype = New_prototype(globals, script);
        LuaFunction function = new LuaClosure(prototype, globals);
        Varargs result = function.invoke();
        Gftest_fxt.Eq__str(expd, result.arg1().tojstring());
	}
	private static Prototype New_prototype(Globals globals, String script) {		
        Reader reader = new StringReader(script);
		try {
			return globals.compilePrototype(reader, "name");
		} catch (IOException e) {
			throw Err_.New_fmt(e, "failed to compile: err={0}", Err_.Message_lang(e));
		}
	}
}
