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

// postfix

// og grammar
// E  -> E E + | E E * | num

import java.util.LinkedList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface Node {
	String toStringPostfix();
}

class ExprNode implements Node {
	final public TermNode t;
	final public ExprNode e;
	final public String op;

	public ExprNode(TermNode t) {
		this(t, null, null);
	}

	public ExprNode(TermNode t, String op, ExprNode e) {
		this.t = t;
		this.op = op;
		this.e = e;
	}

	public String toString() {
		if (e == null) {
			return t.toString();
		}
		return t.toString() + " " + this.op + " " + e.toString();
	}

	public String toStringPostfix() {
		if (e == null) {
			return t.toStringPostfix();
		}
		return t.toStringPostfix() + " " + e.toStringPostfix() + " " + this.op + " ";
	}
}

class TermNode implements Node {
	final public FactorNode f;
	final public TermNode t;
	final public String op;

	public TermNode(FactorNode f) {
		this.f = f;
		this.t = null;
		this.op = null;
	}

	public TermNode(FactorNode f, String op, TermNode t) {
		this.f = f;
		this.op = op;
		this.t = t;
	}

	public String toString() {
		if (t == null) {
			return f.toString();
		}
		return f.toString() + " " + this.op + " " + t.toString();
	}

	public String toStringPostfix() {
		if (t == null) {
			return f.toStringPostfix();
		}
		return f.toStringPostfix() + " " + t.toStringPostfix() + " " + this.op + " ";
	}
}

class FactorNode implements Node {
	public String s;
	public ExprNode e;

	public FactorNode(String s) {
		this.s = s;
		this.e = null;
	}

	public FactorNode(ExprNode e) {
		this.s = null;
		this.e = e;
	}

	public String toString() {
		if (e == null) {
			return s;
		}
		return "(" + e.toString() + ")";
	}

	public String toStringPostfix() {
		if (e == null) {
			return s;
		}
		return e.toStringPostfix();
	}
}

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
			m = Pattern.compile("^(\\d+|[A-Za-z]+).*$").matcher(input);
			if (m.find()) {
				lexemes.addLast(m.group(1));
				input = input.substring(m.group(1).length());
				continue;
			}
			m = Pattern.compile("^([+\\-*/]).*$").matcher(input);
			if (m.find()) {
				lexemes.addLast(m.group(1));
				input = input.substring(m.group(1).length());
				continue;
			}
			throw new IllegalArgumentException("Invalid token \"" + input.substring(0, 1) + "\"");
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
				throw new IllegalArgumentException("Invalid expression (unbalanced parentheses)");
			}
			tokens.extract();
			return new FactorNode(e);
		}
		else if (next.matches("^(\\d|[A-Za-z])+$")) {
			tokens.extract();
			return new FactorNode(next);
		}
		else {
			throw new IllegalArgumentException("Invalid token \"" + next + "\"");
		}
	}

	private TermNode __infixTermPrime(TokStream tokens) {
		String next = tokens.peek();
		if (next == null || !next.equals("*")) {
			return null;
		}
		tokens.extract();
		TermNode t = __infixTerm(tokens);
		if (t == null) {
			throw new IllegalArgumentException("Invalid expression (failed term prime)");
		}
		return t;
	}

	private TermNode __infixTerm(TokStream tokens) {
		FactorNode f = __infixFactor(tokens);
		if (f == null) {
			return null;
		}
		TermNode t = __infixTermPrime(tokens);
		return new TermNode(f, t);
	}

	private ExprNode __infixExprPrime(TokStream tokens) {
		String next = tokens.peek();
		if (next == null || !next.equals("+")) {
			return null;
		}
		tokens.extract();
		ExprNode e =  __infixExpr(tokens);
		if (e == null) {
			throw new IllegalArgumentException("Invalid expression (failed expr prime)");
		}
		return e;
	}

	private ExprNode __infixExpr(TokStream tokens) {
		TermNode t = __infixTerm(tokens);
		if (t == null) {
			return null;
		}
		ExprNode e = __infixExprPrime(tokens);
		return new ExprNode(t, e);
	}

	private static ExprNode combinePlus(Node n1, Node n2) {
		if (n1 instanceof ExprNode) {
			ExprNode e1 = (ExprNode)n1;
			if (n2 instanceof ExprNode) {
				return new ExprNode(new TermNode(new FactorNode(e1)), (ExprNode)n2);
			}
			if (n2 instanceof TermNode) {
				return new ExprNode(new TermNode(new FactorNode(e1)), new ExprNode((TermNode)n2));
			}
			return new ExprNode(new TermNode(new FactorNode(e1)), new ExprNode(new TermNode((FactorNode)n2)));
		}
		if (n1 instanceof TermNode) {
			TermNode t1 = (TermNode)n1;
			if (n2 instanceof ExprNode) {
				return new ExprNode(t1, (ExprNode)n2);
			}
			if (n2 instanceof TermNode) {
				return new ExprNode(t1, new ExprNode((TermNode)n2));
			}
			return new ExprNode(t1, new ExprNode(new TermNode((FactorNode)n2)));
		}
		FactorNode f1 = (FactorNode)n1;
		if (n2 instanceof ExprNode) {
			return new ExprNode(new TermNode(f1), (ExprNode)n2);
		}
		if (n2 instanceof TermNode) {
			return new ExprNode(new TermNode(f1), new ExprNode((TermNode)n2));
		}
		return new ExprNode(new TermNode(f1), new ExprNode(new TermNode((FactorNode)n2)));
	}

	private static TermNode combineMult(Node n1, Node n2) {
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

		return new TermNode(f, t);
	}

	private static ExprNode makeExpr(Node n) {
		if (n instanceof ExprNode)
			return (ExprNode)n;
		if (n instanceof TermNode)
			return new ExprNode((TermNode)n);
		return new ExprNode(new TermNode((FactorNode)n));
	}

	private static int __size(Node n) {
		if (n == null) {
			return 0;
		}
		if (n instanceof ExprNode) {
			ExprNode e = (ExprNode)n;
			return 1 + __size(e.t) + __size(e.e);
		}
		if (n instanceof TermNode) {
			TermNode t = (TermNode)n;
			return 1 + __size(t.f) + __size(t.t);
		}
		FactorNode f = (FactorNode)n;
		return 1 + __size(f.e);
	}

	private static int __leaf(Node n) {
		if (n == null) {
			return 0;
		}
		if (n instanceof ExprNode) {
			ExprNode e = (ExprNode)n;
			return __leaf(e.t) + __leaf(e.e);
		}
		if (n instanceof TermNode) {
			TermNode t = (TermNode)n;
			return __leaf(t.f) + __leaf(t.t);
		}
		FactorNode f = (FactorNode)n;
		return 1 + __leaf(f.e);
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
			throw new IllegalArgumentException("Invalid mode \"" + mode + "\"");
		}
	}

	public boolean buildFromPostfix(String postfix) {
		TokStream stream = new TokStream(postfix);
		Stack<Node> input = new Stack<>();
		while (!stream.isEmpty()) {
			String next = stream.extract();
			if (next.matches("^(\\d|[A-Za-z])+$")) {
				input.push(new FactorNode(next));
			}
			else if (next.equals("+")) {
				if (input.size() < 2) {
					throw new IllegalArgumentException("Stack too short for \"+\"");
				}
				Node i2 = input.pop();
				Node i1 = input.pop();

				input.push(combinePlus(i1, i2));
			}
			else if (next.equals("*")) {
				if (input.size() < 2) {
					throw new IllegalArgumentException("Stack too short for \"+\"");
				}
				Node i2 = input.pop();
				Node i1 = input.pop();

				input.push(combineMult(i1, i2));
			}
			else {
				throw new IllegalArgumentException("Invalid token \"" + next + "\"");
			}
		}
		this.root = makeExpr(input.peek());
		return true;
	}

	public boolean buildFromInfix(String infix) {
		try {
			TokStream stream = new TokStream(infix);
			this.root = __infixExpr(stream);
			if (!stream.isEmpty()) {
				throw new IllegalArgumentException("Trailing tokens");
			}
		}
		catch (IllegalArgumentException e) {
			return false;
		}
		return this.root != null;
	}

	public void printInfixExpression() {
		System.out.println(this.root.toString());
	}

	public void printPostfixExpression() {
		System.out.println(this.root.toStringPostfix());
	}

	public int size() {
		return __size(this.root);
	}

	public boolean isEmpty() {
		return this.root == null;
	}

	public int leafNodes() {
		return __leaf(this.root);
	}
}
