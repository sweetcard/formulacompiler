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
package org.formulacompiler.tutorials;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.formulacompiler.compiler.SaveableEngine;
import org.formulacompiler.runtime.Computation;
import org.formulacompiler.runtime.ComputationFactory;
import org.formulacompiler.runtime.Engine;
import org.formulacompiler.runtime.EngineLoader;
import org.formulacompiler.runtime.FormulaRuntime;
import org.formulacompiler.spreadsheet.EngineBuilder;
import org.formulacompiler.spreadsheet.Spreadsheet;
import org.formulacompiler.spreadsheet.SpreadsheetBuilder;
import org.formulacompiler.spreadsheet.SpreadsheetCompiler;
import org.formulacompiler.spreadsheet.SpreadsheetSaver;

import junit.framework.TestCase;

public final class EnvironmentConfig extends TestCase
{


	private static final String DATA_PATH = "src/test/data/org/formulacompiler/tutorials/";

	public void testCustomLocaleTZ() throws Exception
	{
		EngineBuilder builder = SpreadsheetCompiler.newEngineBuilder();
		builder.loadSpreadsheet( DATA_PATH + "LocaleAndTimeZone.xls" );
		builder.setFactoryClass( MyFactory.class );
		builder.bindAllByName();
		SaveableEngine compiledEngine = builder.compile();
		assertComputations( compiledEngine );

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		compiledEngine.saveTo( out );
		byte[] bytes = out.toByteArray();
		EngineLoader.Config cfg = new EngineLoader.Config();
		Engine loadedEngine = FormulaRuntime.loadEngine( cfg, new ByteArrayInputStream( bytes ) );
		assertComputations( loadedEngine );
	}

	private void assertComputations( Engine _engine )
	{
		// ---- customLocaleTest
		assertComputation( /**/"37287,4211"/**/, _engine, Locale./**/GERMAN/**/ );
		assertComputation( /**/"37287.4211"/**/, _engine, Locale./**/ENGLISH/**/ );
		// ---- customLocaleTest
		assertDefaultLocaleIsPickedUp( _engine );
	}

	private void assertDefaultLocaleIsPickedUp( Engine _engine )
	{
		Locale def = Locale.getDefault();
		try {
			// ---- defaultLocaleTest
			/**/Locale.setDefault/**/( Locale.GERMAN );
			assertComputation( "37287,4211", (MyFactory) _engine.getComputationFactory() );
			/**/Locale.setDefault/**/( Locale.ENGLISH );
			assertComputation( "37287.4211", (MyFactory) _engine.getComputationFactory() );
			// ---- defaultLocaleTest
		}
		finally {
			Locale.setDefault( def );
		}
	}

	private void assertComputation( String _expected, Engine _engine, final Locale _locale )
	{

		// ---- customLocaleFactory
		Computation.Config /**/config/**/ = new Computation.Config( /**/_locale/**/ );
		MyFactory factory = (MyFactory) _engine./**/getComputationFactory( config )/**/;
		// ---- customLocaleFactory

		assertComputation( _expected, factory );
	}

	private void assertComputation( String _expected, MyFactory _factory )
	{
		// ---- customLocaleUse
		MyComputation computation = _factory.newComputation( new MyInputs() );
		String actual = computation.formatted();
		// ---- customLocaleUse

		assertEquals( _expected, actual );
	}


	public static interface MyFactory
	{
		public MyComputation newComputation( MyInputs _inputs );
	}

	public static class MyInputs
	{
		public Date date()
		{
			final Calendar cal = Calendar.getInstance();
			cal.clear();
			cal.set( 2001, 12, 31, 10, 6, 23 );
			return cal.getTime();
		}
	}

	public static interface MyComputation
	{
		public String formatted();
	}


	public void testCompilerConversion() throws Exception
	{
		final String filePath = DATA_PATH + "Locale_en_US.xls";
		// ---- constantValue
		Locale oldLocale = Locale.getDefault();
		Locale./**/setDefault( Locale.GERMANY )/**/;
		try {
			DecimalFormat decimalFormat = ((DecimalFormat) NumberFormat.getInstance());
			assertEquals( /**/','/**/, decimalFormat.getDecimalFormatSymbols().getDecimalSeparator() );

			EngineBuilder builder = SpreadsheetCompiler.newEngineBuilder();
			builder.loadSpreadsheet( filePath );
			/**/builder.getCompileTimeConfig().locale = Locale.US;/**/
			builder.setFactoryClass( ValueFactory.class );
			builder.getByNameBinder().outputs().bindAllMethodsToNamedCells();
			Engine engine = builder.compile();
			ValueFactory factory = (ValueFactory) engine.getComputationFactory();
			ValueComputation computation = factory.newComputation( null );
			assertEquals( /**/1234.56/**/, computation.valueResult(), 0.00001 );
		}
		finally {
			Locale.setDefault( oldLocale );
		}
		// ---- constantValue
	}

	public void testRuntimeConversion() throws Exception
	{
		// ---- boundValue
		Locale oldLocale = Locale.getDefault();
		Locale.setDefault( Locale.GERMANY );
		try {
			DecimalFormat decimalFormat = ((DecimalFormat) NumberFormat.getInstance());
			assertEquals( ',', decimalFormat.getDecimalFormatSymbols().getDecimalSeparator() );

			EngineBuilder builder = SpreadsheetCompiler.newEngineBuilder();
			builder.loadSpreadsheet( DATA_PATH + "Locale_en_US.xls" );
			builder.setFactoryClass( ValueFactory.class );
			builder.getByNameBinder().outputs().bindAllMethodsToNamedCells();
			builder.getByNameBinder()./**/inputs().bindAllMethodsToNamedCells()/**/;
			Engine engine = builder.compile();
			ValueFactory factory = (ValueFactory) engine.getComputationFactory();
			ValueComputation computation = factory.newComputation( new ValueInputs() );
			assertEquals( /**/6543.21/**/, computation.valueResult(), 0.00001 );
		}
		finally {
			Locale.setDefault( oldLocale );
		}
		// ---- boundValue
	}

	public static interface ValueFactory
	{
		public ValueComputation newComputation( ValueInputs _inputs );
	}

	public static class ValueInputs
	{
		// DO NOT REFORMAT BELOW THIS LINE
		// ---- boundValueInput
		public String valueInput() { return /**/"6.543,21"/**/; }
		// ---- boundValueInput
		// DO NOT REFORMAT ABOVE THIS LINE
	}

	public static interface ValueComputation
	{
		public double valueResult();
	}


	public void testSavingDateConstant() throws Exception
	{
		int fourHoursInMS = 4 * 1000 * 60 * 60;

		TimeZone oldTZ = TimeZone.getDefault();
		try {
			TimeZone.setDefault( TimeZone.getTimeZone( "GMT" ) );

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			// ---- setupDateConst
			TimeZone /**/gmt2/**/ = TimeZone.getTimeZone( "GMT+2:00" );
			Calendar cal = Calendar.getInstance( /**/gmt2/**/ );
			cal.clear();
			cal.set( 1970, 6, 13, 12, 13 );
			Date date = cal.getTime();

			SpreadsheetBuilder b = SpreadsheetCompiler.newSpreadsheetBuilder();
			b.newCell( /**/b.cst( date )/**/ );
			b.nameCell( "result" );
			// ---- setupDateConst

			// ---- saveDateConst
			SpreadsheetSaver.Config cfg = new SpreadsheetSaver.Config();
			cfg.spreadsheet = b.getSpreadsheet();
			cfg.typeExtension = ".xls";
			cfg.outputStream = outputStream;
			/**/cfg.timeZone = gmt2;/**/
			SpreadsheetCompiler.newSpreadsheetSaver( cfg ).save();
			// ---- saveDateConst

			outputStream.close();
			byte[] bytes = outputStream.toByteArray();
			InputStream inputStream = new ByteArrayInputStream( bytes );

			// ---- loadDateConst
			Spreadsheet loaded = SpreadsheetCompiler.loadSpreadsheet( ".xls", inputStream );
			EngineBuilder eb = SpreadsheetCompiler.newEngineBuilder();
			eb.setSpreadsheet( loaded );
			eb.setInputClass( Object.class );
			eb.setOutputClass( DateComputation.class );
			eb.bindAllByName();
			SaveableEngine e = eb.compile();

			TimeZone /**/gmt6/**/ = TimeZone.getTimeZone( "GMT+6:00" );
			ComputationFactory f = e.getComputationFactory( new Computation.Config( /**/gmt6/**/ ) );
			DateComputation c = (DateComputation) f.newComputation( null );
			Date result = c.result();
			assertEquals( /**/fourHoursInMS/**/, date.getTime() - result.getTime() );
			// ---- loadDateConst

		}
		finally {
			TimeZone.setDefault( oldTZ );
		}
	}


	public static interface DateComputation
	{
		Date result();
	}

}
