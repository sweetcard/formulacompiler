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

package org.formulacompiler.spreadsheet.internal.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.formulacompiler.compiler.CallFrame;
import org.formulacompiler.compiler.CompilerException;
import org.formulacompiler.compiler.FormulaCompiler;
import org.formulacompiler.runtime.New;
import org.formulacompiler.spreadsheet.Spreadsheet;
import org.formulacompiler.spreadsheet.SpreadsheetBinder;
import org.formulacompiler.spreadsheet.SpreadsheetByNameBinder;
import org.formulacompiler.spreadsheet.SpreadsheetException;
import org.formulacompiler.spreadsheet.Spreadsheet.Cell;


public class SpreadsheetByNameBinderImpl implements SpreadsheetByNameBinder
{
	private final SpreadsheetBinder binder;
	private final CellBinderImpl inputBinder;
	private final CellBinderImpl outputBinder;

	public SpreadsheetByNameBinderImpl( Config _config )
	{
		super();
		_config.validate();
		this.binder = _config.binder;
		this.inputBinder = new InputCellBinder( this.binder.getRoot().getInputClass() );
		this.outputBinder = new OutputCellBinder( this.binder.getRoot().getOutputClass() );
	}

	public static final class Factory implements SpreadsheetByNameBinder.Factory
	{
		public SpreadsheetByNameBinder newInstance( Config _config )
		{
			return new SpreadsheetByNameBinderImpl( _config );
		}
	}


	public CellBinder inputs()
	{
		return this.inputBinder;
	}

	public CellBinder outputs()
	{
		return this.outputBinder;
	}


	public Map<String, Spreadsheet.Range> cellNamesLeftUnbound()
	{
		final Map<String, Spreadsheet.Range> defs = getBinder().getSpreadsheet().getRangeNames();
		final Map<String, Spreadsheet.Range> result = New.map();
		final SpreadsheetBinder binder = getBinder();
		for (Map.Entry<String, Spreadsheet.Range> def : defs.entrySet()) {
			final Spreadsheet.Range range = def.getValue();
			if (range instanceof Cell) {
				final Cell cell = (Cell) range;
				if (!binder.isInputCell( cell ) && !binder.isOutputCell( cell )) {
					result.put( def.getKey(), range );
				}
			}
		}
		return result;
	}

	public void failIfCellNamesAreStillUnbound() throws SpreadsheetException
	{
		final Map<String, Spreadsheet.Range> unbound = cellNamesLeftUnbound();
		if (unbound.size() > 0) {
			final String name = unbound.keySet().iterator().next();
			throw new SpreadsheetException.NameNotFound( "There is no input or output method named "
					+ name + "() or get" + name + "() to bind the cell " + name + " to (character case is irrelevant)." );
		}
	}


	private SpreadsheetBinder getBinder()
	{
		return this.binder;
	}

	private Spreadsheet getSpreadsheet()
	{
		return getBinder().getSpreadsheet();
	}


	private abstract class CellBinderImpl implements CellBinder
	{
		private final Method[] contextMethods;

		protected CellBinderImpl( Class _contextClass )
		{
			super();
			this.contextMethods = _contextClass.getMethods();
		}

		public void bindAllMethodsToNamedCells() throws CompilerException
		{
			bindAllMethodsToPrefixedNamedCells( "" );
		}

		public void bindAllMethodsToPrefixedNamedCells( String _prefix ) throws CompilerException
		{
			for (Method m : this.contextMethods) {
				if (m.getDeclaringClass() != Object.class) {
					if (canBind( m )) {
						bindThisMethodToNamedCell( m, _prefix );
					}
				}
			}
		}

		protected boolean canBind( Method _method )
		{
			return _method.getParameterTypes().length == 0;
		}

		protected boolean canBind( Spreadsheet.Cell _cell )
		{
			return true;
		}

		private void bindThisMethodToNamedCell( Method _m, String _cellNamePrefix ) throws CompilerException
		{
			final String fullCellName = (_cellNamePrefix + _m.getName());
			Cell cell = null;
			try {
				cell = getSpreadsheet().getCell( fullCellName );
			}
			catch (SpreadsheetException.NameNotFound e) {
				final String baseCellName = (_cellNamePrefix + getPlainCellNameFor( _m ));
				try {
					cell = getSpreadsheet().getCell( baseCellName );
				}
				catch (SpreadsheetException.NameNotFound e2) {
					return;
				}
			}
			if (cell != null && canBind( cell )) {
				bindCell( cell, FormulaCompiler.newCallFrame( _m ) );
			}
		}

		private String getPlainCellNameFor( Method _m )
		{
			String cellName = _m.getName();
			if (cellName.length() > 3 && cellName.startsWith( "get" ) && Character.isUpperCase( cellName.charAt( 3 ) )) {
				cellName = cellName.substring( 3 );
			}
			return cellName;
		}


		public void bindAllNamedCellsToMethods() throws CompilerException
		{
			bindAllPrefixedNamedCellsToMethods( null );
		}

		public void bindAllPrefixedNamedCellsToMethods( String _prefix ) throws CompilerException
		{
			final String prefix = (null != _prefix) ? _prefix.toUpperCase() : null;
			final Map<String, Spreadsheet.Range> defs = getSpreadsheet().getRangeNames();
			for (Map.Entry<String, Spreadsheet.Range> def : defs.entrySet()) {
				final Spreadsheet.Range range = def.getValue();
				if (range instanceof Cell) {
					final Cell cell = (Cell) range;
					if (canBind( cell )) {
						final String name = def.getKey().toUpperCase();
						if (null == prefix) {
							bindThisNamedCellToMethod( cell, name );
						}
						else {
							if (name.startsWith( prefix )) {
								final String strippedName = name.substring( prefix.length() );
								bindThisNamedCellToMethod( cell, strippedName );
							}
						}
					}
				}
			}
		}

		private boolean bindThisNamedCellToMethod( Cell _cell, String _methodBaseName ) throws CompilerException
		{
			return bindThisNamedCellToMethod( _methodBaseName, _cell )
					|| bindThisNamedCellToMethod( "get" + _methodBaseName, _cell );
		}

		private boolean bindThisNamedCellToMethod( String _methodName, Cell _cell ) throws CompilerException
		{
			for (Method m : this.contextMethods) {
				if (m.getName().equalsIgnoreCase( _methodName ) && canBind( m )) {
					bindCell( _cell, FormulaCompiler.newCallFrame( m ) );
					return true;
				}
			}
			return false;
		}


		protected abstract void bindCell( Cell _cell, CallFrame _frame ) throws CompilerException;


		public void failIfCellNamesAreStillUnbound( String _prefix ) throws CompilerException
		{
			final Map<String, Spreadsheet.Range> defs = getBinder().getSpreadsheet().getRangeNames();
			for (Map.Entry<String, Spreadsheet.Range> def : defs.entrySet()) {
				final Spreadsheet.Range range = def.getValue();
				if (range instanceof Cell) {
					final Cell cell = (Cell) range;
					final String name = def.getKey();
					if (name.toUpperCase().startsWith( _prefix )) {
						if (!isBound( cell )) {
							throw new SpreadsheetException.NameNotFound( "There is no "
									+ bindingTypeName() + " method named " + name + "() or get" + name + "() to bind the cell "
									+ name + " to (character case is irrelevant)." );
						}
					}
				}
			}
		}

		protected abstract boolean isBound( Cell _cell );
		protected abstract String bindingTypeName();

	}


	private class InputCellBinder extends CellBinderImpl
	{

		protected InputCellBinder( Class _contextClass )
		{
			super( _contextClass );
		}

		@Override
		protected void bindCell( Cell _cell, CallFrame _chain ) throws CompilerException
		{
			getBinder().getRoot().defineInputCell( _cell, _chain );
		}

		@Override
		protected boolean canBind( Cell _cell )
		{
			return !getBinder().isInputCell( _cell );
		}

		@Override
		protected boolean isBound( Cell _cell )
		{
			return getBinder().isInputCell( _cell );
		}

		@Override
		protected String bindingTypeName()
		{
			return "input";
		}

	}


	private class OutputCellBinder extends CellBinderImpl
	{

		protected OutputCellBinder( Class _contextClass )
		{
			super( _contextClass );
		}

		@Override
		protected void bindCell( Cell _cell, CallFrame _chain ) throws CompilerException
		{
			getBinder().getRoot().defineOutputCell( _cell, _chain );
		}

		@Override
		protected boolean canBind( Method _method )
		{
			return super.canBind( _method )
					&& !Modifier.isFinal( _method.getModifiers() ) && !Modifier.isStatic( _method.getModifiers() )
					&& !_method.getName().equals( "reset" );
		}

		@Override
		protected boolean isBound( Cell _cell )
		{
			return getBinder().isOutputCell( _cell );
		}

		@Override
		protected String bindingTypeName()
		{
			return "output";
		}

	}

}
