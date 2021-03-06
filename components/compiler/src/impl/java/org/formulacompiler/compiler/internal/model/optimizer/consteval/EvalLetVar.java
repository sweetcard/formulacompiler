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

package org.formulacompiler.compiler.internal.model.optimizer.consteval;

import org.formulacompiler.compiler.internal.expressions.DataType;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForLetVar;
import org.formulacompiler.compiler.internal.expressions.TypedResult;
import org.formulacompiler.compiler.internal.model.interpreter.InterpretedNumericType;

final class EvalLetVar extends EvalShadow<ExpressionNodeForLetVar>
{
	static final TypedResult UNDEF = new ConstResult( null, DataType.NULL );

	private final String varName;

	public EvalLetVar( ExpressionNodeForLetVar _node, InterpretedNumericType _type )
	{
		super( _node, _type );
		this.varName = _node.varName();
	}


	@Override
	protected TypedResult eval( EvalShadowContext _context )
	{
		final TypedResult val = _context.letDict.lookup( this.varName );
		if (val == UNDEF) {
			if (LOG.e()) LOG.a( "Lookup " ).a( this.varName ).a( " is undefined. " ).lf();
			return node(); // No need to clone leaf node.
		}
		else {
			if (LOG.e()) LOG.a( "Lookup " ).a( this.varName ).a( " = " ).a( val ).lf();
			return val;
		}
	}

	@Override
	protected TypedResult evaluateToConst( TypedResult... _args )
	{
		throw new IllegalStateException( "EvalLetVar.evaluateToConst() should never be called" );
	}

}
