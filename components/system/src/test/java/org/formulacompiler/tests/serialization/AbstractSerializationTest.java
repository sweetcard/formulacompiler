/*
 * Copyright (c) 2006 by Abacus Research AG, Switzerland.
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
package org.formulacompiler.tests.serialization;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.formulacompiler.compiler.NumericType;
import org.formulacompiler.compiler.SaveableEngine;
import org.formulacompiler.spreadsheet.EngineBuilder;
import org.formulacompiler.spreadsheet.Spreadsheet;
import org.formulacompiler.spreadsheet.SpreadsheetCompiler;
import org.formulacompiler.spreadsheet.SpreadsheetBinder.Section;


public abstract class AbstractSerializationTest extends AbstractTestBase
{

	public void testSerialization() throws Exception
	{
		serializeAndTest();
		deserializeAndTest();
	}


	private void serializeAndTest() throws Exception
	{
		final Class<Inputs> inp = Inputs.class;
		final Class<Outputs> outp = Outputs.class;
		final EngineBuilder builder = SpreadsheetCompiler.newEngineBuilder();
		builder.loadSpreadsheet( "src/test/data/org/formulacompiler/tests/serialization/SerializationTest.xls" );
		builder.setInputClass( inp );
		builder.setOutputClass( outp );
		builder.setNumericType( getNumericType() );

		final Section bnd = builder.getRootBinder();
		final Spreadsheet sheet = builder.getSpreadsheet();
		bnd.defineInputCell( sheet.getCell( 0, 1, 0 ), builder.newCallFrame( inp.getMethod( "getA" + getTypeSuffix() ) ) );
		bnd.defineInputCell( sheet.getCell( 0, 1, 1 ), builder.newCallFrame( inp.getMethod( "getB" + getTypeSuffix() ) ) );
		bnd.defineOutputCell( sheet.getCell( 0, 1, 2 ), builder.newCallFrame( outp.getMethod( "getResult" + getTypeSuffix() ) ) );

		final SaveableEngine engine = builder.compile();
		serialize( engine );
		computeAndTestResult( engine );
	}


	protected abstract NumericType getNumericType();


	private void serialize( SaveableEngine _engine ) throws Exception
	{
		OutputStream outStream = new BufferedOutputStream( new FileOutputStream( getEngineFile() ));
		try {
			_engine.saveTo( outStream );
		}
		finally {
			outStream.close();
		}
	}


}
