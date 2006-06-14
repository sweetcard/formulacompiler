package sej.internal.spreadsheet.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import sej.Spreadsheet;
import sej.SpreadsheetError;
import sej.internal.spreadsheet.loader.excel.xls.ExcelXLSLoader;

/**
 * Central dispatcher for the loaders for the various spreadsheet file formats supported by SEJ. You
 * must first register the loaders you want to be active using their {@code register()} method.
 * 
 * @see sej.internal.spreadsheet.loader.excel.xls.ExcelXLSLoader
 * @see sej.internal.spreadsheet.loader.excel.xml.ExcelXMLLoader
 * @author peo
 */
public final class AnyFormatSpreadsheetLoader
{
	private static Collection<Factory> factories = new ArrayList<Factory>();
	
	static {
		ExcelXLSLoader.register();
	}


	/**
	 * Registers a loader for a particular spreadsheet file format. You should not use this method
	 * directly. Rather, use the {@code register()} method of the loaders themselves (which will then
	 * call this method).
	 * 
	 * @param _factory is a factory for a spreadsheet file loader.
	 */
	public static void registerLoader( Factory _factory )
	{
		if (!factories.contains( _factory )) {
			factories.add( _factory );
		}
	}


	/**
	 * Loads a spreadsheet stream into an SEJ spreadsheet model. The loader to use is determined by
	 * giving each registered loader a look at the file name. The first one that signals it can
	 * handle it is used.
	 * 
	 * @param _originalFileName is the complete file name of the original spreadsheet file (for
	 *           example Test.xls or Test.xml).
	 * @return The spreadsheet model loaded from the file.
	 * @throws IOException when there is any proplem accessing the file. May also throw runtime
	 *            exceptions when there are problems in file.
	 */
	public static Spreadsheet loadSpreadsheet( String _originalFileName, InputStream _stream ) throws IOException,
			SpreadsheetError
	{
		for (Factory factory : factories) {
			if (factory.canHandle( _originalFileName )) {
				SpreadsheetLoader loader = factory.newWorkbookLoader();
				return loader.loadFrom( _stream );
			}
		}
		throw new IllegalStateException( "No loader found for file " + _originalFileName );
	}


	/**
	 * Interface that must be implemented by spreadsheet file loader factories to be able to
	 * participate in the central dispatching by {@link SpreadsheetLoader}.
	 * 
	 * @author peo
	 * 
	 */
	public static interface Factory
	{

		public SpreadsheetLoader newWorkbookLoader();

		public boolean canHandle( String _fileName );

	}


}