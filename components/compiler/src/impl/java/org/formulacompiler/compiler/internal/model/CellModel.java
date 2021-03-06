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

package org.formulacompiler.compiler.internal.model;

import org.formulacompiler.compiler.NumericType;
import org.formulacompiler.compiler.internal.YamlBuilder;
import org.formulacompiler.compiler.internal.expressions.DataType;
import org.formulacompiler.compiler.internal.expressions.ExpressionNode;
import org.formulacompiler.compiler.internal.expressions.TypedResult;


public class CellModel extends ElementModel implements TypedResult
{
	private Object constantValue;
	private ExpressionNode expression;
	private int maxFractionalDigits = NumericType.UNLIMITED_FRACTIONAL_DIGITS;
	private int referenceCount = 0;
	private DataType dataType;
	private TypedResult cachedResult = null;
	

	public CellModel( SectionModel _section, Object _source )
	{
		super( _section, _source, null );
		_section.getCells().add( this );
	}


	public Object getConstantValue()
	{
		return this.constantValue;
	}


	public void setConstantValue( Object _constantValue )
	{
		this.constantValue = _constantValue;
	}


	public boolean isConstant()
	{
		return hasConstantValue();
	}

	public boolean hasConstantValue()
	{
		return (this.expression == null) && DataType.isValueConstant( getConstantValue() );
	}


	public ExpressionNode getExpression()
	{
		return this.expression;
	}


	public void setExpression( ExpressionNode _expression )
	{
		this.expression = _expression;
	}


	public int getMaxFractionalDigits()
	{
		return this.maxFractionalDigits;
	}


	public void setMaxFractionalDigits( int _maxFractionalDigits )
	{
		this.maxFractionalDigits = _maxFractionalDigits;
	}


	public int getReferenceCount()
	{
		return this.referenceCount;
	}


	public void addReference()
	{
		this.referenceCount++;
	}


	public boolean isCachingCandidate()
	{
		return (getReferenceCount() > 1) || isInput() || isOutput();
	}


	public DataType getDataType()
	{
		return this.dataType;
	}

	public void setDataType( DataType _dataType )
	{
		this.dataType = _dataType;
	}

	public TypedResult getCachedResult()
	{
		return this.cachedResult;
	}
	
	public void setCachedResult( TypedResult _cachedResult )
	{
		this.cachedResult = _cachedResult;
	}
	
	@Override
	public void yamlTo( YamlBuilder _to )
	{
		_to.nv( "source", getSource().toString() );
		if (getName() != null) _to.nv( "name", getName() );
		if (isInput()) _to.ln( "input" ).l( "calls", getCallChainToCall() );
		if (isOutput()) _to.ln( "output" ).l( "implements", (Object[]) getCallsToImplement() );
		_to.nv( "value", this.constantValue );
		_to.nv( "expr", this.expression );
		if (NumericType.UNLIMITED_FRACTIONAL_DIGITS > this.maxFractionalDigits)
			_to.nv( "maxFractionalDigits", this.maxFractionalDigits );
		_to.nv( "refCount", this.referenceCount );
		_to.nv( "type", this.dataType );
	}

}
