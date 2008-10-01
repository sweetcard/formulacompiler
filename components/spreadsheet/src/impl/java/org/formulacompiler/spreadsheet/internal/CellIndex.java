/*
 * Copyright (c) 2006, 2008 by Abacus Research AG, Switzerland.
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

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.formulacompiler.compiler.internal.DescriptionBuilder;
import org.formulacompiler.spreadsheet.Orientation;
import org.formulacompiler.spreadsheet.Spreadsheet.Cell;
import org.formulacompiler.spreadsheet.Spreadsheet.Range;
import org.formulacompiler.spreadsheet.SpreadsheetException;

public final class CellIndex extends CellRange implements Cell
{
	public static final int BROKEN_REF = -1;
	static final int MAX_INDEX = Integer.MAX_VALUE - 1;

	public final SpreadsheetImpl spreadsheet;
	final int sheetIndex;
	final int columnIndex;
	final int rowIndex;
	public final boolean isColumnIndexAbsolute;
	public final boolean isRowIndexAbsolute;


	public CellIndex( SpreadsheetImpl _spreadsheet, int _sheetIndex, int _columnIndex, boolean _columnIndexAbsolute,
			int _rowIndex, boolean _rowIndexAbsolute )
	{
		assert _sheetIndex < _spreadsheet.getSheetList().size();
		assert _sheetIndex >= BROKEN_REF;
		assert _columnIndex >= BROKEN_REF;
		assert _rowIndex >= BROKEN_REF;
		this.spreadsheet = _spreadsheet;
		this.sheetIndex = _sheetIndex;
		this.columnIndex = _columnIndex;
		this.rowIndex = _rowIndex;
		this.isColumnIndexAbsolute = _columnIndexAbsolute;
		this.isRowIndexAbsolute = _rowIndexAbsolute;
	}


	public CellIndex( SpreadsheetImpl _spreadsheet, int _sheetIndex, int _columnIndex, int _rowIndex )
	{
		this( _spreadsheet, _sheetIndex, _columnIndex, false, _rowIndex, false );
	}


	public int getColumnIndex()
	{
		if (this.columnIndex == BROKEN_REF) {
			throw new SpreadsheetException.BrokenReference( "Broken reference: " + toString() );
		}
		return this.columnIndex;
	}


	public int getRowIndex()
	{
		if (this.rowIndex == BROKEN_REF) {
			throw new SpreadsheetException.BrokenReference( "Broken reference: " + toString() );
		}
		return this.rowIndex;
	}


	public static final CellIndex getTopLeft( SpreadsheetImpl _spreadsheet )
	{
		return new CellIndex( _spreadsheet, 0, 0, 0 );
	}

	public static final CellIndex getBottomRight( SpreadsheetImpl _spreadsheet )
	{
		final int lastSheetIndex = _spreadsheet.getSheetList().size() - 1;
		return new CellIndex( _spreadsheet, lastSheetIndex, MAX_INDEX, MAX_INDEX );
	}


	public boolean equals( CellIndex _other )
	{
		if (this == _other) return true;
		return this.spreadsheet == _other.spreadsheet
				&& this.sheetIndex == _other.sheetIndex && this.rowIndex == _other.rowIndex
				&& this.columnIndex == _other.columnIndex;
	}


	@Override
	public boolean equals( Object _obj )
	{
		if (this == _obj) return true;
		if (_obj instanceof CellIndex) {
			return equals( (CellIndex) _obj );
		}
		return false;
	}


	@Override
	public int hashCode()
	{
		// Final, immutable class, so this should be SMP safe.
		if (this.hashCode == 0) {
			this.hashCode = ((17 * 59 + this.sheetIndex) * 59 + this.rowIndex) * 59 + this.columnIndex;
		}
		return this.hashCode;
	}

	private transient int hashCode;


	public SheetImpl getSheet()
	{
		return this.spreadsheet.getSheetList().get( getSheetIndex() );
	}

	public int getSheetIndex()
	{
		if (this.sheetIndex == BROKEN_REF) {
			throw new SpreadsheetException.BrokenReference( "Broken reference: " + toString() );
		}
		return this.sheetIndex;
	}

	public RowImpl getRow()
	{
		SheetImpl sheet = getSheet();
		if (getRowIndex() < sheet.getRowList().size()) {
			return sheet.getRowList().get( getRowIndex() );
		}
		else {
			return null;
		}
	}


	public CellInstance getCell()
	{
		RowImpl row = getRow();
		if (null != row && getColumnIndex() < row.getCellList().size()) {
			return row.getCellList().get( getColumnIndex() );
		}
		else {
			return null;
		}
	}


	public Object getConstantValue()
	{
		final CellInstance cell = getCell();
		if (cell instanceof CellWithConstant) {
			return cell.getValue();
		}
		else {
			return null;
		}
	}

	public String getErrorText()
	{
		final CellInstance cell = getCell();
		if (cell instanceof CellWithError) {
			return (String) cell.getValue();
		}
		else {
			return null;
		}
	}


	public Object getValue()
	{
		final CellInstance cell = getCell();
		return (cell == null) ? null : cell.getValue();
	}


	public String getExpressionText() throws SpreadsheetException
	{
		final CellInstance cell = getCell();
		if (cell instanceof CellWithExpression) {
			return ((CellWithExpression) cell).getExpression().toString();
		}
		else {
			return null;
		}
	}


	public int getIndex( Orientation _orientation )
	{
		switch (_orientation) {
			case HORIZONTAL:
				return getColumnIndex();
			case VERTICAL:
				return getRowIndex();
		}
		assert false;
		return -1;
	}


	public CellIndex setIndex( Orientation _orientation, int _index )
	{
		switch (_orientation) {
			case HORIZONTAL:
				return new CellIndex( this.spreadsheet, this.sheetIndex, _index, this.rowIndex );
			case VERTICAL:
				return new CellIndex( this.spreadsheet, this.sheetIndex, this.columnIndex, _index );
		}
		assert false;
		return null;
	}


	public CellIndex getAbsoluteIndex( boolean _columnAbsolute, boolean _rowAbsolute )
	{
		return new CellIndex( this.spreadsheet, this.sheetIndex, this.columnIndex, _columnAbsolute, this.rowIndex,
				_rowAbsolute );
	}


	@Override
	public void describeTo( DescriptionBuilder _to )
	{
		final DescribeR1C1Style r1c1Style = _to.getContext( DescribeR1C1Style.class );
		if (null == r1c1Style) {
			final boolean shortStyle = _to.getContext( DescribeShortStyle.class ) != null;
			final boolean columnIndexAbsolute = this.isColumnIndexAbsolute && !shortStyle;
			final boolean rowIndexAbsolute = this.isRowIndexAbsolute && !shortStyle;
			final SheetImpl contextSheet = _to.getContext( SheetImpl.class );
			_to.append( getNameA1ForCellIndex( columnIndexAbsolute, rowIndexAbsolute, contextSheet ) );
		}
		else {
			if (isReferenceBroken( this )) {
				_to.append( "#REF!" );
			}
			else {
				final CellIndex relativeTo = r1c1Style.getRelativeTo();
				if (relativeTo != null && isReferenceBroken( relativeTo )) {
					_to.append( "#REF!" );
				}
				else {
					if (this.sheetIndex != ((relativeTo != null) ? relativeTo.sheetIndex : 0)) {
						_to.append( '\'' ).append( getSheet().getName() ).append( "'!" );
					}
					if (null == relativeTo) {
						_to.append( 'R' ).append( this.rowIndex + 1 ).append( 'C' ).append( this.columnIndex + 1 );
					}
					else {
						if (this.isRowIndexAbsolute) _to.append( 'R' ).append( this.rowIndex + 1 );
						else describeOffsetTo( _to, 'R', this.rowIndex - relativeTo.rowIndex );
						if (this.isColumnIndexAbsolute) _to.append( 'C' ).append( this.columnIndex + 1 );
						else describeOffsetTo( _to, 'C', this.columnIndex - relativeTo.columnIndex );
					}
				}
			}
		}
	}

	String getNameA1ForCellIndex( boolean _columnIndexAbsolute, boolean _rowIndexAbsolute, SheetImpl _contextSheet )
	{
		final StringBuilder result = new StringBuilder();
		if (this.sheetIndex == CellIndex.BROKEN_REF) {
			result.append( "#REF!" );
		}
		else if (_contextSheet == null || this.sheetIndex != _contextSheet.getSheetIndex()) {
			final SheetImpl sheet = getSheet();
			final String name = sheet.getName();
			final boolean quoted = name.contains( " " ) || name.contains( "'" ) || name.contains( "-" );
			if (quoted) {
				result.append( '\'' );
			}
			result.append( sheet.getName().replace( "'", "''" ) );
			if (quoted) {
				result.append( '\'' );
			}
			result.append( "!" );
		}
		appendNameA1ForCellIndex( result, this.columnIndex, _columnIndexAbsolute, this.rowIndex, _rowIndexAbsolute );
		return result.toString();
	}

	public static void appendNameA1ForCellIndex( final StringBuilder _result, final CellIndex _cellIndex )
	{
		appendNameA1ForCellIndex( _result, _cellIndex.columnIndex, _cellIndex.isColumnIndexAbsolute, _cellIndex.rowIndex, _cellIndex.isRowIndexAbsolute );
	}

	public static void appendNameA1ForCellIndex( final StringBuilder _result, final int _columnIndex, final boolean _columnIndexAbsolute, final int _rowIndex, final boolean _rowIndexAbsolute )
	{
		if (_columnIndexAbsolute) {
			_result.append( '$' );
		}
		if (_columnIndex == BROKEN_REF) {
			_result.append( "#REF!" );
		}
		else {
			appendColumn( _result, _columnIndex );
		}
		if (_rowIndexAbsolute) {
			_result.append( '$' );
		}
		if (_rowIndex == BROKEN_REF) {
			_result.append( "#REF!" );
		}
		else {
			_result.append( _rowIndex + 1 );
		}
	}

	private static void appendColumn( StringBuilder _result, int _columnIndex )
	{
		int insPos = _result.length();
		int col = _columnIndex;
		while (col >= 0) {
			int digit = col % 26;
			_result.insert( insPos, (char) ('A' + digit) );
			col = col / 26 - 1;
		}
	}


	private void describeOffsetTo( DescriptionBuilder _to, char _prefix, int _offset )
	{
		_to.append( _prefix );
		if (_offset != 0) {
			_to.append( '[' ).append( _offset ).append( ']' );
		}
	}

	private static boolean isReferenceBroken( CellIndex _cellIndex )
	{
		return _cellIndex.sheetIndex == BROKEN_REF || _cellIndex.rowIndex == BROKEN_REF || _cellIndex.columnIndex == BROKEN_REF;
	}

	@Override
	public CellIndex getFrom()
	{
		return this;
	}

	@Override
	public CellIndex getTo()
	{
		return this;
	}

	@Override
	public CellIndex getCellIndexRelativeTo( final CellIndex _cell ) throws SpreadsheetException
	{
		return this;
	}

	public boolean contains( final Range _other )
	{
		return equals( _other );
	}

	public Cell getTopLeft()
	{
		return this;
	}

	public Cell getBottomRight()
	{
		return this;
	}

	public Iterable<Cell> cells()
	{
		return Collections.singleton( (Cell) this );
	}

	public Iterator<CellIndex> iterator()
	{
		return new Iterator<CellIndex>()
		{
			private boolean hasNext = true;

			public boolean hasNext()
			{
				return this.hasNext;
			}

			public CellIndex next()
			{
				if (this.hasNext) {
					this.hasNext = false;
					return CellIndex.this;
				}
				throw new NoSuchElementException();
			}

			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public CellRange clone( int _colOffset, int _rowOffset )
	{
		final int colIndex = this.isColumnIndexAbsolute ? this.columnIndex : (this.columnIndex + _colOffset);
		final int rowIndex = this.isRowIndexAbsolute ? this.rowIndex : (this.rowIndex + _rowOffset);
		return new CellIndex( this.spreadsheet, this.sheetIndex,
				colIndex, this.isColumnIndexAbsolute, rowIndex, this.isRowIndexAbsolute);
	}

}