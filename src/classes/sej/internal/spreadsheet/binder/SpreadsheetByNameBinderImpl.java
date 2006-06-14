package sej.internal.spreadsheet.binder;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import sej.api.CallFrame;
import sej.api.CompilerError;
import sej.api.Spreadsheet;
import sej.api.SpreadsheetBinder;
import sej.api.SpreadsheetByNameBinder;
import sej.api.Spreadsheet.Cell;
import sej.api.Spreadsheet.CellNameDefinition;
import sej.api.Spreadsheet.NameDefinition;


/**
 * Utility class that implements simple cell binding using the cell names in the spreadsheet and
 * reflection on the input and output types.
 * 
 * @author peo
 */
public class SpreadsheetByNameBinderImpl implements SpreadsheetByNameBinder
{

	private final SpreadsheetBinder binder;
	private final CellBinderImpl inputBinder;
	private final CellBinderImpl outputBinder;
	private final Set<String> boundNames = new HashSet<String>();

	public SpreadsheetByNameBinderImpl(Config _config)
	{
		super();
		_config.validate();
		this.binder = _config.binder;
		this.inputBinder = new InputCellBinder( this.binder.getRoot().getInputClass() );
		this.outputBinder = new OutputCellBinder( this.binder.getRoot().getOutputClass() );
	}

	public CellBinder inputs()
	{
		return this.inputBinder;
	}

	public CellBinder outputs()
	{
		return this.outputBinder;
	}

	private SpreadsheetBinder getBinder()
	{
		return this.binder;
	}

	private Spreadsheet getSpreadsheet()
	{
		return getBinder().getSpreadsheet();
	}

	private Set<String> getBoundNames()
	{
		return this.boundNames;
	}

	private void haveBound( String _name )
	{
		this.boundNames.add( _name );
	}


	private abstract class CellBinderImpl implements CellBinder
	{
		private final Method[] contextMethods;

		protected CellBinderImpl(Class _contextClass)
		{
			super();
			this.contextMethods = _contextClass.getMethods();
		}

		public void bindAllMethodsToNamedCells() throws CompilerError
		{
			for (Method m : this.contextMethods) {
				if (m.getDeclaringClass() != Object.class) {
					final int mods = m.getModifiers();
					if (!Modifier.isFinal( mods ) && !Modifier.isStatic( mods )) {
						bindThisMethodToNamedCell( m );
					}
				}
			}
		}

		private void bindThisMethodToNamedCell( Method _m ) throws CompilerError
		{
			final String cellName = getCellNameFor( _m );
			final Cell cell = getSpreadsheet().getCell( cellName );
			if (cell != null) {
				haveBound( cellName );
				bindCell( cell, new CallFrame( _m ) );
			}
		}

		private String getCellNameFor( Method _m )
		{
			String cellName = _m.getName();
			if (cellName.length() > 3 && cellName.startsWith( "get" ) && Character.isUpperCase( cellName.charAt( 3 ) )) {
				cellName = cellName.substring( 3 );
			}
			return cellName.toUpperCase();
		}


		public void bindAllNamedCellsToMethods() throws CompilerError
		{
			final NameDefinition[] defs = getSpreadsheet().getDefinedNames();
			for (NameDefinition def : defs) {
				final String defName = def.getName();
				if (def instanceof Spreadsheet.CellNameDefinition) {
					if (!getBoundNames().contains( defName )) {
						bindThisNamedCellToMethod( (Spreadsheet.CellNameDefinition) def );
					}
				}
			}
		}

		private void bindThisNamedCellToMethod( CellNameDefinition _def ) throws CompilerError
		{
			final String cellName = _def.getName();
			if (!bindThisNamedCellToMethod( cellName, _def ) && !bindThisNamedCellToMethod( "get" + cellName, _def )) {
				throw new CompilerError.NameNotFound( "There is no input method named either '"
						+ cellName + "' or 'get" + cellName + "' with no parameters" );
			}
		}

		private boolean bindThisNamedCellToMethod( String _methodName, CellNameDefinition _def ) throws CompilerError
		{
			for (Method m : this.contextMethods) {
				if (m.getName().equalsIgnoreCase( _methodName ) && m.getParameterTypes().length == 0) {
					haveBound( _def.getName() );
					bindCell( _def.getCell(), new CallFrame( m ) );
					return true;
				}
			}
			return false;
		}


		protected abstract void bindCell( Cell _cell, CallFrame _frame ) throws CompilerError;

	}


	private class InputCellBinder extends CellBinderImpl
	{

		protected InputCellBinder(Class _contextClass)
		{
			super( _contextClass );
		}

		@Override
		protected void bindCell( Cell _cell, CallFrame _chain ) throws CompilerError
		{
			getBinder().getRoot().defineInputCell( _cell, _chain );
		}

	}


	private class OutputCellBinder extends CellBinderImpl
	{

		protected OutputCellBinder(Class _contextClass)
		{
			super( _contextClass );
		}

		@Override
		protected void bindCell( Cell _cell, CallFrame _chain ) throws CompilerError
		{
			getBinder().getRoot().defineOutputCell( _cell, _chain );
		}

	}

}
