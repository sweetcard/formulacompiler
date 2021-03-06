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

import org.formulacompiler.runtime.Engine;
import org.formulacompiler.runtime.Resettable;
import org.formulacompiler.spreadsheet.EngineBuilder;
import org.formulacompiler.spreadsheet.Orientation;
import org.formulacompiler.spreadsheet.Spreadsheet;
import org.formulacompiler.spreadsheet.Spreadsheet.Range;
import org.formulacompiler.spreadsheet.SpreadsheetBinder.Section;
import org.formulacompiler.spreadsheet.SpreadsheetCompiler;
import org.formulacompiler.spreadsheet.SpreadsheetException;
import org.formulacompiler.tests.utils.MultiFormat;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;


@RunWith( MultiFormat.class )
public class ErrorImproperInnerSectionReference
{
	private final String spreadsheetExtension;

	public ErrorImproperInnerSectionReference( final String _spreadsheetExtension )
	{
		this.spreadsheetExtension = _spreadsheetExtension;
	}

	private String getSpreadsheetExtension()
	{
		return this.spreadsheetExtension;
	}

	@Test
	public void testGoodRef() throws Exception
	{
		// ---- goodRef
		EngineBuilder builder = builderForComputationOfCellNamed( /**/"GoodRef"/**/ );
		Engine engine = builder.compile();
		MyFactory factory = (MyFactory) engine.getComputationFactory();
		MyComputation computation = factory.newComputation( new MyInputs() );
		assertEquals( 4 + 5 + 6 + 7, computation.result() );
		// ---- goodRef
	}

	@Test
	public void testBadRef() throws Exception
	{
		// ---- badRef
		EngineBuilder builder = builderForComputationOfCellNamed( /**/"BadRef"/**/ );
		try {
			builder.compile();
			fail();
		}
		catch (/**/SpreadsheetException.SectionExtentNotCovered e/**/) {
			String err = /**/"Sheet1!C2 does not fully cover the height of its parent section Sheet1!B2:C4 (which iterates section()).\n"
					+ "Referenced by cell Sheet1!B10."/**/;
			assertEquals( err, e.getMessage() );
		}
		// ---- badRef
	}

	@Test
	public void testBadRange1() throws Exception
	{
		// ---- badRange1
		EngineBuilder builder = builderForComputationOfCellNamed( /**/"BadRange1"/**/ );
		try {
			builder.compile();
			fail();
		}
		catch (/**/SpreadsheetException.SectionExtentNotCovered e/**/) {
			String err = /**/"Sheet1!C2:C3 does not fully cover the height of its parent section Sheet1!B2:C4 (which iterates section()).\n"
					+ "Referenced by cell Sheet1!B11."/**/;
			assertEquals( err, e.getMessage() );
		}
		// ---- badRange1
	}

	@Test
	public void testBadRange2() throws Exception
	{
		// ---- badRange2
		EngineBuilder builder = builderForComputationOfCellNamed( /**/"BadRange2"/**/ );
		try {
			builder.compile();
			fail();
		}
		catch (/**/SpreadsheetException.SectionExtentNotCovered e/**/) {
			String err = /**/"Sheet1!C3:C4 does not fully cover the height of its parent section Sheet1!B2:C4 (which iterates section()).\n"
					+ "Referenced by cell Sheet1!B12."/**/;
			assertEquals( err, e.getMessage() );
		}
		// ---- badRange2
	}

	@Test
	public void testBadRange3() throws Exception
	{
		// ---- badRange3
		EngineBuilder builder = builderForComputationOfCellNamed( /**/"BadRange3"/**/ );
		try {
			builder.compile();
			fail();
		}
		catch (/**/SpreadsheetException.SectionExtentNotCovered e/**/) {
			String err = /**/"Sheet1!C3 does not fully cover the height of its parent section Sheet1!B2:C4 (which iterates section()).\n"
					+ "Referenced by cell Sheet1!B13."/**/;
			assertEquals( err, e.getMessage() );
		}
		// ---- badRange3
	}

	@Test
	public void testBadRange4() throws Exception
	{
		// ---- badRange4
		EngineBuilder builder = builderForComputationOfCellNamed( /**/"BadRange4"/**/ );
		try {
			builder.compile();
			fail();
		}
		catch (/**/SpreadsheetException.SectionExtentNotCovered e/**/) {
			String err = /**/"Sheet1!C3 does not fully cover the height of its parent section Sheet1!B2:C4 (which iterates section()).\n"
					+ "Referenced by cell Sheet1!B14."/**/;
			assertEquals( err, e.getMessage() );
		}
		// ---- badRange4
	}


	private EngineBuilder builderForComputationOfCellNamed( String _cellName ) throws Exception
	{
		EngineBuilder builder = SpreadsheetCompiler.newEngineBuilder();
		File file = new File( "src/test/data/org/formulacompiler/tutorials/ErrorImproperInnerSectionReference" + getSpreadsheetExtension() );
		builder.loadSpreadsheet( file );
		builder.setFactoryClass( MyFactory.class );
		Spreadsheet sheet = builder.getSpreadsheet();
		Section root = builder.getRootBinder();

		root.defineOutputCell( sheet.getCell( _cellName ), MyComputation.class.getMethod( "result" ) );

		// ---- bindSection
		Range range = sheet.getRange( "Section" );
		Section section = root.defineRepeatingSection( range, Orientation.REPEAT_ROWS, "section", MyElement.class, null,
				null );
		section.defineInputCell( sheet.getRange( "Name" ).getTopLeft(), "name" );
		section.defineInputCell( sheet.getRange( "Value" ).getTopLeft(), "value" );
		// ---- bindSection

		return builder;
	}

	public static interface MyFactory
	{
		public MyComputation newComputation( MyInputs _inputs );
	}

	public static class MyInputs
	{
		public MyElement[] section()
		{
			return new MyElement[] { new MyElement( 4 ), new MyElement( 5 ), new MyElement( 6 ), new MyElement( 7 ) };
		}
	}

	public static class MyElement
	{
		private final int value;

		public MyElement( int _value )
		{
			super();
			this.value = _value;
		}

		public String name()
		{
			return toString();
		}

		public int value()
		{
			return this.value;
		}
	}

	public static interface MyComputation extends Resettable
	{
		public int result();
	}

}
