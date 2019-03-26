import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// lexer / token stream class
class TokStream {
	// the tokens
	private LinkedList<String> lexemes;

	public TokStream(String input) {
		// splits the input stream into identifiers and operators (tokens)
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
			// if the string starts with an alphanumeric substring or punctuation
			m = Pattern.compile("^(\\w+|[+\\-*/()]).*$").matcher(input);
			if (m.find()) {
				// we read an id
				lexemes.addLast(m.group(1));
				input = input.substring(m.group(1).length());
				continue;
			}
			/*
			// if the string is punctuation
			m = Pattern.compile("^([+\\-/*()]).*$").matcher(input);
			if (m.find()) {
				// we read punctuation
				lexemes.addLast(m.group(1));
				input = input.substring(m.group(1).length());
				continue;
			}
			*/
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
	private static boolean isAddop(String s) {
		return s != null && s.matches("^[+\\-]$");
	}

	// returns true if s is a "*" or "/"
	private static boolean isMulop(String s) {
		return s != null && s.matches("^[*/]$");
	}

	// returns true if s is an identifier
	private static boolean isId(String s) {
		return s != null && s.matches("^\\w+$");
	}

	// standard binary node class
	private static class BinaryNode {
		// getters and setters are for chumps
		public BinaryNode left;
		public String mid;
		public BinaryNode right;

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

	// the root of the tree
	private BinaryNode root;

	/*
	 * Making an infix parser is hard
	 * Making a *left associative* infix parser is next to impossible
	 *
	 * Why not just convert it to postfix, which is much easier to parse?
	 */
	private static String __infixToPostfix(String infix) {
		// uses a modified version of the shunting yard algorithm
		TokStream stream = new TokStream(infix);
		Stack<String> ops = new Stack<>();
		Queue<String> out = new LinkedList<>();

		while (!stream.isEmpty()) {
			String next = stream.extract();
			if (isId(next)) {
				out.add(next);
			}
			else if (isAddop(next)) {
				// must also pop addops so the resulting postfix is left associative
				while (!ops.empty() && !ops.peek().equals("(")) {
					out.add(ops.pop());
				}
				ops.push(next);
			}
			else if (isMulop(next)) {
				// pop only mulops, because addops need to go after
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
					throw new IllegalStateException("Mismatched parentheses (unexpected \")\")");
				}
				// find the matching "("
				while (!ops.peek().equals("(")) {
					out.add(ops.pop());
					if (ops.empty()) {
						throw new IllegalStateException("Mismatched parentheses (unexpected \")\")");
					}
				}
				ops.pop();
			}
			else {
				throw new IllegalStateException("Unexpected \"" + next + "\"");
			}
		}
		while (!ops.isEmpty()) {
			if (ops.peek().equals("(")) {
				throw new IllegalStateException("Mismatched parentheses (missing \")\")");
			}
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

	// this is the much easier postfix parser
	private boolean __buildFromPostfix(String postfix) {
		TokStream stream = new TokStream(postfix);
		Stack<BinaryNode> input = new Stack<>();
		// while we have tokens
		while (!stream.isEmpty()) {
			String next = stream.extract();
			// push any identifiers on to the stack
			if (isId(next)) {
				input.push(new BinaryNode(next));
			}
			// pop 2 entries, push our + expression
			else if (isAddop(next)) {
				if (input.size() < 2) {
					throw new IllegalStateException("Stack too short for \"+\"");
				}
				BinaryNode i2 = input.pop();
				BinaryNode i1 = input.pop();

				// put the two on either side of the [+-]
				input.push(new BinaryNode(i1, next, i2));
			}
			// pop 2 entries, push our * expression
			else if (isMulop(next)) {
				if (input.size() < 2) {
					throw new IllegalStateException("Stack too short for \"+\"");
				}
				BinaryNode i2 = input.pop();
				BinaryNode i1 = input.pop();

				// put the two on either side of the [*/]
				input.push(new BinaryNode(i1, next, i2));
			}
			else {
				throw new IllegalStateException("Unexpected token \"" + next + "\"");
			}
		}
		if (input.size() > 1) {
			throw new IllegalStateException("Missing operator");
		}
		this.root = input.peek();
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
		return __buildFromPostfix(postfix);
	}

	public boolean buildFromInfix(String infix) {
		return buildFromPostfix(__infixToPostfix(infix));
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
		if (n.left != null || n.right != null) {
			System.out.print("( ");
		}
		printInfixExpression(n.left);
		if (n.left != null) {
			System.out.print(" ");
		}
		System.out.print(n.mid);
		if (n.right != null) {
			System.out.print(" ");
		}
		printInfixExpression(n.right);
		if (n.left != null || n.right != null) {
			System.out.print(" )");
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
