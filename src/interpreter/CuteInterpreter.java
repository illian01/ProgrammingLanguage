package interpreter;

import java.util.HashMap;
import java.util.Scanner;

import parser.*;

public class CuteInterpreter {

	public static HashMap<String, Node> VariableMap = new HashMap<String, Node>();
	
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		while (true) {
			System.out.print("> ");
			String input = sc.nextLine();
			CuteParser cuteParser = new CuteParser(input);
			CuteInterpreter interpreter = new CuteInterpreter();
			Node parseTree = cuteParser.parseExpr();
			Node resultNode = interpreter.runExpr(parseTree);
			NodePrinter nodePrinter = new NodePrinter(resultNode);
			nodePrinter.prettyPrint();
		}
	}

	private void errorLog(String err) {
		System.out.println(err);
	}

	public Node runExpr(Node rootExpr) {
		if (rootExpr == null)
			return null;
		if (rootExpr instanceof IdNode)
			return rootExpr;
		else if (rootExpr instanceof IntNode)
			return rootExpr;
		else if (rootExpr instanceof BooleanNode)
			return rootExpr;
		else if (rootExpr instanceof ListNode)
			return runList((ListNode) rootExpr);
		else
			errorLog("run Expr error");
		return null;
	}

	private Node runList(ListNode list) {
		if (list.equals(ListNode.EMPTYLIST))
			return list;
		if (list.car() instanceof FunctionNode) {
			return runFunction((FunctionNode) list.car(), (ListNode) stripList(list.cdr()));
		}
		if (list.car() instanceof BinaryOpNode) {
			return runBinary(list);
		}
		return list;
	}

	private Node runFunction(FunctionNode operator, ListNode operand) {
		switch (operator.funcType) {
		case CAR:
			ListNode node1 = (ListNode) ((QuoteNode) operand.car()).nodeInside();
			// car이 ListNode일 경우 -> Quote로 묶어서 리턴
			if (node1.car() instanceof ListNode)
				return new QuoteNode((ListNode) node1.car());
			// car이 ValueNode일 경우 -> 그대로 리턴
			else if (node1.car() instanceof ValueNode)
				return (ValueNode) node1.car();

			break;
		case CDR:
			ListNode node2 = (ListNode) ((QuoteNode) operand.car()).nodeInside();
			// cdr값을 Quote로 묶어서 리턴
			return new QuoteNode((ListNode) node2.cdr());
		case CONS:
			Node head = operand.car();
			// tail은 QuoteNode형태로 들어온다. QuoteNode의 값을 빼낸다.
			ListNode tail = (ListNode) ((QuoteNode) ((ListNode) operand.cdr().car()).car()).nodeInside();
			// car이 ValueNode일때
			if (operand.car() instanceof ValueNode)
				head = operand.car();
			// car이 QuoteNode일때 값을 빼낸다.
			else if (((ListNode) operand.car()).car() instanceof QuoteNode)
				head = ((QuoteNode) ((ListNode) operand.car()).car()).nodeInside();
			// cons로 head와 tail을 묶고, QuoteNode로 감싸서 리턴한다.
			return new QuoteNode(ListNode.cons(head, tail));

		case NULL_Q:
			// QuoteNode 인 car의 내부 값이 null인지 확인
			ListNode NullTest = (ListNode) ((QuoteNode) operand.car()).nodeInside();
			// EMPTYIST일때 true
			if (NullTest.equals(ListNode.EMPTYLIST))
				return BooleanNode.TRUE_NODE;
			// 아니라면 false
			else
				return BooleanNode.FALSE_NODE;
		case ATOM_Q:
			// QuoteNode car이 atom인지 확인
			QuoteNode AtomTest = (QuoteNode) operand.car();

			if (AtomTest.nodeInside() instanceof ListNode) {
				// List인데 EMPTY이면 true
				if (((ListNode) AtomTest.nodeInside()).equals(ListNode.EMPTYLIST))
					return BooleanNode.TRUE_NODE;
				// List값이 존재하면 false
				else
					return BooleanNode.FALSE_NODE;
			}
			// atom 이므로 true
			else
				return BooleanNode.TRUE_NODE;

		case EQ_Q:
			// 비교할 앞 뒤 원소들을 추출한다.
			QuoteNode EQTest1 = (QuoteNode) ((ListNode) operand.car()).car();
			QuoteNode EQTest2 = (QuoteNode) ((ListNode) operand.cdr().car()).car();

			if (EQTest1.nodeInside() instanceof ValueNode) {
				// 1번노드가 Value이고 2번노드가 List일 때 false
				if (EQTest2.nodeInside() instanceof ListNode)
					return BooleanNode.FALSE_NODE;
				else {
					// 둘 다 Value일 때 값을 비교, 같으면 true 다르면 false
					if (((ValueNode) EQTest1.nodeInside()).equals(EQTest2.nodeInside()))
						return BooleanNode.TRUE_NODE;
					else
						return BooleanNode.FALSE_NODE;
				}
			} else {
				return BooleanNode.FALSE_NODE;
			}

		case NOT:
			// car이 BooleanNode이면 값을 뒤집어서 리턴
			if (operand.car() instanceof BooleanNode)
				return ((BooleanNode) operand.car()).value ? BooleanNode.FALSE_NODE : BooleanNode.TRUE_NODE;
			// car이 연산을 해야하는 Node이면 연산 후 값을 뒤집어서 리턴
			else if (operand.car() instanceof BinaryOpNode)
				return ((BooleanNode) runBinary(ListNode.cons(operand.car(), operand.cdr()))).value
						? BooleanNode.FALSE_NODE
						: BooleanNode.TRUE_NODE;
			else
				return ((BooleanNode) runFunction((FunctionNode) operand.car(), operand.cdr())).value
						? BooleanNode.FALSE_NODE
						: BooleanNode.TRUE_NODE;

		case COND:
			Node remainder = operand;

			// car이 ListNode일 때 연산을 한다. 연산 결과가 참이면 cdr.car리턴
			if (((ListNode) remainder).car() instanceof ListNode) {
				ListNode decision = (ListNode) ((ListNode) remainder).car();
				if (decision.car() instanceof ListNode) {
					if (((BooleanNode) runList((ListNode) decision.car())).value)
						return decision.cdr().car();
				}
			}
			// car이 BooleanNode일 때 true이면 cdr.car을 리턴
			else if (((ListNode) remainder).car() instanceof BooleanNode) {
				if (((BooleanNode) ((ListNode) remainder).car()).value)
					return ((ListNode) remainder).cdr().car();
			}
			// 모든 조건이 false이면 null을 출력하도록 설정
			if (((ListNode) remainder).cdr() == null)
				break;
			// 다음 조건 검사. cdr에 cond토큰을 줘서 runList한다.
			FunctionNode cond = new FunctionNode();
			cond.setValue(lexer.TokenType.COND);
			return runList(ListNode.cons(cond, ((ListNode) remainder).cdr()));
			
		case DEFINE:
			
			
			break;
		default:
			break;
		}

		return null;
	}

	private Node stripList(ListNode node) {
		if (node.car() instanceof ListNode && node.cdr() == ListNode.EMPTYLIST) {
			Node listNode = node.car();
			return listNode;
		} else {
			return node;
		}
	}

	private Node runBinary(ListNode list) {
		BinaryOpNode operator = (BinaryOpNode) list.car();

		// operand2개를 추출한다.
		IntNode Operand1;
		IntNode Operand2;

		// 각 Node가 List라면 재귀적으로 연산해서 값을 얻어낸 뒤 그 값을 이용해 연산한다.
		if (list.cdr().car() instanceof ListNode)
			Operand1 = (IntNode) runBinary((ListNode) list.cdr().car());
		else
			Operand1 = (IntNode) list.cdr().car();

		if (list.cdr().cdr().car() instanceof ListNode)
			Operand2 = (IntNode) runBinary((ListNode) list.cdr().cdr().car());
		else
			Operand2 = (IntNode) list.cdr().cdr().car();

		switch (operator.binType) {
		// 연산에 맞게 Node의 값을 연산하고, IntNode로 묶어서 리턴한다.
		case PLUS:
			return new IntNode(Integer.toString(Operand1.value + Operand2.value));
		case MINUS:
			return new IntNode(Integer.toString(Operand1.value - Operand2.value));
		case TIMES:
			return new IntNode(Integer.toString(Operand1.value * Operand2.value));
		case DIV:
			return new IntNode(Integer.toString(Operand1.value / Operand2.value));
		// 비교연산에 맞게 true와 false를 구분한다.
		case LT:
			return Operand1.value < Operand2.value ? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
		case GT:
			return Operand1.value > Operand2.value ? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
		case EQ:
			return Operand1.value.intValue() == Operand2.value.intValue() ? BooleanNode.TRUE_NODE
					: BooleanNode.FALSE_NODE;
		default:
			break;
		}

		return null;
	}

	private Node runQuote(ListNode node) {
		return ((QuoteNode) node.car()).nodeInside();
	}

	private void insertTable(Node id, Node value) {
		
	}
}
