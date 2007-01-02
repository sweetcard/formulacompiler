/*
 * Copyright � 2006 by Abacus Research AG, Switzerland.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are prohibited, unless you have been explicitly granted 
 * more rights by Abacus Research AG.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS 
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE 
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package sej.internal.spreadsheet.loader.excel;

import sej.internal.expressions.ExpressionNode;
import sej.internal.spreadsheet.CellIndex;
import sej.internal.spreadsheet.CellInstance;
import sej.internal.spreadsheet.CellRange;
import sej.internal.spreadsheet.CellRefFormat;
import sej.internal.spreadsheet.CellWithConstant;
import sej.internal.spreadsheet.ExpressionNodeForCell;
import sej.internal.spreadsheet.RowImpl;
import sej.internal.spreadsheet.SheetImpl;
import sej.internal.spreadsheet.SpreadsheetImpl;
import junit.framework.TestCase;


public class ExcelExpressionParserTest extends TestCase
{
	SpreadsheetImpl workbook = new SpreadsheetImpl();
	SheetImpl sheet = new SheetImpl( this.workbook, "One" );
	RowImpl row1 = new RowImpl( this.sheet );
	CellInstance cell11 = new CellWithConstant( this.row1, 123 );
	CellInstance cell12 = new CellWithConstant( this.row1, 123 );
	RowImpl row2 = new RowImpl( this.sheet );
	CellInstance cell21 = new CellWithConstant( this.row2, 123 );
	CellInstance cell22 = new CellWithConstant( this.row2, 123 );
	ExcelExpressionParser parser = new ExcelExpressionParser( this.cell22 );

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		this.workbook.getNameMap().put( "_A1_", this.cell11.getCellIndex() );
		this.workbook.getNameMap().put( "_B1_", this.cell12.getCellIndex() );
		this.workbook.getNameMap().put( "_A2_", this.cell21.getCellIndex() );
		this.workbook.getNameMap().put( "_B2_", this.cell22.getCellIndex() );
		this.workbook.getNameMap().put( "_A_", new CellRange( this.cell11.getCellIndex(), this.cell21.getCellIndex() ) );
		this.workbook.getNameMap().put( "_B_", new CellRange( this.cell12.getCellIndex(), this.cell22.getCellIndex() ) );
		this.workbook.getNameMap().put( "_1_", new CellRange( this.cell11.getCellIndex(), this.cell12.getCellIndex() ) );
		this.workbook.getNameMap().put( "_2_", new CellRange( this.cell21.getCellIndex(), this.cell22.getCellIndex() ) );
		this.workbook.getNameMap().put( "_ALL_", new CellRange( this.cell11.getCellIndex(), this.cell22.getCellIndex() ) );
	}

	public void testRC() throws Exception
	{
		assertRefR1C1( "B2", "RC" );
		assertRefR1C1( "A1", "R1C1" );
		assertRefR1C1( "A2", "RC1" );
		assertRefR1C1( "B1", "R1C" );
		assertRefR1C1( "C2", "RC[1]" );
		assertRefR1C1( "A2", "RC[-1]" );
		assertRefR1C1( "A1", "R[-1]C[-1]" );
		assertRefR1C1( "B1", "R[-1]C" );
	}


	public void testAbs() throws Exception
	{
		assertRefA1( "B2", "B2" );
		assertRefA1( "D4", "D4" );
		assertRefA1( "D4", "$D$4" );
		assertRefA1( "D4", "$D4" );
		assertRefA1( "D4", "D$4" );
	}


	public void testCellRefs() throws Exception
	{
		assertParseableA1( "((((A1 + B1) + B2) + A2) + B2)", "A1 + _1_ + _2_ + _A_ + _B_" );
		assertParseableR1C1( "((((A1 + B1) + B2) + A2) + B2)", "R1C1 + _1_ + _2_ + _A_ + _B_" );
		assertParseableR1C1( "((((A1 + C3) + A2) + B1) + A1)", "R1C1 + R[1]C[1] + RC1 + R1C + R[-1]C[-1]" );
	}


	public void testOperators()
	{
		assertParseableA1( "((((A1 + (-B1)) + B2) - B3) - (-B4))", "A1 + -B1 + +B2 - +B3 - -B4" );
		assertParseableA1( "(A1 + (A2 * A3))", "A1 + A2 * A3" );
	}


	public void testConcat() throws Exception
	{
		assertParseableAll( "(1.0 & 2.0 & 3.0 & 4.0)", "1 & 2 & 3 & 4" );
		assertParseableAll( "(1.0 & (2.0 & 3.0) & 4.0)", "1 & (2 & 3) & 4" );
		assertParseableAll( "(1.0 & 2.0 & 3.0 & 4.0)", "CONCATENATE( 1, 2, 3, 4 )" );
	}


	public void testCellAndRangeMixes() throws Exception
	{
		assertParseableA1( "SUM( A1:B2, C5, A1:B5 A2:E8, (A1 + A2) )", "SUM( A1:B2, C5, A1:B5 A2:E8, A1+A2 )" );
		assertParseableA1( "(SUM( A1:B1, A2:B2 A1:A2, B1:B2, A1:B2 ) + B2)", "SUM( _1_, _2_ _A_, _B_, _ALL_ ) + _2_" );

		// LATER assertParseableA1( "(SUM( A1:A2, A1:A2 A1:A2 ))", "SUM( A1:_A2_, _A1_:A2 _A1_:_A2_ )"
		// );
	}


	public void testTrueFalse() throws Exception
	{
		assertParseableAll( "AND( 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0 )",
				"AND( TRUE, FALSE, true, false, @TRUE, @FALSE, @true, @false )" );
	}


	public void testNames() throws Exception
	{
		final String[] names = { "_A1", "A.1", "A1.", "A_1", "A.B", "_A", "_1", "_.", "A.B.C", "R1C.", "R1C3A" };

		final CellIndex a1 = this.cell11.getCellIndex();
		for (String n : names) {
			this.workbook.getNameMap().put( n, a1 );
		}

		for (String n : names) {
			assertParseableAll( "SUM( A1 )", "SUM( " + n + " )" );
		}
	}


	public void testOldStyleFunctions() throws Exception
	{
		assertParseableA1( "((1.0 + ROUND( A1, 2.0 )) + 2.0)", "1 + @ROUND(A1,2) + 2" );
		assertParseableA1( "((1.0 + ROUND( A1, 2.0 )) + 2.0)", "1 + ROUND(A1,2) + 2" );
	}


	public void testUnsupported() throws Exception
	{
		assertErr(
				"2 + SOMEFUNC(A2)",
				"Undefined name or unsupported function encountered in expression 2 + SOMEFUNC( <<? A2); error location indicated by <<?." );
	}


	public void testReferenceToSecondarySheet() throws Exception
	{
		new SheetImpl( this.workbook, "Two" );
		new SheetImpl( this.workbook, "*()__ 123\"yes\"" );
		assertRefA1( "'Two'!A2", "Two!A2" );
		assertRefR1C1( "'Two'!A2", "Two!R2C1" );
		assertRefA1( "'*()__ 123\"yes\"'!A1", "'*()__ 123\"yes\"'!A1" );
		assertRefR1C1( "'*()__ 123\"yes\"'!A1", "'*()__ 123\"yes\"'!R1C1" );
	}


	public void testReferenceFromSecondarySheet() throws Exception
	{
		SheetImpl sheet2 = new SheetImpl( this.workbook, "Two" );
		RowImpl row21 = new RowImpl( sheet2 );
		CellInstance cell211 = new CellWithConstant( row21, 4711 );
		this.parser = new ExcelExpressionParser( cell211 );
		assertRefA1( "'Two'!A2", "A2" );
		assertRefA1( "A2", "One!A2" );
		assertRefR1C1( "'Two'!A2", "R2C1" );
		assertRefR1C1( "A2", "One!R2C1" );
	}


	public void testReferencesToSecondarySheet() throws Exception
	{
		new SheetImpl( this.workbook, "Two" );
		assertParseableA1( "('Two'!A2 + 'Two'!B1)", "Two!A2+Two!B1" );
		assertParseableR1C1( "('Two'!A2 + 'Two'!B1)", "Two!R2C1+Two!R1C2" );
	}


	private void assertParseableAll( String _expected, String _string )
	{
		assertParseableA1( _expected, _string );
		assertParseableR1C1( _expected, _string );
	}


	private void assertParseableA1( String _expected, String _string )
	{
		assertParseable( _expected, _string, CellRefFormat.A1 );
	}


	private void assertParseableR1C1( String _expected, String _string )
	{
		assertParseable( _expected, _string, CellRefFormat.R1C1 );
	}


	private void assertParseable( String _expected, String _expr, CellRefFormat _format )
	{
		final ExpressionNode node = this.parser.parseText( _expr, _format );
		final String actual = node.describe();
		assertEquals( _expected, actual );
	}


	private void assertRefA1( String _canonicalName, String _ref ) throws Exception
	{
		assertRef( _canonicalName, _ref, CellRefFormat.A1 );
	}


	private void assertRefR1C1( String _canonicalName, String _ref ) throws Exception
	{
		assertRef( _canonicalName, _ref, CellRefFormat.R1C1 );
	}


	private void assertRef( String _canonicalName, String _ref, CellRefFormat _format )
	{
		ExpressionNode parsed = this.parser.parseText( _ref, _format );
		ExpressionNodeForCell node = (ExpressionNodeForCell) parsed;
		CellIndex ref = node.getCellIndex();
		String actual = ref.toString();
		assertEquals( _canonicalName, actual );
	}


	private void assertErr( String _ref, String _msg ) throws Exception
	{
		try {
			this.parser.parseText( _ref, CellRefFormat.A1 );
			fail( "Expected exception: " + _msg );
		}
		catch (ExcelExpressionParserError e) {
			assertEquals( _msg, e.getMessage() );
		}
	}


}
