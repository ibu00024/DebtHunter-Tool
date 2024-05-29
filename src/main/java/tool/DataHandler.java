package tool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

// import org.apache.commons.csv.CSVFormat;
// import org.apache.commons.csv.CSVPrinter;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemoveWithValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DataHandler {
	
	public static String cleanComment (String comment) {
		
		String regex = "\\b([a-zA-Z][\\w_]*(\\.[a-zA-Z][\\w_]*)+)\\(\\)";
		
		comment.replaceAll(regex, "JavaClassSATD").replaceAll("[0-9]", " ")
		.replaceAll("`", "\'")
		.replaceAll("won\'\\s*t", "will not").replaceAll("let\'\\s*s", "let us") // irregular contractions
		.replaceAll("n\'\\s*t", " not") //all "not" contractions
		.replaceAll("\'\\s*ll", " will").replaceAll("\'\\s*ve", " have").replaceAll("\'\\s*m", " am").replaceAll("\'\\s*re", " are").replaceAll("\'\\s*s", " is")  //most used contractions
		.replaceAll("gotta", "have got to").replaceAll("kinda", "kind of").replaceAll("wanna", "want to").replaceAll("gimme", "give me").replaceAll("lotta", "lot of") // slang
		.replaceAll("lemme", "let me").replaceAll("dunno", "do not know").replaceAll("prolly", "probably") // slang
		.replaceAll("\\p{Punct}", " ")
		.replaceAll("\\s+", " ");
		
		return comment;
    }
	
    // エスケープがうまくいかないので，jsonで出力する
    // public static void saveData(Instances data, String dataName, String path) throws IOException {
    //     // Save as ARFF
    //     ArffSaver saver = new ArffSaver();
    //     saver.setInstances(data);
    //     saver.setFile(new File(path + "/" + dataName + ".arff"));
    //     saver.writeBatch();

    //     // Save as CSV
    //     BufferedWriter writer = new BufferedWriter(new FileWriter(path + "/" + dataName + ".csv"));
    //     CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
    //             .withQuoteMode(org.apache.commons.csv.QuoteMode.ALL)
    //             .withEscape('\\')
    //             .withRecordSeparator(System.lineSeparator()));

    //     // Write header
    //     for (int i = 0; i < data.numAttributes(); i++) {
    //         csvPrinter.print(data.attribute(i).name());
    //     }
    //     csvPrinter.println();

    //     // Write data
    //     for (int i = 0; i < data.numInstances(); i++) {
    //         for (int j = 0; j < data.numAttributes(); j++) {
    //             String value = data.instance(i).toString(j);
    //             if (value == null || value.isEmpty() || value.equals("''")) {
    //                 csvPrinter.print("");
    //             } else {
    //                 // どこかに''で囲む処理があるので，それを削除して出力するようにする．根本的な解決策ではないので，要修正
    //                 if (value.startsWith("'") && value.endsWith("'")) {
    //                     value = value.substring(1, value.length() - 1);
    //                 }
	// 				csvPrinter.print(value.replace("\n", "\\n").replace("\r", "\\r"));
    //             }
    //         }
    //         csvPrinter.println();
    //     }

    //     csvPrinter.flush();
    //     csvPrinter.close();
    //     writer.close();
    // }

    // シングルクォートで囲まれている値を削除するメソッド
    public static String removeSingleQuotes(String value) {
        if (value != null && value.length() > 1 && value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    // エスケープシーケンスを削除するメソッド
    public static String unescapeString(String value) {
        if (value != null) {
            value = value.replace("\\'", "'").replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return value;
    }

    // Required full path
    public static void saveData(Instances data, String dataName, String path) throws IOException {
        // Save as ARFF
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(path + "/" + dataName + ".arff"));
        saver.writeBatch();

        // Save as JSON
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode rootNode = mapper.createArrayNode();

        for (int i = 0; i < data.numInstances(); i++) {
            ObjectNode instanceNode = mapper.createObjectNode();
            for (int j = 0; j < data.numAttributes(); j++) {
                String attributeName = data.attribute(j).name();
                String value = data.instance(i).toString(j);
                value = removeSingleQuotes(value);
                value = unescapeString(value);
                instanceNode.put(attributeName, value);
            }
            rootNode.add(instanceNode);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path + "/" + dataName + ".json"))) {
            writer.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));
        }
    }
	
	public static Instances binarization(Instances data) throws Exception {
		
		// create new feature
		Add add = new Add();
		add.setAttributeIndex("last");
		add.setNominalLabels("SATD, WITHOUT_CLASSIFICATION");
		add.setAttributeName("BinaryClassification");
		add.setInputFormat(data);
		data = Filter.useFilter(data, add);
		

		// set BinaryClassification value into "SATD" if the label is one of {TEST,IMPLEMENTATION,DESIGN,DEFECT,DOCUMENTATION}
		for (int i = 0; i < data.numInstances(); i++) {

			// get original class
			Double SATDclass = data.instance(i).value(1);
			
			if (SATDclass == 2.0) {
				data.instance(i).setValue(data.numAttributes() - 1, "WITHOUT_CLASSIFICATION");
			} else {
				data.instance(i).setValue(data.numAttributes() - 1, "SATD");
			}
		}

		
		// select the BinaryClassification attribute as a class target and not consider
		// Classification attribute
		Remove remove = new Remove();
		remove.setAttributeIndices("2");
		remove.setInputFormat(data);
		data = Filter.useFilter(data, remove);
			
		data.setClassIndex(data.numAttributes() - 1);
		
		return data;
	}
	
	public static Instances removeClass(Instances data) throws Exception {

		RemoveWithValues rm = new RemoveWithValues();
		rm.setInputFormat(data);
		String[] options = { "-C", "2", "-L", "3", "-H" }; // 2: 2nd column is the attribute for type of SATD.
															// 3:WITHOUT_CLASSIFICATION is the 3rd class
		rm.setOptions(options);
		data = Filter.useFilter(data, rm);

		return data;
	}
	
	public static Instances removeAttribute(Instances data, String index) throws Exception {
		
		Remove rm = new Remove();
		rm.setAttributeIndices(index);
		rm.setInputFormat(data);
		Instances newData = Filter.useFilter(data, rm);
		
		return newData;
	}
	
	// delete the piece of string that starts from index "start" and finish to "end"
	public static String pathModifier(String path, int start, int end) {
		
		StringBuilder builder = new StringBuilder(path);
		builder.delete(start, end);
		String newPath = builder.toString();
		
		return newPath;
		
	}

	
}
