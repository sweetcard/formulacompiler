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

package org.formulacompiler.tutorials;

import java.io.File;

import org.formulacompiler.compiler.CompilerException;
import org.formulacompiler.spreadsheet.EngineBuilder;
import org.formulacompiler.spreadsheet.Spreadsheet.Cell;
import org.formulacompiler.spreadsheet.SpreadsheetCompiler;
import org.formulacompiler.tests.utils.MultiFormat;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


@RunWith( MultiFormat.class )
public class ErrorUnsupportedOutputMethodReturnType
{
	private final String spreadsheetExtension;

	public ErrorUnsupportedOutputMethodReturnType( final String _spreadsheetExtension )
	{
		this.spreadsheetExtension = _spreadsheetExtension;
	}

	private String getSpreadsheetExtension()
	{
		return this.spreadsheetExtension;
	}


	@Test
	public void testStringAsInt() throws Exception
	{
		EngineBuilder builder = builderForComputationOfCellNamed( /**/"stringOutput"/**/ );
		bindInputNamed( builder, "stringInput" );
		// ---- TryCompile
		try {
			builder.compile();
			fail();
		}
		catch (/**/CompilerException.UnsupportedDataType e/**/) {
			String err = /**/"The method public abstract char org.formulacompiler.tutorials.ErrorUnsupportedOutputMethodReturnType$MyComputation.result() has an unsupported return type"
					+ "\nCell containing expression is Sheet1!A1."
					+ "\nReferenced by cell Sheet1!A1."/**/;
			assertEquals( err, e.getMessage() );
		}
		// ---- TryCompile
	}


	private EngineBuilder builderForComputationOfCellNamed( String _cellName ) throws Exception
	{
		EngineBuilder builder = SpreadsheetCompiler.newEngineBuilder();
		File file = new File( "src/test/data/org/formulacompiler/tutorials/ErrorUnsupportedConversion" + getSpreadsheetExtension() );
		builder.loadSpreadsheet( file );
		builder.setFactoryClass( MyFactory.class );
		Cell cell = builder.getSpreadsheet().getCell( _cellName );
		// ---- Bind
		builder.getRootBinder().defineOutputCell( cell, MyComputation.class.getMethod( "result" ) );
		// ---- Bind
		return builder;
	}

	private void bindInputNamed( EngineBuilder _builder, String _cellName ) throws Exception
	{
		_builder.getRootBinder().defineInputCell( _builder.getSpreadsheet().getCell( _cellName ), "value" );
	}

	public static interface MyFactory
	{
		public MyComputation newComputation( MyInputs _inputs );
	}

	public static class MyInputs
	{
		public int value()
		{
			return 30;
		}
	}

	// ---- MyComputation
	public static interface MyComputation
	{
		public char result();
	}
	// ---- MyComputation

}
