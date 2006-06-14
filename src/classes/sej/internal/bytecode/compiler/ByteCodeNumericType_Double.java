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
package sej.internal.bytecode.compiler;

import java.util.Date;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import sej.CompilerError;
import sej.NumericType;
import sej.expressions.Operator;
import sej.internal.runtime.RuntimeDouble_v1;

final class ByteCodeNumericType_Double extends ByteCodeNumericType
{

	ByteCodeNumericType_Double(NumericType _type, ByteCodeSectionCompiler _compiler)
	{
		super( _type, _compiler );
	}


	@Override
	Type getType()
	{
		return Type.DOUBLE_TYPE;
	}


	@Override
	int getReturnOpcode()
	{
		return Opcodes.DRETURN;
	}


	private static final Type RUNTIME_TYPE = Type.getType( RuntimeDouble_v1.class );

	@Override
	Type getRuntimeType()
	{
		return RUNTIME_TYPE;
	}


	@Override
	void compile( GeneratorAdapter _mv, Operator _operator, int _numberOfArguments ) throws CompilerError
	{
		switch (_operator) {

			case PLUS:
				_mv.visitInsn( Opcodes.DADD );
				break;

			case MINUS:
				if (1 == _numberOfArguments) {
					_mv.visitInsn( Opcodes.DNEG );
				}
				else {
					_mv.visitInsn( Opcodes.DSUB );
				}
				break;

			case TIMES:
				_mv.visitInsn( Opcodes.DMUL );
				break;

			case DIV:
				_mv.visitInsn( Opcodes.DDIV );
				break;

			case PERCENT:
				_mv.visitLdcInsn( 100.0 );
				_mv.visitInsn( Opcodes.DDIV );
				break;

			case EXP:
				_mv.visitMethodInsn( Opcodes.INVOKESTATIC, ByteCodeEngineCompiler.MATH.getInternalName(), "pow", "(DD)D" );
				break;

			case MIN:
				compileRuntimeMethod( _mv, "min", "(DD)D" );
				break;

			case MAX:
				compileRuntimeMethod( _mv, "max", "(DD)D" );
				break;

			default:
				super.compile( _mv, _operator, _numberOfArguments );
		}
	}


	@Override
	void compileConst( GeneratorAdapter _mv, Object _constantValue ) throws CompilerError
	{
		if (null == _constantValue) {
			_mv.visitInsn( Opcodes.DCONST_0 );
		}
		else if (_constantValue instanceof Number) {
			double val = ((Number) _constantValue).doubleValue();
			_mv.push( val );
		}
		else if (_constantValue instanceof Boolean) {
			double val = ((Boolean) _constantValue) ? 1 : 0;
			_mv.push( val );
		}
		else if (_constantValue instanceof Date) {
			Date date = (Date) _constantValue;
			double val = RuntimeDouble_v1.dateToExcel( date );
			_mv.push( val );
		}
		else {
			super.compileConst( _mv, _constantValue );
		}
	}


	@Override
	void compileZero( GeneratorAdapter _mv )
	{
		_mv.push( 0.0D );
	}


	@Override
	void compileComparison( GeneratorAdapter _mv, int _comparisonOpcode )
	{
		_mv.visitInsn( _comparisonOpcode );
	}
	
	
	@Override
	protected String getRoundMethodSignature()
	{
		return "(DI)D";
	}

}