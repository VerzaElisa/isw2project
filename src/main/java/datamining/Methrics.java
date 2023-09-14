package datamining;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

public class Methrics {

    private static void resetFile(List<List<String>> file) throws IOException{
        Integer i;
        try(FileWriter csvWriter = new FileWriter(Variables.CSV_METHRICS)){
            csvWriter.append("versione,file,LOC Touched,LOC added,Max LOC added,Avg LOC added,Churn,Max churn,Avg churn,Change set size,Max change set,Avg change set\n");
            for(i=1; i<file.size(); i++){
                csvWriter.append(file.get(i).get(0)+","+file.get(i).get(1)+",0,0,0,0,0,0,0,0,0,0,"+file.get(i).get(12)+"\n");
            }

        }
    }

    public static void methricsWriter(String filesString, String addedString, String deletedString, List<List<String>> methrics, Integer j){
        Integer k;
        Integer currLocT;
        String[] files = filesString.split(" ");
        String[] added = addedString.split(" ");
        String[] deleted = deletedString.split(" ");

        for(k=0; k<files.length; k++){
            if(files[k].equals(methrics.get(j).get(1))){
                currLocT = Integer.valueOf(methrics.get(j).get(2));
                currLocT = currLocT+Integer.valueOf(added[k])+Integer.valueOf(deleted[k]);
                Integer counter = Integer.valueOf(methrics.get(j).get(5))+1;
                Integer counterChurn = Integer.valueOf(methrics.get(j).get(8))+1;
                Integer counterChg = Integer.valueOf(methrics.get(j).get(11))+1;
                methrics.get(j).set(2, currLocT.toString());
                methrics.get(j).set(5, counter.toString());
                methrics.get(j).set(8, counterChurn.toString());
                methrics.get(j).set(11, counterChg.toString());
                locAdded(methrics, added[k], j);
                maxLocAdded(methrics, added[k], j);
                churn(methrics, addedString, deletedString, j, k);
                maxChurn(methrics, added, deleted, j, k);
                chgSetSize(methrics, j, filesString);
                maxChgSet(methrics, j, filesString);
            }
        }
    }

    public static void locTouched() throws IOException, CsvException, NumberFormatException{
        Integer i;
        Integer j;
        List<List<String>> commit = Utility.csvToList(Variables.CSV_JIRA);
        List<List<String>> methrics = Utility.csvToList(Variables.CSV_METHRICS);
        if(!Variables.DOWNLOAD_DATA){
            resetFile(methrics);
            methrics = Utility.csvToList(Variables.CSV_METHRICS);
        }

        for(i=1; i<commit.size(); i++){
            j=1;
            while(j<methrics.size() && !commit.get(i).get(0).equals(methrics.get(j).get(0))){
                j++;
            }
            while(j<methrics.size() && commit.get(i).get(0).equals(methrics.get(j).get(0))){
                if(commit.get(i).size()>9){
                    methricsWriter(commit.get(i).get(8), commit.get(i).get(9), commit.get(i).get(10), methrics, j);
                }
                j++;
            }
        }

        avgMethrics(methrics, 5, 3);
        avgMethrics(methrics, 8, 6);
        avgMethrics(methrics, 11, 9);
        try(CSVWriter csvWriter = new CSVWriter(new FileWriter(Variables.CSV_METHRICS));){
            List<String[]> collect = new ArrayList<>();
            for(i=0; i<methrics.size();i++){
                String[] strArray = methrics.get(i).toArray(String[]::new);
                collect.add(strArray);
            }
            csvWriter.writeAll(collect);        
        }
    }

    public static void locAdded(List<List<String>> methrics, String added, Integer row){
        Integer currLocA = Integer.valueOf(methrics.get(row).get(3));
        currLocA = currLocA+Integer.valueOf(added);
        methrics.get(row).set(3, currLocA.toString());
    }

    public static void maxLocAdded(List<List<String>> methrics, String currAdded, Integer row){
        Integer lastLocA = Integer.valueOf(methrics.get(row).get(4));
        if(lastLocA<Integer.valueOf(currAdded)){
            methrics.get(row).set(4, currAdded);
        }
    }

    public static void avgMethrics(List<List<String>> methrics, int denumCol, int numCol){
        Integer i;
        Float denum;
        Float num;
        for(i=1; i<methrics.size();i++){
            denum = Float.parseFloat(methrics.get(i).get(denumCol));
            num = Float.parseFloat(methrics.get(i).get(numCol));
            if(denum == 0){
                denum = 1f;
                Float result = num/denum;
                methrics.get(i).set(denumCol, result.toString());
            }
            else{
                Float result = num/denum;
                methrics.get(i).set(denumCol, result.toString());
            }
        }
    }

    public static void churn(List<List<String>> methrics, String addedString, String deletedString, Integer index, Integer k){
        Integer currChurn;
        String[] added = addedString.split(" ");
        String[] deleted = deletedString.split(" ");

        currChurn = Integer.valueOf(methrics.get(index).get(6));
        currChurn = currChurn+Integer.valueOf(added[k])-Integer.valueOf(deleted[k]);
        methrics.get(index).set(6, currChurn.toString());
    }

    public static void maxChurn(List<List<String>> methrics, String[] added, String[] deleted, Integer index, Integer k){
        Integer currMaxChurn = Integer.valueOf(methrics.get(index).get(7));
        Integer nextChurn = Integer.valueOf(added[k])-Integer.valueOf(deleted[k]);
        if(currMaxChurn<nextChurn){
            methrics.get(index).set(7, nextChurn.toString());   
        }

    }

    public static void chgSetSize(List<List<String>> methrics, Integer j, String commitFiles){
        String[] filesList = commitFiles.split(" ");
        methrics.get(j).set(9, String.valueOf(filesList.length+Integer.valueOf(methrics.get(j).get(9))));
    }

    public static void maxChgSet(List<List<String>> methrics, Integer index, String commitFiles){
        Integer currMaxChgSet = Integer.valueOf(methrics.get(index).get(10));
        Integer nextChgSet = commitFiles.split(" ").length;
        if(currMaxChgSet<nextChgSet){
            methrics.get(index).set(10, nextChgSet.toString());   
        }
    }

    public static void main(String[] args) throws IOException, CsvException, NumberFormatException{
        locTouched();
    }

}