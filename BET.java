/*
 * dear grader,
 * i'm sorry for forcing you to read through this awful code
 * sincerely,
 * jon "arch jesus" lemos
 *
 * infix
 *
 * og grammar
 * E -> E + T | E - T | T
 * T -> T * F | T / F | F
 * F -> id | ( E )
 *
 * fixed grammar
 * E  -> T E'
 * E' -> + E | - E | epsilon
 * T  -> F T'
 * T' -> * T | / T | epsilon
 * F  -> id | ( E )
 */

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// lexer / token stream class
class TokStream {
	// the tokens
	private LinkedList<String> lexemes;

	public TokStream(String input) {
		// splits the input stream into identifiers and operators (tokens)
		// a.k.a. "lexical analysis"
		this.lexemes = new LinkedList<>();
		while (!input.isEmpty()) {
			Matcher m;
			// if the string starts with whitespace
			m = Pattern.compile("^(\\s+).*$").matcher(input);
			if (m.find()) {
				// get rid of it
				input = input.substring(m.group(1).length());
				continue;
			}
			// if the string starts with [A-Za-z0-9]
			m = Pattern.compile("^(\\w+).*$").matcher(input);
			if (m.find()) {
				// we read an id
				lexemes.addLast(m.group(1));
				input = input.substring(m.group(1).length());
				continue;
			}
			// if the string is punctuation
			m = Pattern.compile("^([+\\-*/()]).*$").matcher(input);
			if (m.find()) {
				// we read punctuation
				lexemes.addLast(m.group(1));
				input = input.substring(m.group(1).length());
				continue;
			}
			throw new IllegalStateException("Invalid token \"" + input.substring(0, 1) + "\"");
		}
	}

	// returns the next token in the stream without extracting it
	public String peek() {
		if (this.isEmpty()) {
			return null;
		}
		return this.lexemes.getFirst();
	}

	// removes the next token from the stream and returns it
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
	// returns true if s is a "+" or "-"
	public static boolean isAddop(String s) {
		return s != null && s.matches("^[+\\-]$");
	}

	// returns true if s is a "*" or "/"
	public static boolean isMulop(String s) {
		return s != null && s.matches("^[*/]$");
	}

	// returns true if s is an identifier
	public static boolean isId(String s) {
		return s != null && s.matches("^\\w+$");
	}

	// standard binary node class
	private static class BinaryNode {
		protected BinaryNode left;
		protected String mid;
		protected BinaryNode right;

		public BinaryNode() {
			this(null, null, null);
		}

		public BinaryNode(String mid) {
			this(null, mid, null);
		}

		public BinaryNode(BinaryNode left, String mid, BinaryNode right) {
			this.left = left;
			this.mid = mid;
			this.right = right;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (left != null) {
				sb.append(left.toString());
			}
			if (mid != null) {
				sb.append(" ");
				sb.append(mid);
				sb.append(" ");
			}
			if (right != null) {
				sb.append(right.toString());
			}
			return sb.toString();
		}
	}

	/*
	 * so let me try to explain the clusterfuck below
	 * see the grammar at the top of the file? that's how i parse infix
	 * so parsing the expression "3 * (4 + 5)" would look like
	 *
	 *   E
	 *   |
	 *   T
	 *   |
	 * T-|-----F
	 * | |     |
	 * | | ----E----
	 * | | |   |   |
	 * | | | E-|-T |
	 * | | | | | | |
	 * | | | T | | |
	 * | | | | | | |
	 * F | | F | F |
	 * | | | | | | |
	 * 3 * ( 4 + 5 )
	 *
	 * These node classes represent the E's, T's and F's
	 */

	// stores an expression (E -> E + T | E - T | T). used for the infix parser
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

	// stores a term (T -> T * F | T / F | F). used for the infix parser
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

	// stores a factor (F -> id | ( E )). used for the infix parser
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

	// the root of the tree
	private BinaryNode root;

	// F -> ID | ( E )
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

	// T' -> * T | / T | epsilon
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

	// T -> F T'
	private TermNode __infixTerm(TokStream tokens) {
		FactorNode f = __infixFactor(tokens);
		if (f == null) {
			return null;
		}
		TermNode t = __infixTermPrime(tokens, f);
		return t;
	}

	// E' -> + E | - E | epsilon
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

	// E -> T E'
	private ExprNode __infixExpr(TokStream tokens) {
		TermNode t = __infixTerm(tokens);
		if (t == null) {
			return null;
		}
		ExprNode e = __infixExprPrime(tokens, t);
		return e;
	}

	private String infixToPostfix(String infix) {
		TokStream stream = new TokStream(infix);
		Stack<String> ops = new Stack<>();
		Queue<String> out = new LinkedList<>();

		while (!stream.isEmpty()) {
			String next = stream.extract();
			if (isId(next)) {
				out.add(next);
			}
			else if (isAddop(next)) {
				while (!ops.empty() && !ops.peek().equals("(")) {
					out.add(ops.pop());
				}
				ops.push(next);
			}
			else if (isMulop(next)) {
				while (!ops.empty() && isMulop(ops.peek())) {
					out.add(ops.pop());
				}
				ops.push(next);
			}
			else if (next.equals("(")) {
				ops.push("(");
			}
			else if (next.equals(")")) {
				if (ops.empty()) {
					throw new IllegalStateException("Mismatched parentheses");
				}
				while (!ops.peek().equals("(")) {
					out.add(ops.pop());
					if (ops.empty()) {
						throw new IllegalStateException("Mismatched parentheses");
					}
				}
				ops.pop();
			}
		}
		while (!ops.isEmpty()) {
			out.add(ops.pop());
		}

		StringBuilder ret = new StringBuilder();
		for (String s : out) {
			ret.append(s);
			ret.append(" ");
		}
		ret.deleteCharAt(ret.length() - 1);
		return ret.toString();
	}

	// used for postfix expressions. combines two exprs into one
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

	// used for postfix expressions. combines two terms into one
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

	// converts a node into its equivalent expr node
	private static ExprNode makeExpr(BinaryNode n) {
		if (n instanceof ExprNode)
			return (ExprNode)n;
		if (n instanceof TermNode)
			return new ExprNode((TermNode)n);
		return new ExprNode(new TermNode((FactorNode)n));
	}

	// so at this point we have a tree of ExprNodes, TermNodes, and FactorNodes
	// we now need to turn it into a regular binary tree
	// the below function does that (BinaryNode is a superclass of these three)
	private static void __generalize(BinaryNode n) {
		if (n == null) {
			return;
		}
		__generalize(n.left);
		__generalize(n.right);
		if (n.left != null) {
			n.left = new BinaryNode(n.left.left, n.left.mid, n.left.right);
		}
		if (n.right != null) {
			n.right = new BinaryNode(n.right.left, n.right.mid, n.right.right);
		}
	}

	// the above function cannot affect the root node because java doesn't have ref parameters like big daddy C#
	// this function takes care of that
	private void __generalize() {
		__generalize(this.root);
		this.root = new BinaryNode(this.root.left, this.root.mid, this.root.right);
	}

	// the above function will have a lot of nodes with only left or right children and no middle
	// this function gets rid of those extraneous nodes
	private static void __condense(BinaryNode n) {
		if (n == null || (n.left == null && n.right == null)) {
			return;
		}

		__condense(n.left);
		__condense(n.right);

		if (n.left != null) {
			if (n.left.mid == null && n.left.left != null) {
				n.left = n.left.left;
			} else if (n.left.mid == null && n.left.right != null) {
				n.left = n.left.right;
			}
		}

		if (n.right != null) {
			if (n.right.mid == null && n.right.left != null) {
				n.right = n.right.left;
			} else if (n.right.mid == null && n.right.right != null) {
				n.right = n.right.right;
			}
		}
	}

	private void __condense() {
		if (this.root == null) {
			return;
		}
		__condense(this.root);
		if (this.root.mid != null) {
			return;
		}
		if (this.root.left != null && this.root.right != null) {
			throw new IllegalStateException("how did we get here?");
		}
		if (this.root.left != null) {
			this.root = this.root.left;
		}
		else if (this.root.right != null) {
			this.root = this.root.right;
		}
	}

	/*
	 * we've been doing the above for this function
	 *
	 * you see, the above grammar is ambiguous
	 * for a given expression "3 + 4 + 5", you could have either
	 *
	 *     +
	 *    / \
	 *   +   5
	 *  / \
	 * 3   4
	 *
	 *   or
	 *
	 *   +
	 *  / \
	 * 3   +
	 *    / \
	 *   4   5
	 *
	 * left recursion is a pain in the ass and can only be handled by a shift-reduce parser
	 * shift-reduce parsers are a pain in the ass because you have to make first/follow sets along with the rest of the state machine only to figure out that your grammar isn't actually LR(1)
	 * so a recursive descent parser like this one is the only real option
	 * as a result, we end up making the second tree, which is the "right associative" one, because the recursive descent happens on the right half
	 * unfortunately, while this tree produces the correct result when evaluated, we need the "left associative" tree for some bizarre reason
	 * this function converts the second (wrong) tree to the first (correct) one
	*/
	private static void __fixAssociativity(BinaryNode n) {
		// I'M BURNING THROUGH THE SKY, YEAH, 200 DEGREES THAT'S WHY THEY CALL ME MR FAHRRENHEIT
		if (n == null || (n.left == null && n.right == null)) {
			return;
		}
		if (
				(isAddop(n.mid) && isId(n.left.mid) && isAddop(n.right.mid)) ||
				(isMulop(n.mid) && isId(n.left.mid) && isMulop(n.right.mid))
		) {
			String tmp = n.left.mid;
			n.left = new BinaryNode(n.mid);
			n.left.left = new BinaryNode(tmp);
			n.left.right = n.right.left;
			n.mid = n.right.mid;
			n.right = n.right.right;
		}

		if (
				(isAddop(n.mid) && isAddop(n.left.mid) && isAddop(n.right.mid)) ||
				(isMulop(n.mid) && isMulop(n.left.mid) && isMulop(n.right.mid))
		) {
			String tmp = n.mid;
			BinaryNode tmp2 = n.right.left;
			n.mid = n.right.mid;
			n.right = n.right.right;
			BinaryNode tmp3 = n.left;
			n.left = new BinaryNode(tmp3, tmp, tmp2);
		}

		__fixAssociativity(n.left);
		__fixAssociativity(n.right);
	}

	// this is the much easier postfix parser
	private boolean __buildFromPostfix(String postfix) {
		TokStream stream = new TokStream(postfix);
		Stack<BinaryNode> input = new Stack<>();
		// while we have tokens
		while (!stream.isEmpty()) {
			String next = stream.extract();
			// push any identifiers on to the stack
			if (next.matches("^\\w+$")) {
				input.push(new FactorNode(next));
			}
			// pop 2 entries, push our + expression
			else if (next.equals("+") || next.equals("-")) {
				if (input.size() < 2) {
					throw new IllegalStateException("Stack too short for \"+\"");
				}
				BinaryNode i2 = input.pop();
				BinaryNode i1 = input.pop();

				input.push(combinePlus(i1, next, i2));
			}
			// pop 2 entries, push our * expression
			else if (next.equals("*") || next.equals("/")) {
				if (input.size() < 2) {
					throw new IllegalStateException("Stack too short for \"+\"");
				}
				BinaryNode i2 = input.pop();
				BinaryNode i1 = input.pop();

				input.push(combineMult(i1, next, i2));
			}
			else {
				throw new IllegalStateException("Unexpected token \"" + next + "\"");
			}
		}
		this.root = makeExpr(input.peek());
		return true;
	}

	// --------------- public interface starts here ----------------

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
		if (!__buildFromPostfix(postfix)) {
			return false;
		}
		__generalize();
		__condense();
		return true;
	}

	public boolean buildFromInfix(String infix) {
		String s = infixToPostfix(infix);
		return buildFromPostfix(infixToPostfix(infix));

		/*
		TokStream stream = new TokStream(infix);
		this.root = __infixExpr(stream);
		if (!stream.isEmpty()) {
			throw new IllegalStateException("Trailing tokens");
		}
		if (this.root == null) {
			return false;
		}
		__generalize();
		__condense();
		__condense();
		__fixAssociativity(this.root);
		return true;
		*/
	}

	public void printInfixExpression() {
		if (this.root != null) {
			printInfixExpression(this.root);
			System.out.println();
		}
		else {
			System.out.println("Empty");
		}
	}

	public void printPostfixExpression() {
		if (this.root != null) {
			printPostfixExpression(this.root);
			System.out.println();
		}
		else {
			System.out.println("Empty");
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
		if (n.mid != null && isMulop(n.mid) && n.left != null && isAddop(n.left.mid)) {
			System.out.print("( ");
			printInfixExpression(n.left);
			System.out.print(") ");
		}
		else {
			printInfixExpression(n.left);
		}
		if (n.mid != null) {
			System.out.print(n.mid + " ");
		}
		if (n.mid != null && isMulop(n.mid) && n.right != null && n.right.mid != null && isAddop(n.right.mid)) {
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
		return 1 + size(n.left) + size(n.right);
	}

	private static int leafNodes(BinaryNode n) {
		if (n == null) {
			return 0;
		}
		if (n.left == null && n.right == null) {
			return 1;
		}
		return leafNodes(n.left) + leafNodes(n.right);
	}
}
