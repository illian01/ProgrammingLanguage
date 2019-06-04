package parser;

public class NodePrinter {
	private StringBuffer sb = new StringBuffer();
	private Node root;
	public NodePrinter(Node root) {
		this.root = root;
	}

	private void printList(ListNode listNode) {
		if(listNode == ListNode.EMPTYLIST) 
			return ;
		
		printNode(listNode.car());			// ListNode의 head 출력
		printList(listNode.cdr());			// ListNode의 tail 출력, printNode가 아닌 printList를 사용한다. printNode사용 시 괄호 갯수 안맞음
	}
	
	private void printNode(QuoteNode quoteNode) {
		if(quoteNode.nodeInside() == null)
			return ;
		
		sb.append("' ");							// ' 출력
		printNode(quoteNode.nodeInside());			// QuoteNode에는 Node를 필드 값으로 갖는다. 이를 printNode에 인자로 출력시킨다.
	}
	
	private void printNode(Node node) {
		if(node == null)
			return ;
		
		if(node.getClass().getName().contains("List")) {
			ListNode tmp = (ListNode) node;
			if(tmp == ListNode.EMPTYLIST) {
				sb.append("( ) ");											// EMPTYLIST일 때 처리
				return ;
			}
			if(tmp.car().getClass().getName().contains("Quote"))			// quote 일 때에는 괄호 출력 X
				printList((ListNode)node);
			else {
				sb.append("( ");
				printList((ListNode)node);									// ListNode이므로 List를 출력하는 메소드 호출 
				sb.append(") ");
			}
		}
		else if(node.getClass().getName().contains("Quote"))				// QuoteNode이므로 Quote 출력하는 메소드 호출
			printNode((QuoteNode) node);
		else 
			sb.append(node + " ");									// 둘 다 아니면 노드 값 toString이용해 출력
	}
	
	public void prettyPrint() {
		sb.append("… ");
		printNode(root);
		System.out.println(sb.toString());
	}
}