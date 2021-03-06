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

package org.formulacompiler.compiler.internal.bytecode;

import java.util.Collection;
import java.util.List;

import org.formulacompiler.compiler.CompilerException;
import org.formulacompiler.compiler.internal.expressions.ArrayDescriptor;
import org.formulacompiler.compiler.internal.expressions.DataType;
import org.formulacompiler.compiler.internal.expressions.ExpressionNode;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForArrayReference;
import org.formulacompiler.compiler.internal.expressions.ExpressionNodeForFoldDatabase;
import org.formulacompiler.compiler.internal.expressions.LetDictionary.LetEntry;
import org.formulacompiler.compiler.internal.model.ExpressionNodeForSubSectionModel;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

final class HelperCompilerForFoldDatabase extends HelperCompilerForFoldApply
{
	private final ExpressionNodeForFoldDatabase db;
	private final ExpressionNodeForArrayReference table;
	private final ArrayDescriptor tableDescriptor;
	private final String[] colNames;
	private final DataType[] colTypes;
	private final ExpressionNode filterExpr;

	public HelperCompilerForFoldDatabase( SectionCompiler _section, ExpressionNodeForFoldDatabase _applyNode,
			Iterable<LetEntry<Compilable>> _closure )
	{
		super( _section, _applyNode, _closure );
		this.db = _applyNode;
		this.table = _applyNode.table();
		this.tableDescriptor = _applyNode.table().arrayDescriptor();
		this.colNames = _applyNode.filterColumnNames();
		this.colTypes = _applyNode.filterColumnTypes();
		this.filterExpr = _applyNode.filter();
	}

	@Override
	protected boolean argumentsAreVectors()
	{
		return false;
	}


	@Override
	protected void compileTraversal() throws CompilerException
	{
		final int colIdx = this.db.staticFoldedColumnIndex();
		if (colIdx < 0) {
			final ExpressionNode colIdxExpr = this.db.foldedColumnIndex();
			compileColumnSwitch( colIdxExpr );
		}
		else {
			compileFixedColumnTraversal( colIdx );
		}
	}


	private void compileColumnSwitch( ExpressionNode _colIdxExpr ) throws CompilerException
	{
		final ExpressionCompilerForNumbers num = numericCompiler();
		final ExpressionNodeForFoldDatabase node = this.db;

		num.compileInt( _colIdxExpr );
		compileTableSwitch( node.foldableColumnKeys(), new TableSwitchGenerator()
		{

			@Override
			protected void generateCase( int _key, Label _end ) throws CompilerException
			{
				final int iCol = _key - 1;
				compileFixedColumnTraversal( iCol );
				mv().goTo( _end );
			}

			@Override
			protected void generateDefault() throws CompilerException
			{
				final ExpressionCompiler retCompiler = expressionCompiler( node.getDataType() );
				if (null != node.fold().whenEmpty()) {
					retCompiler.compile( node.fold().whenEmpty() );
				}
				else {
					retCompiler.compileZero();
				}
				mv().returnValue();
			}

		} );

	}


	private MethodCompiler matcher;

	private final void compileFixedColumnTraversal( int _foldedCol ) throws CompilerException
	{
		final List<ExpressionNode> elts = this.table.arguments();
		final int nElt = elts.size();
		int iElt = 0;
		while (iElt < nElt) {
			iElt = compileRowTraversal( _foldedCol, elts, iElt );
		}
	}


	private int compileRowTraversal( int _foldedCol, List<ExpressionNode> _elts, int _iElt ) throws CompilerException
	{
		final ExpressionNode elt = _elts.get( _iElt );
		if (elt instanceof ExpressionNodeForArrayReference) {
			compileRowTraversal( _foldedCol, elt.arguments(), 0 );
			return _iElt + 1;
		}
		else if (elt instanceof ExpressionNodeForSubSectionModel) {
			compileSubSectionTraversal( _foldedCol, (ExpressionNodeForSubSectionModel) elt );
			return _iElt + 1;
		}
		else {
			final GeneratorAdapter mv = mv();
			final int iFoldedElt = _iElt + _foldedCol;
			final ExpressionNode foldedElt = _elts.get( iFoldedElt );
			final Label noMatch = mv.newLabel();
			compileCallToMatcherAndBuildItInFirstPass( _elts, _iElt );
			compileSkipFoldIfNoMatch( noMatch );
			compileElementFold( foldedElt );
			mv.mark( noMatch );
			return _iElt + this.tableDescriptor.numberOfColumns();
		}
	}


	private void compileSubSectionTraversal( final int _foldedCol, final ExpressionNodeForSubSectionModel _sub )
			throws CompilerException
	{
		expressionCompiler().compileSubSectionTraversal( _sub, new ExpressionCompiler.SubSectionTraversal()
		{

			public void compile( Collection<ExpressionNode> _elements, int _indexLocalOffset ) throws CompilerException
			{
				compileRowTraversal( _foldedCol, _sub.arguments(), 0 );
			}

		} );
	}


	private void compileElementFold( ExpressionNode _foldedElt ) throws CompilerException
	{
		final String eltName = this.db.fold().eltName( 0 );
		letDict().let( eltName, _foldedElt.getDataType(), new CompilableExpressionNode( _foldedElt ) );
		try {
			compileFoldStepsWithEltsBound();
		}
		finally {
			letDict().unlet( eltName );
		}
	}


	private void compileCallToMatcherAndBuildItInFirstPass( List<ExpressionNode> _elts, int _iElt )
			throws CompilerException
	{
		final int nCols = this.tableDescriptor.numberOfColumns();
		final GeneratorAdapter mv = mv();
		try {
			for (int iCol = 0; iCol < nCols; iCol++) {
				final ExpressionNode elt = _elts.get( _iElt + iCol );
				letDict().let( this.colNames[ iCol ], this.colTypes[ iCol ], new CompilableExpressionNode( elt ) );
			}
			final Iterable<LetEntry<Compilable>> closure = closureOf( this.filterExpr );
			if (this.matcher == null) {
				this.matcher = new HelperCompilerForDatabaseMatch( section(), this.filterExpr, closure );
				this.matcher.compile();
			}
			mv().loadThis();
			compileClosure( closure );
			this.matcher.compileCall( mv );
		}
		finally {
			letDict().unlet( nCols );
		}
	}


	private void compileSkipFoldIfNoMatch( final Label _noMatch )
	{
		mv().ifZCmp( Opcodes.IFEQ, _noMatch );
	}

}
