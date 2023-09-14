package datamining;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.json.JSONException;

import java.io.File;
import java.io.FileWriter;

import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;


public class Proportion {
    private static final Logger LOGGER = Logger.getLogger(Proportion.class.getName());

    private Proportion() {
        throw new IllegalStateException("Proportion class");
    }
    public static Integer indexCalc(List<List<String>> versions, String verToFind){
        Integer i;
        Integer index = 0;
        for(i=0; i<versions.size(); i++){
            if(verToFind.contains(versions.get(i).get(0))){
                index = i;
            }
        }
        return index;
    }


    public static Float coldStart() throws IOException, JSONException, ParseException, CsvException, InterruptedException{
        Float[] pCold = new Float[Variables.PRJ_NAME.length-1];
        Integer i = 0;
        for(i=1; i<Variables.PRJ_NAME.length; i++){
            if(Variables.DOWNLOAD_VERSIONS){
                LOGGER.warning("Download elenco versioni in corso...");
    
                File versionsFile = new File(Variables.CSV_VERSIONS);
                try(FileWriter versionsWriter = new FileWriter(versionsFile)){
                    versionsWriter.append("name, release date\n");
                    DataRetrieve.versionsData(versionsWriter, i);
                }
                Utility.sortVersions();
    
            }
            if(Variables.DOWNLOAD_COMMIT){
                LOGGER.warning("Download informazioni commit in corso...");
    
                File commitFile = new File(Variables.CSV_COMMIT);
                try(FileWriter commitWriter = new FileWriter(commitFile)){
                    commitWriter.append("commit date,commit sha,jira_id\n");
                    DataRetrieve.commitData(commitWriter, i);
                }
            }
            if(Variables.DOWNLOAD_JIRA){
                LOGGER.warning("Download informazioni ticket in corso...");
    
                File jiraFile = new File(Variables.CSV_JIRA);
                try(FileWriter jiraWriter = new FileWriter(jiraFile)){
                    jiraWriter.append("version,commit sha,jira_id,affected versions,fixed version,opening version,opening data,fixed data,commit file,added,deleted\n");
                    DataRetrieve.jiraData(jiraWriter, i); 
                }
            }
            pCold[i-1] = propColdStart(Variables.CSV_JIRA);
            File jiraFile = new File(Variables.CSV_JIRA);
            File commitFile = new File(Variables.CSV_COMMIT);
            File versionsFile = new File(Variables.CSV_VERSIONS);
            File prjJiraFile = new File(Variables.PRJ_NAME[i]+Variables.CSV_JIRA);
            File prjCommitFile = new File(Variables.PRJ_NAME[i]+Variables.CSV_COMMIT);
            File prjVersionsFile = new File(Variables.PRJ_NAME[i]+Variables.CSV_VERSIONS);
            Boolean jiraSuccess = jiraFile.renameTo(prjJiraFile);
            Boolean commitSuccess = commitFile.renameTo(prjCommitFile);
            Boolean versionsSuccess = versionsFile.renameTo(prjVersionsFile);
            if(Boolean.FALSE.equals(jiraSuccess) || Boolean.FALSE.equals(commitSuccess) || Boolean.FALSE.equals(versionsSuccess)){
                LOGGER.warning("Cannot rename file");
            }
        }
        return proportionAvg(Arrays.asList(pCold));
    }

    public static Float propColdStart(String file) throws CsvValidationException, IOException{
        Integer j = 1;
        Float p;
        List<List<String>> csvS = Utility.csvToList(file);
        csvS.remove(0);
        while(csvS.get(j).get(3).length()!=1){
            j++;
        }
        Integer index = csvS.size()-j;
        p = Proportion.pCalc(file, 0f, index);
        return p;
    }

    public static Float pCalc(String file, Float cs, int...index) throws IOException, CsvValidationException{
        Integer i = 0;
        Integer affectedCounter = 0;
        Integer injIndex = 0;
        Integer openIndex = 0;
        Integer fixIndex = 0;
        List<Float> propArray = new ArrayList<>();
        String injected = " ";
        List<List<String>> csv = Utility.csvToList(file);
        Collections.reverse(csv);
        csv.remove(csv.size()-1);
        if(index.length == 0){
            index[0] = csv.size(); 
        }
        while(affectedCounter<6 || i<csv.size()){
            if(csv.get(i).get(3).length()!=1){
                affectedCounter++;
            }
            i++;
        }
        if(affectedCounter<6){
            return cs;
        }

        for(i = 0; i<index[0]; i++) {
            String[] affected = csv.get(i).get(3).split(" ");
            if(affected.length != 0){
                injected = affected[0];
            }
            List<List<String>> versions = Utility.csvToList(Variables.CSV_VERSIONS);

            injIndex = indexCalc(versions, injected);
            fixIndex = indexCalc(versions,  csv.get(i).get(4));
            openIndex = indexCalc(versions,  csv.get(i).get(5));

            Integer num = fixIndex-injIndex;
            Integer denum = fixIndex-openIndex;
            if(denum == 0){
                denum = 1;
            }
            propArray.add(Float.valueOf(num)/Float.valueOf(denum));
        }
        return proportionAvg(propArray);
    }

    public static Float proportionAvg(List<Float> propArray){
        Integer i = 0;
        Float sum = 0f;
        while (i < propArray.size()) {
            sum = sum+propArray.get(i);
            i++;
        }
        return sum/propArray.size();
    }

    public static String ivCalc(String fixed, String ov, Float p) throws CsvValidationException, IOException{
        List<List<String>> csv = Utility.csvToList(Variables.CSV_VERSIONS);
        String[] ovArray = {ov};
        String[] fvArray = {fixed};
        Integer ovIndex = (Integer) DataRetrieve.minVersion(ovArray).getValue(0);
        Integer fvIndex = (Integer) DataRetrieve.minVersion(fvArray).getValue(0);
        Integer difference = fvIndex-ovIndex;

        if(difference == 0){
            difference = 1;
        }
        Integer ivIndex = Math.round(fvIndex-(difference)*p);
        if(ivIndex>=csv.size()){
            ivIndex = csv.size()-1;
        }
        if(ivIndex==0){
            ivIndex = 1;
        }
        return csv.get(ivIndex).get(0);
    }
    public static void loggerSetup() throws SecurityException, IOException { 
        FileHandler fh = new FileHandler("MyLogFile.log");  
        LOGGER.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();  
        fh.setFormatter(formatter);  
    }
    
}