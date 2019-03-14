// infix

// og grammar
// E -> T + E | T
// T -> F x T | F
// F -> ( E ) | num

// fixed grammar
// E  -> T E'
// E' -> + E | epsilon
// T  -> F T'
// T' -> x T | epsilon
// F  -> num | ( E )

import java.util.LinkedList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TokStream {
	private LinkedList<String> lexemes;

	public TokStream(String input) {
		this.lexemes = new LinkedList<>();
		while (!input.isEmpty()) {
			Matcher m;
			m = Pattern.compile("^(\\s+).*$").matcher(input);
			if (m.find()) {
				input = input.substring(m.group(1).length());
				continue;
			}
			m = Pattern.compile("^(\\w+).*$").matcher(input);
			if (m.find()) {
				lexemes.addLast(m.group(1));
				input = input.substring(m.group(1).length());
				continue;
			}
			m = Pattern.compile("^([+\\-*/()]).*$").matcher(input);
			if (m.find()) {
				lexemes.addLast(m.group(1));
				input = input.substring(m.group(1).length());
				continue;
			}
			throw new IllegalStateException("Invalid token \"" + input.substring(0, 1) + "\"");
		}
	}

	public String peek() {
		if (this.isEmpty()) {
			return null;
		}
		return this.lexemes.getFirst();
	}

	public String extract() {
		if (this.isEmpty()) {
			return null;
		}
		return this.lexemes.removeFirst();
	}

	public boolean isEmpty() {
		return this.lexemes.isEmpty();
	}
}


public class BET {
	private abstract static class BinaryNode {
		protected BinaryNode left;
		protected String mid;
		protected BinaryNode right;

		abstract String toStringPostfix();
	}

	private static class ExprNode extends BinaryNode {
		public ExprNode(TermNode t) {
			this(t, null, null);
		}

		public ExprNode(TermNode t, String op, ExprNode e) {
			this.left = t;
			this.mid = op;
			this.right = e;
		}

		public TermNode t() {
			return (TermNode)this.left;
		}

		public ExprNode e() {
			return (ExprNode)this.right;
		}

		public String toString() {
			if (e() == null) {
				return t().toString();
			}
			return t().toString() + " " + this.mid + " " + e().toString();
		}

		public String toStringPostfix() {
			if (e() == null) {
				return t().toStringPostfix();
			}
			return t().toStringPostfix() + " " + e().toStringPostfix() + " " + this.mid;
		}
	}

	private static class TermNode extends BinaryNode {

		public TermNode(FactorNode f) {
			this(f, null, null);
		}

		public TermNode(FactorNode f, String op, TermNode t) {
			this.left = f;
			this.mid = op;
			this.right = t;
		}

		public FactorNode f() {
			return (FactorNode)this.left;
		}

		public TermNode t() {
			return (TermNode)this.right;
		}

		public String toString() {
			if (t() == null) {
				return f().toString();
			}
			return f().toString() + " " + this.mid + " " + t().toString();
		}

		public String toStringPostfix() {
			if (t() == null) {
				return f().toStringPostfix();
			}
			return f().toStringPostfix() + " " + t().toStringPostfix() + " " + this.mid;
		}
	}

	private static class FactorNode extends BinaryNode {
		public FactorNode(String s) {
			this.mid = s;
			this.right = null;
		}

		public FactorNode(ExprNode e) {
			this.mid = null;
			this.right = e;
		}

		public String s() {
			return this.mid;
		}

		public ExprNode e() {
			return (ExprNode)this.right;
		}

		public String toString() {
			if (e() == null) {
				return s();
			}
			return "(" + e().toString() + ")";
		}

		public String toStringPostfix() {
			if (e() == null) {
				return s();
			}
			return e().toStringPostfix();
		}
	}

	private ExprNode root;

	private FactorNode __infixFactor(TokStream tokens) {
		String next = tokens.peek();
		if (next == null) {
			return null;
		}
		if (next.equals("(")) {
			tokens.extract();
			ExprNode e = __infixExpr(tokens);
			String next2 = tokens.peek();
			if (next2 == null || !next2.equals(")")) {
				throw new IllegalStateException("Invalid expression (unbalanced parentheses)");
			}
			tokens.extract();
			return new FactorNode(e);
		}
		else if (next.matches("^\\w+$")) {
			tokens.extract();
			return new FactorNode(next);
		}
		else {
			throw new IllegalStateException("Invalid token \"" + next + "\"");
		}
	}

	private TermNode __infixTermPrime(TokStream tokens, FactorNode f) {
		String next = tokens.peek();
		if (next == null || (!next.equals("*") && !next.equals("/"))) {
			return new TermNode(f);
		}
		tokens.extract();
		TermNode t = __infixTerm(tokens);
		if (t == null) {
			throw new IllegalStateException("Invalid expression (failed term prime)");
		}
		return new TermNode(f, next, t);
	}

	private TermNode __infixTerm(TokStream tokens) {
		FactorNode f = __infixFactor(tokens);
		if (f == null) {
			return null;
		}
		TermNode t = __infixTermPrime(tokens, f);
		return t;
	}

	private ExprNode __infixExprPrime(TokStream tokens, TermNode t) {
		String next = tokens.peek();
		if (next == null || (!next.equals("+") && !next.equals("-"))) {
			return new ExprNode(t);
		}
		tokens.extract();
		ExprNode e =  __infixExpr(tokens);
		if (e == null) {
			throw new IllegalStateException("Invalid expression (failed expr prime)");
		}
		return new ExprNode(t, next, e);
	}

	private ExprNode __infixExpr(TokStream tokens) {
		TermNode t = __infixTerm(tokens);
		if (t == null) {
			return null;
		}
		ExprNode e = __infixExprPrime(tokens, t);
		return e;
	}

	private static ExprNode combinePlus(BinaryNode n1, String op, BinaryNode n2) {
		if (n1 instanceof ExprNode) {
			ExprNode e1 = (ExprNode)n1;
			if (n2 instanceof ExprNode) {
				return new ExprNode(new TermNode(new FactorNode(e1)), op, (ExprNode)n2);
			}
			if (n2 instanceof TermNode) {
				return new ExprNode(new TermNode(new FactorNode(e1)), op, new ExprNode((TermNode)n2));
			}
			return new ExprNode(new TermNode(new FactorNode(e1)), op, new ExprNode(new TermNode((FactorNode)n2)));
		}
		if (n1 instanceof TermNode) {
			TermNode t1 = (TermNode)n1;
			if (n2 instanceof ExprNode) {
				return new ExprNode(t1, op, (ExprNode)n2);
			}
			if (n2 instanceof TermNode) {
				return new ExprNode(t1, op, new ExprNode((TermNode)n2));
			}
			return new ExprNode(t1, op, new ExprNode(new TermNode((FactorNode)n2)));
		}
		FactorNode f1 = (FactorNode)n1;
		if (n2 instanceof ExprNode) {
			return new ExprNode(new TermNode(f1), op, (ExprNode)n2);
		}
		if (n2 instanceof TermNode) {
			return new ExprNode(new TermNode(f1), op, new ExprNode((TermNode)n2));
		}
		return new ExprNode(new TermNode(f1), op, new ExprNode(new TermNode((FactorNode)n2)));
	}

	private static TermNode combineMult(BinaryNode n1, String op, BinaryNode n2) {
		FactorNode f;
		if (n1 instanceof ExprNode) {
			f = new FactorNode((ExprNode)n1);
		}
		else if (n1 instanceof TermNode) {
			f = new FactorNode(new ExprNode((TermNode)n1));
		}
		else {
			f = (FactorNode)n1;
		}

		TermNode t;
		if (n2 instanceof ExprNode) {
			t = new TermNode(new FactorNode((ExprNode)n2));
		}
		else if (n2 instanceof TermNode) {
			t = (TermNode)n2;
		}
		else {
			t = new TermNode((FactorNode)n2);
		}

		return new TermNode(f, op, t);
	}

	private static ExprNode makeExpr(BinaryNode n) {
		if (n instanceof ExprNode)
			return (ExprNode)n;
		if (n instanceof TermNode)
			return new ExprNode((TermNode)n);
		return new ExprNode(new TermNode((FactorNode)n));
	}

	public BET() {
		root = null;
	}

	public BET(String expr, char mode) {
		if (mode == 'p' || mode == 'P') {
			buildFromPostfix(expr);
		}
		else if (mode == 'i' || mode == 'I') {
			buildFromInfix(expr);
		}
		else {
			throw new IllegalStateException("Invalid mode \"" + mode + "\"");
		}
	}

	public boolean buildFromPostfix(String postfix) {
		TokStream stream = new TokStream(postfix);
		Stack<BinaryNode> input = new Stack<>();
		while (!stream.isEmpty()) {
			String next = stream.extract();
			if (next.matches("^\\w+$")) {
				input.push(new FactorNode(next));
			}
			else if (next.equals("+") || next.equals("-")) {
				if (input.size() < 2) {
					throw new IllegalStateException("Stack too short for \"+\"");
				}
				BinaryNode i2 = input.pop();
				BinaryNode i1 = input.pop();

				input.push(combinePlus(i1, next, i2));
			}
			else if (next.equals("*") || next.equals("/")) {
				if (input.size() < 2) {
					throw new IllegalStateException("Stack too short for \"+\"");
				}
				BinaryNode i2 = input.pop();
				BinaryNode i1 = input.pop();

				input.push(combineMult(i1, next, i2));
			}
			else {
				throw new IllegalStateException("Invalid token \"" + next + "\"");
			}
		}
		this.root = makeExpr(input.peek());
		return true;
	}

	public boolean buildFromInfix(String infix) {
		TokStream stream = new TokStream(infix);
		this.root = __infixExpr(stream);
		if (!stream.isEmpty()) {
			throw new IllegalStateException("Trailing tokens");
		}
		return this.root != null;
	}

	public void printInfixExpression() {
		if (this.root != null) {
			printInfixExpression(this.root);
			System.out.println();
		}
	}

	public void printPostfixExpression() {
		if (this.root != null) {
			printPostfixExpression(this.root);
			System.out.println();
		}
	}

	public int size() {
		return size(this.root);
	}

	public boolean isEmpty() {
		return this.root == null;
	}

	public int leafNodes() {
		return leafNodes(this.root);
	}

	private static void printInfixExpression(BinaryNode n) {
		if (n == null) {
			return;
		}
		printInfixExpression(n.left);
		if (n.mid != null) {
			System.out.print(n.mid + " ");
		}
		if (n instanceof FactorNode && n.right != null) {
			System.out.print("( ");
			printInfixExpression(n.right);
			System.out.print(") ");
		}
		else {
			printInfixExpression(n.right);
		}
	}

	private static void makeEmpty(BinaryNode t) {
		if (t == null) {
			return;
		}
		makeEmpty(t.left);
		t.left = null;
		makeEmpty(t.right);
		t.right = null;
	}

	private static void printPostfixExpression(BinaryNode n) {
		if (n == null) {
			return;
		}
		printPostfixExpression(n.left);
		printPostfixExpression(n.right);
		if (n.mid != null) {
			System.out.print(n.mid + " ");
		}
	}

	private static int size(BinaryNode n) {
		if (n == null) {
			return 0;
		}
		if (n instanceof ExprNode) {
			ExprNode e = (ExprNode)n;
			if (e.e() == null) {
				return size(e.t());
			}
			return 1 + size(e.t()) + size(e.e());
		}
		if (n instanceof TermNode) {
			TermNode t = (TermNode)n;
			if (t.t() == null) {
				return size(t.f());
			}
			return 1 + size(t.f()) + size(t.t());
		}
		FactorNode f = (FactorNode)n;
		if (f.e() != null) {
			return size(f.e());
		}
		return 1;
	}

	private static int leafNodes(BinaryNode n) {
		if (n == null) {
			return 0;
		}
		if (n instanceof ExprNode) {
			ExprNode e = (ExprNode)n;
			return leafNodes(e.t()) + leafNodes(e.e());
		}
		if (n instanceof TermNode) {
			TermNode t = (TermNode)n;
			return leafNodes(t.f()) + leafNodes(t.t());
		}
		FactorNode f = (FactorNode)n;
		if (f.e() != null) {
			return leafNodes(f.e());
		}
		return 1;
	}
}
