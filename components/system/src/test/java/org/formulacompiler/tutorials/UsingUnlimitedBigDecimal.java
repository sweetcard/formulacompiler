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

import java.math.BigDecimal;

import org.formulacompiler.decompiler.FormulaDecompiler;
import org.formulacompiler.runtime.Engine;
import org.formulacompiler.spreadsheet.EngineBuilder;
import org.formulacompiler.spreadsheet.SpreadsheetCompiler;
import org.formulacompiler.tests.utils.MultiFormatTestFactory;

import junit.framework.Test;

public class UsingUnlimitedBigDecimal extends AbstractUsingBigDecimalTest
{


	public void testUsingBigDecimalN() throws Exception
	{
		String path = getPath();
		EngineBuilder builder = SpreadsheetCompiler.newEngineBuilder();
		builder.loadSpreadsheet( path );
		builder.setFactoryClass( Factory.class );
		// ---- buildCompilerN
		builder.setNumericType( /**/SpreadsheetCompiler.getNumericType( BigDecimal.class )/**/ );
		// ---- buildCompilerN
		builder.bindAllByName();
		Engine engine = builder.compile();
		Factory factory = (Factory) engine.getComputationFactory();

		{
			// ---- checkResultNa
			Output output = factory.newInstance( new Input( /**/1, 4/**/ ) );
			assertEquals( /**/"1.25"/**/, output.getResult().toPlainString() );
			// ---- checkResultNa
		}

		{
			// ---- checkResultNb
			try {
				Output output = factory.newInstance( new Input( 1, /**/3/**/ ) );
				output.getResult();
				fail( "ArithmeticException expected" );
			}
			catch (/**/ArithmeticException e/**/) {
				assertEquals( "Non-terminating decimal expansion; no exact representable decimal result.", e.getMessage() );
			}
			// ---- checkResultNb
		}

		FormulaDecompiler.decompile( engine ).saveTo( "temp/test/decompiled/numeric_type/bigdecimal0" );
	}


	public static Test suite()
	{
		return MultiFormatTestFactory.testSuite( UsingUnlimitedBigDecimal.class );
	}


}
