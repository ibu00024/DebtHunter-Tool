package parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import tool.DataHandler;
import utils.DirectoryIterator;
import utils.FileIterator;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;


public class JavaParsing {

	private static Map<String, List<CommentInfo>> parseClass(InputStream filePath, String fileName) throws IOException {
		CompilationUnit cu = null;
		Map<String, List<CommentInfo>> elements = new HashMap<>();
		
		try {
			ParserConfiguration configuration = new ParserConfiguration();
			configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
			configuration.setDoNotAssignCommentsPrecedingEmptyLines(false);
			JavaParser javaParser = new JavaParser(configuration);
			ParseResult<CompilationUnit> result = javaParser.parse(filePath);
	
			if (result.isSuccessful() && result.getResult().isPresent()) {
				cu = result.getResult().get();
			}
	
		} catch (ParseProblemException e) {
			System.out.println("PARSING ERROR!");
		}
	
		if (cu == null) return elements;
	
		String packageName = cu.getPackageDeclaration()
			.map(PackageDeclaration::getNameAsString)
			.orElse("default");
	
		MethodVisitor methodVisitor = new MethodVisitor();
		cu.accept(methodVisitor, null);
	
		List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
		for (ClassOrInterfaceDeclaration ci : classes) {
			String className = packageName + "." + ci.getName().asString();
			List<CommentInfo> comments = ci.getAllContainedComments().stream().map(c -> {
				int beginLineNumber = c.getBegin().map(p -> p.line).orElse(-1);
				int endLineNumber = c.getEnd().map(p -> p.line).orElse(-1);
				String methodName = methodVisitor.getMethodNameForLine(beginLineNumber);
				return new CommentInfo(c.getContent().trim().replaceAll("//|\\*", "").replaceAll("\\s+", " "), beginLineNumber, endLineNumber, methodName, fileName);
			}).collect(Collectors.toList());
	
			elements.put(className, comments);
		}
		return elements;
	}
	

	static class MethodVisitor extends VoidVisitorAdapter<Void> {
		private List<MethodRange> methodRanges = new ArrayList<>();
	
		@Override
		public void visit(MethodDeclaration md, Void arg) {
			super.visit(md, arg);
			int beginLine = md.getBegin().map(p -> p.line).orElse(-1);
			int endLine = md.getEnd().map(p -> p.line).orElse(-1);
			String methodName = md.getNameAsString();
			methodRanges.add(new MethodRange(beginLine, endLine, methodName));
		}
	
		// TODO: コメントが所属するメソッドのアルゴリズムは変更する
		public String getMethodNameForLine(int lineNumber) {
			for (MethodRange range : methodRanges) {
				if (lineNumber >= range.getBeginLine() && lineNumber <= range.getEndLine()) {
					return range.getMethodName();
				}
			}
			return "";
		}
	
		static class MethodRange {
			private int beginLine;
			private int endLine;
			private String methodName;
	
			public MethodRange(int beginLine, int endLine, String methodName) {
				this.beginLine = beginLine;
				this.endLine = endLine;
				this.methodName = methodName;
			}
	
			public int getBeginLine() {
				return beginLine;
			}
	
			public int getEndLine() {
				return endLine;
			}
	
			public String getMethodName() {
				return methodName;
			}
		}
	}
	

    public static Instances processDirectory(String projectPath, String outputPath) throws Exception {
        Boolean firstTime = true;

        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("projectname", (ArrayList<String>) null));
        attributes.add(new Attribute("package", (ArrayList<String>) null));
        attributes.add(new Attribute("top_package", (ArrayList<String>) null));
        attributes.add(new Attribute("comment", (ArrayList<String>) null));
        attributes.add(new Attribute("fileName", (ArrayList<String>) null));
        attributes.add(new Attribute("methodName", (ArrayList<String>) null));
        attributes.add(new Attribute("beginLine", (ArrayList<String>) null));
        attributes.add(new Attribute("endLine", (ArrayList<String>) null));
        Instances data = new Instances("comments", attributes, 1);

		File f = new File(projectPath);
		for (File ff : f.listFiles()) {
			String full_path = null;
			if (ff.isDirectory())
				full_path = ff.getAbsolutePath();
			else if (ff.isFile() && (ff.getName().endsWith(".jar") || ff.getName().endsWith(".zip") || ff.getName().endsWith("tag.gz")))
				full_path = ff.getAbsolutePath().substring(0, ff.getAbsolutePath().lastIndexOf("."));

			if (full_path == null) continue;

			data = saveComments(full_path, projectPath, outputPath, firstTime, data);
            firstTime = false;
		}

        return data;
    }
	
	private static Instances saveComments(String full_path, String path, String outputPath, Boolean firstTime, Instances data) throws Exception {
		System.out.println("Getting comments... " + full_path + " " + new Date());
	
		FileIterator it = FileIterator.getIterator(full_path);
	
		Map<String, List<CommentInfo>> comments = new HashMap<>();
		Set<String> packages = new HashSet<>();
		InputStream clas = it.nextStream();
		while (clas != null) {
			String fileName = ((DirectoryIterator) it).getCurrentFileName();
	
			Map<String, List<CommentInfo>> aux = parseClass(clas, fileName);
			if (aux.size() > 0) {
				comments.putAll(aux);
				String cc = findClass(aux.keySet());
				if (cc != null && cc.contains(".")) packages.add(cc.substring(0, cc.lastIndexOf(".")));
			}
	
			clas = it.nextStream();
		}
	
		if (firstTime) {
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath + "/comments.csv"));
	
			Set<String> top_levels = getTopLevelPackages(packages);
	
			CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("projectname", "package", "top_package", "comment", "fileName", "methodName", "beginLine", "endLine")
					.withQuoteMode(QuoteMode.ALL)
					.withEscape('\\')
					.withRecordSeparator(System.lineSeparator()));
			String projectname = new File(full_path).getName();
			for (String cla : comments.keySet()) {
				String pack = cla.contains(".") ? cla.substring(0, cla.lastIndexOf(".")) : cla;
				String top = getTop(top_levels, pack);
	
				List<CommentInfo> comms = comments.get(cla);
				for (CommentInfo ci : comms) {
					csvPrinter.printRecord(projectname, pack, top, ci.getComment(), ci.getFileName(), ci.getMethodName(), ci.getBeginLineNumber(), ci.getEndLineNumber());
					String c = DataHandler.cleanComment(ci.getComment());
	
					if (c.contentEquals(" ")) continue;
	
					Instance inst = new DenseInstance(data.numAttributes());
					inst.setDataset(data);
					inst.setValue(0, projectname);
					inst.setValue(1, pack);
					inst.setValue(2, top);
					inst.setValue(3, c);
					inst.setValue(4, ci.getFileName());
					inst.setValue(5, ci.getMethodName());
					inst.setValue(6, Integer.toString(ci.getBeginLineNumber())); // Convert int to String
					inst.setValue(7, Integer.toString(ci.getEndLineNumber()));
					data.add(inst);
				}
			}
	
			csvPrinter.flush();
			csvPrinter.close();
	
			return data;
	
		} else {
			FileWriter csv = new FileWriter(outputPath + "/comments.csv", true);
			BufferedWriter writer = new BufferedWriter(csv);
	
			Set<String> top_levels = getTopLevelPackages(packages);
	
			CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withQuoteMode(QuoteMode.ALL)
					.withEscape('\\')
					.withRecordSeparator(System.lineSeparator()));
	
			String projectname = new File(full_path).getName();
			for (String cla : comments.keySet()) {
				String pack = cla.contains(".") ? cla.substring(0, cla.lastIndexOf(".")) : cla;
				String top = getTop(top_levels, pack);
	
				List<CommentInfo> comms = comments.get(cla);
				for (CommentInfo ci : comms) {
					csvPrinter.printRecord(projectname, pack, top, ci.getComment(), ci.getFileName(), ci.getMethodName(), ci.getBeginLineNumber(), ci.getEndLineNumber());
	
					String c = DataHandler.cleanComment(ci.getComment());
	
					if (c.contentEquals(" ")) continue;
	
					Instance inst = new DenseInstance(data.numAttributes());
					inst.setDataset(data);
					inst.setValue(0, projectname);
					inst.setValue(1, pack);
					inst.setValue(2, top);
					inst.setValue(3, c);
					inst.setValue(4, ci.getFileName());
					inst.setValue(5, ci.getMethodName());
					inst.setValue(6, Integer.toString(ci.getBeginLineNumber())); // Convert int to String
					inst.setValue(7, Integer.toString(ci.getEndLineNumber()));
					data.add(inst);
				}
			}
	
			csvPrinter.flush();
			csvPrinter.close();
	
			writer.close();
			return data;
		}
	}
	
	private static String getTop(Set<String> tops, String p) {
		String top = "";
		for(String t : tops)
			if(p.startsWith(t)){
				if(t.length() >= top.length())
					top = t;
			}
	
		return top;
	}

	private static String findClass(Set<String> keySet) {
		for(String k : keySet)
			if(!k.contains("#") && !k.contains("$"))
				return k;
		return "";
	}

	public static Set<String> getTopLevelPackages(Set<String> allPackage) {
		
		String highLevelRoot = org.apache.commons.lang3.StringUtils.getCommonPrefix(allPackage.toArray(new String[]{}));
		
		if(highLevelRoot.endsWith("."))
			highLevelRoot = highLevelRoot.substring(0, highLevelRoot.length()-1);

		if(highLevelRoot.length() > 0){
			Set<String> naive_packages = getNaivePackages(highLevelRoot,allPackage);
			if(highLevelRoot.contains("."))
				return naive_packages;
			
			Set<String> aux_packages = new HashSet<>();
			for(String np : naive_packages){
				
				Set<String> aux = getNaivePackages(np, allPackage);
					
				if(aux.size() == 1)
					aux_packages.addAll(aux);
				else
					aux_packages.add(np);
				
			}

			Set<String> packages = new HashSet<>();
			if(aux_packages.size() > 0){
				for(String ap : aux_packages){
					System.out.println(ap+" -- "+getNaivePackages(ap, allPackage));
					packages.addAll(getNaivePackages(ap, allPackage));
				}
					
				return packages;
			}
		}
		
		//we have no common root - we need to find the potential commons!
		Set<String> packages = new HashSet<>();
		Map<String,Set<String>> potentialRoots = new HashMap<>();
		for(String a : allPackage){
			int index = a.indexOf(".");
			String a_edited = a;
			if(index > 0)
				a_edited = a.substring(0,index);

			Set<String> aux = potentialRoots.get(a_edited);
			if(aux == null){
				aux = new HashSet<>();
				potentialRoots.put(a_edited, aux);
			}

			aux.add(a);
		}
			
		for(String pr : potentialRoots.keySet()){
			
//			highLevelRoot = org.apache.commons.lang3.StringUtils.getCommonPrefix(potentialRoots.get(pr).toArray(new String[]{}));
//			packages.addAll(getNaivePackages(highLevelRoot,potentialRoots.get(pr)));	
			packages.addAll(getTopLevelPackages(potentialRoots.get(pr)));
//			System.out.println(pr+" :: "+packages);
		}
		
		return packages;
	}

	private static Set<String> getNaivePackages(String topLevel, Set<String> packagesToSearch) {
		Set<String> packages = new HashSet<>();
				
		if(topLevel.endsWith("."))
			topLevel = topLevel.substring(0,topLevel.length()-1);
		
		for(String a : packagesToSearch){ //for every package we got here

			if(!a.startsWith(topLevel))
				continue;
			
			a = a.replace(topLevel, "");
		
			if(a.length() == 0)
				packages.add(topLevel);
			else{
				if(a.startsWith("."))
					a = a.substring(1);
					
				int index = a.indexOf(".");
				if(index > 0)
					a = a.substring(0,index);
					
				if(!a.contains("impl."))
					if(topLevel.length() > 0)
						packages.add(topLevel+"."+a);
					else
						packages.add(a);
				
				else
					packages.add(topLevel); //no need to check it, if there was a parent, we may have already added it
			
			}

		}
		return packages;
	}
	
	public static void main(String[] args) throws Exception {
		
		String path = args[0];
		String outputPath = args[1];
		Instances data = processDirectory(path, outputPath);
		System.out.println(data);

	}

}
