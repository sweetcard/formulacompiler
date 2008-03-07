/*
 * Copyright (c) 2006, 2008 by Abacus Research AG, Switzerland.
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

package org.formulacompiler.runtime;

import java.nio.charset.Charset;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.TimeZone;


/**
 * Marker interface which is implemented by all computations generated by AFC. The interface is for
 * documentation purposes only, meaning you always have to cast it to the application-specific
 * output type you supplied to AFC's compiler. If you specify an application-specific factory to
 * AFC, then this cast is not necessary as the factory already returns the proper
 * application-specific computation type.
 * 
 * @author peo
 */
public interface Computation
{


	/**
	 * Provides configuration information for an execution environment for computations.
	 * <p>
	 * Please refer to the <a target="_top" href="{@docRoot}/../tutorial/locale.htm"
	 * target="_top">tutorial</a> for details.
	 * </p>
	 * 
	 * @author peo
	 */
	public final class Config
	{

		/**
		 * Locale to use for, for example, labelling weekdays and months. If left {@code null} will
		 * use the default locale at the time the computation is run.
		 */
		public Locale locale = null;

		/**
		 * A set of symbols (such as the decimal separator, the grouping separator, and so on) needed
		 * to format numbers. If this field is not set, the default values for the {@link #locale} are
		 * used.
		 */
		public DecimalFormatSymbols decimalFormatSymbols = null;

		/**
		 * Time zone to use to, for example, convert spreadsheet-internal numeric dates to
		 * {@link java.util.Date}. If left {@code null} will use the default time zone at the time
		 * the computation is run.
		 */
		public TimeZone timeZone = null;

		/**
		 * Charset, which is used in CHAR and CODE functions. If left {@code null}, the default
		 * charset for the locale will be used.
		 */
		public Charset charset = null;


		/**
		 * Uses default settings for all fields.
		 */
		public Config()
		{
			super();
		}

		/**
		 * Overrides the {@link #locale} field.
		 */
		public Config( Locale _locale )
		{
			this.locale = _locale;
		}

		/**
		 * Overrides the {@link #timeZone} field.
		 */
		public Config( TimeZone _timeZone )
		{
			this.timeZone = _timeZone;
		}

		/**
		 * Overrides the {@link #locale} and {@link #timeZone} fields.
		 */
		public Config( Locale _locale, TimeZone _timeZone )
		{
			this.locale = _locale;
			this.timeZone = _timeZone;
		}

		/**
		 * Overrides the {@link #locale}, and {@link #decimalFormatSymbols} fields.
		 */
		public Config( Locale _locale, DecimalFormatSymbols _decimalFormatSymbols )
		{
			this.locale = _locale;
			this.decimalFormatSymbols = _decimalFormatSymbols;
		}

		/**
		 * Overrides the {@link #locale}, and {@link #decimalFormatSymbols}, and {@link #timeZone}
		 * fields.
		 */
		public Config( Locale _locale, DecimalFormatSymbols _decimalFormatSymbols, TimeZone _timeZone )
		{
			this.locale = _locale;
			this.decimalFormatSymbols = _decimalFormatSymbols;
			this.timeZone = _timeZone;
		}

		/**
		 * Validates the configuration for missing or improperly set values.
		 * 
		 * @throws IllegalArgumentException
		 */
		public void validate()
		{
			// Nothing so far.
		}

		@Override
		public String toString()
		{
			final StringBuilder res = new StringBuilder();
			if (null != this.locale) res.append( "[locale: " ).append( this.locale ).append( " ]" );
			if (null != this.timeZone) res.append( "[timeZone: " ).append( this.timeZone ).append( " ]" );
			if (null != this.decimalFormatSymbols)
				res.append( "[decimalFormatSymbols: " ).append( this.decimalFormatSymbols ).append( " ]" );
			return res.toString();
		}

	}

}
