package org.luaj.vm2.xowa;

import org.junit.*;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;

public class LuaTable_tst {
	@Test public void set_should_not_autocast_strings_to_int() {
		LuaTable tbl = new LuaTable();
		tbl.set("ignore", "a");
		tbl.set("k", 2);
		tbl.set("k", "2.");
		Assert.assertEquals("fail", LuaString.valueOf("2."), tbl.get("k"));
	}
}
