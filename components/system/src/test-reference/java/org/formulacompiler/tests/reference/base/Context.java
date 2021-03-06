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

package org.formulacompiler.tests.reference.base;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Collections;

import org.formulacompiler.compiler.FormulaCompiler;
import org.formulacompiler.compiler.NumericType;
import org.formulacompiler.compiler.SaveableEngine;
import org.formulacompiler.runtime.Computation;
import org.formulacompiler.runtime.ComputationFactory;
import org.formulacompiler.runtime.New;
import org.formulacompiler.runtime.Computation.Config;
import org.formulacompiler.spreadsheet.SpreadsheetException;
import org.formulacompiler.spreadsheet.internal.BaseRow;
import org.formulacompiler.spreadsheet.internal.BaseSheet;
import org.formulacompiler.spreadsheet.internal.BaseSpreadsheet;
import org.formulacompiler.spreadsheet.internal.CellIndex;
import org.formulacompiler.spreadsheet.internal.CellInstance;

import junit.framework.Test;

public final class Context
{
	protected static final File SHEET_PATH = new File( "src/test-reference/data/org/formulacompiler/tests/reference/" );

	private final Context parent;

	private BindingType numberBindingType;
	private Boolean explicitCaching;
	private Documenter documenter;

	private SpreadsheetInfo ss;
	private RowInfo row;
	private Integer inputBindingBits;
	private EngInfo eng;
	private RunInfo run;

	private static final class SpreadsheetInfo
	{
		String spreadsheetFileBaseName;
		File spreadsheetFile;
		BaseSpreadsheet spreadsheet;
		BaseSheet sheet;
		RowSetup.Builder rowSetupBuilder;
		Computation.Config computationConfig;
		FailedEngineReporter failedEngineReporter;
		Collection<Context> variants;
		AbstractVariantRowVerificationTestCase.Factory rowVerificationTestCaseFactory;
	}


	private static final class RowInfo implements Cloneable
	{
		BaseRow row;
		CellIndex expectedCell;
		CellIndex outputCell;
		Integer inputCellCount;

		@Override
		protected RowInfo clone()
		{
			try {
				return (RowInfo) super.clone();
			}
			catch (CloneNotSupportedException e) {
				throw new InternalError();
			}
		}
	}

	private static final class EngInfo
	{
		SaveableEngine engine;
		ComputationFactory factory;
	}

	private static final class RunInfo
	{
		CellIndex[] inputCells;
		Inputs expected;
		Inputs inputs;
	}


	public Context( Context _parent )
	{
		this.parent = _parent;
	}

	public Context( String _spreadsheetFileBaseName, String _extension )
	{
		this( (Context) null );
		this.ss = new SpreadsheetInfo();
		this.ss.spreadsheetFileBaseName = _spreadsheetFileBaseName;
		this.ss.spreadsheetFile = new File( SHEET_PATH, _spreadsheetFileBaseName + _extension );
	}

	public Context( File _spreadsheetFile )
	{
		this( (Context) null );
		this.ss = new SpreadsheetInfo();
		this.ss.spreadsheetFile = _spreadsheetFile;
		this.ss.spreadsheetFileBaseName = _spreadsheetFile.getName();
	}


	public boolean getExplicitCaching()
	{
		return this.explicitCaching != null ? this.explicitCaching : this.parent == null ? false : this.parent
				.getExplicitCaching();
	}

	public void setExplicitCaching( Boolean _value )
	{
		this.explicitCaching = _value;
	}


	public BindingType getNumberBindingType()
	{
		return this.numberBindingType != null ? this.numberBindingType : this.parent == null ? BindingType.DOUBLE
				: this.parent.getNumberBindingType();
	}

	public void setNumberBindingType( BindingType _value )
	{
		this.numberBindingType = _value;
	}

	public NumericType getNumericType()
	{
		switch (getNumberBindingType()) {
			case DOUBLE:
				return FormulaCompiler.DOUBLE;
			case BIGDEC_PREC:
				return FormulaCompiler.BIGDECIMAL128;
			case BIGDEC_SCALE:
				return FormulaCompiler.BIGDECIMAL_SCALE8;
			case LONG:
				return FormulaCompiler.LONG_SCALE6;
		}
		throw new IllegalArgumentException( "Invalid number binding type." );
	}


	public File getSpreadsheetFile()
	{
		return ss().spreadsheetFile;
	}

	public String getSpreadsheetFileBaseName()
	{
		return ss().spreadsheetFileBaseName;
	}


	public BaseSpreadsheet getSpreadsheet()
	{
		return ss().spreadsheet;
	}

	public void setSpreadsheet( BaseSpreadsheet _spreadsheet )
	{
		final SpreadsheetInfo ss = ss();
		ss.spreadsheet = _spreadsheet;
		ss.sheet = _spreadsheet.getSheetList().get( 0 );
	}

	public List<? extends BaseRow> getSheetRows()
	{
		return ss().sheet.getRowList();
	}

	public BaseRow getSheetRow( int _rowIndex )
	{
		List<? extends BaseRow> rows = getSheetRows();
		if (_rowIndex < 0 && _rowIndex >= rows.size()) return null;
		return rows.get( _rowIndex );
	}


	public Computation.Config getComputationConfig()
	{
		return ss().computationConfig;
	}

	public void setComputationConfig( Computation.Config _value )
	{
		ss().computationConfig = _value;
	}


	public RowSetup.Builder getRowSetupBuilder()
	{
		return ss().rowSetupBuilder;
	}

	public void setRowSetupBuilder( RowSetup.Builder _value )
	{
		ss().rowSetupBuilder = _value;
		for (Context variant : variants()) {
			variant.setRowSetupBuilder( _value );
		}
	}

	public RowSetup getRowSetup()
	{
		return getRowSetupBuilder().newInstance( this );
	}


	public FailedEngineReporter getFailedEngineReporter()
	{
		return ss().failedEngineReporter;
	}

	public void setFailedEngineReporter( FailedEngineReporter _value )
	{
		ss().failedEngineReporter = _value;
	}

	void reportFailedEngineAndRethrow( Test _test, SaveableEngine _engine, Throwable _failure ) throws Throwable
	{
		try {
			final FailedEngineReporter reporter = getFailedEngineReporter();
			if (null != reporter) {
				reporter.reportFailedEngine( _test, _engine, _failure );
			}
		}
		catch (Throwable t) {
			System.err.println( "Error reporting failed engine:" );
			t.printStackTrace();
		}
		throw _failure;
	}

	protected static interface FailedEngineReporter
	{
		void reportFailedEngine( Test _test, SaveableEngine _engine, Throwable _failure ) throws Throwable;
	}


	public Collection<Context> variants()
	{
		final Collection<Context> variants = ss().variants;
		return variants != null ? variants : Collections.<Context>emptyList();
	}

	public void addVariant( Context _value )
	{
		SpreadsheetInfo ss = ss();
		if (ss.variants == null) ss.variants = New.collection();
		ss.variants.add( _value );
	}

	public AbstractVariantRowVerificationTestCase.Factory getRowVerificationTestCaseFactory()
	{
		return ss().rowVerificationTestCaseFactory;
	}

	public void setRowVerificationTestCaseFactory( AbstractVariantRowVerificationTestCase.Factory _value )
	{
		ss().rowVerificationTestCaseFactory = _value;
	}


	public BaseRow getRow()
	{
		final RowInfo rowInfo = row();
		return (null == rowInfo) ? null : rowInfo.row;
	}

	public int getRowIndex()
	{
		final BaseRow row = getRow();
		return (null == row) ? -1 : row.getRowIndex();
	}

	public void setRow( int _rowIndex )
	{
		setRow( getSheetRow( _rowIndex ) );
	}

	public void setRow( BaseRow _row )
	{
		rowPut().row = _row;
	}

	public List<? extends CellInstance> getRowCells()
	{
		BaseRow row = getRow();
		if (row == null) return NO_CELLS;
		return row.getCellList();
	}
	private static final List<CellInstance> NO_CELLS = New.list( 0 );

	public CellInstance getRowCell( int _columnIndex )
	{
		List<? extends CellInstance> cells = getRowCells();
		if (_columnIndex < 0 || _columnIndex >= cells.size()) return null;
		return cells.get( _columnIndex );
	}


	public CellIndex getRowCellIndex( int _columnIndex )
	{
		return new CellIndex(getSpreadsheet(), ss().sheet.getSheetIndex(), _columnIndex, getRowIndex() );
	}


	public CellIndex getExpectedCell()
	{
		return row().expectedCell;
	}

	public void setExpectedCell( CellIndex _cell )
	{
		row().expectedCell = _cell;
	}


	public CellIndex getOutputCell()
	{
		return row().outputCell;
	}

	public void setOutputCell( CellIndex _cell )
	{
		row().outputCell = _cell;
	}

	public String getOutputExpr()
	{
		final CellIndex cell = getOutputCell();
		if (null == cell) return null;
		try {
			return cell.getExpressionText();
		}
		catch (SpreadsheetException e) {
			throw new RuntimeException( e );
		}
	}


	public Integer getInputCellCount()
	{
		return row().inputCellCount;
	}

	public void setInputCellCount( Integer _value )
	{
		row().inputCellCount = _value;
	}


	public Integer getInputBindingBits()
	{
		Context at = this;
		do {
			if (at.inputBindingBits != null) return at.inputBindingBits;
			at = at.parent;
		} while (at != null);
		return null;
	}

	public boolean[] getInputBindingFlags()
	{
		return decodeBinding( getInputBindingBits() );
	}

	private boolean[] decodeBinding( int _bitset )
	{
		final int n = getInputCellCount();
		final boolean[] flags = new boolean[ n ];
		for (int i = 0; i < n; i++) {
			flags[ i ] = (_bitset & (1 << i)) != 0;
		}
		return flags;
	}

	public void setInputBindingBits( int _value )
	{
		this.inputBindingBits = _value;
	}

	public void setInputBindingBits( String _bitstring )
	{
		setInputBindingBits( Integer.parseInt( _bitstring, 2 ) );
	}


	public CellIndex[] getInputCells()
	{
		return run().inputCells;
	}

	public void setInputCells( CellIndex... _cells )
	{
		runPut().inputCells = _cells;
	}


	public Inputs getInputs()
	{
		return run().inputs;
	}

	public void setInputs( Inputs _cell )
	{
		runPut().inputs = _cell;
	}


	public Inputs getExpected()
	{
		return run().expected;
	}

	public void setExpected( Inputs _cell )
	{
		runPut().expected = _cell;
	}


	public SaveableEngine getEngine() throws Exception
	{
		return eng().engine;
	}

	public void setEngine( SaveableEngine _value )
	{
		engPut().engine = _value;
	}


	public ComputationFactory getFactory() throws Exception
	{
		return eng().factory;
	}

	public void setFactory( ComputationFactory _value )
	{
		eng().factory = _value;
	}


	public Documenter getDocumenter()
	{
		return this.documenter != null ? this.documenter : this.parent == null ? Documenter.Mock.INSTANCE : this.parent
				.getDocumenter();
	}

	public void setDocumenter( Documenter _value )
	{
		this.documenter = _value;
	}


	public String getDescription()
	{
		StringBuilder s = new StringBuilder();
		s.append( getSpreadsheetFile().getName() );
		if (getRow() != null) s.append( ", row " ).append( getRowIndex() + 1 );
		BindingType type = getNumberBindingType();
		if (type != null) s.append( ", type:" ).append( type.name() );
		if (getExplicitCaching()) s.append( ", caching" );
		Config config = getComputationConfig();
		if (config != null) s.append( ", config:" ).append( config.toString() );
		return s.toString();
	}


	private SpreadsheetInfo ss()
	{
		Context at = this;
		do {
			if (at.ss != null) return at.ss;
			at = at.parent;
		} while (at != null);
		return null;
	}


	private RowInfo row()
	{
		Context at = this;
		do {
			if (at.row != null) return at.row;
			at = at.parent;
		} while (at != null);
		return null;
	}

	private RowInfo rowPut()
	{
		if (this.row == null) {
			final RowInfo was = row();
			this.row = (null == was) ? new RowInfo() : was.clone();
		}
		return this.row;
	}


	private EngInfo eng()
	{
		Context at = this;
		do {
			if (at.eng != null) return at.eng;
			at = at.parent;
		} while (at != null);
		return null;
	}

	private EngInfo engPut()
	{
		if (this.eng == null) this.eng = new EngInfo();
		return this.eng;
	}

	public void releaseEngine()
	{
		assert null != this.eng;
		this.eng = null;
	}


	private RunInfo run()
	{
		Context at = this;
		do {
			if (at.run != null) return at.run;
			at = at.parent;
		} while (at != null);
		return null;
	}

	private RunInfo runPut()
	{
		if (this.run == null) this.run = new RunInfo();
		return this.run;
	}

	public void releaseInputs()
	{
		assert null != this.run;
		this.run = null;
	}


}
