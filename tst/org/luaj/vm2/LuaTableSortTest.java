package org.luaj.vm2;

import org.junit.Assert;
import org.junit.Test;

public class LuaTableSortTest {
	private final Luaj_script_fxt fxt = new Luaj_script_fxt();

	@Test
	public void Basic() {
		// reverse-sorted
		testSortBasic("'b', 'a'", "'a', 'b'");
		testSortBasic("'c', 'b', 'a'", "'a', 'b', 'c'");
		testSortBasic("'d', 'c', 'b', 'a'", "'a', 'b', 'c', 'd'");
		testSortBasic("'e', 'd', 'c', 'b', 'a'", "'a', 'b', 'c', 'd', 'e'");

		// already-sorted
		testSortBasic("'a'");
		testSortBasic("'a', 'b'");
		testSortBasic("'a', 'b', 'c'");
		testSortBasic("'a', 'b', 'c', 'd'");
		testSortBasic("'a', 'b', 'c', 'd', 'e'");
		testSortBasic("'a', 'a', 'a', 'a', 'a'");

		// randomly-sorted
		testSortBasic("'d', 'c', 'b', 'a'", "'a', 'b', 'c', 'd'");
		testSortBasic("'a', 'd', 'c', 'b', 'e'", "'a', 'b', 'c', 'd', 'e'");
		testSortBasic
		("'a', 'b', 'c', 'd', 'e', 'b', 'c', 'd', 'e', 'b', 'c', 'd', 'e', 'b', 'c', 'd', 'e', 'b', 'c', 'd', 'e', 'd', 'e', 'b', 'c', 'd', 'e', 'd', 'e', 'b', 'c', 'd', 'e', 'd', 'e', 'b', 'c', 'd', 'e', 'd', 'e', 'b', 'c', 'd', 'e'"
		, "'a', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e', 'e'");
	}

	@Test
	public void Comparator() {
		// reverse-sorted
		testSortComparator("{id=3, data='c'}, {id=2, data='b'}, {id=1, data='a'}", "'1', '2', '3'");

		// randomly-sorted
		testSortComparator("{id=1, data='a'}, {id=4, data='d'}, {id=3, data='c'}, {id=2, data='b'}, {id=5, data='e'}", "'1', '2', '3', '4', '5'");

		// stable-sort; ISSUE#:743 DATE:2020-06-13
		testSortComparator("{id=1, data='a'}, {id=2, data='a'}, {id=3, data='a'}", "'1', '2', '3'");
	}

	private void testSortBasic(String tblStr) {testSortBasic(tblStr, tblStr);}
	private void testSortBasic(String tblStr, String expd) {
		fxt.Clear();
		fxt.Init__script
			( "local tbl = {" + tblStr + "};"
			, "table.sort(tbl);"
			, "local rv = '';"
			, "table.foreachi"
			, "( tbl"
			, ", function (k, v)"
			, "    if rv ~= '' then"
			, "      rv = rv .. ', '"
			, "    end"
			, "    rv = rv .. '\\'' .. v .. '\\''"
			, "  end"
			, ");"
			, "return rv;"
			);
		fxt.Test(expd);
	}
	private void testSortComparator(String tblStr, String expd) {
		fxt.Clear();
		fxt.Init__script
			( "local tbl = {" + tblStr + "};"
			, "table.sort(tbl, function(lhs, rhs) return lhs.data < rhs.data end);"
			, "local rv = '';"
			, "table.foreachi"
			, "( tbl"
			, ", function (k, v)"
			, "    if rv ~= '' then"
			, "      rv = rv .. ', '"
			, "    end"
			, "    rv = rv .. '\\'' .. v.id .. '\\''"
			, "  end"
			, ");"
			, "return rv;"
			);
		fxt.Test(expd);
	}
}