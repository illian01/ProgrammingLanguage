package parser;

public interface ListNode extends Node {
	static ListNode EMPTYLIST = new ListNode() {
		public Node car() {
			return null;
		}
		public ListNode cdr() {
			return null;
		}
	};
	
	static ListNode cons(Node head, ListNode tail) {
		return new ListNode() {
			public Node car() {
				return head;
			}
			public ListNode cdr() {
				return tail;
			}
		};
	}
	Node car();
	ListNode cdr();
}