package org.luaj.vm2;

/*
REF.CODE:https://algs4.cs.princeton.edu/23quicksort/Quick.java.html
* Reproduction of Sedgewick's sort
* Note that the sort is not stable
*/
public class LuaTableSortQuick {
    private final LuaTable table;
    private final LuaValue cmpfunc;
    public LuaTableSortQuick(LuaTable table, LuaValue cmpfunc) {
        this.table = table;
        this.cmpfunc = cmpfunc;
    }
    // quicksort the subarray from a[lo] to a[hi]
    public void sort(int lo, int hi) {
        if (hi <= lo) return;
        int j = partition(lo, hi);
        sort(lo, j-1);
        sort(j+1, hi);
    }
    private int partition(int lo, int hi) {
        int i = lo;
        int j = hi + 1;
        LuaValue v = table.array[lo];
        while (true) {

            // find item on lo to swap
            while (less(++i, lo)) {
                if (i == hi) break;
            }

            // find item on hi to swap
            while (less(lo, --j)) {
                if (j == lo) break;      // redundant since a[lo] acts as sentinel
            }

            // check if pointers cross
            if (i >= j) break;

            exch(i, j);
        }

        // put partitioning item v at a[j]
        exch(lo, j);

        // now, a[lo .. j-1] <= a[j] <= a[j+1 .. hi]
        return j;
    }

    // is v < w ?
    private boolean less(int i, int j) {
        if (i == j) return false;   // optimization when reference equals
        return table.compare(i, j, cmpfunc);
    }

    // exchange a[i] and a[j]
    private void exch(int i, int j) {
        LuaValue swap = table.array[i];
        table.array[i] = table.array[j];
        table.array[j] = swap;
    }
}
