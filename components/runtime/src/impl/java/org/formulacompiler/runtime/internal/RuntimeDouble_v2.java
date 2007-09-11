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
package org.formulacompiler.runtime.internal;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;


public final class RuntimeDouble_v2 extends Runtime_v2
{
	private static final double EXCEL_EPSILON = 0.0000001;
	private static final double[] POW10 = { 1E-10, 1E-9, 1E-8, 1E-7, 1E-6, 1E-5, 1E-4, 1E-3, 1E-2, 1E-1, 1, 1E+1, 1E+2,
			1E+3, 1E+4, 1E+5, 1E+6, 1E+7, 1E+8, 1E+9, 1E+10 };


	public static double max( final double a, final double b )
	{
		return a >= b? a : b;
	}

	public static double min( final double a, final double b )
	{
		return a <= b? a : b;
	}

	public static double fun_CEILING( final double _number, final double _significance )
	{
		final double a = _number / _significance;
		if (a < 0) {
			return 0.0; // Excel #NUM
		}
		return roundUp( a ) * _significance;
	}

	public static double fun_FLOOR( final double _number, final double _significance )
	{
		final double a = _number / _significance;
		if (a < 0) {
			return 0.0; // Excel #NUM
		}
		return roundDown( a ) * _significance;
	}

	// Leave this comment in. It is used to cite the code into the documentation.
	// ---- round
	public static double round( final double _val, final int _maxFrac )
	{
		final double shift = pow10( _maxFrac );
		if (0 > _val) {
			return Math.ceil( _val * shift - 0.5 ) / shift;
		}
		else {
			return Math.floor( _val * shift + 0.5 ) / shift;
		}
	}
	// ---- round

	public static double fun_ROUNDDOWN( final double _val, final int _maxFrac )
	{
		final double shift = pow10( _maxFrac );
		return roundDown( _val * shift ) / shift;
	}

	public static double fun_ROUNDUP( final double _val, final int _maxFrac )
	{
		final double shift = pow10( _maxFrac );
		return roundUp( _val * shift ) / shift;
	}

	public static double trunc( final double _val, final int _maxFrac )
	{
		final double shift = pow10( _maxFrac );
		return roundDown( _val * shift ) / shift;
	}

	private static double roundDown( final double _val )
	{
		return 0 > _val? Math.ceil( _val ) : Math.floor( _val );
	}

	private static double roundUp( final double _val )
	{
		return 0 > _val? Math.floor( _val ) : Math.ceil( _val );
	}

	private static double pow10( final int _exp )
	{
		return (_exp >= -10 && _exp <= 10)? POW10[ _exp + 10 ] : Math.pow( 10, _exp );
	}

	public static boolean booleanFromNum( final double _val )
	{
		return (_val != 0);
	}

	public static double booleanToNum( final boolean _val )
	{
		return _val? 1.0 : 0.0;
	}

	public static double numberToNum( final Number _num )
	{
		return (_num == null)? 0.0 : _num.doubleValue();
	}


	public static double fromScaledLong( long _scaled, long _scalingFactor )
	{
		return ((double) _scaled) / ((double) _scalingFactor);
	}

	public static long toScaledLong( double _value, long _scalingFactor )
	{
		return (long) (_value * _scalingFactor);
	}


	public static String toExcelString( double _value, Environment _environment )
	{
		return stringFromBigDecimal( BigDecimal.valueOf( _value ), _environment );
	}


	// ---- Excel date conversion; copied from JExcelAPI (DateRecord.java)

	public static Date dateFromNum( final double _excel, TimeZone _timeZone )
	{
		return new Date( msSinceUTC1970FromNum( _excel, _timeZone ) );
	}

	public static long msSinceUTC1970FromNum( final double _excel, TimeZone _timeZone )
	{
		final long msSinceLocal1970 = msSinceLocal1970FromExcelDate( _excel );
		final int timeZoneOffset = _timeZone.getOffset( msSinceLocal1970 - _timeZone.getRawOffset() );
		final long msSinceUTC1970 = msSinceLocal1970 - timeZoneOffset;
		return msSinceUTC1970;
	}

	public static long msFromNum( final double _excel )
	{
		final long ms = Math.round( _excel * SECS_PER_DAY ) * MS_PER_SEC;
		return ms;
	}

	public static double dateToNum( final Date _date, TimeZone _timeZone )
	{
		if (_date == null) {
			return 0;
		}
		else {
			final long msSinceLocal1970 = dateToMsSinceLocal1970( _date, _timeZone );
			final double excel = msSinceLocal1970ToExcelDate( msSinceLocal1970 );
			return excel;
		}
	}

	public static double msSinceUTC1970ToNum( final long _msSinceUTC1970, TimeZone _timeZone )
	{
		final int timeZoneOffset = _timeZone.getOffset( _msSinceUTC1970 );
		final long msSinceLocal1970 = _msSinceUTC1970 + timeZoneOffset;
		final double excel = msSinceLocal1970ToExcelDate( msSinceLocal1970 );
		return excel;
	}

	public static double msToNum( final long _ms )
	{
		final double excel = (double) _ms / (double) MS_PER_DAY;
		return excel;
	}

	private static long msSinceLocal1970FromExcelDate( final double _excelDate )
	{
		final boolean time = (Math.abs( _excelDate ) < 1);
		double numValue = _excelDate;

		// Work round a bug in excel. Excel seems to think there is a date
		// called the 29th Feb, 1900 - but in actual fact this was not a leap year.
		// Therefore for values less than 61 in the 1900 date system,
		// add one to the numeric value
		if (!BASED_ON_1904 && !time && numValue < NON_LEAP_DAY) {
			numValue += 1;
		}

		// Convert this to the number of days since 01 Jan 1970
		final int offsetDays = BASED_ON_1904? UTC_OFFSET_DAYS_1904 : UTC_OFFSET_DAYS;
		final double utcDays = numValue - offsetDays;

		// Convert this into utc by multiplying by the number of milliseconds
		// in a day. Use the round function prior to ms conversion due
		// to a rounding feature of Excel (contributed by Jurgen
		final long msSinceLocal1970 = Math.round( utcDays * SECS_PER_DAY ) * MS_PER_SEC;
		return msSinceLocal1970;
	}

	private static double msSinceLocal1970ToExcelDate( final long _msSinceLocal1970 )
	{
		// Convert this to the number of days, plus fractions of a day since
		// 01 Jan 1970
		final double utcDays = (double) _msSinceLocal1970 / (double) MS_PER_DAY;

		// Add in the offset to get the number of days since 01 Jan 1900
		double value = utcDays + UTC_OFFSET_DAYS;

		// Work round a bug in excel. Excel seems to think there is a date
		// called the 29th Feb, 1900 - but this was not a leap year.
		// Therefore for values less than 61, we must subtract 1. Only do
		// this for full dates, not times
		if (value < NON_LEAP_DAY) {
			value -= 1;
		}

		return value;
	}

	private static double valueOrZero( final double _value )
	{
		if (Double.isNaN( _value ) || Double.isInfinite( _value )) {
			return 0.0; // Excel #NUM
		}
		return _value;
	}


	public static double fun_DATE( final int _year, final int _month, final int _day )
	{
		final int year = _year < 1899? _year + 1900 : _year;
		return dateToNum( year, _month, _day );
	}

	private static double dateToNum( final int _year, final int _month, final int _day )
	{
		final Calendar calendar = new GregorianCalendar( TimeZone.getTimeZone( "GMT" ) );
		calendar.clear();
		calendar.setLenient( true );
		calendar.set( Calendar.YEAR, _year );
		calendar.set( Calendar.MONTH, _month - 1 );
		calendar.set( Calendar.DAY_OF_MONTH, _day );
		final Date date = calendar.getTime();
		final TimeZone timeZone = calendar.getTimeZone();
		return dateToNum( date, timeZone );
	}

	public static int fun_WEEKDAY( final double _date, int _type )
	{
		final int dayOfWeek = getCalendarValueFromNum( _date, Calendar.DAY_OF_WEEK );
		switch (_type) {
			case 1:
				return dayOfWeek;
			case 2:
				return dayOfWeek > 1? dayOfWeek - 1 : 7;
			case 3:
				return dayOfWeek > 1? dayOfWeek - 2 : 6;
			default:
				return 0; // Excel #NUM
		}
	}

	static long getDaySecondsFromNum( final double _time )
	{
		final double time = _time % 1;
		return Math.round( time * SECS_PER_DAY );
	}

	public static int fun_DAY( final double _date )
	{
		return getCalendarValueFromNum( _date, Calendar.DAY_OF_MONTH );
	}

	public static int fun_MONTH( final double _date )
	{
		return getCalendarValueFromNum( _date, Calendar.MONTH ) + 1;
	}

	public static int fun_YEAR( final double _date )
	{
		return getCalendarValueFromNum( _date, Calendar.YEAR );
	}

	private static int getCalendarValueFromNum( double _date, int _field )
	{
		final Calendar calendar = new GregorianCalendar( TimeZone.getTimeZone( "GMT" ) );
		final TimeZone timeZone = calendar.getTimeZone();
		final Date date = dateFromNum( _date, timeZone );
		calendar.setTime( date );
		return calendar.get( _field );
	}

	public static double fun_NOW( final Environment _environment, final ComputationTime _computationTime )
	{
		return dateToNum( now( _computationTime ), _environment.timeZone() );
	}

	public static double fun_TODAY( final Environment _environment, final ComputationTime _computationTime )
	{
		final TimeZone timeZone = _environment.timeZone();
		return dateToNum( today( timeZone, _computationTime ), timeZone );
	}

	public static double fun_TIME( double _hour, double _minute, double _second )
	{
		final long seconds = ((long) _hour * SECS_PER_HOUR + (long) _minute * 60 + (long) _second) % SECS_PER_DAY;
		return (double) seconds / SECS_PER_DAY;
	}

	public static double fun_SECOND( double _date )
	{
		final long seconds = getDaySecondsFromNum( _date ) % 60;
		return seconds;
	}

	public static double fun_MINUTE( double _date )
	{
		final long minutes = getDaySecondsFromNum( _date ) / 60 % 60;
		return minutes;
	}

	public static double fun_HOUR( double _date )
	{
		final long hours = getDaySecondsFromNum( _date ) / SECS_PER_HOUR % 24;
		return hours;
	}


	public static double fun_ACOS( double _a )
	{
		if (_a < -1 || _a > 1) {
			return 0.0; // Excel #NUM!
		}
		else {
			return Math.acos( _a );
		}
	}

	public static double fun_ASIN( double _a )
	{
		if (_a < -1 || _a > 1) {
			return 0.0; // Excel #NUM!
		}
		else {
			return Math.asin( _a );
		}
	}

	public static double fun_TRUNC( final double _val )
	{
		return roundDown( _val );
	}

	public static double fun_EVEN( final double _val )
	{
		return roundUp( _val / 2 ) * 2;
	}

	public static double fun_ODD( final double _val )
	{
		if (0 > _val) {
			return Math.floor( (_val - 1) / 2 ) * 2 + 1;
		}
		else {
			return Math.ceil( (_val + 1) / 2 ) * 2 - 1;
		}
	}

	public static double fun_POWER( final double _n, final double _p )
	{
		return valueOrZero( Math.pow( _n, _p ) );
	}

	public static double fun_LN( final double _p )
	{
		return valueOrZero( Math.log( _p ) );
	}

	public static double fun_LOG( final double _n, final double _x )
	{
		final double lnN = Math.log( _n );
		if (Double.isNaN( lnN ) || Double.isInfinite( lnN )) {
			return 0; // Excel #NUM!
		}
		final double lnX = Math.log( _x );
		if (Double.isNaN( lnX ) || Double.isInfinite( lnX )) {
			return 0; // Excel #NUM!
		}
		if (lnX == 0) {
			return 0; // Excel #DIV/0!
		}
		return lnN / lnX;
	}

	public static double fun_LOG10( final double _p )
	{
		return valueOrZero( Math.log10( _p ) );
	}

	/**
	 * Evaluates the given polynomial of degree <tt>N</tt> at <tt>x</tt>, assuming coefficient of N is 1.0.
	 * Otherwise same as <tt>polevl()</tt>.
	 * <pre>
	 *                     2          N
	 * y  =  C  + C x + C x  +...+ C x
	 *        0    1     2          N
	 * <p/>
	 * where C  = 1 and hence is omitted from the array.
	 *        N
	 * <p/>
	 * Coefficients are stored in reverse order:
	 * <p/>
	 * coef[0] = C  , ..., coef[N-1] = C  .
	 *            N-1                   0
	 * <p/>
	 * Calling arguments are otherwise the same as polevl().
	 * </pre>
	 * In the interest of speed, there are no checks for out of bounds arithmetic.
	 * <p/>
	 * <p/>
	 * This code is adopted from <a href="http://dsd.lbl.gov/~hoschek/colt/">Colt Library</a>.
	 * Copyright (c) 1999 CERN - European Organization for Nuclear Research.
	 *
	 * @param x    argument to the polynomial.
	 * @param coef the coefficients of the polynomial.
	 * @param N    the degree of the polynomial.
	 */
	private static double p1evl( double x, double coef[], int N )
	{
		double ans = x + coef[ 0 ];

		for (int i = 1; i < N; i++) {
			ans = ans * x + coef[ i ];
		}

		return ans;
	}

	/**
	 * Evaluates the given polynomial of degree <tt>N</tt> at <tt>x</tt>.
	 * <pre>
	 *                     2          N
	 * y  =  C  + C x + C x  +...+ C x
	 *        0    1     2          N
	 * <p/>
	 * Coefficients are stored in reverse order:
	 * <p/>
	 * coef[0] = C  , ..., coef[N] = C  .
	 *            N                   0
	 * </pre>
	 * In the interest of speed, there are no checks for out of bounds arithmetic.
	 * <p/>
	 * <p/>
	 * This code is adopted from <a href="http://dsd.lbl.gov/~hoschek/colt/">Colt Library</a>.
	 * Copyright (c) 1999 CERN - European Organization for Nuclear Research.
	 *
	 * @param x    argument to the polynomial.
	 * @param coef the coefficients of the polynomial.
	 * @param N    the degree of the polynomial.
	 */
	private static double polevl( double x, double coef[], int N )
	{
		double ans = coef[ 0 ];

		for (int i = 1; i <= N; i++) {
			ans = ans * x + coef[ i ];
		}

		return ans;
	}

	/**
	 * Returns the error function of the normal distribution; formerly named <tt>erf</tt>.
	 * The integral is
	 * <pre>
	 *                           x
	 *                            -
	 *                 2         | |          2
	 *   erf(x)  =  --------     |    exp( - t  ) dt.
	 *              sqrt(pi)   | |
	 *                          -
	 *                           0
	 * </pre>
	 * <b>Implementation:</b>
	 * For <tt>0 <= |x| < 1, erf(x) = x * P4(x**2)/Q5(x**2)</tt>; otherwise
	 * <tt>erf(x) = 1 - erfc(x)</tt>.
	 * <p/>
	 * Code adapted from the <A HREF="http://www.sci.usq.edu.au/staff/leighb/graph/Top.html">Java 2D Graph Package 2.4</A>,
	 * which in turn is a port from the <A HREF="http://people.ne.mediaone.net/moshier/index.html#Cephes">Cephes 2.2</A> Math Library (C).
	 * <p/>
	 * <p/>
	 * This code is adopted from <a href="http://dsd.lbl.gov/~hoschek/colt/">Colt Library</a>.
	 * Copyright (c) 1999 CERN - European Organization for Nuclear Research.
	 *
	 * @param x the argument to the function.
	 */
	public static double fun_ERF( double x )
	{
		final double T[] = {
				9.60497373987051638749E0,
				9.00260197203842689217E1,
				2.23200534594684319226E3,
				7.00332514112805075473E3,
				5.55923013010394962768E4
		};
		final double U[] = {
				3.35617141647503099647E1,
				5.21357949780152679795E2,
				4.59432382970980127987E3,
				2.26290000613890934246E4,
				4.92673942608635921086E4
		};

		if (Math.abs( x ) > 1.0) {
			return 1.0 - fun_ERFC( x );
		}
		final double z = x * x;
		final double y = x * polevl( z, T, 4 ) / p1evl( z, U, 5 );
		return y;
	}

	/**
	 * Returns the complementary Error function of the normal distribution; formerly named <tt>erfc</tt>.
	 * <pre>
	 *  1 - erf(x) =
	 * <p/>
	 *                           inf.
	 *                             -
	 *                  2         | |          2
	 *   erfc(x)  =  --------     |    exp( - t  ) dt
	 *               sqrt(pi)   | |
	 *                           -
	 *                            x
	 * </pre>
	 * <b>Implementation:</b>
	 * For small x, <tt>erfc(x) = 1 - erf(x)</tt>; otherwise rational
	 * approximations are computed.
	 * <p/>
	 * Code adapted from the <A HREF="http://www.sci.usq.edu.au/staff/leighb/graph/Top.html">Java 2D Graph Package 2.4</A>,
	 * which in turn is a port from the <A HREF="http://people.ne.mediaone.net/moshier/index.html#Cephes">Cephes 2.2</A> Math Library (C).
	 * <p/>
	 * <p/>
	 * This code is adopted from <a href="http://dsd.lbl.gov/~hoschek/colt/">Colt Library</a>.
	 * Copyright (c) 1999 CERN - European Organization for Nuclear Research.
	 *
	 * @param a the argument to the function.
	 */
	public static double fun_ERFC( double a )
	{
		final double MAXLOG = 7.09782712893383996732E2;
		final double P[] = {
				2.46196981473530512524E-10,
				5.64189564831068821977E-1,
				7.46321056442269912687E0,
				4.86371970985681366614E1,
				1.96520832956077098242E2,
				5.26445194995477358631E2,
				9.34528527171957607540E2,
				1.02755188689515710272E3,
				5.57535335369399327526E2
		};
		final double Q[] = {
				//1.0
				1.32281951154744992508E1,
				8.67072140885989742329E1,
				3.54937778887819891062E2,
				9.75708501743205489753E2,
				1.82390916687909736289E3,
				2.24633760818710981792E3,
				1.65666309194161350182E3,
				5.57535340817727675546E2
		};
		final double R[] = {
				5.64189583547755073984E-1,
				1.27536670759978104416E0,
				5.01905042251180477414E0,
				6.16021097993053585195E0,
				7.40974269950448939160E0,
				2.97886665372100240670E0
		};
		final double S[] = {
				2.26052863220117276590E0,
				9.39603524938001434673E0,
				1.20489539808096656605E1,
				1.70814450747565897222E1,
				9.60896809063285878198E0,
				3.36907645100081516050E0
		};

		final double x = a < 0.0 ? -a : a;

		if (x < 1.0) {
			return 1.0 - fun_ERF( a );
		}

		double z = -a * a;

		if (z < -MAXLOG) {
			return a < 0 ? 2.0 : 0.0;
		}

		z = Math.exp( z );

		final double p, q;
		if (x < 8.0) {
			p = polevl( x, P, 8 );
			q = p1evl( x, Q, 8 );
		}
		else {
			p = polevl( x, R, 5 );
			q = p1evl( x, S, 6 );
		}

		double y = (z * p) / q;

		if (a < 0) {
			y = 2.0 - y;
		}

		if (y == 0.0) {
			return a < 0 ? 2.0 : 0.0;
		}

		return y;
	}

	public static double fun_MOD( double _n, double _d )
	{
		if (_d == 0) {
			return 0; // Excel #DIV/0!
		}
		final double remainder = _n % _d;
		if (remainder != 0 && Math.signum( remainder ) != Math.signum( _d )) {
			return remainder + _d;
		}
		else {
			return remainder;
		}
	}

	public static double fun_SQRT( double _n )
	{
		if (_n < 0) {
			return 0; // Excel #NUM!
		}
		return Math.sqrt( _n );
	}

	public static double fun_FACT( double _a )
	{
		if (_a < 0.0) {
			return 0.0; // Excel #NUM!
		}
		else {
			int a = (int) _a;
			if (a < FACTORIALS.length) {
				return FACTORIALS[ a ];
			}
			else {
				double r = 1;
				while (a > 1)
					r *= a--;
				return r;
			}
		}
	}


	/**
	 * Computes IRR using Newton's method, where x[i+1] = x[i] - f( x[i] ) / f'( x[i] )
	 */
	public static double fun_IRR( double[] _values, double _guess )
	{
		final int EXCEL_MAX_ITER = 20;

		double x = _guess;
		int iter = 0;
		while (iter++ < EXCEL_MAX_ITER) {

			final double x1 = 1.0 + x;
			double fx = 0.0;
			double dfx = 0.0;
			for (int i = 0; i < _values.length; i++) {
				final double v = _values[ i ];
				final double x1_i = Math.pow( x1, i );
				fx += v / x1_i;
				final double x1_i1 = x1_i * x1;
				dfx += -i * v / x1_i1;
			}
			final double new_x = x - fx / dfx;
			final double epsilon = Math.abs( new_x - x );

			if (epsilon <= EXCEL_EPSILON) {
				if (_guess == 0.0 && Math.abs( new_x ) <= EXCEL_EPSILON) {
					return 0.0; // OpenOffice calc does this
				}
				else {
					return new_x;
				}
			}
			x = new_x;

		}
		return Double.NaN;
	}

	public static double fun_DB( double _cost, double _salvage, double _life, double _period, double _month )
	{
		final double month = Math.floor( _month );
		final double rate = round( 1 - Math.pow( _salvage / _cost, 1 / _life ), 3 );
		final double depreciation1 = _cost * rate * month / 12;
		double depreciation = depreciation1;
		if ((int) _period > 1) {
			double totalDepreciation = depreciation1;
			final int maxPeriod = (int) (_life > _period? _period : _life);
			for (int i = 2; i <= maxPeriod; i++) {
				depreciation = (_cost - totalDepreciation) * rate;
				totalDepreciation += depreciation;
			}
			if (_period > _life) {
				depreciation = (_cost - totalDepreciation) * rate * (12 - month) / 12;
			}
		}
		return depreciation;
	}

	public static double fun_DDB( double _cost, double _salvage, double _life, double _period, double _factor )
	{
		final double remainingCost;
		final double newCost;
		final double k = 1 - _factor / _life;
		if (k <= 0) {
			remainingCost = _period == 1? _cost : 0;
			newCost = _period == 0? _cost : 0;
		}
		else {
			final double k_p1 = Math.pow( k, _period - 1 );
			final double k_p = k_p1 * k;
			remainingCost = _cost * k_p1;
			newCost = _cost * k_p;
		}

		double depreciation = remainingCost - (newCost < _salvage? _salvage : newCost);
		if (depreciation < 0) {
			depreciation = 0;
		}
		return depreciation;
	}

	public static double fun_RATE( double _nper, double _pmt, double _pv, double _fv, double _type, double _guess )
	{
		final int MAX_ITER = 50;
		final boolean type = _type != 0;
		double eps = 1;
		double rate0 = _guess;
		for (int count = 0; eps > EXCEL_EPSILON && count < MAX_ITER; count++) {
			final double rate1;
			if (rate0 == 0) {
				final double a = _pmt * _nper;
				final double b = a + (type? _pmt : -_pmt);
				rate1 = rate0 - (_pv + _fv + a) / (_nper * (_pv + b / 2));
			}
			else {
				final double a = 1 + rate0;
				final double b = Math.pow( a, _nper - 1 );
				final double c = b * a;
				final double d = _pmt * (1 + (type? rate0 : 0));
				final double e = rate0 * _nper * b;
				final double f = c - 1;
				final double g = rate0 * _pv;
				rate1 = rate0 * (1 - (g * c + d * f + rate0 * _fv) / (g * e - _pmt * f + d * e));
			}
			eps = Math.abs( rate1 - rate0 );
			rate0 = rate1;
		}
		if (eps >= EXCEL_EPSILON) {
			return 0; // Excel #NUM!
		}
		return rate0;
	}


	public static double fun_VALUE( String _text, final Environment _environment )
	{
		final String text = _text.trim();
		final Number number = parseNumber( text, false, _environment );
		if (number != null) {
			return number.doubleValue();
		}
		return 0.0; // Excel #NUM!
	}


	public static int fun_MATCH_Exact( double _x, double[] _xs )
	{
		for (int i = 0; i < _xs.length; i++) {
			if (_x == _xs[ i ]) return i + 1; // Excel is 1-based
		}
		return 0;
	}

	public static int fun_MATCH_Ascending( double _x, double[] _xs )
	{
		final int iLast = _xs.length - 1;
		int iLeft = 0;
		int iRight = iLast;
		while (iLeft < iRight) {
			final int iMid = iLeft + ((iRight - iLeft) >> 1);
			if (_x > _xs[ iMid ]) iLeft = iMid + 1;
			else iRight = iMid;
		}
		if (iLeft > iLast || _x < _xs[ iLeft ]) iLeft--;
		return iLeft + 1; // Excel is 1-based
	}

	public static int fun_MATCH_Descending( double _x, double[] _xs )
	{
		final int iLast = _xs.length - 1;
		int iLeft = 0;
		int iRight = iLast;
		while (iLeft < iRight) {
			final int iMid = iLeft + ((iRight - iLeft) >> 1);
			if (_x < _xs[ iMid ]) iLeft = iMid + 1;
			else iRight = iMid;
		}
		if (iLeft > iLast || _x > _xs[ iLeft ]) iLeft--;
		return iLeft + 1; // Excel is 1-based
	}


}
