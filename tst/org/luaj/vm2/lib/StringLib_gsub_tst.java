package org.luaj.vm2.lib;

import org.junit.Test;
import org.luaj.vm2.Luaj_script_fxt;

public class StringLib_gsub_tst {
	private final StringLib_fxt fxt = new StringLib_fxt();
	@Test public void Malformed() {
		Luaj_script_fxt script_fxt = new Luaj_script_fxt();
		script_fxt.Clear();
		script_fxt.Init__script
			( "return string.gsub"
			, "( arg1"
			, ", '([^a-z])'"
			, ", function (c)"
			, "    return '{' .. string.byte(c, 1, 1) .. '}'"
			, "  end"
			, ");"
			);
		script_fxt.Init__arg("arg1", "xæy");
		script_fxt.Test("x{195}{166}y"); // fails if {239}{239}; ISSUE#:504; DATE:2019-07-22
	}
	@Test public void Percent_at_eos() { // ISSUE#:571; DATE:2019-09-08
		fxt.Test__gsub("a", "a", "%", "%");
	}
}
