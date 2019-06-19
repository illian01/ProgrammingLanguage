package interpreter;

import java.util.HashMap;
import java.util.Scanner;

import lexer.TokenType;
import parser.*;
import parser.FunctionNode.FunctionType;

public class CuteInterpreter {

	public static HashMap<String, Node> VariableMap = new HashMap<String, Node>(); // HashMap으로 작성된 변수테이블
	// String을 key값으로 그에 맞는 Node를 반환해준다

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

	private void errorLog(String err) { //에러 로그를 찍어주는 함수
		System.out.println(err);
	}

	public Node runExpr(Node rootExpr) {
		if (rootExpr == null)
			return null;
		if (rootExpr instanceof IdNode){ // ID노드일 경우 HashMap으로 작성된 테이블에서 ID노드의 String값으로 탐색을하고 있을땐 그에 맞는 노드 없다면 Id노드반환
			if(VariableMap.containsKey(((IdNode) rootExpr).toString())){		//키를 포함하고 있을때
				if( VariableMap.get(((IdNode) rootExpr).toString()) instanceof ListNode )	//테이블에서 가져온 데이터가 List라면
					return runList( (ListNode) VariableMap.get(((IdNode) rootExpr).toString()) );	//List에 대한 내용을 수행하고 가져온다.
				return VariableMap.get(((IdNode) rootExpr).toString());	//그 외에는 그냥 가져온다
			}
			else				
				return rootExpr;	// 매치되는게 없을경우 자기자신 반환
		}
		else if (rootExpr instanceof IntNode)	// 이후 ListNode가 아니라면 자기자신을 리턴한다.
			return rootExpr;
		else if (rootExpr instanceof BooleanNode)
			return rootExpr;
		else if (rootExpr instanceof ListNode) {	// ListNode라면
			return runList((ListNode) rootExpr);	// List내의 내용을 수행한다.
		} else
			errorLog("run Expr error");
		return null;
	}

	private Node runList(ListNode list) {
		if (list.car() instanceof IdNode) {		//List의 첫 시작이 idNode라면
			return runList(ListNode.cons( runExpr(list.car()) , list.cdr())); //runExpr을 통해 테이블에 있는값을 가져오고, 뒤의 인자를 넘겨준다.
		}
		if (list.equals(ListNode.EMPTYLIST)) {
			return list;
		}
		if (list.car() instanceof FunctionNode) {	// 시작이 FuntionNode일 경우
			if( ((FunctionNode)list.car()).funcType == FunctionType.LAMBDA ){	// 첫 시작이 Lamda일 경우 반환을 해준다. ( 인자 없을때 )
				return list;
			}
			return runFunction((FunctionNode) list.car(), (ListNode) stripList(list.cdr())); //그 외에는 각 명령어에 맞는 동작을 수행한다.
		}
		if (list.car() instanceof BinaryOpNode) {	//이항 연산자도 같은 연산을 수행한다.
			return runBinary(list);
		}
		if (list.car() instanceof ListNode) {
			if (((ListNode)list.car()).car() instanceof FunctionNode) {
				FunctionNode op = (FunctionNode) ((ListNode)list.car()).car();
				if(op.funcType == FunctionType.LAMBDA) {
					ListNode formal = (ListNode) ((ListNode)list.car()).cdr().car();			// formal parameter list
					ListNode actual = list.cdr();												// actual parameter list
					ListNode operation = ((ListNode)list.car()).cdr().cdr();					// function body
					HashMap<String, Node> localExtract = (HashMap<String, Node>) VariableMap.clone();			// 지역변수 처리를 위한 버퍼
					
					if (actual.equals(ListNode.EMPTYLIST)) return list;	//인자가 없으면 걍 자기자신 리턴
					
					for (ListNode i = formal; !i.equals(ListNode.EMPTYLIST); i = i.cdr()) { // formal parameter에 actual parameter를 대입
						Node temp = runExpr(actual.car());
						insertTable((IdNode)i.car(), runExpr(actual.car()));
						actual = actual.cdr();
					}
					
					Node tmp = null;
					while(!operation.equals(ListNode.EMPTYLIST)) {							// function body 실행
						tmp = runExpr(operation.car());
						operation = operation.cdr();
					}
					
					VariableMap = localExtract;		// 지역변수 초기화
					
					return tmp;
				}
			}
		}
		return list;
	}

	private Node runFunction(FunctionNode operator, ListNode operand) {
		switch (operator.funcType) {
		case CAR:
			Node node = runExpr(operand);	//Car의 대상이 되는 부분을 수행하고
			if( node instanceof ListNode )	//결과가 ListNode라면
				node = ((ListNode) stripList((ListNode) node)).car(); // 핵심부분을 가져오게된다.
			if (node instanceof QuoteNode) { // car의 대상은 Quote노드여야한다.
				if (((QuoteNode) node).nodeInside().equals(ListNode.EMPTYLIST)) { // 비어있는 quote에 대해서는 에러를 리턴한다.
					errorLog("Invalid Systax");
					return null;
				}
				if (((QuoteNode) node).nodeInside() instanceof ListNode) {	//비어있지않다면
					Node targetNode = ((ListNode) ((QuoteNode) node).nodeInside()).car(); // Quote노드의 첫 노드가
					return targetNode instanceof IntNode ? targetNode : new QuoteNode(targetNode); // Int노드가 아니라면 전부 '에 감싸서 나옴
				}
			}
			
			errorLog("Invalid Systax");
			return null;

		case CDR:
			Node cdrNode = runExpr(operand);	//car와 유사하게 작동한다.
			if( cdrNode instanceof ListNode ){
				cdrNode = ((ListNode) stripList((ListNode) cdrNode)).car();
			}
			if (cdrNode instanceof QuoteNode) {
				if (((QuoteNode) cdrNode).nodeInside().equals(ListNode.EMPTYLIST)) {
					errorLog("Invalid Systax");
					return null;
				}
				
				if (((QuoteNode) cdrNode).nodeInside() instanceof ListNode) {
					Node targetNode = ((ListNode) ((QuoteNode) cdrNode).nodeInside()).cdr();
					return targetNode instanceof IntNode ? targetNode : new QuoteNode(targetNode);
				}
			}
			
			errorLog("Invalid Systax");
			return null;
			
		case CONS:
			Node head = operand.car();
			Node tail = operand.cdr().car();
			
			// head 부분 처리 Id일 때는 값을 가져온다
			if (head instanceof IdNode) {
				head = lookupTable(((IdNode)head).idString);
				if (head instanceof QuoteNode)
					head = ((QuoteNode)head).nodeInside();
			}
			// ListNode로 값이 나오면 Quote인지 확인 후 처리
			else if (head instanceof ListNode) {
				if (((ListNode)head).car() instanceof QuoteNode)
					head = ((QuoteNode) ((ListNode)head).car()).nodeInside();
				else {
					head = runExpr(head);
					// runExpr의 결과에 대한 처리
					if (head instanceof QuoteNode) 
						head = ((QuoteNode)head).nodeInside();
				}
			}
			
			
			// head와 동일한 처리과정
			if (tail instanceof IdNode) {
				tail = lookupTable(((IdNode)tail).idString);
				if (tail instanceof QuoteNode)
					tail = ((QuoteNode)tail).nodeInside();
			}
			else if (tail instanceof ListNode) {
				if (((ListNode)tail).car() instanceof QuoteNode)
					tail = ((QuoteNode) ((ListNode)tail).car()).nodeInside();
				else {
					tail = runExpr(tail);
					if (tail instanceof QuoteNode) 
						tail = ((QuoteNode)tail).nodeInside();
				}
			}
			
			// tail이 ListNode형태가 아니면 cons로 묶을 수 없음
			if (!(tail instanceof ListNode))
				tail = ListNode.cons(tail, ListNode.EMPTYLIST);
			
			return new QuoteNode(ListNode.cons(head, (ListNode)tail));

		case NULL_Q:
			Node nullNode = operand.car() instanceof QuoteNode // null? '( Something ) 이런 형태일때 ' node라면
					? runQuote(operand) // 'node의 자식을 반환하고
					: (runExpr(operand) instanceof QuoteNode) // 그외에는 뒤의 문법을 실행하고 그 친구가 QuoteNode라면
							? runQuote(ListNode.cons(runExpr(operand), ListNode.EMPTYLIST)) // QuoteNode를 때고
							: runQuote((ListNode) runExpr(operand)); // 그외에는 ListNode이고 그 아래에 QuoteNode일 경우이므로 그냥 넣는다
			return nullNode.equals(ListNode.EMPTYLIST) ? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE; // 위에서 nullNode가 '()이면 true

		case ATOM_Q:
			Node atomNode = operand.car() instanceof QuoteNode // 위와 같은원리로 작동하며
					? runQuote(operand)
					: (operand.car() instanceof FunctionNode || operand.car() instanceof BinaryOpNode) // 실행할 수 있는 문법이면
							? runExpr(operand)
							: runExpr(operand.car()); // 실행을 해주고
			if (atomNode instanceof QuoteNode)
				atomNode = runQuote(ListNode.cons(atomNode, ListNode.EMPTYLIST)); // 다시 QuoteNode일경우 NullQ의 조건과 같이 작동한다.
			if (atomNode instanceof ListNode) { // ListNode가 비어있으면 True 그외에는 False
				return atomNode.equals(ListNode.EMPTYLIST) ? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
			} // 그 외에 InT나 IdNode는 무조건 True
			return BooleanNode.TRUE_NODE;

		case EQ_Q:
			// 비교할 앞 뒤 원소들을 추출한다.
			FunctionNode atom = new FunctionNode();
			atom.setValue(TokenType.ATOM_Q);

			ListNode eqVar1 = runExpr(operand.car()) instanceof ListNode // 두개의 operand를 문법이 실행되고 원하는 형태로 가공한다.
					? (ListNode) runExpr(operand.car())
					: ListNode.cons(runExpr(operand.car()), ListNode.EMPTYLIST);
			ListNode eqVar2 = runExpr(operand.cdr().car()) instanceof ListNode ? (ListNode) runExpr(operand.cdr().car())
					: ListNode.cons(runExpr(operand.cdr().car()), ListNode.EMPTYLIST); // tail

			if (runFunction(atom, eqVar1).equals(BooleanNode.TRUE_NODE) // 두개의 operand가 atom일경우에만 True이므로 조건을 걸고
					&& runFunction(atom, eqVar2).equals(BooleanNode.TRUE_NODE)) {
				if (eqVar1.car() instanceof QuoteNode && eqVar2.car() instanceof QuoteNode) // 만약 두노드가 QuoteNode라면 같은지
																							// 비교 '(a) 같은건 atom이 아니라서
																							// 걸러진다.
					return runQuote(eqVar1).equals(runQuote(eqVar2)) ? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
				return eqVar1.car().equals(eqVar2.car()) ? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
			}
			return BooleanNode.FALSE_NODE; // atom이 아니라면 False

		case NOT:
			if (operand.car().equals(BooleanNode.TRUE_NODE)) { // TrueNode가 대상이라면
				return BooleanNode.FALSE_NODE; // False로
			} else if (operand.car().equals(BooleanNode.FALSE_NODE)) { // FalseNode라면
				return BooleanNode.TRUE_NODE; // TrueNode로
			} else { // 그외에 다른게 왔다면, 먼저 결과를 처리하고 Not을 반환
				if (runExpr(operand) instanceof BooleanNode) {
					return runFunction(operator, ListNode.cons(runExpr(operand), ListNode.EMPTYLIST));
				} else {
					return null;
				}
			}

		case COND:
			Node condNode = operand.car(); // cond의 대상이 1개뿐이라면 그 객체의 car 아니라면 cond를 비교할 List객체가 반환된다
			if (runExpr(condNode) instanceof BooleanNode) { // 이때 condeNode의 실행결과값이 Boolean이라면 ( cond의 대상이 1개일때 )
				return runExpr(condNode).equals(BooleanNode.TRUE_NODE) ? runExpr(operand.cdr().car())
						: new IdNode("Nothing True");
				// condNode가 #T 라면 뒤의 문법에 대해 실행한 결과를 반환 그외에는 True인게 없음을 반환
			}
			if (condNode instanceof ListNode) { // ListNode일경우 비교할대상이 여러개인경우이다.
				return runExpr(((ListNode) condNode).car()).equals(BooleanNode.TRUE_NODE) // 이때 car가 #T나 #F가 되어야하고
						? runExpr(((ListNode) condNode).cdr().car()) // #T일 경우 뒤의 문법에 대해 수행후 반환
						: (operand.cdr().equals(ListNode.EMPTYLIST) // #F일 경우 뒤에 비교할 조건이없다면 Nothing True
								? new IdNode("Nothing True")
								: runFunction(operator, operand.cdr())); // 아니면 뒤의 객체에 대해 재귀호출
			}
			break;
		case DEFINE:		//define을 만나게 되면 중간 인자를 key로
			Node ret = operand.cdr().car(); //이후에 내용을
			insertTable(operand.car(), ret); //매칭시켜 테이블에 저장한다.
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

		Node o1 = runExpr(list.cdr().car()); // operand 의 두개를 가공해서 받고
		Node o2 = runExpr(list.cdr().cdr().car());
		if (!(o1 instanceof IntNode) || !(o2 instanceof IntNode)) { // 두 Node가 Int가 아니라면 False
			return null;
		}

		// operand2개를 추출한다.
		IntNode Operand1 = (IntNode) o1;
		IntNode Operand2 = (IntNode) o2; // 그외에는 각 연산을 수행해준다.

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

	private void insertTable(Node id, Node value) { // id는 변수명, value는 변수값
		VariableMap.put((((IdNode) id).idString), value);
	}

	private Node lookupTable(String id) {
		return VariableMap.get(id);
	}
	
}