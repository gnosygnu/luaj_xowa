package org.luaj.vm2;

import junit.framework.TestCase;

public class LuaDoubleTest extends TestCase {

	public void test_tojstring() {
		assert_tojstring(123d                      , "123");                // tests "if ( l == v )"
		assert_tojstring(Double.NaN                , "nan");                // tests "if ( Double.isNaN(v) )"
		assert_tojstring(Double.POSITIVE_INFINITY  , "inf");                // tests "if ( Double.isInfinite(v) )"
		assert_tojstring(Double.NEGATIVE_INFINITY  , "-inf");               // tests "if ( Double.isInfinite(v) )"
		assert_tojstring(12.34                     , "12.34");              // fails with "12.340000000000"
		assert_tojstring(12.34000d                 , "12.34");              // tests trim last zeroes
		assert_tojstring(12.00000d                 , "12");                 // tests trim all zeroes
		assert_tojstring(12.01010d                 , "12.0101");            // tests trim alternating zeroes
		assert_tojstring(4070000000.0000005d       , "4070000000");         // fails with "4070000000.0000"
		assert_tojstring(.00000000000000005d       , "5e-17");              // fails with "5.0000000000000e-17"
		assert_tojstring(.000000000000000051234d   , "5.1234e-17");         // tests fraction in exponent
		assert_tojstring(94510558.33929849d        , "94510558.339298");    // asserts no failure when no zeroes; EX: "94510558.33929849"
	}

	private void assert_tojstring(double val, String expd) {
		LuaNumber lua = LuaDouble.valueOf(val);
		assertEquals(expd, lua.tojstring());
	}
}
