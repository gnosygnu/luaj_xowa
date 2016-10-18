/*******************************************************************************
* Copyright (c) 2011 Luaj.org. All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
******************************************************************************/
package org.luaj.vm2.lib.jse;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceLuaToJava.Coercion;

/**
 * Java method or constructor.
 * <p>
 * Primarily handles argument coercion for parameter lists including scoring of compatibility and 
 * java varargs handling.
 * <p>
 * This class is not used directly.  
 * It is an abstract base class for {@link JavaConstructor} and {@link JavaMethod}.
 * @see JavaConstructor
 * @see JavaMethod
 * @see CoerceJavaToLua
 * @see CoerceLuaToJava
 */
abstract
class JavaMember extends VarArgFunction {
	
	static final int METHOD_MODIFIERS_VARARGS = 0x80;

	final Coercion[] fixedargs;
	final Coercion varargs;
	
	protected JavaMember(Class[] params, int modifiers) {
		boolean isvarargs = ((modifiers & METHOD_MODIFIERS_VARARGS) != 0);
		fixedargs = new CoerceLuaToJava.Coercion[isvarargs? params.length-1: params.length];
		for ( int i=0; i<fixedargs.length; i++ )
			fixedargs[i] = CoerceLuaToJava.getCoercion( params[i] );
		varargs = isvarargs? CoerceLuaToJava.getCoercion( params[params.length-1] ): null;
	}
	
	int score(Varargs args) {
		int n = args.narg();
		int s = n>fixedargs.length? CoerceLuaToJava.SCORE_WRONG_TYPE * (n-fixedargs.length): 0;
		for ( int j=0; j<fixedargs.length; j++ )
			s += fixedargs[j].score( args.arg(j+1) );
		if ( varargs != null )
			for ( int k=fixedargs.length; k<n; k++ )
				s += varargs.score( args.arg(k+1) );
		return s;
	}
	
	protected Object[] convertArgs(Varargs args) {
		Object[] a;
		if ( varargs == null ) {
			a = new Object[fixedargs.length];
			for ( int i=0; i<a.length; i++ )
				a[i] = fixedargs[i].coerce( args.arg(i+1) );
		} else {
			int max_lhs = fixedargs.length;
			int max_rhs = args.narg();					
			int n = max_lhs > max_rhs ? max_lhs : max_rhs; // XOWA.PERF:Math.max(fixedargs.length,args.narg()); DATE:2014-08-13
			a = new Object[n];
			for ( int i=0; i<fixedargs.length; i++ )
				a[i] = fixedargs[i].coerce( args.arg(i+1) );
			for ( int i=fixedargs.length; i<n; i++ )
				a[i] = varargs.coerce( args.arg(i+1) );
		}
		return a;
	}
	protected Object[] convertArgsWithParams(Method method, Varargs args) {	// handle varargs in Java; "int, int, int..." -> new Object[int, int, new int[]] x> new Object[int, int, int, int, int]; DATE:2016-10-15   
		Object[] rv;
		int fixed_len = fixedargs.length;
		// no varargs; just return Object[] of all fixed args; EX: "int, int, int" -> "new Object[] {int, int, int};"
		if (varargs == null) {
			rv = new Object[fixed_len];
			for (int i = 0; i < fixed_len; ++i)
				rv[i] = fixedargs[i].coerce(args.arg(i + 1));	// +1=Base1
		}
		// varargs exists; assuming luaj varargs detection is right
		else {
			int total_len = args.narg();
			int vargs_len = total_len - fixed_len;
			
			// instantiate array; note that this assumes luaj varargs detection is correct, else out of bounds error 
			Class vargs_type = method.getParameterTypes()[fixed_len];	// last arg is varargs
			Object vargs_ary = Array.newInstance(vargs_type.getComponentType(), vargs_len);
			for (int i = 0; i < vargs_len; ++i) {
				Array.set(vargs_ary, i, varargs.coerce(args.arg(fixed_len + i + 1)));
			}

			// EX: "int, int, int..." and (1, 2, 10, 11, 12) will have fixed_len of 2 and total_len of 5
			rv = new Object[fixed_len + 1];
			// fill fixed args
			for (int i = 0; i < fixed_len; ++i)
				rv[i] = fixedargs[i].coerce(args.arg(i + 1));	// +1=Base1
			rv[fixed_len] = vargs_ary;
		}
		return rv;
	}
}
