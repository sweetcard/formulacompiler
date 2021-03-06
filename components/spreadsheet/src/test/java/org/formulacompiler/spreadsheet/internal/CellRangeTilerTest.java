/*
 * Copyright (c) 2006-2009 by Abacus Research AG, Switzerland.
 * All rights reserved.
 *
 * This file is part of the Abacus Formula Compiler (AFC).
 *
 * For commercial licensing, please contact sales(at)formulacompiler.com.
 *
 * AFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AFC.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.formulacompiler.spreadsheet.internal;

import junit.framework.TestCase;


public class CellRangeTilerTest extends TestCase
{
	private static final Rect ORIG = r( 2, 4, 6, 8 );
	private static final Rect N = null;
	private final SpreadsheetImpl ss = new SpreadsheetImpl();

	public CellRangeTilerTest()
	{
		new SheetImpl( this.ss );
	}


	public void testTilingAround()
	{
		assertTiling( +5, +5, new Rect[ CellRange.NO_INTERSECTION ] );

		assertTiling( 0, 0, ORIG );

		assertTiling( -1, 0, N, N, N, r( 1, 1, 6, 8 ), r( 2, 3, 6, 8 ), N, N, N, N );
		assertTiling( +1, 0, N, N, N, N, r( 3, 4, 6, 8 ), r( 5, 5, 6, 8 ), N, N, N );
		assertTiling( 0, -1, N, r( 2, 4, 5, 5 ), N, N, r( 2, 4, 6, 7 ), N, N, N, N );
		assertTiling( 0, +1, N, N, N, N, r( 2, 4, 7, 8 ), N, N, r( 2, 4, 9, 9 ), N );

		assertTiling( -1, -1, r( 1, 1, 5, 5 ), r( 2, 3, 5, 5 ), N, r( 1, 1, 6, 7 ), r( 2, 3, 6, 7 ), N, N, N, N );
		assertTiling( +1, -1, N, r( 3, 4, 5, 5 ), r( 5, 5, 5, 5 ), N, r( 3, 4, 6, 7 ), r( 5, 5, 6, 7 ), N, N, N );
		assertTiling( -1, +1, N, N, N, r( 1, 1, 7, 8 ), r( 2, 3, 7, 8 ), N, r( 1, 1, 9, 9 ), r( 2, 3, 9, 9 ), N );
		assertTiling( +1, +1, N, N, N, N, r( 3, 4, 7, 8 ), r( 5, 5, 7, 8 ), N, r( 3, 4, 9, 9 ), r( 5, 5, 9, 9 ) );

		assertTiling( -1, -1, +2, +2, r( 1, 1, 5, 5 ), r( 2, 4, 5, 5 ), r( 5, 5, 5, 5 ), r( 1, 1, 6, 8 ), ORIG, r( 5, 5,
				6, 8 ), r( 1, 1, 9, 9 ), r( 2, 4, 9, 9 ), r( 5, 5, 9, 9 ) );
		assertTiling( +1, +1, -2, -2, r( 3, 3, 7, 7 ) );
	}


	private void assertTiling( int _moveLeftBy, int _moveDownBy, Rect... _expected )
	{
		assertTiling( ORIG.moveBy( _moveLeftBy, _moveDownBy ), _expected );
	}

	private void assertTiling( int _moveLeftBy, int _moveDownBy, int _extendRightBy, int _extendDownBy,
			Rect... _expected )
	{
		assertTiling( ORIG.moveBy( _moveLeftBy, _moveDownBy ).extendBy( _extendRightBy, _extendDownBy ), _expected );
	}

	private void assertTiling( Rect _with, Rect... _expected )
	{
		final CellRange inner = cr( ORIG );
		final CellRange with = cr( _with );
		final CellRange[] result = with.tilingAround( inner );

		assertEquals( "Length", _expected.length, result.length );
		for (int iResult = 0; iResult < result.length; iResult++) {
			final Rect e = _expected[ iResult ];
			if (null == e) {
				assertNull( "Slot " + iResult, result[ iResult ] );
			}
			else {
				final Rect r = r( result[ iResult ] );
				assertRect( "Slot " + iResult, e, r );
			}
		}
	}


	private void assertRect( String _msg, Rect _e, Rect _a )
	{
		assertEquals( _msg + " left", _e.left, _a.left );
		assertEquals( _msg + " right", _e.right, _a.right );
		assertEquals( _msg + " top", _e.top, _a.top );
		assertEquals( _msg + " bottom", _e.bottom, _a.bottom );
	}


	private CellRange cr( Rect _rect )
	{
		final CellIndex from = new CellIndex( this.ss, 0, _rect.left, _rect.top );
		final CellIndex to = new CellIndex( this.ss, 0, _rect.right, _rect.bottom );
		return CellRange.getCellRange( from, to );
	}


	private static Rect r( int l, int r, int t, int b )
	{
		return new Rect( l, r, t, b );
	}

	private static Rect r( CellRange _rng )
	{
		final CellIndex tl = _rng.getFrom();
		final CellIndex br = _rng.getTo();
		return new Rect( tl.columnIndex, br.columnIndex, tl.rowIndex, br.rowIndex );
	}


	private static final class Rect
	{
		public final int left, right, top, bottom;

		public Rect( int l, int r, int t, int b )
		{
			this.left = l;
			this.right = r;
			this.top = t;
			this.bottom = b;
		}

		public Rect moveBy( int _moveLeftBy, int _moveDownBy )
		{
			return new Rect( this.left + _moveLeftBy, this.right + _moveLeftBy, this.top + _moveDownBy, this.bottom
					+ _moveDownBy );
		}

		public Rect extendBy( int _extendRightBy, int _extendDownBy )
		{
			return new Rect( this.left, this.right + _extendRightBy, this.top, this.bottom + _extendDownBy );
		}

	}

}
