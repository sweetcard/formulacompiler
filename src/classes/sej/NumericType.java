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
package sej;

import java.math.BigDecimal;


/**
 * Immutable class representing the type to be used by the numeric computations of generated
 * engines.
 * 
 * @author peo
 */
public abstract class NumericType
{

	/**
	 * For BigDecimal types, indicates that no explicit scaling should be performed by the engine.
	 */
	public static final int UNDEFINED_SCALE = Integer.MAX_VALUE;

	/**
	 * Default type, consistent with the type used internally by Excel and other spreadsheet
	 * applications.
	 */
	public static final NumericType DOUBLE = getInstance( Double.TYPE );

	/**
	 * BigDecimal with a fixed scale of 8 and using {@link BigDecimal#ROUND_HALF_UP}. A good choice
	 * for financial applications.
	 */
	public static final NumericType BIGDECIMAL8 = getInstance( BigDecimal.class, 8, BigDecimal.ROUND_HALF_UP );

	/**
	 * BigDecimal with a fixed scale of 9 and using {@link BigDecimal#ROUND_HALF_UP}. This type has
	 * the same precision as {@link #DOUBLE} for the automated tests.
	 */
	public static final NumericType BIGDECIMAL9 = getInstance( BigDecimal.class, 9, BigDecimal.ROUND_HALF_UP );

	/**
	 * Unscaled {@code long} for fast, strictly integer computations.
	 */
	public static final NumericType LONG = getInstance( Long.TYPE, 0 );

	/**
	 * {@code long} scaled to 4 decimal places for fast, fixed point computations (similar to the
	 * currency type found in Microsoft COM and Borland Delphi).
	 */
	public static final NumericType LONG4 = getInstance( Long.TYPE, 4 );

	/**
	 * Scaled {@code long} with 4 decimal places. Corresponds to the Currency type found in Microsoft
	 * COM and Borland Delphi.
	 */
	public static final NumericType CURRENCY = getInstance( Long.TYPE, 4 );

	private final Class valueType;
	private final int scale;
	private final int roundingMode;

	/**
	 * To ensure compatibility with JRE 1.4 I cannot use a MathContext here.
	 */
	protected NumericType(Class _valueType, int _scale, int _roundingMode)
	{
		super();
		this.valueType = _valueType;
		this.scale = _scale;
		this.roundingMode = _roundingMode;
	}


	/**
	 * Same as {@link #getInstance(Class, int, int)} with an undefined scale and truncating results.
	 */
	public static NumericType getInstance( Class _valueType )
	{
		return getInstance( _valueType, UNDEFINED_SCALE, BigDecimal.ROUND_DOWN );
	}

	/**
	 * Same as {@link #getInstance(Class, int, int)} and truncating results.
	 */
	public static NumericType getInstance( Class _valueType, int _scale )
	{
		return getInstance( _valueType, _scale, BigDecimal.ROUND_DOWN );
	}

	/**
	 * Returns an instance with the specified attributes.
	 */
	public static NumericType getInstance( Class _valueType, int _scale, int _roundingMode )
	{
		if (_valueType == Double.TYPE) {
			if (_scale != UNDEFINED_SCALE) throw new IllegalArgumentException( "Scale is not supported for double" );
			if (_roundingMode != BigDecimal.ROUND_DOWN)
				throw new IllegalArgumentException( "Rounding is not supported for double" );
			return new DoubleType();
		}
		else if (_valueType == Long.TYPE) {
			if (_scale == 0) {
				if (_roundingMode != BigDecimal.ROUND_DOWN)
					throw new IllegalArgumentException( "Unscaled long can only be rounded down" );
				return new LongType();
			}
			else {
				if (_roundingMode != BigDecimal.ROUND_DOWN)
					throw new IllegalArgumentException( "Scaled long can only be rounded down" );
				return new ScaledLongType( _scale );
			}
		}
		else if (_valueType == BigDecimal.class) {
			return new BigDecimalType( _scale, _roundingMode );
		}
		throw new IllegalArgumentException( "Unsupported numeric type" );
	}


	public Class getValueType()
	{
		return this.valueType;
	}

	public int getScale()
	{
		return this.scale;
	}

	public int getRoundingMode()
	{
		return this.roundingMode;
	}


	/**
	 * Returns the number 0.
	 */
	public abstract Number getZero();

	/**
	 * Returns the number 1.
	 */
	public abstract Number getOne();


	/**
	 * Parses a string into a value. Null and the empty string return zero (see {@link #getZero()}).
	 */
	public final Number valueOf( String _value )
	{
		if (null == _value) return getZero();
		if (0 == _value.length()) return getZero();
		return convertFromString( _value );
	}

	/**
	 * Returns the value as a string in its canonical representation. Null returns the empty string.
	 */
	public final String valueToString( Number _value )
	{
		if (null == _value) return "";
		return convertToString( _value );
	}

	/**
	 * Returns the value as a string with no superfluous leading or trailing zeroes and decimal
	 * point. Null returns the empty string.
	 */
	public final String valueToConciseString( Number _value )
	{
		if (null == _value) return "";
		return convertToConciseString( _value );
	}


	protected String convertToConciseString( Number _value )
	{
		return trimTrailingZerosAndPoint( valueToString( _value ) );
	}


	private String trimTrailingZerosAndPoint( String _string )
	{
		String result = _string;
		if (result.contains( "." )) {
			int l = result.length();
			while ('0' == result.charAt( l - 1 ))
				l--;
			if ('.' == result.charAt( l - 1 )) l--;
			result = result.substring( 0, l );
		}
		return result;
	}


	protected abstract Number convertFromString( String _value );
	protected abstract String convertToString( Number _value );


	@Override
	public String toString()
	{
		return getValueType().getName() + ((UNDEFINED_SCALE != getScale()) ? "." + Integer.toString( getScale() ) : "");
	}


	/**
	 * Internal to SEJ.
	 * 
	 * @author peo
	 */
	public static final class DoubleType extends NumericType
	{

		protected DoubleType()
		{
			super( Double.TYPE, UNDEFINED_SCALE, BigDecimal.ROUND_UNNECESSARY );
		}

		@Override
		public Number getZero()
		{
			return Double.valueOf( 0.0 );
		}

		@Override
		public Number getOne()
		{
			return Double.valueOf( 1.0 );
		}

		@Override
		protected Number convertFromString( String _value )
		{
			return Double.valueOf( _value );
		}

		@Override
		protected String convertToString( Number _value )
		{
			return _value.toString();
		}

	}


	/**
	 * Internal to SEJ.
	 * 
	 * @author peo
	 */
	public static final class BigDecimalType extends NumericType
	{

		protected BigDecimalType(int _scale, int _roundingMode)
		{
			super( BigDecimal.class, _scale, _roundingMode );
		}

		@Override
		public Number getZero()
		{
			return BigDecimal.ZERO;
		}

		@Override
		public Number getOne()
		{
			return BigDecimal.ONE;
		}

		@Override
		protected Number convertFromString( String _value )
		{
			BigDecimal result = new BigDecimal( _value );
			if (getScale() != UNDEFINED_SCALE) {
				result = result.setScale( getScale(), getRoundingMode() );
			}
			return result;
		}

		@Override
		protected String convertToString( Number _value )
		{
			return ((BigDecimal) _value).toPlainString();
		}

	}


	/**
	 * Internal to SEJ.
	 * 
	 * @author peo
	 */
	public static abstract class AbstractLongType extends NumericType
	{

		protected AbstractLongType(int _scale, int _roundingMode)
		{
			super( Long.TYPE, _scale, _roundingMode );
		}

		@Override
		public Number getZero()
		{
			return Long.valueOf( zero() );
		}

		@Override
		public Number getOne()
		{
			return Long.valueOf( one() );
		}

		public long zero()
		{
			return 0L;
		}

		public abstract long one();
		
		public abstract long parse( String _value );

		public abstract String format( long _value );

		public final long fromNumber( Number _value )
		{
			String asString;
			if (_value instanceof BigDecimal) {
				final BigDecimal big = (BigDecimal) _value;
				asString = big.toPlainString();
			}
			else {
				asString = _value.toString();
			}
			return parse( asString );
		}

		@Override
		protected final Number convertFromString( String _value )
		{
			return Long.valueOf( parse( _value ) );
		}

		@Override
		protected final String convertToString( Number _value )
		{
			return format( (Long) _value );
		}

	}


	/**
	 * Internal to SEJ.
	 * 
	 * @author peo
	 */
	public static final class LongType extends AbstractLongType
	{

		protected LongType()
		{
			super( UNDEFINED_SCALE, BigDecimal.ROUND_DOWN );
		}

		@Override
		public long one()
		{
			return 1;
		}

		@Override
		public long parse( String _value )
		{
			return Long.parseLong( _value );
		}

		@Override
		public String format( long _value )
		{
			return Long.toString( _value );
		}

		@Override
		protected String convertToConciseString( Number _value )
		{
			return convertToString( _value ); // Need no trimming here.
		}

	}


	/**
	 * Internal to SEJ.
	 * 
	 * @author peo
	 */
	public static final class ScaledLongType extends AbstractLongType
	{
		private static long[] SCALING_FACTORS = new long[] { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000,
				100000000, 1000000000, 10000000000L, 100000000000L, 1000000000000L, 10000000000000L, 100000000000000L,
				1000000000000000L, 10000000000000000L, 100000000000000000L, 1000000000000000000L };

		private final long scalingFactor;

		protected ScaledLongType(int _scale)
		{
			super( _scale, BigDecimal.ROUND_DOWN );
			if (_scale < 1 || _scale >= SCALING_FACTORS.length) {
				throw new IllegalArgumentException( "Scale is out of range" );
			}
			this.scalingFactor = SCALING_FACTORS[ _scale ];
		}

		@Override
		public long one()
		{
			return this.scalingFactor;
		}

		@Override
		public long parse( String _value )
		{
			final int posOfDecPoint = _value.indexOf( '.' );
			if (posOfDecPoint < 0) {
				return Long.parseLong( _value ) * this.scalingFactor;
			}
			else {
				final int scaleOfResult = getScale();
				final int scaleOfValue = _value.length() - posOfDecPoint - 1;
				final int scaleOfDigits = (scaleOfValue > scaleOfResult) ? scaleOfResult : scaleOfValue;
				final String digits = _value.substring( 0, posOfDecPoint )
						+ _value.substring( posOfDecPoint + 1, posOfDecPoint + 1 + scaleOfDigits );
				final long unscaled = Long.parseLong( digits );
				if (scaleOfDigits == scaleOfResult) {
					return unscaled;
				}
				else {
					assert scaleOfDigits < scaleOfResult;
					final long rescalingFactor = SCALING_FACTORS[ scaleOfResult - scaleOfDigits ];
					return unscaled * rescalingFactor;
				}
			}
		}

		@Override
		public String format( long _value )
		{
			final long abs = (_value < 0) ? -_value : _value;
			final String sign = (_value < 0) ? "-" : "";
			String digits = Long.toString( abs );
			int len = digits.length();
			if (len <= getScale()) {
				int addlZeroes = getScale() - len + 1;
				digits = "0000000000000000000000000000000".substring( 0, addlZeroes ) + digits;
				len += addlZeroes;
			}
			final String whole = digits.substring( 0, len - getScale() );
			final String fract = digits.substring( len - getScale() );
			return sign + whole + '.' + fract;
		}

	}

}
