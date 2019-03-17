public class Main {
	public static void main(String[] args) {
		BET t = new BET();
		t.buildFromInfix("((5+2) - (8-3))/4");
		t.printInfixExpression();
		t.printPostfixExpression();
		System.out.println();

		try {
			System.out.println("\n\ntest1: a b c + * d -");
			BET test = new BET("a b c + * d -" , 'p');
			System.out.print("postfix: ");
			test.printPostfixExpression();
			System.out.print("infix: ");
			test.printInfixExpression();
			System.out.print("size: ");
			System.out.println(test.size());
			System.out.print("isEmpty: ");
			System.out.println(test.isEmpty());
			System.out.print("# of leaves: ");
			System.out.println(test.leafNodes());
			System.out.println("\n\ntest2: ( 3 + 2 ) * 3 + 1");
			test = new BET("( 3 + 2 ) * 3 + 1" , 'i');
			System.out.print("postfix: ");
			test.printPostfixExpression();
			System.out.print("infix: ");
			test.printInfixExpression();
			System.out.print("size: ");
			System.out.println(test.size());
			System.out.print("isEmpty: ");
			System.out.println(test.isEmpty());
			System.out.print("# of leaves: ");
			System.out.println(test.leafNodes());
			System.out.println("\n\ntest3: abc / 2 / f3 + z4 - 1 * 2");
			test.buildFromInfix("abc / 2 / f3 + z4 - 1 * 2");
			System.out.print("postfix: ");
			test.printPostfixExpression();
			System.out.print("infix: ");
			test.printInfixExpression();
			System.out.print("size: ");
			System.out.println(test.size());
			System.out.print("isEmpty: ");
			System.out.println(test.isEmpty());
			System.out.print("# of leaves: ");
			System.out.println(test.leafNodes());
			System.out.println("\n\ntest4: ( 3 + 2 * 3 + 1");
			test = new BET("( 3 + 2 * 3 + 1" , 'i');
			System.out.print("postfix: ");
			test.printPostfixExpression();
			System.out.print("infix: ");
			test.printInfixExpression();
			System.out.print("size: ");
			System.out.println(test.size());
			System.out.print("isEmpty: ");
			System.out.println(test.isEmpty());
			System.out.print("# of leaves: ");
			System.out.println(test.leafNodes());
		}
		catch(IllegalStateException e) {
			System.out.println(e.getMessage());
		}
	}
}
