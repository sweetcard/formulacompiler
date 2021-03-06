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

package org.formulacompiler.compiler.internal.model.analysis;

import java.util.Collection;
import java.util.List;

import org.formulacompiler.compiler.CompilerException;
import org.formulacompiler.compiler.internal.expressions.DataType;
import org.formulacompiler.compiler.internal.expressions.ExpressionNode;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForArrayReference;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForConstantValue;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForFoldDatabase;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForFoldDefinition;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForFoldList;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForFoldVectors;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForFunction;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForLet;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForLetVar;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForLogging;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForOperator;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForSubstitution;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForSwitch;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForSwitchCase;
import org.formulacompiler.compiler.internal.expressions.LetDictionary;
import org.formulacompiler.compiler.internal.model.AbstractComputationModelVisitor;
import org.formulacompiler.compiler.internal.model.CellModel;
import org.formulacompiler.compiler.internal.model.ExpressionNodeForCellModel;
import org.formulacompiler.compiler.internal.model.ExpressionNodeForCount;
import org.formulacompiler.compiler.internal.model.ExpressionNodeForParentSectionModel;
import org.formulacompiler.compiler.internal.model.ExpressionNodeForSubSectionModel;
import org.formulacompiler.runtime.New;


public final class TypeAnnotator extends AbstractComputationModelVisitor
{

	public static final DataType annotateExpr( ExpressionNode _expr ) throws CompilerException
	{
		return new TypeAnnotator().annotate( _expr );
	}


	private final LetDictionary<Void> letDict = new LetDictionary<Void>();

	private LetDictionary<Void> letDict()
	{
		return this.letDict;
	}


	@Override
	protected boolean visitCell( CellModel _cell ) throws CompilerException
	{
		annotate( _cell );
		return true;
	}


	private DataType annotate( CellModel _cell ) throws CompilerException
	{
		if (_cell == null) return DataType.NULL;
		final DataType type = _cell.getDataType();
		if (null != type) {
			return type;
		}
		else {
			final Object cst = _cell.getConstantValue();
			if (null != cst) {
				_cell.setDataType( typeOfConstant( cst ) );
			}
			else {
				final ExpressionNode expr = _cell.getExpression();
				if (null != expr) {
					_cell.setDataType( annotate( expr ) );
				}
				else {
					_cell.setDataType( DataType.NULL );
				}
			}
			return _cell.getDataType();
		}
	}


	private DataType annotate( ExpressionNode _expr ) throws CompilerException
	{
		if (null == _expr) {
			return DataType.NULL;
		}
		else {
			final DataType type = _expr.getDataType();
			if (null != type) {
				return type;
			}
			else {
				final DataType inferredDataType = dispatch( _expr );
				final DataType declaredDataType = _expr.getDeclaredDataType();
				if (declaredDataType != null) {
					if (inferredDataType != null && inferredDataType != declaredDataType) {
						throw new CompilerException.DataTypeError( "The declared type " + declaredDataType
								+ " of expression " + _expr + " does not match its inferred type " + inferredDataType + "." );
					}
					_expr.setDataType( declaredDataType );
				}
				else {
					_expr.setDataType( inferredDataType );
				}
				return _expr.getDataType();
			}
		}
	}

	private DataType dispatch( ExpressionNode _expr ) throws CompilerException
	{
		if (_expr instanceof ExpressionNodeForConstantValue) return typeOf( (ExpressionNodeForConstantValue) _expr );
		if (_expr instanceof ExpressionNodeForCellModel) return typeOf( (ExpressionNodeForCellModel) _expr );
		if (_expr instanceof ExpressionNodeForArrayReference) return typeOf( (ExpressionNodeForArrayReference) _expr );
		if (_expr instanceof ExpressionNodeForOperator) return typeOf( (ExpressionNodeForOperator) _expr );
		if (_expr instanceof ExpressionNodeForFunction) return typeOf( (ExpressionNodeForFunction) _expr );
		if (_expr instanceof ExpressionNodeForSwitch) return typeOf( (ExpressionNodeForSwitch) _expr );
		if (_expr instanceof ExpressionNodeForSwitchCase) return typeOf( (ExpressionNodeForSwitchCase) _expr );
		if (_expr instanceof ExpressionNodeForParentSectionModel)
			return typeOf( (ExpressionNodeForParentSectionModel) _expr );
		if (_expr instanceof ExpressionNodeForSubSectionModel) return typeOf( (ExpressionNodeForSubSectionModel) _expr );
		if (_expr instanceof ExpressionNodeForCount) return DataType.NUMERIC;

		if (_expr instanceof ExpressionNodeForLet) return typeOf( (ExpressionNodeForLet) _expr );
		if (_expr instanceof ExpressionNodeForLetVar) return typeOf( (ExpressionNodeForLetVar) _expr );

		if (_expr instanceof ExpressionNodeForFoldList) return typeOf( (ExpressionNodeForFoldList) _expr );
		if (_expr instanceof ExpressionNodeForFoldVectors) return typeOf( (ExpressionNodeForFoldVectors) _expr );
		if (_expr instanceof ExpressionNodeForFoldDatabase) return typeOf( (ExpressionNodeForFoldDatabase) _expr );

		if (_expr instanceof ExpressionNodeForSubstitution) return typeOf( (ExpressionNodeForSubstitution) _expr );

		if (_expr instanceof ExpressionNodeForLogging) return typeOf( (ExpressionNodeForLogging) _expr );

		unsupported( _expr );
		return null;
	}


	private void annotateArgs( ExpressionNode _expr ) throws CompilerException
	{
		for (ExpressionNode arg : _expr.arguments()) {
			annotate( arg );
		}
	}


	private DataType typeOfConstant( Object _value )
	{
		if (null == _value) {
			return DataType.NULL;
		}
		else if (_value instanceof String) {
			return DataType.STRING;
		}
		else {
			return DataType.NUMERIC;
		}
	}

	private DataType typeOf( Iterable<ExpressionNode> _exprs )
	{
		DataType type = DataType.NULL;
		for (ExpressionNode arg : _exprs) {
			if (null != arg) {
				DataType argType = arg.getDataType();
				switch (argType) {
					case NUMERIC:
						return argType;
					case STRING:
						type = argType;
						break;
				}
			}
		}
		return type;
	}

	private DataType typeOf( ExpressionNodeForConstantValue _expr )
	{
		return typeOfConstant( _expr.value() );
	}

	private DataType typeOf( ExpressionNodeForCellModel _expr ) throws CompilerException
	{
		return annotate( _expr.getCellModel() );
	}

	private DataType typeOf( ExpressionNodeForArrayReference _expr ) throws CompilerException
	{
		annotateArgs( _expr );
		return typeOf( _expr.arguments() );
	}

	private DataType typeOf( ExpressionNodeForOperator _expr ) throws CompilerException
	{
		annotateArgs( _expr );
		switch (_expr.getOperator()) {
			case CONCAT:
				return DataType.STRING;
			default:
				return DataType.NUMERIC;
		}
	}

	// Leave this comment in. It is used to cite the code into the documentation.
	// ---- typeOfFun
	private DataType typeOf( ExpressionNodeForFunction _expr ) throws CompilerException
	{
		annotateArgs( _expr );
		switch (_expr.getFunction()) {
			// ...
			// ---- typeOfFun

			case IF:
				final ExpressionNode thenArg = _expr.argumentOrNull( 1 );
				if (null != thenArg) {
					final ExpressionNode elseArg = _expr.argumentOrNull( 2 );
					if (null != elseArg) {
						return commonTypeOf( _expr, thenArg, elseArg );
					}
					else {
						return commonTypeOf( _expr, thenArg );
					}
				}
				else {
					unsupported( _expr );
					return DataType.NULL;
				}

			//注意: 增加VLOOKUP
			//TODO: VLOOKUP需要使用返回列的数据类型
//			case VLOOKUP:
//				if (_expr.arguments().size() >= 1) {
//					return _expr.arguments().get( 0 ).getDataType();
//				}else {
//					unsupported( _expr );
//					return DataType.NULL;
//				}
			case INDEX:
				if (_expr.arguments().size() >= 1) {
					return _expr.arguments().get( 0 ).getDataType();
				}
				else {
					unsupported( _expr );
					return DataType.NULL;
				}

				// ---- typeOfFun
			case CONCATENATE:
			case MID:
				// ...
				// ---- typeOfFun
			case ADDRESS:
			case CLEAN:
			case CHAR:
			case DOLLAR:
			case FIXED:
			case ROMAN:
			case LEFT:
			case RIGHT:
			case SUBSTITUTE:
			case REPLACE:
			case LOWER:
			case UPPER:
			case PROPER:
			case REPT:
			case TRIM:
			case TEXT:
				// ---- typeOfFun
				return DataType.STRING;

			case ERROR:
				return null; // must be provided by surrounding context, like a CASE statement

			default:
				return DataType.NUMERIC;
		}
	}
	// ---- typeOfFun

	private DataType typeOf( ExpressionNodeForSwitch _expr ) throws CompilerException
	{
		annotateArgs( _expr );
		final Collection<ExpressionNodeForSwitchCase> cases = _expr.cases();
		final ExpressionNode[] expressions = cases.toArray( new ExpressionNode[cases.size() + 1] );
		expressions[ expressions.length - 1 ] = _expr.defaultValue();
		return commonTypeOf( _expr, expressions );
	}

	private DataType typeOf( ExpressionNodeForSwitchCase _expr ) throws CompilerException
	{
		annotateArgs( _expr );
		return _expr.value().getDataType();
	}

	private DataType typeOf( ExpressionNodeForParentSectionModel _expr ) throws CompilerException
	{
		if (1 != _expr.arguments().size()) unsupported( _expr );
		return annotate( _expr.arguments().get( 0 ) );
	}

	private DataType typeOf( ExpressionNodeForSubSectionModel _expr ) throws CompilerException
	{
		annotateArgs( _expr );
		return typeOf( _expr.arguments() );
	}


	private DataType typeOf( ExpressionNodeForLet _expr ) throws CompilerException
	{
		final DataType valType = annotate( _expr.value() );
		letDict().let( _expr.varName(), valType, null );
		try {
			return annotate( _expr.in() );
		}
		finally {
			letDict().unlet( _expr.varName() );
		}
	}

	private DataType typeOf( ExpressionNodeForLetVar _expr )
	{
		return letDict().lookupType( _expr.varName() );
	}

	private DataType typeOf( ExpressionNodeForFoldDefinition _expr, DataType... _eltTypes ) throws CompilerException
	{
		for (int i = 0; i < _expr.accuCount(); i++)
			annotate( _expr.accuInit( i ) );

		for (int i = 0; i < _expr.accuCount(); i++)
			letDict().let( _expr.accuName( i ), _expr.accuInit( i ).getDataType(), null );
		if (_expr.isIndexed()) letDict().let( _expr.indexName(), DataType.NUMERIC, null );
		for (int i = 0; i < _expr.eltCount(); i++)
			letDict().let( _expr.eltName( i ), _eltTypes[ i ], null );

		for (int i = 0; i < _expr.accuCount(); i++)
			annotate( _expr.accuStep( i ) );

		letDict().unlet( _expr.eltCount() );
		if (_expr.isIndexed()) letDict().unlet( _expr.indexName() );
		if (_expr.isCounted()) letDict().let( _expr.countName(), DataType.NUMERIC, null );

		annotate( _expr.merge() );

		if (_expr.isCounted()) letDict().unlet( _expr.countName() );
		letDict().unlet( _expr.accuCount() );

		annotate( _expr.whenEmpty() );

		return (_expr.isMergedExplicitly() ? _expr.merge() : _expr.accuStep( 0 )).getDataType();
	}

	private DataType typeOf( ExpressionNodeForFoldList _expr ) throws CompilerException
	{
		for (ExpressionNode elt : _expr.elements()) {
			annotate( elt );
		}
		_expr.fold().setDataType( typeOf( _expr.fold(), typeOf( _expr.elements() ) ) );
		return _expr.fold().getDataType();
	}

	private DataType typeOf( ExpressionNodeForFoldVectors _expr ) throws CompilerException
	{
		final List<DataType> eltTypes = New.list();
		for (ExpressionNode elt : _expr.elements()) {
			annotate( elt );
			eltTypes.add( elt.getDataType() );
		}
		_expr.fold().setDataType( typeOf( _expr.fold(), eltTypes.toArray( new DataType[ eltTypes.size() ] ) ) );
		return _expr.fold().getDataType();
	}

	private DataType typeOf( ExpressionNodeForFoldDatabase _expr ) throws CompilerException
	{
		final String[] colNames = _expr.filterColumnNames();
		final DataType[] colTypes = _expr.filterColumnTypes();
		for (int iCol = 0; iCol < colNames.length; iCol++) {
			letDict().let( colNames[ iCol ], colTypes[ iCol ], null );
		}
		try {
			annotate( _expr.filter() );
		}
		finally {
			letDict().unlet( colNames.length );
		}
		annotate( _expr.foldedColumnIndex() );
		annotate( _expr.table() );
		_expr.fold().setDataType( typeOf( _expr.fold(), _expr.table().getDataType() ) );
		return _expr.fold().getDataType();
	}

	private DataType typeOf( ExpressionNodeForSubstitution _expr ) throws CompilerException
	{
		annotateArgs( _expr );
		return typeOf( _expr.arguments() );
	}

	private DataType typeOf( final ExpressionNodeForLogging _expr ) throws CompilerException
	{
		final ExpressionNode exp = _expr.argument( 0 );
		return annotate( exp );
	}

	private DataType commonTypeOf( ExpressionNode _parent, ExpressionNode... _args ) throws CompilerException
	{
		DataType refType = null;
		ExpressionNode refExpr = null;
		for (ExpressionNode arg : _args) {
			final DataType type = arg.getDataType();
			if (type != null) {
				refType = type;
				refExpr = arg;
				break;
			}
		}
		if (refType == null) {
			throw new CompilerException.DataTypeError( "Cannot determine type of expression " + _parent
					+ " because its argument(s) are untyped." );
		}
		for (ExpressionNode arg : _args) {
			final DataType type = arg.getDataType();
			if (type != null) {
				//注意: 忽略类型转换
				if (refType != type) {
					throw new CompilerException.DataTypeError( "Arguments of expression " + _parent
							+ " must have the same type."
							+ "\nExpression " + arg + " has type " + type + "."
							+ "\nExpression " + refExpr + " has type " + refType + "." );
				}
			}
			else {
				arg.setDataType( refType );
			}
		}
		return refType;
	}

	private void unsupported( ExpressionNode _expr ) throws CompilerException
	{
		throw new CompilerException.UnsupportedExpression( _expr.toString() );
	}


}
