package datamining;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CsvCreator {

    private CsvCreator() {
        throw new IllegalStateException("Utility class");
    }

    private static final Logger LOGGER = Logger.getLogger(CsvCreator.class.getName());

    public static void filesRetrieve(Integer j, List<List<String>> ticket, List<String> files, Integer prj) throws JSONException, IOException{
        Integer i;
        String filesUrl = "https://api.github.com/repos/apache/"+Variables.PRJ_NAME[prj]+"/git/trees/"+ticket.get(j).get(1)+"?recursive=1";
        JSONObject filesJsonObj = Utility.readJsonObjFromUrl(filesUrl, true);
        JSONArray jsonFiles = new JSONArray(filesJsonObj.getJSONArray("tree"));
        for(i = 0; i<jsonFiles.length(); i++){
            if(jsonFiles.getJSONObject(i).getString("path").contains(".java")){
                files.add(jsonFiles.getJSONObject(i).getString("path"));
            }
        }

    }

    public static void downloadFiles() throws IOException, CsvValidationException, ParseException{
        LOGGER.warning("Download file per release in corso...");
        Integer z;
        Integer i;
        List<List<String>> versions = Utility.csvToList(Variables.CSV_VERSIONS);
        List<List<String>> ticket = new ArrayList<>();
        List<List<String>> rawTicket = Utility.csvToList(Variables.CSV_COMMIT);
        for(i=1; i<rawTicket.size(); i++){
            String version = Utility.getVersionByDate(rawTicket.get(i).get(0));
            List<String> line = Arrays.asList(version, rawTicket.get(i).get(1), rawTicket.get(i).get(2));
            ticket.add(line);
        }
        Integer len = versions.size()/2;
        try(FileWriter csvWriter = new FileWriter(Variables.CSV_METHRICS)){
            csvWriter.append("versione,file,LOC Touched,LOC added,Max LOC added,Avg LOC added,Churn,Max churn,Avg churn,Change set size,Max change set,Avg change set\n");
            for(z=1; z<len+1; z++){
                Integer j = 0;
                List<String> files = new ArrayList<>();
                LOGGER.warning(versions.get(z).get(0));
                while(j<ticket.size() && !ticket.get(j).get(0).equals(versions.get(z).get(0))){
                    j++;
                }
                while(j<ticket.size() && ticket.get(j).get(0).equals(versions.get(z).get(0))){
                    filesRetrieve(j, ticket, files, 0);
                    j++;
                }
                List<String> toCpoy = files.stream().distinct().collect(Collectors.toList());
                for(i=0; i<toCpoy.size(); i++){
                    csvWriter.append(versions.get(z).get(0)+","+toCpoy.get(i)+",0,0,0,0,0,0,0,0,0,0,"+"No\n");
                }
            }
        }
    }

    public static void searchBugginess(String affectedVersion, String commitFiles) throws IOException, CsvException{
        Integer j=1;
        List<List<String>> dataCsv = Utility.csvToList(Variables.CSV_METHRICS);
        while(j<dataCsv.size() && !dataCsv.get(j).get(0).equals(affectedVersion)){
            j++;
        }
        while(j<dataCsv.size() && dataCsv.get(j).get(0).equals(affectedVersion)){
            if(commitFiles.contains(dataCsv.get(j).get(1))){
                updateDataCSV(Variables.CSV_METHRICS, "YES", j, 12);
            }
            j++;
        }
    }


    /**
     * Metodo che ottiene una lista delle versioni del progetto tramite il metodo projectVersions()
     * per ogni versione legge il file ticketdata contenente tutti i commit con le informazioni dei
     * ticket. Per ogni riga chiama il metodo fileTouched() che controlla se la versione corrente
     * rientra nella lista delle affected versions (se presenti), prende lo sha del commit e segna
     * come buggy tutti i file toccati dal commit risolutivo del ticket.
     * 
     * @return void
     * @throws ParseException
     * @throws InterruptedException
     */
    public static void bugginess() throws IOException, JSONException, CsvException, ParseException{
        Integer z;
        if(Variables.DOWNLOAD_FILES){
            downloadFiles();
        }
        LOGGER.warning("Matching ticket in corso...");

        try(BufferedReader brTd = new BufferedReader(new FileReader(Variables.CSV_JIRA))){
            String lineTd = brTd.readLine();
            while ( (lineTd = brTd.readLine()) != null ) {
                String[] ticketValues = lineTd.split(",");
                LOGGER.warning(ticketValues[0]);

                String valuesPure = ticketValues[3].replace("[", "");
                valuesPure = valuesPure.replace("]", "");
                valuesPure = valuesPure.replace("\"", "");
                
                String[] ticketAffectedVer =  valuesPure.split(" ");
                for(z=0; z<ticketAffectedVer.length; z++){
                    if(ticketValues.length > 8) {
                            searchBugginess(ticketAffectedVer[z], ticketValues[8]);
                        } 
                    }
                
            }
        }  
    }
    
    /**
    * Update CSV by row and column
    * 
    * @param fileToUpdate CSV file path to update e.g. D:\\chetan\\test.csv
    * @param replace Replacement for your cell value
    * @param row Row for which need to update 
    * @param col Column for which you need to update
    * @throws IOException
    * @throws CsvException
    */
    public static void updateDataCSV(String fileToUpdate, String replace, int row, int col) throws IOException, CsvException {
        List<String[]> csvBody = new ArrayList<>();
        File inputFile = new File(fileToUpdate);

        // Read existing file 
        try(CSVReader reader = new CSVReader(new FileReader(inputFile))){
            csvBody = reader.readAll();
            // get CSV row column  and replace with by using row and column
            csvBody.get(row)[col] = replace;
        }
            // Write to CSV file which is open
        try(CSVWriter writer = new CSVWriter(new FileWriter(inputFile))){
            writer.writeAll(csvBody);
            writer.flush();
        }
    }

    public static String dateSearch(String dateStr) throws ParseException, IOException{
        Date releaseDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
        File file = new File(Variables.CSV_COMMIT);
        Integer i;
        String lastCommitSha = " ";
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String line;
            line = br.readLine();
            while ( (line = br.readLine()) != null ) {
                String[] values = line.split(",");
                Date commitDate = new SimpleDateFormat("yyyy-MM-dd").parse(values[0].substring(0, 10));
                i = releaseDate.compareTo(commitDate);
                if(i>=0){
                    lastCommitSha = values[1];
                    break;
                }
            }          
        }
        return lastCommitSha;
    }


    public static void data(String file, boolean incremental) throws IOException, InterruptedException, JSONException, ParseException, CsvException{
        DataRetrieve.fileHandler(file, incremental);
        if(Variables.DOWNLOAD_DATA){
            bugginess();
        }
        Methrics.locTouched();
    }

    public static void main(String[] args) throws IOException, JSONException, InterruptedException, ParseException, CsvException{
        data(Variables.CSV_JIRA, true);
    }
}