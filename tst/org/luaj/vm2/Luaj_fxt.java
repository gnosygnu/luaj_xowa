package org.luaj.vm2;

import gplx.objects.errs.Err_;
import gplx.objects.types.Type_ids_;

public class Luaj_fxt {
	public static Varargs New_varargs(Object... ary) {
		int ary_len = ary.length;
		LuaValue[] rv = new LuaValue[ary_len];
		for (int i = 0; i < ary_len; i++) {
			Object itm = ary[i];
			LuaValue lv = null;
			if (itm instanceof LuaFunction) {
				lv = (LuaFunction)itm;
			}
			else if (itm instanceof LuaString) {
				lv = (LuaString)itm;
			}
			else if (itm instanceof LuaTable) {
				lv = (LuaTable)itm;
			}
			else {
				int itm_type = Type_ids_.To_id_by_obj(itm);
				switch (itm_type) {
					case Type_ids_.Id__str:
						lv = LuaString.valueOf((String)itm);
						break;
					case Type_ids_.Id__int:
						lv = LuaNumber.valueOf((Integer)itm);
						break;
					default:
						throw Err_.New_unhandled_default(itm_type);
				}
			}
			rv[i] = lv;
		}		
		return LuaValue.varargsOf(rv);
	} 
}
