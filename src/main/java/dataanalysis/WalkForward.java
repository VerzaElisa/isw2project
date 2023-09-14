package dataanalysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.javatuples.Pair;

import com.opencsv.exceptions.CsvValidationException;

import datamining.CsvCreator;
import datamining.Utility;

import weka.core.Instances;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;

public class WalkForward {

    private static final Logger LOGGER = Logger.getLogger(WalkForward.class.getName());
    
    private static final String CSV_JIRA = "02-ticketdata.csv";
    private static final String CSV_METHRICS = "04-data.csv";
    private static final String CSV_TRAINING = "training.csv";
    private static final String CSV_TESTING = "testing.csv";
    private static final String CSV_FINAL = "finalresult.csv";
    private static final String ARFF_TRAINING = "training.arff";
    private static final String ARFF_TESTING = "testing.arff";
    private static final String PRJ_NAME = "Bookkeeper";
    private static String selection = "";
    private static final Double CFP = 1d;
    private static final Double CFN = 1d;
    private static final String SAMPLING_TYPE = "smote";
    private static final String SM = "smote";

    private static final boolean FS = true;
    private static final boolean SAMPLING = false;
    private static final boolean COST_SENSITIVE = true;

    public static  List<Pair<List<String>, String>> versionSplit() throws CsvValidationException, IOException{
        Integer j;
        Integer i;
        List<String> versions = new ArrayList<>();
        List<List<String>> files = Utility.csvToList(CSV_METHRICS);
        List<Pair<List<String>, String>> iteration = new ArrayList<>();

        for(j=0; j<files.size(); j++){
            if(!versions.contains(files.get(j).get(0))){
                versions.add(files.get(j).get(0));
            }
        }

        for(i=2; i<versions.size(); i++){
            String testing = versions.get(i);
            List<String> training = new ArrayList<>();
            for(j=1; j<i; j++){
                training.add(versions.get(j));
            } 
            iteration.add(new Pair<>(training, testing));
        }
        return iteration;
    }

    public static List<Float> defectiveness() throws CsvValidationException, IOException{
        List<List<String>> training = Utility.csvToList(CSV_TRAINING);
        List<List<String>> testing = Utility.csvToList(CSV_TESTING);
        Integer columnNum = training.get(0).size()-1;
        Integer i;
        List<Float> defectiveNum = Arrays.asList(0f, 0f);

        for(i=0; i<training.size(); i++){
            if(training.get(i).get(columnNum).equals("YES")){
                defectiveNum.set(0, defectiveNum.get(0)+1);
            }  
        }

        for(i=0; i<testing.size(); i++){
            if(testing.get(i).get(columnNum).equals("YES")){
                defectiveNum.set(1, defectiveNum.get(1)+1);
            }  
        }

        return defectiveNum;
    }

    public static List<String> filesFromVersion(String version) throws CsvValidationException, IOException{
        Integer i=0;
        Integer j;
        List<String> csvLines = new ArrayList<>();
        List<List<String>> files = Utility.csvToList(CSV_METHRICS);

        while(i<files.size() && !files.get(i).get(0).equals(version)){
            i++;
        }
        while(i<files.size() && files.get(i).get(0).equals(version)){
            StringBuilder line = new StringBuilder();
            for(j=2; j<files.get(i).size(); j++){
                line.append(files.get(i).get(j));
                if(j!=files.get(i).size()-1){
                    line.append(",");
                }
            }
            csvLines.add(line.toString());
            i++;
        }
        return csvLines;
    }

    public static Evaluation classifier(Instances training, Instances testing, int classifierIndex) throws Exception{
        Evaluation eval;
        switch (classifierIndex) {
            case 1:
                NaiveBayes classifierNB = new NaiveBayes();
                classifierNB.buildClassifier(training);
                eval = new Evaluation(testing);	
                eval.evaluateModel(classifierNB, testing); 
                break;
            case 2:
                RandomForest classifierRF = new RandomForest();
                classifierRF.buildClassifier(training);
                eval = new Evaluation(testing);	
                eval.evaluateModel(classifierRF, testing); 
                break;
            case 3:
                IBk classifierIBK = new IBk();
                classifierIBK.buildClassifier(training);
                eval = new Evaluation(testing);	
                eval.evaluateModel(classifierIBK, testing); 
                break;
            default:
                eval = new Evaluation(null);
                break;
        }
        
        return eval;
    }

    public static AttributeSelection featureSelection(Instances training) throws Exception{
        //create AttributeSelection object
		AttributeSelection filter = new AttributeSelection();
		//create evaluator and search algorithm objects
		CfsSubsetEval eval = new CfsSubsetEval();
		GreedyStepwise search = new GreedyStepwise();
		//set the algorithm to search backward
		search.setSearchBackwards(true);
		//set the filter to use the evaluator and search algorithm
		filter.setEvaluator(eval);
		filter.setSearch(search);
		//specify the dataset
		filter.setInputFormat(training);

        return filter;
    }


    public static Evaluation sampling(Instances training, Instances testing, Integer classifierIndex) throws Exception{
        List<List<String>> trainingFile = Utility.csvToList(CSV_TRAINING);
        Evaluation eval;

        if(SAMPLING_TYPE.equals(SM)){
            SMOTE resampleSmote = new SMOTE();
            resampleSmote.setInputFormat(training);
            switch (classifierIndex) {
                case 1:
                    FilteredClassifier fcNB = new FilteredClassifier();
                    NaiveBayes classifierNB = new NaiveBayes();
                    fcNB.setClassifier(classifierNB);
                    fcNB.setFilter(resampleSmote);
                    fcNB.buildClassifier(training);
                    eval = new Evaluation(testing);	
                    eval.evaluateModel(fcNB, testing); 
                    break;
                case 2:
                    FilteredClassifier fcRF = new FilteredClassifier();
                    RandomForest classifierRF = new RandomForest();
                    fcRF.setClassifier(classifierRF);
                    fcRF.setFilter(resampleSmote);
                    fcRF.buildClassifier(training);
                    eval = new Evaluation(testing);	
                    eval.evaluateModel(fcRF, testing); 
                    break;
                case 3:
                    FilteredClassifier fcIBK = new FilteredClassifier();
                    IBk classifierIBK = new IBk();
                    fcIBK.setClassifier(classifierIBK);
                    fcIBK.setFilter(resampleSmote);
                    classifierIBK.buildClassifier(training);
                    eval = new Evaluation(testing);	
                    eval.evaluateModel(fcIBK, testing); 
                    break;
    
                default:
                    eval = new Evaluation(null);
                    break;
            }             
        }
        else{
            Resample resample = new Resample();
            resample.setInputFormat(training);
            if(SAMPLING_TYPE.equals("over")){
                Float minority = (1-((defectiveness().get(0))/trainingFile.size()))*2;
                resample.setOptions(new String[] {"-B","1.0","-Z",minority.toString()});
            }
            else{
                Float minority = (defectiveness().get(0)/trainingFile.size())*2;
                resample.setOptions(new String[] {"-B","1.0","-Z",minority.toString()});
            }
            switch (classifierIndex) {
                case 1:
                    FilteredClassifier fcNB = new FilteredClassifier();
                    NaiveBayes classifierNB = new NaiveBayes();
                    fcNB.setClassifier(classifierNB);
                    fcNB.setFilter(resample);
                    fcNB.buildClassifier(training);
                    eval = new Evaluation(testing);	
                    eval.evaluateModel(fcNB, testing); 
                    break;
                case 2:
                    FilteredClassifier fcRF = new FilteredClassifier();
                    RandomForest classifierRF = new RandomForest();
                    fcRF.setClassifier(classifierRF);
                    fcRF.setFilter(resample);
                    fcRF.buildClassifier(training);
                    eval = new Evaluation(testing);	
                    eval.evaluateModel(fcRF, testing); 
                    break;
                case 3:
                    FilteredClassifier fcIBK = new FilteredClassifier();
                    IBk classifierIBK = new IBk();
                    fcIBK.setClassifier(classifierIBK);
                    fcIBK.setFilter(resample);
                    classifierIBK.buildClassifier(training);
                    eval = new Evaluation(testing);	
                    eval.evaluateModel(fcIBK, testing); 
                    break;
    
                default:
                    eval = new Evaluation(null);
                    break;
            } 
        }
        return eval;
    }
    
    public static Evaluation filters(Instances training, Instances testing) throws Exception{
        Evaluation eval;
        if(!FS){
            //Evaluation senza feature selection
            selection = "No";
            
            if(SAMPLING){eval = sampling(training, testing, 1);}
            else{eval = classifier(training, testing, 1);}
        }
        else{
            //Evaluation con feature selection
            selection = "Yes";
            AttributeSelection filterTrain = featureSelection(training);
            Instances newDataTrain = Filter.useFilter(training, filterTrain);
            AttributeSelection filterTest = featureSelection(testing);
            Instances newDataTest = Filter.useFilter(training, filterTest);
            ArffSaver saver = new ArffSaver();
            saver.setInstances(newDataTest);
            saver.setFile(new File("sqdb3.arff"));
            saver.writeBatch();
            if(SAMPLING){eval = sampling(newDataTrain, newDataTest, 1);}
            else{eval = classifier(newDataTrain, newDataTest, 1);}
        }
        return eval;
    }

    private static CostMatrix createCostMatrix(double weightFalsePositive, double weightFalseNegative) {
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 0, weightFalsePositive);
        costMatrix.setCell(0, 1, weightFalseNegative);
        costMatrix.setCell(1, 1, 0.0);
        return costMatrix;
    }
    
    public static Evaluation costSensitive(Instances training, Instances testing, Integer classifierIndex) throws Exception{
        CostSensitiveClassifier c1 = new CostSensitiveClassifier();  
        Evaluation eval;      
        
        switch (classifierIndex) {
            case 1:
                c1.setClassifier(new NaiveBayes());
                c1.setCostMatrix(createCostMatrix(CFP, CFN));
                c1.buildClassifier(training);
        
                eval = new Evaluation(testing);	
                eval.evaluateModel(c1, testing); 
                break;
            case 2:
                c1.setClassifier(new RandomForest());
                c1.setCostMatrix(createCostMatrix(CFP, CFN));
                c1.buildClassifier(training);
        
                eval = new Evaluation(testing);	
                eval.evaluateModel(c1, testing); 
                break;
            case 3:
                c1.setClassifier(new IBk());
                c1.setCostMatrix(createCostMatrix(CFP, CFN));
                c1.buildClassifier(training);
        
                eval = new Evaluation(testing);	
                eval.evaluateModel(c1, testing); 
                break;


            default:
                eval = new Evaluation(null);
                break;
        }    
        return eval;
    }
    

    public static void wekaApi(int iteration, int classifierIndex) throws Exception{
        File results = new File(CSV_FINAL);
        String classifier = "";
        boolean boolAppend = true;
        Evaluation eval;
        if(iteration == 1){
            boolAppend = false;
        }
        
        switch (classifierIndex) {
            case 1:
                classifier = "NaiveBayes";
                break;
            case 2:
                classifier = "RandomForest";
                break;
            case 3:
                classifier = "iBk";
                break;

        
            default:
                break;
        }
        
        //Costruzione classificatore
        Utility.csvToArff(CSV_TRAINING, ARFF_TRAINING);
        Utility.csvToArff(CSV_TESTING, ARFF_TESTING);
        
        DataSource source1 = new DataSource(ARFF_TRAINING);
        Instances training = source1.getDataSet();

        DataSource source2 = new DataSource(ARFF_TESTING);
        Instances testing = source2.getDataSet();

        int numAttr = training.numAttributes();
        training.setClassIndex(numAttr - 1);
        testing.setClassIndex(numAttr - 1);

        if(COST_SENSITIVE){
            eval = costSensitive(training, testing, 1);
        }
        else{
            eval = filters(training, testing);
        }

        //Calcolo % of training
        List<List<String>> trainingFile = Utility.csvToList(CSV_TRAINING);
        List<List<String>> testingFile = Utility.csvToList(CSV_TESTING);
        List<List<String>> dataFile = Utility.csvToList(CSV_METHRICS);
        float trainingPerc = (trainingFile.size()/(float)dataFile.size())*100f;
        
        Double precision = eval.precision(1);
        Double tP = eval.numTruePositives(1);
        Double fP = eval.numFalsePositives(1);
        Double tN = eval.numTrueNegatives(1);
        Double fN = eval.numFalseNegatives(1);

        //Calcolo percentuale defective in training e testing
        List<Float> defective = defectiveness();
        Float trainDefectPrec = defective.get(0)/trainingFile.size();
        Float testDefectPrec = defective.get(1)/testingFile.size();
        if(precision.isNaN() && fP!=0d ||precision.isNaN() && fN!=0d){
            precision = 0d;
        }
        else if(precision.isNaN() && fP==0d){
            precision = 1d;
        }
        try(FileWriter finalResults = new FileWriter(results, boolAppend)){
            if(iteration==1){finalResults.append("project,release,%training,%defective training,%defective testing,classifier,balancing,feature selection,TP,FP,TN,FN,precision,recall,AUC,kappa\n");}
            finalResults.append(PRJ_NAME+","+iteration+","+trainingPerc+","+trainDefectPrec+","+testDefectPrec+","+classifier+","+SAMPLING_TYPE+","+selection+","+
                                tP+","+fP+","+tN+","+fN+","+precision+","+eval.recall(1)+","+
                                eval.areaUnderROC(1)+","+eval.kappa()+"\n");
        }
	}


    public static void walkFwIncremental() throws Exception{
        CsvCreator.data(CSV_JIRA, true);
        Integer i;
        Integer j;
        Integer k;
        File testingFile = new File(CSV_TESTING);
        File trainingFile = new File(CSV_TRAINING);
        List<Pair<List<String>, String>> trainingTesting = versionSplit();

        for(i=0; i<trainingTesting.size();i++){
            LOGGER.log(Level.INFO, "Iterazione walk foreward: {0}", i+1);
            List<String> training = trainingTesting.get(i).getValue0();
            String testing = trainingTesting.get(i).getValue1();
            try(FileWriter trainingWriter = new FileWriter(trainingFile);
                FileWriter testingWriter = new FileWriter(testingFile);){
                trainingWriter.append("LOCTouched,LOCadded,MaxLOCadded,AvgLOCadded,churn,MaxChurn,AvgChurn,ChgSetSize,MaxChgSet,AvgChgSet,Bugginess\n");
                for(j=0;  j<training.size(); j++){
                    List<String> csvTraining = filesFromVersion(training.get(j));
                    for(k=0;k<csvTraining.size();k++){
                        String line = csvTraining.get(k);
                        line = csvTraining.get(k).replace("[","");
                        line = line.replace("]","");
                        line = line.replace(" ","");
                        trainingWriter.append(line+"\n");
                        trainingWriter.flush();
                    }
                }
                testingWriter.append("LOCTouched,LOCadded,MaxLOCadded,AvgLOCadded,churn,MaxChurn,AvgChurn,ChgSetSize,MaxChgSet,AvgChgSet,Bugginess\n");
                List<String> csvTesting = filesFromVersion(testing);
                for(k=0; k<csvTesting.size(); k++){
                    String line = csvTesting.get(k).replace("[","");
                    line = line.replace("]","");
                    line = line.replace(" ","");
                    testingWriter.append(line+"\n");
                    testingWriter.flush();
                }
            }   
            wekaApi(i+1, 1);
        }
    }

    public static void main(String[] args) throws Exception{
        walkFwIncremental();
    }

}