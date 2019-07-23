package org.luaj.vm2.lib;

import org.junit.Test;
import org.luaj.vm2.Luaj_script_fxt;

public class StringLib_gsub_tst {
	private final Luaj_script_fxt fxt = new Luaj_script_fxt();
	@Test public void Malformed() {
		fxt.Clear();
		fxt.Init__script
			( "return string.gsub"
			, "( arg1"
			, ", '([^a-z])'"
			, ", function (c)"
			, "    return '{' .. string.byte(c, 1, 1) .. '}'"
			, "  end"
			, ");"
			);
		fxt.Init__arg("arg1", "xæy");
		fxt.Test("x{195}{166}y"); // fails if {239}{239}; ISSUE#:504; DATE:2019-07-22
	}	
}
