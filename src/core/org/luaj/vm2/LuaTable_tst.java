package org.luaj.vm2;

import org.junit.*;

public class LuaTable_tst {
	private final Luaj_script_fxt fxt = new Luaj_script_fxt();
	@Test public void set_should_not_autocast_strings_to_int() {
		LuaTable tbl = new LuaTable();
		tbl.set("ignore", "a");
		tbl.set("k", 2);
		tbl.set("k", "2.");
		Assert.assertEquals("fail", LuaString.valueOf("2."), tbl.get("k"));
	}
	@Test public void foreach() {
		fxt.Clear();
		fxt.Init__script
			( "local tbl =  {['a'] = 1, ['b'] = 2};"
			, "local rv = '';"
			, "table.foreach"
			, "( tbl"
			, ", function (k, v)"
			, "    rv = rv .. k .. '=' .. v .. ';'" 
			, "  end"
			, ");"
			, "return rv;"
			);
		fxt.Test("a=1;b=2;");
	}	
	@Test public void foreachi() {
		fxt.Clear();
		fxt.Init__script
			( "local tbl =  {'a', 'b'};"
			, "local rv = '';"
			, "table.foreachi"
			, "( tbl"
			, ", function (k, v)"
			, "    rv = rv .. k .. '=' .. v .. ';'" 
			, "  end"
			, ");"
			, "return rv;"
			);
		fxt.Test("1=a;2=b;");
	}	
}