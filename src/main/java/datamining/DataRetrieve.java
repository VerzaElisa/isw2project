package datamining;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;

import org.javatuples.Pair;
import org.javatuples.Tuple;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DataRetrieve 
{
    private DataRetrieve() {
        throw new IllegalStateException("Utility class");
    }
    private static final Logger LOGGER = Logger.getLogger(DataRetrieve.class.getName());


    public static Boolean validTicket(String iVersion, String ovDate) throws IOException, ParseException{
        Boolean valid = false;
        String [] versionDate = Utility.searchCsvLine(0, iVersion, Variables.CSV_VERSIONS);
        String[] values = versionDate[0].split(",");
        if(values.length != 0){
            Date initialVersion = new SimpleDateFormat(Variables.DATE_FORMAT).parse(values[1]);
            Date openingVersion = new SimpleDateFormat(Variables.DATE_FORMAT).parse(ovDate.substring(0, 10));
            valid = initialVersion.compareTo(openingVersion)<=0;
        }
        return valid;
    }

    public static Tuple minVersion(String[] versionArray) throws IOException, CsvValidationException{
        List<List<String>> csv = Utility.csvToList(Variables.CSV_VERSIONS);
        Integer i;
        Integer j;
        Integer lastIndex = csv.size()+1;
        Tuple lastVer = Pair.with(0, " ");
        for(i=0; i<versionArray.length; i++){
            for(j=1; j<csv.size(); j++){
                if(versionArray[i]!=null && versionArray[i].equals(csv.get(j).get(0)) && j<lastIndex){
                    lastIndex = j;
                    lastVer = Pair.with(lastIndex, csv.get(lastIndex).get(0));
                    break;
                }
            }
        }
        return lastVer;
    }
    
    public static Tuple maxVersion(String[] versionArray) throws IOException, CsvValidationException{
        List<List<String>> csv = Utility.csvToList(Variables.CSV_VERSIONS);
        Integer i;
        Integer j;
        Integer lastIndex = 0;
        Tuple lastVer = Pair.with(0, " ");
        for(i=0; i<versionArray.length; i++){
            for(j=1; j<csv.size(); j++){
                if(versionArray[i]!=null && versionArray[i].equals(csv.get(j).get(0)) && j>lastIndex){
                    lastIndex = j;
                    lastVer = Pair.with(lastIndex, csv.get(lastIndex).get(0));
                    break;
                }
            }
        }
        return lastVer;
    }

    public static String[] findAffected(JSONArray versionArray) throws IOException{
        Integer j = 0;
        Integer i = 0;
        String[] version = new String[0];

        while(versionArray.length()!=0 && i<versionArray.length()){  
            String currAffVersion = versionArray.getJSONObject(i).getString("name");
            Boolean isReleasedVersion = Utility.searchCsvLine(0, currAffVersion, Variables.CSV_VERSIONS).length != 0;
            if(Boolean.TRUE.equals(isReleasedVersion)){
                String[] newArray = new String[version.length + 1];
                System.arraycopy(version, 0, newArray, 0, version.length);
                version = newArray;
                version[j] = currAffVersion;
                j++;
            }
            i++;
        }
        return version;
    }

    public static String[] findFixed(JSONArray fixVersionArray) throws IOException{
        Integer i = 0;
        Integer j = 0;
        String[] fixVersion = new String[0];

        while(fixVersionArray.length()!=0 && i<fixVersionArray.length()){    
            String currFixVersion = fixVersionArray.getJSONObject(i).getString("name");
            //Controllo sulle fixed versions per vedere se sono released
            Boolean isReleasedVersion = Utility.searchCsvLine(0, currFixVersion, Variables.CSV_VERSIONS).length != 0;
            if(Boolean.TRUE.equals(isReleasedVersion)){
                String[] newArray = new String[fixVersion.length + 1];
                System.arraycopy(fixVersion, 0, newArray, 0, fixVersion.length);
                fixVersion = newArray;
                fixVersion[j] = currFixVersion;
                j++;
            }
            i++;
        }

        return fixVersion;
    }

    public static String[] affectedCalculated(Tuple affected, Tuple fixed) throws CsvValidationException, IOException{
        List<List<String>> versions = Utility.csvToList(Variables.CSV_VERSIONS);
        Integer startIndex = (Integer) affected.getValue(0);
        Integer endIndex = (Integer) fixed.getValue(0);
        Integer i;
        String[] affectedArray = new String[0];
        for(i=startIndex; i<endIndex; i++){
            String[] newArray = new String[affectedArray.length + 1];
            System.arraycopy(affectedArray, 0, newArray, 0, affectedArray.length);
            affectedArray = newArray;
            affectedArray[i-startIndex] = versions.get(i).get(0);
        }
        return affectedArray;
    }

    /**
     * Metodo per ottenere i dati necessari dal jsonArray contenente il informazioni dei ticket 
     * jira ed inserirli, tramite la chiave jira id nel csv con le informazioni dei commit.
     * Vengono ricavati i valori delle Affected Version, Fixed Version, id ticket jira. Il metodo
     * searchCsvLine trova la riga del csv dei commit che risolve il ticket in questione.
     * Quest'ultima insieme ai dati del ticket vengono scritti sul csv specificato dal file writer.
     * 
     * @param i indice dell'arrayJson in cui leggere i dati
     * @param json JsonArray contenente le informazioni di tutti i ticket
     * @param commitWriter File Writer in cui copiare dati del commit associati a dati del ticket
     */
    public static void jiraJsonArray(Integer i, JSONArray json, FileWriter jiraWriter, Integer prj) throws IOException, ParseException, CsvValidationException{
        String jsonKey = "fields";
        Boolean isValid = true;
        JSONArray versionArray = json.getJSONObject(i).getJSONObject(jsonKey).getJSONArray("versions");
        String fixDate = json.getJSONObject(i).getJSONObject(jsonKey).getString("resolutiondate");
        String ovDate = json.getJSONObject(i).getJSONObject(jsonKey).getString("created");
        Tuple oldestAffected = Pair.with(0, " ");
        Tuple newestFixed = Pair.with(0, " ");
        Integer k = 0;

        String[] version = findAffected(versionArray);

        if(version.length != 0){
            oldestAffected = minVersion(version);
            //Controllo per verificare che iv<ov
            isValid = validTicket(version[0], ovDate);
        }

        if(fixDate.length() != 0){
            String fv = Utility.getVersionByDate(fixDate);
            String[] fvArray = {fv};
            newestFixed = maxVersion(fvArray);
        }

        //Controllo per vedere se è un post release bug
        Boolean isPostReleaseTicket = oldestAffected.getValue(1).equals(newestFixed.getValue(1));

        if(Boolean.FALSE.equals(isPostReleaseTicket) && Boolean.TRUE.equals(isValid) && newestFixed.getValue(1)!= " "){
            String affectedStr = " ";
            String ov = Utility.getVersionByDate(ovDate);

            if(oldestAffected.getValue(1)!= " " && newestFixed.getValue(1)!= " "){
                String[] affected = affectedCalculated(oldestAffected, newestFixed);
                affectedStr = Arrays.toString(affected).replace(",", " ");
                affectedStr = affectedStr.replace("[", "");
                affectedStr = affectedStr.replace("]", "");
            }
            String key = json.getJSONObject(i).get("key").toString();
            String[] commit = Utility.searchCsvLine(2, key, Variables.CSV_COMMIT);
            for(k=0;k<commit.length;k++){
                if(commit[k] != null){
                    commit[k] = commit[k].replace("\n", " ");
                    String[] commitArray = commit[k].split(",");
                    String methricsData = downloadCommitData(commitArray[1], prj);

                    jiraWriter.append(commit[k]+","+affectedStr+","+newestFixed.getValue(1)+","+ov+","+ovDate+","+fixDate+","+methricsData+"\n");
                }
            }
        }
    }

    public static String downloadCommitData(String sha, Integer prj) throws JSONException, IOException{
        Integer k;
        String filesStr = "";
        String addedStr = "";
        String deletedStr = "";
        String classesUrl = "https://api.github.com/repos/apache/"+Variables.PRJ_NAME[prj]+"/commits/"+sha.replace("\"", "");
        JSONObject classesJsonObj = Utility.readJsonObjFromUrl(classesUrl, true);
        JSONArray jsonClasses = new JSONArray(classesJsonObj.getJSONArray("files"));
        List<String> files = new ArrayList<>();
        List<Integer> added = new ArrayList<>();
        List<Integer> deleted = new ArrayList<>();
        for(k=0; k<jsonClasses.length();k++){
            if(jsonClasses.getJSONObject(k).getString("filename").contains(".java")){
                files.add(jsonClasses.getJSONObject(k).getString("filename"));
                added.add(jsonClasses.getJSONObject(k).getInt("additions"));
                deleted.add(jsonClasses.getJSONObject(k).getInt("deletions"));
            }
        }
        if(!files.isEmpty()){
            filesStr = files.stream().map(String::valueOf).collect(Collectors.joining(" ", "", ""));
            addedStr = added.stream().map(String::valueOf).collect(Collectors.joining(" ", "", ""));
            deletedStr = deleted.stream().map(String::valueOf).collect(Collectors.joining(" ", "", ""));
        }
        return filesStr+","+addedStr+","+deletedStr;
    }

    public static void labeling(String file, Float cs, boolean incremental) throws IOException, CsvException, ParseException{
        LOGGER.warning("Labeling in corso...");

        Integer j = 1;
        Float p = 0f;
        List<List<String>> csvS = Utility.csvToList(file);
        Collections.reverse(csvS);
        csvS.remove(csvS.size()-1);
        List<List<String>> toCopy = new ArrayList<>(0);
        for(j=0; j<csvS.size(); j++) {  
            String values = csvS.get(j).get(3);

            if(values.length()==1 && incremental){
                p = Proportion.pCalc(file, cs, j);
                LOGGER.info("Ticket: "+csvS.get(j).get(2));
                String[] injVer = new String[] {Proportion.ivCalc(csvS.get(j).get(4),csvS.get(j).get(5), p)};
                String[] fixVer = new String[] {csvS.get(j).get(4)};
                if(!injVer[0].equals(fixVer[0])){
                    Tuple ivTuple = minVersion(injVer);
                    Tuple fvTuple = maxVersion(fixVer);
                    String affected = Arrays.toString(affectedCalculated(ivTuple, fvTuple)).replace("[", "");
                    affected = affected.replace("]", "");
                    affected = affected.replace("\"", "");
                    affected = affected.replace(",", " ");
                    csvS.get(j).set(3, affected);
                    csvS.get(j).set(0, Utility.getVersionByDate(csvS.get(j).get(0)));
                    toCopy.add(csvS.get(j));
                }
            }
            else{
                csvS.get(j).set(0, Utility.getVersionByDate(csvS.get(j).get(0)));
                toCopy.add(csvS.get(j));
            }
        }
        File ticketFile = new File(file);
        try(FileWriter versionsWriter = new FileWriter(ticketFile)){

            versionsWriter.append("version,commit sha,jira_id,affected versions,fixed version,opening version,opening data,fixed data,commit file,added,deleted\n");
            for(j=0; j<toCopy.size(); j++){
                versionsWriter.append(toCopy.get(j).get(0)+","+toCopy.get(j).get(1)+","+toCopy.get(j).get(2)
                +","+toCopy.get(j).get(3)+","+toCopy.get(j).get(4)+","+toCopy.get(j).get(5)+","
                +toCopy.get(j).get(6)+","+toCopy.get(j).get(7)+","+toCopy.get(j).get(8)+","
                +toCopy.get(j).get(9)+","+toCopy.get(j).get(10)+"\n");
            }
        }
    }
    

    /** 
    * Metodo per ottenere dal jsonObject dei ticket jira il jsonArray "issue" cotenente i dati utili.
    * Viene fatta la query su tutti i ticket di tipo BUG con stato CLOSED o RESOLVED e resolution FIXED.
    * Nella stringa url viene specificata la query che il metodo readJsonObjectFromUrl esegue.
    * Infine viene chimato il metodo jiraJsonArray per estrarre dal jsonArray i dati.
    *
    * @param jiraWriter FileWriter del csv su cui verranno scritti i dati necessari dei ticket jira
    */
    public static void jiraData(FileWriter jiraWriter, Integer prj) throws JSONException, IOException, ParseException, CsvException{
        Integer j = 100;
        Integer i = 0;
        Integer max = 100;
        Integer total = 1;
        do {
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + Variables.PRJ_NAME[prj] + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,fixVersions,created&startAt="
                    + i.toString() + "&maxResults=" + j.toString();
            JSONObject jsonObj = Utility.readJsonObjFromUrl(url, false);
            JSONArray json = new JSONArray(jsonObj.getJSONArray("issues"));
            total = jsonObj.getInt("total");

            for (; i < total && i < max; i++) {
                jiraJsonArray(i%j, json, jiraWriter, prj);
            }
            max = max+j;
      } while (i < total);
    }

    /** 
    * Metodo per ottenere dal jsonArray dei commit, scaricato da github, le informazioni necessarie:
    * data, messaggio, sha del commit. Nella stringa url viene specificata la query che il metodo
    * readJsonArrayFromUrl esegue tramite chiamata get. Dal campo "message", con il metodo parseId,
    * viene ricavato l'id del corrispondente ticket jira di cui il commit è risolutivo.
    *
    * @param commitWriter FileWriter del csv su cui verranno scritti i dati necessari dei commit
    */
    public static void commitData(FileWriter commitWriter, Integer prj) throws IOException, InterruptedException{
        Integer i = 1; 
        Integer l = 0;
        Integer k;
        String date;
        String sha;
        String jiraId;
        do{
            String url = "https://api.github.com/repos/apache/"+Variables.PRJ_NAME[prj]+"/commits?page="+i.toString()+"&per_page=100";
            JSONArray jPage = new JSONArray(Utility.readJsonArrayFromUrl(url, true));
            l = jPage.length();
            Thread.sleep(1000);
            for(k=0; k<l; k++){
                jiraId = Utility.parseId(jPage.getJSONObject(k).getJSONObject("commit").getString("message"), prj);
                date = jPage.getJSONObject(k).getJSONObject("commit").getJSONObject("committer").getString("date");
                
                if(jiraId.contains(Variables.PRJ_NAME[prj])){           
                    sha = jPage.getJSONObject(k).getString("sha");
                    commitWriter.append(date + "," +sha+","+ jiraId +"\n");
                }
            }                
            i++;
        } while(l != 0);
    }

    /**
     * Metodo che ottiene da jira le informazioni sulle versioni del progetto e salva nel relativo
     * csv il nome della versione e la data di release solo se la versione è stata rilasciata.
     * 
     * @param versionsWriter File writer del csv contenente tutte le release con nome e data
     */
    public static void versionsData(FileWriter versionsWriter, Integer prj) throws JSONException, IOException{
        String url = "https://issues.apache.org/jira/rest/api/2/project/"+Variables.PRJ_NAME[prj]+"/";
        String releaseDate = " ";
        Integer len;
        Integer i;
        JSONObject jsonObj = Utility.readJsonObjFromUrl(url, false);
        JSONArray json = new JSONArray(jsonObj.getJSONArray("versions"));
        len = json.length();
        for(i = 0; i<len; i++){
            if(json.getJSONObject(i).getBoolean("released")){
                if(json.getJSONObject(i).has("releaseDate")){
                    releaseDate = json.getJSONObject(i).getString("releaseDate");
                }
                versionsWriter.append(json.getJSONObject(i).getString("name")+","+
                                      releaseDate+"\n");
            }
        }

    }


    /**
    * Metodo che gestisce i file prodotti dalla classe. Crea i file csv che conterranno le versioni del progetto, 
    * i dati dei commit provenienti da github ed i dati dei ticket da jira. Invoca i metodi
    * che popleranno i csv. Ciò avviene solo se le variabili booleane DOWNLOAD_VERSIONS, DOWNLOAD_COMMIT e
    * DOWNLOAD_JIRA sono poste a true. Nel caso siano false non viene effettuato il download
    * dei dati assumendo che i file siano già stati popolati in precedenza.
    *
    * @param file
    * @param incremental 
    */
    public static void fileHandler(String file, Boolean incremental) throws IOException, InterruptedException, ParseException, JSONException, CsvException{
        Float cs = 1.462139f; //Proportion.coldStart(); //Se non si vuole ricalcolare il cold start usare cs=1.462139f

        if(Variables.DOWNLOAD_VERSIONS){
            LOGGER.warning("Download elenco versioni in corso...");

            File versionsFile = new File(Variables.CSV_VERSIONS);
            try(FileWriter versionsWriter = new FileWriter(versionsFile)){
                versionsWriter.append("name, release date\n");
                versionsData(versionsWriter, 0);
            }
            Utility.sortVersions();
        }
        if(Variables.DOWNLOAD_COMMIT){
            LOGGER.warning("Download informazioni commit in corso...");

            File commitFile = new File(Variables.CSV_COMMIT);
            try(FileWriter commitWriter = new FileWriter(commitFile)){
                commitWriter.append("commit date,commit sha,jira_id\n");
                commitData(commitWriter, 0);
            }
        }
        if(Variables.DOWNLOAD_JIRA){
            LOGGER.warning("Download informazioni ticket in corso...");

            File jiraFile = new File(Variables.CSV_JIRA);
            try(FileWriter jiraWriter = new FileWriter(jiraFile)){
                jiraWriter.append("version,commit sha,jira_id,affected versions,fixed version,opening version,opening data,fixed data,commit file,added,deleted\n");
                jiraData(jiraWriter, 0); 
            }
        }
        if(Variables.LABELING){
            Proportion.loggerSetup();
            labeling(file, cs, incremental);
        }
    }
    public static void main(String[] args) throws IOException, JSONException, InterruptedException, ParseException, CsvException{
        fileHandler(Variables.CSV_JIRA, true);
    }

}