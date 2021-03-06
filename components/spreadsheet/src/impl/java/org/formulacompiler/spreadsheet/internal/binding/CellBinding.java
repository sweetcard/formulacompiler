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

package org.formulacompiler.spreadsheet.internal.binding;

import org.formulacompiler.compiler.CallFrame;
import org.formulacompiler.compiler.CompilerException;
import org.formulacompiler.spreadsheet.internal.CellIndex;

public abstract class CellBinding extends ElementBinding
{
	private final SectionBinding section;
	private final CellIndex index;


	public CellBinding( SectionBinding _space, CellIndex _index ) throws CompilerException
	{
		this.section = _space;
		this.index = _index;

		_space.checkChildInSection( this, _index );
	}


	public CellIndex getIndex()
	{
		return this.index;
	}


	public SectionBinding getSection()
	{
		return this.section;
	}


	public abstract CallFrame boundCall();

}