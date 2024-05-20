package tool;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import weka.core.Instances;
import weka.core.converters.ArffLoader;
import parsing.IssueTrackerProcessing;
import parsing.JavaParsing;
import parsing.JiraMiner;

@Command(name = "DebtHunter", mixinStandardHelpOptions = true, version = "DebtHunter 1.0.0",
description = "DebtHunter detect the Self-Admitted in your project or in issue tracker.")
public class DebtHunterTool implements Runnable {
	
	@Option(names = {"-u", "--usecase"}, description = "first, second.")
    private String useCase = "second";
	
	// Java source code test set
	@Option(names = {"-p", "--project"}, description = "The java project path.")
    private String projectPath = "";
	
	// Jira test set
	@Option(names = {"-pv", "--projectversion"}, description = "The Jira project version to analyse.")
    private String projectversion = "";
	@Option(names = {"-jp", "--jiraproject"}, description = "The Jira project name to analyse.")
    private String jiraproject = "";
	@Option(names = {"-c", "--component"}, description = "The Jira component to analyse.")
    private String component = "";
	@Option(names = {"-bu", "--baseUrl"}, description = "The Jira project base URL to analyse.")
    private String baseurl = "";
	
	@Option(names = {"-m1", "--model1"}, description = "The path of your binary model.")
    private String firstModelPath = "";
	@Option(names = {"-m2", "--model2"}, description = "The path of your multi-class model.")
    private String secondModelPath = "";
	
	//training set
    @Option(names = {"-l", "--labeled"}, description = "The path of your labeled data.")
    private String LabeledPath = "";

    // output directory
    @Option(names = {"-o", "--output"}, description = "The path where the DebtHunt outputs are saved.")
    private String outputPath = "";

	@Override
	public void run() {
		
		if(useCase.equals("")) {
			System.out.println("Please, select the use case you want to use.");
		    System.exit(0);
		}
		
		if(outputPath.equals("")) {
			System.out.println("Please, indicate the outputs directory.");
		    System.exit(1);
		}
		
		
		System.out.println("Let's start!");
		
		
		if (useCase.equals("first")) {
			
			System.out.println("You selected the first use case!");
			Instances test = null;
			
			// if the input data is java files
			if (StringUtils.isNotEmpty(projectPath)) {
				try {
					test = JavaParsing.processDirectory(projectPath, outputPath);
				} catch (Exception e) {
					e.printStackTrace();
				}
			
			// if the input data is issue tracker
			} else if (StringUtils.isNotEmpty(jiraproject) && StringUtils.isNotEmpty(baseurl) && StringUtils.isNotEmpty(projectversion) && StringUtils.isNotEmpty(component)) {
				try {
					JiraMiner.downloadIssuesAffectingVersion(baseurl, outputPath, jiraproject, projectversion, component);
					String projectName = jiraproject + "-" + projectversion;
					test = IssueTrackerProcessing.processDirectory(outputPath + "/" + projectversion, outputPath, projectName);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("You miss to pass me some information.");
			    System.exit(2);
			}

					
			// remove not useful columns (i.e. project name, package name and top package)
			Instances onlyComments = test;
			while (onlyComments.numAttributes() > 1) {
				try {
					onlyComments = DataHandler.removeAttribute(onlyComments, "first");
					onlyComments = DataHandler.removeAttribute(onlyComments, "1,2,4,5,6,7");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// if the user chooses the first use case and not provides a pre-trained model
			if((StringUtils.isEmpty(firstModelPath)) && (StringUtils.isEmpty(secondModelPath))) {
				try {
					test = UseCaseOne.debtHunterLabeling(onlyComments, test);
				} catch (Exception e) {
					e.printStackTrace();
				}
			
			// The user provides 2 pre-trained models in input
			} else if((StringUtils.isNotEmpty(firstModelPath)) && (StringUtils.isNotEmpty(secondModelPath))) {
				try {
					test = UseCaseOne.userClassifierLabeling(onlyComments, test, firstModelPath, secondModelPath);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			} else if(((StringUtils.isEmpty(firstModelPath)) && (StringUtils.isNotEmpty(secondModelPath))) || ((StringUtils.isNotEmpty(firstModelPath)) && (StringUtils.isEmpty(secondModelPath)))) {
				System.out.println("You not provided one of two pre-trained models.");
				System.exit(3);
			}
			
			
			// save labeled comments
			try {
				DataHandler.saveData(test, "commentsLabeled", outputPath);
			} catch (IOException e) {
				e.printStackTrace();
			}

		
			
			
		// The user provides labeled data (training set).
		} else if (useCase.equals("second")) {
			
			if (LabeledPath.equals("")) {
				System.out.println("Please, indicate the labeled data directory.");
			    System.exit(4);
			}
			
			System.out.println("You selected the second use case!");

			String endPath = LabeledPath.substring(1);
			
			ArffLoader loader = new ArffLoader();
			Instances training = null;
			try {
				loader.setSource(new File(endPath));
				training = loader.getDataSet();
			} catch (IOException e) {
				e.printStackTrace();
			}

			
			// remove the not useful columns (i.e. project name, package name and top package or only project name)
			while (training.numAttributes() > 2) {
				try {
					training = DataHandler.removeAttribute(training, "first");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			training.setClassIndex(training.numAttributes() - 1);

			System.out.println("The training data is ok!");
			
			Instances multiTraining = null;
			Instances binTraining = null;
			
			try {
				// binarization
				binTraining = DataHandler.binarization(training);

				// remove non-SATD instances
				multiTraining = DataHandler.removeClass(training);
				UseCaseTwo.trainModel(binTraining, multiTraining, LabeledPath, outputPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		int exitCode = new CommandLine(new DebtHunterTool()).execute(args);
        System.exit(exitCode);
			
		
	}

}
