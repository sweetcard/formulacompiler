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

package org.formulacompiler.tests.utils;

import java.io.File;
import java.io.IOException;

import org.formulacompiler.compiler.internal.IOUtil;
import org.formulacompiler.spreadsheet.Spreadsheet;
import org.formulacompiler.spreadsheet.SpreadsheetCompiler;

public abstract class AbstractSpreadsheetDescriptionsTestSuite extends AbstractInitializableTestSuite
{

	protected AbstractSpreadsheetDescriptionsTestSuite( String _name )
	{
		super( _name );
	}

	protected AbstractSpreadsheetDescriptionsTestSuite()
	{
		this( "Check loaded spreadsheets against their .yaml descriptions" );
	}


	protected boolean allow( File _new )
	{
		return true;
	}


	@Override
	protected void addTests() throws Exception
	{
		addTestsFor( ".xls" );
		addTestsFor( ".xlsx" );
		addTestsFor( ".ods" );
	}

	protected abstract void addTestsFor( String _ext ) throws Exception;

	protected final void addTestsIn( String _path, final String _ext, boolean _recurse ) throws Exception
	{
		File path = new File( _path );
		IOUtil.iterateFiles( path, "*" + _ext, path, _recurse, new IOUtil.FileVisitor()
		{

			public void visit( final File _inputFile, final File _outputFile ) throws IOException
			{
				if (allow( _inputFile )) {
					final String fileName = _inputFile.getName();
					final String baseName = fileName.substring( 0, fileName.length() - _ext.length() );
					addTestFor( _inputFile, baseName );
				}
			}

		} );
	}

	private void addTestFor( final File _file, final String _baseName )
	{
		addTest( new AbstractSpreadsheetTestCase( _file.getPath() )
		{

			@Override
			protected void runTest() throws Throwable
			{
				final Spreadsheet sheet = SpreadsheetCompiler.loadSpreadsheet( _file );
				assertYaml( _file.getParentFile(), _baseName, sheet, _file.getName() );
			}

		} );
	}

}
