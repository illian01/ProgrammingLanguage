package parser;

import java.util.Iterator;
import lexer.Scanner;
import lexer.Token;
import lexer.TokenType;

public class CuteParser {
	private Iterator<Token> tokens;
	private static Node END_OF_LIST = new Node() {};

	public CuteParser(String file) {
			tokens = Scanner.scan(file);
	}

	private Token getNextToken() {
		if (!tokens.hasNext())
			return null;
		return tokens.next();
	}

	public Node parseExpr() {
		Token t = getNextToken();
		if (t == null) {
			System.out.println("No more token");
			return null;
		}
		TokenType tType = t.type();
		String tLexeme = t.lexme();
		switch (tType) {
		case ID:
			return new IdNode(tLexeme);
		case INT:
			if(tLexeme == null)
				System.out.println("???");
			return new IntNode(tLexeme);
		// BinaryOpNode에 대하여 작성
		// +, -, /, *가 해당
		case DIV:
		case EQ:
		case MINUS:
		case GT:
		case PLUS:
		case TIMES:
		case LT:
			BinaryOpNode opNode = new BinaryOpNode();		// 위 7개는 OpNode에 해당
			opNode.setValue(tType);							// BinaryOpNode 선언해 초기화 후 리턴
			return opNode;
			// 내용 채우기

			// FunctionNode에 대하여 작성
			// 키워드가 FunctionNode에 해당
		case ATOM_Q:
		case CAR:
		case CDR:
		case COND:
		case CONS:
		case DEFINE:
		case EQ_Q:
		case LAMBDA:
		case NOT:
		case NULL_Q:
			FunctionNode funcNode = new FunctionNode();		// 위 10개는 FunctionNode에 해당
			funcNode.setValue(tType);						// FunctionNode 선언해 초기화 후 리턴
			return funcNode;
			// 내용 채우기

			// BooleanNode에 대하여 작성
		case FALSE:
			return BooleanNode.FALSE_NODE;
		case TRUE:
			return BooleanNode.TRUE_NODE;

		// case L_PAREN일 경우와 case R_PAREN일 경우에 대해서 작성
		// L_PAREN일 경우 parseExprList()를 호출하여 처리
		case L_PAREN:
			return parseExprList();
			// 내용 채우기
		case R_PAREN:								// )는 List가 끝났다는 뜻
			return END_OF_LIST;
			
		case APOSTROPHE:
			QuoteNode quoteNode = new QuoteNode(parseExpr());
			ListNode listNode = ListNode.cons(quoteNode, ListNode.EMPTYLIST);
			return listNode;
		case QUOTE:
			return new QuoteNode(parseExpr());
			
		default:
			// head의 next를 만들고 head를 반환하도록 작성
			System.out.println("Parsing Error!");
			return null;
		}
	}

	private ListNode parseExprList() {
		Node head = parseExpr();
		if (head == null) return null;
		if (head == END_OF_LIST) return ListNode.EMPTYLIST;
		ListNode tail = parseExprList();
		if (tail == null) return null;
		return ListNode.cons(head, tail);
	}
}
