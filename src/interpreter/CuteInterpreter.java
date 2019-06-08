package interpreter;

import java.util.HashMap;
import java.util.Scanner;

import lexer.TokenType;
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
			return VariableMap.containsKey( ((IdNode) rootExpr).toString()) ? VariableMap.get(((IdNode) rootExpr).toString()):rootExpr;
		else if (rootExpr instanceof IntNode)
			return rootExpr;
		else if (rootExpr instanceof BooleanNode)
			return rootExpr;
		else if (rootExpr instanceof ListNode){
			return runList((ListNode) rootExpr);
		}
		else
			errorLog("run Expr error");
		return null;
	}

	private Node runList(ListNode list) {
		if (list.car() instanceof IdNode) {
			return runExpr(list.car());
		}
		if (list.equals(ListNode.EMPTYLIST)){
			return list;
		}
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
				Node node = operand.car();
				if (node.equals(ListNode.EMPTYLIST)) { return null; } //Error
				if (node instanceof QuoteNode){
					if (((QuoteNode) node).nodeInside() instanceof ListNode){
						Node targetNode = ((ListNode) ((QuoteNode) node).nodeInside()).car();
						return targetNode instanceof IntNode ? targetNode : new QuoteNode(targetNode);
					}
					return null; //error
				}
				if (operand instanceof ListNode){
					return runFunction(operator, ListNode.cons(runExpr(operand),ListNode.EMPTYLIST));
				}
				break;

			case CDR:
				Node cdrNode = operand.car();
				if (cdrNode.equals(ListNode.EMPTYLIST)) { return null; } //Error
				if (cdrNode instanceof QuoteNode){
					if (((QuoteNode) cdrNode).nodeInside() instanceof ListNode){
						Node targetNode = ((ListNode) ((QuoteNode) cdrNode).nodeInside()).cdr();
						return targetNode instanceof IntNode ? targetNode : new QuoteNode(targetNode);
					}
					return null; //error
				}
				if (operand instanceof ListNode){
					return runFunction(operator, ListNode.cons(runExpr(operand),ListNode.EMPTYLIST));
				}
				break;


				/*ListNode node2 = (ListNode) ((QuoteNode) operand.car()).nodeInside();
				// cdr값을 Quote로 묶어서 리턴
				return new QuoteNode((ListNode) node2.cdr());*/
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
                Node test = runExpr(operand);
				Node nullNode = operand.car() instanceof QuoteNode
                        ? runQuote(operand)
                        :(runExpr(operand) instanceof QuoteNode)
                            ? runQuote(ListNode.cons(runExpr(operand),ListNode.EMPTYLIST))
                            : runQuote((ListNode)runExpr(operand));
                return nullNode.equals(ListNode.EMPTYLIST)? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;

			case ATOM_Q:
				Node atomNode = operand.car() instanceof QuoteNode
						? runQuote(operand)
						: (operand.car() instanceof FunctionNode || operand.car() instanceof BinaryOpNode)
							? runExpr(operand)
							: runExpr(operand.car());
				if (atomNode instanceof QuoteNode) atomNode = runQuote(ListNode.cons(atomNode, ListNode.EMPTYLIST));
				if (atomNode instanceof ListNode) {
					return atomNode.equals(ListNode.EMPTYLIST)? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
				}
				return BooleanNode.TRUE_NODE;

			case EQ_Q:
				// 비교할 앞 뒤 원소들을 추출한다.
				FunctionNode atom = new FunctionNode();
				atom.setValue(TokenType.ATOM_Q);

				ListNode eqVar1 = runExpr(operand.car()) instanceof ListNode
						? (ListNode)runExpr(operand.car())
						: ListNode.cons(runExpr(operand.car()), ListNode.EMPTYLIST);
				ListNode eqVar2 = runExpr(operand.cdr().car()) instanceof ListNode
						? (ListNode)runExpr(operand.cdr().car())
						: ListNode.cons(runExpr(operand.cdr().car()), ListNode.EMPTYLIST); //tail

				if (runFunction(atom, eqVar1).equals(BooleanNode.TRUE_NODE)
                && runFunction(atom, eqVar2).equals(BooleanNode.TRUE_NODE)){
				    if (eqVar1.car() instanceof QuoteNode && eqVar2.car() instanceof QuoteNode)
				        return runQuote(eqVar1).equals(runQuote(eqVar2))? BooleanNode.TRUE_NODE:BooleanNode.FALSE_NODE;
				    return eqVar1.car().equals(eqVar2.car()) ? BooleanNode.TRUE_NODE: BooleanNode.FALSE_NODE;
                }
				return BooleanNode.FALSE_NODE;

			case NOT:
				if(operand.car().equals(BooleanNode.TRUE_NODE)){	//TrueNode가 대상이라면
					return BooleanNode.FALSE_NODE;	//False로
				}
				else if(operand.car().equals(BooleanNode.FALSE_NODE)){	//FalseNode라면
					return BooleanNode.TRUE_NODE;	//TrueNode로
				}
				else{	//그외에 다른게 왔다면, 먼저 결과를 처리하고 Not을 반환
					if(runExpr(operand) instanceof BooleanNode){
						return runFunction(operator, ListNode.cons(runExpr(operand), ListNode.EMPTYLIST) );
					}
					else{
						return null;
					}
				}

			case COND:
                Node condNode = operand.car();
                if (condNode instanceof BooleanNode){
                    return condNode.equals(BooleanNode.TRUE_NODE) ? runExpr(operand.cdr().car()) : new IdNode("Nothing True");
                }
                if (condNode instanceof ListNode){
                    return runExpr(((ListNode) condNode).car()).equals(BooleanNode.TRUE_NODE)
                            ? runExpr(((ListNode)condNode).cdr().car())
                            : (operand.cdr().equals(ListNode.EMPTYLIST)
                                ? new IdNode("Nothing True")
                                : runFunction(operator,operand.cdr()));
                }
				break;
			case DEFINE:
				insertTable(operand.car(), operand.cdr().car()); //첫번째 인자로 변수명, 2번째 인자로 변수값
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

		Node o1 = runExpr(list.cdr().car());
		Node o2 = runExpr(list.cdr().cdr().car());
		if( !(o1 instanceof IntNode) || !(o2 instanceof IntNode) ){
			return null;
		}

		// operand2개를 추출한다.
		IntNode Operand1 = (IntNode) o1;
		IntNode Operand2 = (IntNode) o2;

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

	private void insertTable(Node id, Node value) { //id는 변수명, value는 변수값
		Node tmp;
		if (value instanceof ListNode) { //value가 ListNode일 경우
			if (((ListNode)value).car() instanceof BinaryOpNode) //첫번째 노드가 BinaryOpNode일 경우
				tmp = runExpr(value);
			else // List안의 Int, id, boolean
				// List 안에 FunctionNode 기능 구현해야하는가? 문의하기
				// define으로 함수정의 기능 (추가구현)
				tmp = ((ListNode)value).car();
		}
		else
			tmp = value;
		VariableMap.put((((IdNode) id).idString), tmp);
	}

	private Node lookupTable(String id) {
		return VariableMap.get(id);
	}
}