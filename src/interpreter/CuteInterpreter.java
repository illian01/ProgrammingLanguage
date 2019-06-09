package interpreter;

import java.util.HashMap;
import java.util.Scanner;

import lexer.TokenType;
import parser.*;

public class CuteInterpreter {

	public static HashMap<String, Node> VariableMap = new HashMap<String, Node>(); //HashMap으로 작성된 변수테이블
	//String을 key값으로 그에 맞는 Node를 반환해준다
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
		if (rootExpr instanceof IdNode)	//ID노드일 경우 HashMap으로 작성된 테이블에서 ID노드의 String값으로 탐색을하고 있을땐 그에 맞는 노드 없다면 Id노드반환
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
				Node node = operand.car();	//  ListNode의 첫번째 원소를 받아
				if (node.equals(ListNode.EMPTYLIST)) { return null; } //비어있을땐 예외
				if (node instanceof QuoteNode){	//Quote노드일경우
					if (((QuoteNode) node).nodeInside() instanceof ListNode){
						Node targetNode = ((ListNode) ((QuoteNode) node).nodeInside()).car(); //Quote노드의 첫 노드가
						return targetNode instanceof IntNode ? targetNode : new QuoteNode(targetNode);	//Int노드가 아니라면 전부 '에 감싸서 나옴
					}
					return null; //error
				}
				if (operand instanceof ListNode){		//그 외에 operand가 ListNode라면
					return runFunction(operator, ListNode.cons(runExpr(operand),ListNode.EMPTYLIST)); //재귀적으로 뒷부분을 처리하고
				}																						  //처리한 부분에 대해 car를 처리한다.
				break;

			case CDR:
				Node cdrNode = operand.car();															//CAR와 마찬가지로 동작
				if (cdrNode.equals(ListNode.EMPTYLIST)) { return null; } //Error
				if (cdrNode instanceof QuoteNode){
					if (((QuoteNode) cdrNode).nodeInside() instanceof ListNode){
						Node targetNode = ((ListNode) ((QuoteNode) cdrNode).nodeInside()).cdr();		//이부분만 다름
						return targetNode instanceof IntNode ? targetNode : new QuoteNode(targetNode);
					}
					return null; //error
				}
				if (operand instanceof ListNode){
					return runFunction(operator, ListNode.cons(runExpr(operand),ListNode.EMPTYLIST));
				}
				break;
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
				Node nullNode = operand.car() instanceof QuoteNode	// null? '( Something ) 이런 형태일때 ' node라면
						? runQuote(operand)	// 'node의 자식을 반환하고
						:(runExpr(operand) instanceof QuoteNode) //그외에는 뒤의 문법을 실행하고 그 친구가 QuoteNode라면
						? runQuote(ListNode.cons(runExpr(operand),ListNode.EMPTYLIST)) //QuoteNode를 때고
						: runQuote((ListNode)runExpr(operand)); //그외에는 ListNode이고 그 아래에 QuoteNode일 경우이므로 그냥 넣는다
				return nullNode.equals(ListNode.EMPTYLIST)? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE; //위에서 nullNode가 '()이면 true

			case ATOM_Q:
				Node atomNode = operand.car() instanceof QuoteNode //위와 같은원리로 작동하며
						? runQuote(operand)
						: (operand.car() instanceof FunctionNode || operand.car() instanceof BinaryOpNode)	//실행할 수 있는 문법이면
						? runExpr(operand)
						: runExpr(operand.car());	//실행을 해주고
				if (atomNode instanceof QuoteNode) atomNode = runQuote(ListNode.cons(atomNode, ListNode.EMPTYLIST)); //다시 QuoteNode일경우 NullQ의 조건과 같이 작동한다.
				if (atomNode instanceof ListNode) {	//ListNode가 비어있으면 True 그외에는 False
					return atomNode.equals(ListNode.EMPTYLIST)? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
				}//그 외에 InT나 IdNode는 무조건 True
				return BooleanNode.TRUE_NODE;

			case EQ_Q:
				// 비교할 앞 뒤 원소들을 추출한다.
				FunctionNode atom = new FunctionNode();
				atom.setValue(TokenType.ATOM_Q);

				ListNode eqVar1 = runExpr(operand.car()) instanceof ListNode	//두개의 operand를 문법이 실행되고 원하는 형태로 가공한다.
						? (ListNode)runExpr(operand.car())
						: ListNode.cons(runExpr(operand.car()), ListNode.EMPTYLIST);
				ListNode eqVar2 = runExpr(operand.cdr().car()) instanceof ListNode
						? (ListNode)runExpr(operand.cdr().car())
						: ListNode.cons(runExpr(operand.cdr().car()), ListNode.EMPTYLIST); //tail

				if (runFunction(atom, eqVar1).equals(BooleanNode.TRUE_NODE)	//두개의 operand가 atom일경우에만 True이므로 조건을 걸고
						&& runFunction(atom, eqVar2).equals(BooleanNode.TRUE_NODE)){
					if (eqVar1.car() instanceof QuoteNode && eqVar2.car() instanceof QuoteNode)	//만약 두노드가 QuoteNode라면 같은지 비교 '(a) 같은건 atom이 아니라서 걸러진다.
						return runQuote(eqVar1).equals(runQuote(eqVar2))? BooleanNode.TRUE_NODE:BooleanNode.FALSE_NODE;
					return eqVar1.car().equals(eqVar2.car()) ? BooleanNode.TRUE_NODE: BooleanNode.FALSE_NODE;
				}
				return BooleanNode.FALSE_NODE; //atom이 아니라면 False

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
				Node condNode = operand.car();	// cond의 대상이 1개뿐이라면 그 객체의 car 아니라면 cond를 비교할 List객체가 반환된다
				if ( runExpr(condNode) instanceof BooleanNode){	//이때 condeNode의 실행결과값이 Boolean이라면 ( cond의 대상이 1개일때 )
					return runExpr(condNode).equals(BooleanNode.TRUE_NODE) ? runExpr(operand.cdr().car()) : new IdNode("Nothing True");
					//     condNode가 #T 라면 뒤의 문법에 대해 실행한 결과를 반환 그외에는 True인게 없음을 반환
				}
				if (condNode instanceof ListNode){	//ListNode일경우 비교할대상이 여러개인경우이다.
					return runExpr(((ListNode) condNode).car()).equals(BooleanNode.TRUE_NODE)	//이때 car가 #T나 #F가 되어야하고
							? runExpr(((ListNode)condNode).cdr().car())	//#T일 경우 뒤의 문법에 대해 수행후 반환
							: (operand.cdr().equals(ListNode.EMPTYLIST)	//#F일 경우 뒤에 비교할 조건이없다면 Nothing True
							? new IdNode("Nothing True")
							: runFunction(operator,operand.cdr())); //아니면 뒤의 객체에 대해 재귀호출
				}
				break;
			case DEFINE:
				insertTable(operand.car(), operand.cdr().car()); //첫번째 인자로 변수명, 2번째 인자로 변수값
				//define을 만나면 Id노드에 대해 무조건적인 뒷부분 값을 넣음
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

		Node o1 = runExpr(list.cdr().car());		//operand 의 두개를 가공해서 받고
		Node o2 = runExpr(list.cdr().cdr().car());
		if( !(o1 instanceof IntNode) || !(o2 instanceof IntNode) ){	//두 Node가 Int가 아니라면 False
			return null;
		}

		// operand2개를 추출한다.
		IntNode Operand1 = (IntNode) o1;
		IntNode Operand2 = (IntNode) o2;	//그외에는 각 연산을 수행해준다.

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