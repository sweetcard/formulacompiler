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

package org.formulacompiler.spreadsheet.internal.excel.xlsx;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.formulacompiler.tests.utils.SpreadsheetVerifier;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XlsxSpreadsheetVerifier implements SpreadsheetVerifier
{
    private static final class XMLErrorHandler implements ErrorHandler {

        public void warning( SAXParseException exception ) {
            System.out.println( "WARNING: " + exception.toString() );
        }

        public void error( SAXParseException exception ) {
        	System.out.println( "ERROR: " + exception.toString() );
        }

        public void fatalError( SAXParseException exception ) {
        	System.out.println( "FATAL ERROR: " + exception.toString() );
        }
    }

    private static final class NonClosableInputStream extends FilterInputStream
	{
		private NonClosableInputStream( InputStream _in ) {
			super( _in );
		}

		@Override
		public void close() throws IOException {
			// Do not close.
		}
	}
	
	public void verify( InputStream _odsInputStream ) throws Exception
	{
		final Validator workbookValidator = getValidator( "sml-workbook.xsd" );
		final Validator worksheetValidator = getValidator( "sml-sheet.xsd" );

		final ZipInputStream zipInputStream = new ZipInputStream( _odsInputStream );
		ZipEntry zipEntry;
		while ((zipEntry = zipInputStream.getNextEntry()) != null) {
			final String name = zipEntry.getName();
			final InputStream inputStream = new NonClosableInputStream( zipInputStream );
			if ("xl/workbook.xml".equals( name )) {
				workbookValidator.reset();
				workbookValidator.validate( new StreamSource( inputStream ) );
			}
			else if ("xl/worksheets/sheet1.xml".equals( name )) {
				workbookValidator.reset();
				worksheetValidator.validate( new StreamSource( inputStream ) );
			}
		}
	}
	
	private static Validator getValidator( String _schema ) throws SAXException
	{
		final ClassLoader classLoader = XlsxSpreadsheetVerifier.class.getClassLoader();
		final URL url = classLoader.getResource( "schema/" + _schema );
		final SchemaFactory factory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
		final Schema schema = factory.newSchema( url );
		final Validator validator = schema.newValidator();
		validator.setErrorHandler( new XMLErrorHandler() );
		return validator;
	}

	public static final class Factory implements SpreadsheetVerifier.Factory
	{
		public SpreadsheetVerifier getInstance()
		{
			return new XlsxSpreadsheetVerifier();
		}

		public boolean canHandle( String _fileExtension )
		{
			return _fileExtension.equalsIgnoreCase( ".xlsx" );
		}
	}
}
