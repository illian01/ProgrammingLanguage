package lexer;

public enum TokenType {
	INT,
	ID, 
	TRUE, FALSE, NOT,
	PLUS, MINUS, TIMES, DIV,   //special chractor
	LT, GT, EQ, APOSTROPHE,    //special chractor
	L_PAREN, R_PAREN,QUESTION, //special chractor
	DEFINE, LAMBDA, COND, QUOTE,
	CAR, CDR, CONS,
	ATOM_Q, NULL_Q, EQ_Q; 
	
	static TokenType fromSpecialCharactor(char ch) {
		switch ( ch ) {
			case '+':
				return PLUS;
			// 나머지 Special Charactor에 대해 토큰을 반환하도록 작성
			// Special Character가 무엇인지는 위에 써져있다. 알맞게 매칭만 해주면 된다.
			case '-':
				return MINUS;
			case '*':
				return TIMES;
			case '/':
				return DIV;
			case '<':
				return LT;
			case '>' :
				return GT;
			case '=' :
				return EQ;
			case '\'' :
				return APOSTROPHE;
			case '(' :
				return L_PAREN;
			case ')' :
				return R_PAREN;
			case '?' :
				return QUESTION;
			default:
				throw new IllegalArgumentException("unregistered char: " + ch);
		}
	}
}
