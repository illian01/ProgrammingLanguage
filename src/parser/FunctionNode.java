package parser;

import java.util.HashMap;
import java.util.Map;

import lexer.TokenType;

public class FunctionNode implements ValueNode{
	// BinaryOpNode와 같은 구조를 사용
	// 각 Token값만 수정
	public enum FunctionType {
		ATOM_Q { TokenType tokenType() { return TokenType.ATOM_Q; } },
		CAR { TokenType tokenType() { return TokenType.CAR; } },
		CDR { TokenType tokenType() { return TokenType.CDR; } },
		COND { TokenType tokenType() { return TokenType.COND; } },
		CONS { TokenType tokenType() { return TokenType.CONS; } },
		DEFINE { TokenType tokenType() { return TokenType.DEFINE; } },
		EQ_Q { TokenType tokenType() { return TokenType.EQ_Q; } },
		LAMBDA { TokenType tokenType() {return TokenType.LAMBDA;} },
		NOT { TokenType tokenType() {return TokenType.NOT;} },
		NULL_Q { TokenType tokenType() {return TokenType.NULL_Q;} };
		
		private static Map<TokenType, FunctionType> fromTokenType = new HashMap<TokenType, FunctionType>();
		static {
			for (FunctionType bType : FunctionType.values()) {
				fromTokenType.put(bType.tokenType(), bType);
			}
		}

		static FunctionType getFunctionType(TokenType tType) {
			return fromTokenType.get(tType);
		}

		abstract TokenType tokenType();
	}
	
	public FunctionType funcType;
	
	public String toString() {
		return funcType.name();
	}
	
	public void setValue(TokenType tType) {
		FunctionType bType = FunctionType.getFunctionType(tType);
		funcType = bType;
	}
}
