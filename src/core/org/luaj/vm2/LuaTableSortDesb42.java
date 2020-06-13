package org.luaj.vm2;

class LuaTableSortDesb42 {
	private final LuaValue[] array;
	private final Metatable m_metatable;
	private final LuaValue cmpfunc;

	public LuaTableSortDesb42(Metatable m_metatable, LuaValue[] array, int size, LuaValue comparator) {
		this.array = array;
		this.m_metatable = m_metatable;
		this.cmpfunc = comparator;
	}
	public void sort(int size) {
		auxsort(0, size-1);
	}
	private void auxsort (int lower, int upper) {
		while (lower < upper) {	/* for tail recursion */
			int i, j;
			/* sort elements a[lower], a[(lower+upper)/2] and a[upper] */
			if (compare(upper, lower))	/* a[upper] < a[lower]? */
				swap(lower, upper);	/* swap a[lower] - a[upper] */
			if (upper-lower == 1) break;	/* only 2 elements */
			i = (lower+upper)/2;
			if (compare(i, lower))	/* a[i]<a[lower]? */
				swap(i, lower);
			else {
				if (compare(upper, i))	/* a[upper]<a[i]? */
					swap(i, upper);
			}
			if (upper-lower == 2) break;	/* only 3 elements */
			swap(i, upper); // pivot
			/* a[lower] <= P == a[upper-1] <= a[upper], only need to sort from lower+1 to upper-2 */
			i = lower-1; j = upper;
			for (;;) {	/* invariant: a[lower..i] <= P <= a[j..upper] */
				compare(0, upper); // condition 'b' as Pivot
				/* repeat ++i until a[i] >= P */
				while (compare_b(++i) && i <= upper) {
				}
				compare(upper, 0); // condition 'a' as Pivot
				/* repeat --j until a[j] <= P */
				while (compare_a(--j) && j > lower) {
				}
				// check if pointers cross
				if (j<=i) {
					break;
				}
				swap(i, j);
			}
			swap(upper, i);	/* swap pivot (a[upper]) with a[i] */
			/* a[lower..i-1] <= a[i] == P <= a[i+1..upper] */
			/* adjust so that smaller half is in [j..i] and larger one in [lower..upper] */
			if (i-lower < upper-i) {
				j=lower; i=i-1; lower=i+2;
			}
			else {
				j=i+1; i=upper; upper=j-2;
			}
			auxsort(j, i);	/* call recursively the smaller one */
		} /* repeat the routine for the larger one */
	}

	// NOTE: caching values `a` and `b` for PERF reasons; also, emulates LuaC's stack-processing
	private LuaValue a, b;
	private boolean compare_a(int j) {
		if (m_metatable == null) {
			b = array[j];
		} else {
			b = m_metatable.arrayget(array, j);
		}
		return cmp(a, b);
	}

	private boolean compare_b(int i) {
		if (m_metatable == null) {
			a = array[i];
		} else {
			a = m_metatable.arrayget(array, i);
		}
		return cmp(a, b);
	}

	private boolean compare(int i, int j) {
		if (m_metatable == null) {
			a = array[i];
			b = array[j];
		} else {
			a = m_metatable.arrayget(array, i);
			b = m_metatable.arrayget(array, j);
		}
		return cmp(a, b);
	}

	private boolean cmp(LuaValue a, LuaValue b) {
		if ( a == null || b == null )
			return false;
		if ( ! cmpfunc.isnil() ) {
			return cmpfunc.call(a,b).toboolean();
		} else {
			return a.lt_b(b);
		}
	}

	private void swap(int i, int j) {
		LuaValue a = array[i];
		array[i] = array[j];
		array[j] = a;
	}
}
