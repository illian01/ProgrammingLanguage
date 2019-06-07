package interpreter;

import java.util.HashMap;
import java.util.List;
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
                Node carNode = ((ListNode) (runQuote(operand))).car();
                if (carNode instanceof IntNode) {return carNode;}
                else {return new QuoteNode(carNode);}

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
				Node eqVar1 = operand.car(); //head
				Node eqVar2 = (ListNode) operand.cdr().car(); //tail

				/*if (eqVar1.equals(ListNode.EMPTYLIST) && eqVar2.equals(ListNode.EMPTYLIST)) return BooleanNode.TRUE_NODE;

				if (eqVar1 instanceof ListNode){
					if ()
				}*/



				/*if (eqVar1 instanceof ListNode){
					if ( ((ListNode) eqVar1).car() instanceof QuoteNode) runQuote((ListNode) eqVar1);
					eqVar1 = runExpr(eqVar1);}
				if (eqVar2 instanceof ListNode) eqVar2 = runExpr(eqVar2); //중첩의 중첩 되는지 확인하기


				if (eqVar1 instanceof IntNode && eqVar2 instanceof IntNode)
					return ((IntNode)eqVar1).getValue() == ((IntNode) eqVar2).getValue();
				if (eqVar1 instanceof IdNode && eqVar2 instanceof IdNode){
					return (eqVar1.toString().equals(eqVar2.toString()))? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
				}*/
//
//
//
//				QuoteNode EQTest1 = (QuoteNode) ((ListNode) operand.car()).car();
//				QuoteNode EQTest2 = (QuoteNode) ((ListNode) operand.cdr().car()).car();
//
//				if (EQTest1.nodeInside() instanceof ValueNode) {
//					// 1번노드가 Value이고 2번노드가 List일 때 false
//					if (EQTest2.nodeInside() instanceof ListNode)
//						return BooleanNode.FALSE_NODE;
//					else {
//						// 둘 다 Value일 때 값을 비교, 같으면 true 다르면 false
//						if (((ValueNode) EQTest1.nodeInside()).equals(EQTest2.nodeInside()))
//							return BooleanNode.TRUE_NODE;
//						else
//							return BooleanNode.FALSE_NODE;
//					}
//				} else {
//					return BooleanNode.FALSE_NODE;
//				}

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
				if (operand == ListNode.EMPTYLIST) return new IdNode("Nothing true"); // operand is EMPTYLIST
				Node condCar = operand.car();
				if (condCar instanceof BooleanNode) {
					if (condCar == BooleanNode.TRUE_NODE) return isBool(((ListNode) operand.car()).cdr().car());
					return runFunction(operator, operand.cdr());
				}
				if (((ListNode) condCar).car() instanceof BooleanNode) {
					if (((ListNode) condCar).car() == BooleanNode.TRUE_NODE) return isBool(((ListNode) condCar).cdr().car());
					return runFunction(operator, (ListNode) operand.cdr());
				}
				if (((ListNode) condCar).car() instanceof ListNode) {
					if (((ListNode) ((ListNode) condCar).car()).car() instanceof BinaryOpNode)
						condCar = runBinary((ListNode) ((ListNode) condCar).car());
					else condCar = runFunction((FunctionNode) ((ListNode) ((ListNode) condCar).car()).car(), ((ListNode) ((ListNode) condCar).car()).cdr());
					if (condCar == BooleanNode.TRUE_NODE) return isBool(((ListNode) operand.car()).cdr().car());
					return runFunction(operator, (ListNode) operand.cdr());
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

	private Node isBool(Node value) {
		if (value instanceof ListNode){
			if (((ListNode) value).car() instanceof BinaryOpNode){
				return runBinary((ListNode)value);
			}
			if (((ListNode) value).car() instanceof FunctionNode){
				return runFunction((FunctionNode)((ListNode) value).car(), ((ListNode) value).cdr());
			}
		}
		return value;
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

		// 각 Node가 List라면 재귀적으로 연산해서 값을 얻어낸 뒤 그 값을 이용해 연산한다.
		if (list.cdr().car() instanceof ListNode)
			Operand1 = (IntNode) runBinary((ListNode) list.cdr().car());
		else if (list.cdr().car() instanceof IdNode)
			Operand1 = (IntNode) lookupTable(((IdNode)list.cdr().car()).idString);
		else
			Operand1 = (IntNode) list.cdr().car();

		if (list.cdr().cdr().car() instanceof ListNode)
			Operand2 = (IntNode) runBinary((ListNode) list.cdr().cdr().car());
		else if (list.cdr().cdr().car() instanceof IdNode)
			Operand2 = (IntNode) lookupTable(((IdNode)list.cdr().cdr().car()).idString);
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
