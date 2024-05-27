package utils;

import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class Utils {

	// public static String getQualifiedName(CompilationUnit cu, ClassOrInterfaceDeclaration ci, String package_declaration) {

	// 	StringBuilder sb = new StringBuilder();
	// 	sb.append(package_declaration+".");
	// 	if(!ci.isNestedType())
	// 		return sb.toString()+ci.getNameAsString();

	// 	Optional<ClassOrInterfaceDeclaration> ancestor = ci.getAncestorOfType(ClassOrInterfaceDeclaration.class);
	// 	while(ancestor.isPresent()){
	// 		sb.append(ancestor.get().getNameAsString()+"#");
	// 		ancestor = ancestor.get().getAncestorOfType(ClassOrInterfaceDeclaration.class);
	// 	}

	// 	sb.append(ci.getNameAsString());

	// 	return sb.toString();
	// }

	public static class MethodCallVisitor extends VoidVisitorAdapter<Void> {

		StringBuilder sb;

		public MethodCallVisitor(StringBuilder s){
			sb = s;
		}

		@Override
		public void visit(MethodCallExpr n, Void arg) {
			// Found a method call
			if(n.getScope().isPresent())
				//	    		sb.append(n.getScope().get() + "." + n.getName()+" ");
				sb.append(n.getName()+" ");
			// Don't forget to call super, it may find more method calls inside the arguments of this method call, for example.
			super.visit(n, arg);
		}
	}

	public static String splitCamelCase(String s) {
		return s.replaceAll(
				String.format("%s|%s|%s",
						"(?<=[A-Z])(?=[A-Z][a-z])",
						"(?<=[^A-Z])(?=[A-Z])",
						"(?<=[A-Za-z])(?=[^A-Za-z])"
						),
				" "
				);
	}



}
