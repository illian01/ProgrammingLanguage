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
				Node eqVar2 = operand.cdr().car(); //tail

				if (eqVar2 instanceof ListNode){//ListNode중에서 반환값이 FunctionNode이거나 BinaryOpNode일경우
					if (((ListNode) eqVar2).car() instanceof FunctionNode){
						eqVar2 = runFunction((FunctionNode)((ListNode) eqVar2).car(), ((ListNode) eqVar2).cdr());
					}
					if (((ListNode) eqVar2).car() instanceof BinaryOpNode){eqVar2 = runBinary((ListNode)eqVar2);}
				}
				if (eqVar1 instanceof ListNode){//ListNode중에서 반환값이 FunctionNode이거나 BinaryOpNode일경우
					if (((ListNode) eqVar1).car() instanceof FunctionNode){
						eqVar1 = runFunction((FunctionNode)((ListNode) eqVar1).car(), ((ListNode) eqVar1).cdr());
					}
					if (((ListNode) eqVar1).car() instanceof BinaryOpNode){ eqVar1 = runBinary((ListNode)eqVar1); }
				}
				if (!(eqVar1 instanceof ListNode)){
					if (!(eqVar2 instanceof ListNode)){//둘 다atom일 경우
						if (VariableMap.containsKey(eqVar1.toString())) { eqVar1 = VariableMap.get(eqVar1.toString()); }
						if (VariableMap.containsKey(eqVar2.toString())) { eqVar2 = VariableMap.get(eqVar2.toString()); }
						return eqVar1.equals(eqVar2)? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
					}
				}

				if (((ListNode)eqVar1).car() instanceof QuoteNode){
					if (((ListNode)eqVar2).car() instanceof QuoteNode){ //둘 다 QuoteNode일 때
						QuoteNode EQTest1 = (QuoteNode) ((ListNode) operand.car()).car();
						QuoteNode EQTest2 = (QuoteNode) ((ListNode) operand.cdr().car()).car();
						//둘 다 Quote 중 EMPTYLIST일 경우
						if (EQTest2.nodeInside().equals(ListNode.EMPTYLIST) && EQTest1.nodeInside().equals(ListNode.EMPTYLIST)) return BooleanNode.TRUE_NODE;
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
					}
				}
				break;

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