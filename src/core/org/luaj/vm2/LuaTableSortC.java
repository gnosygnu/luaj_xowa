package org.luaj.vm2;

/*
REF.LUA:https://www.lua.org/source/5.1/ltablib.c.html#auxsort
* Emulated version of Lua C's auxsort, without the `L` state and its associated stack-processing
* Original code snippets left in reference and commented out in blocks
*/
public class LuaTableSortC {
    private final LuaTable table;
    private final LuaValue cmpfunc;
    private final LuaValue[] array;
    private static final int BASE_1 = 1;
    public LuaTableSortC(LuaTable table, LuaValue cmpfunc) {
        this.table = table;
        this.array = table.array;
        this.cmpfunc = cmpfunc;
    }
    private void set2(int i, int j) {
        /*
        lua_rawseti(L, 1, i);
        lua_rawseti(L, 1, j);
        */
        LuaValue a = array[i - BASE_1];
        array[i - BASE_1] = array[j - BASE_1];
        array[j - BASE_1] = a;
    }
    private boolean sort_comp(int a, int b) {
        /*
        if (!lua_isnil(L, 2)) {  // function?
            int res;
            lua_pushvalue(L, 2);
            lua_pushvalue(L, a-1);  // -1 to compensate function
            lua_pushvalue(L, b-2);  // -2 to compensate function and `a'
            lua_call(L, 2, 1);
            res = lua_toboolean(L, -1);
            lua_pop(L, 1);
            return res;
        }
        else  // a < b?
            return lua_lessthan(L, a, b);
        */
        return table.compare(a - BASE_1, b - BASE_1, cmpfunc);
    }
    public void auxsort(int l, int u) {
        while (l < u) {  /* for tail recursion */
            int i, j;
            /* sort elements a[l], a[(l+u)/2] and a[u] */
            /*
            lua_rawgeti(L, 1, l);
            lua_rawgeti(L, 1, u);
            if (sort_comp(L, -1, -2))  // a[u] < a[l]?
            */
            if (sort_comp(u, l))  /* a[u] < a[l]? */
                set2(l, u);  /* swap a[l] - a[u] */
            /*
            else
                lua_pop(L, 2);
            */
            if (u-l == 1) break;  /* only 2 elements */
            i = (l+u)/2;
            /*
            lua_rawgeti(L, 1, i);
            lua_rawgeti(L, 1, l);
            if (sort_comp(L, -2, -1))  // a[i]<a[l]?
            */
            if (sort_comp(i, l)) {  /* a[i]<a[l]? */
                set2(i, l);
            }
            else {
                /*
                lua_pop(L, 1);  // remove a[l]
                lua_rawgeti(L, 1, u);
                if (sort_comp(tbl, -1, -2))  // a[u]<a[i]?
                */
                if (sort_comp(u, i))  /* a[u]<a[i]? */
                    set2(i, u);
                /*
                else
                    lua_pop(L, 2);
                */
            }
            if (u-l == 2) break;  /* only 3 elements */
            /*
            lua_rawgeti(L, 1, i);  // Pivot
            lua_pushvalue(L, -1);
            lua_rawgeti(L, 1, u-1);
            */
            set2(i, u-1);
            /* a[l] <= P == a[u-1] <= a[u], only need to sort from l+1 to u-2 */
            i = l; j = u-1;
            for (;;) {  /* invariant: a[l..i] <= P <= a[j..u] */
                /* repeat ++i until a[i] >= P */
                /*
                while (lua_rawgeti(L, 1, ++i), sort_comp(-1, -2)) {
                */
                while (++i < u && sort_comp(i, u - 1)) { // NOTE: u-1 b/c `lua_rawgeti(L, 1, u-1);`
                    if (i>u) table.error("invalid order function for sorting");
                    /*
                    lua_pop(L, 1);  // remove a[i]
                    */
                }
                /* repeat --j until a[j] <= P */
                /*
                while (lua_rawgeti(L, 1, --j), sort_comp( -3, -1)) {
                */
                while (--j > l && sort_comp(u - 1, j)) { // NOTE: u-1 b/c `lua_rawgeti(L, 1, u-1);`
                    if (j<l) table.error("invalid order function for sorting");
                    /*
                    lua_pop(L, 1);  // remove a[j]
                    */
                }
                if (j<i) {
                    /*
                    lua_pop(L, 3);  // pop pivot, a[i], a[j]
                    */
                    break;
                }
                set2(i, j);
            }
            /*
            lua_rawgeti(L, 1, u-1);
            lua_rawgeti(L, 1, i);
            */
            set2(u - 1, i);  /* swap pivot (a[u-1]) with a[i] */
            /* a[l..i-1] <= a[i] == P <= a[i+1..u] */
            /* adjust so that smaller half is in [j..i] and larger one in [l..u] */
            if (i-l < u-i) {
                j=l; i=i-1; l=i+2;
            }
            else {
                j=i+1; i=u; u=j-2;
            }
            auxsort(j, i);  /* call recursively the smaller one */
        } /* repeat the routine for the larger one */
    }
}
