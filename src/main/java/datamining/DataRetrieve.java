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

public class DataRetrieve {

    private static final Logger LOGGER = Logger.getLogger(DataRetrieve.class.getName());

    private DataRetrieve() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Metodo che ottiene da jira le informazioni sulle versioni del progetto e salva nel relativo
     * csv il nome della versione e la data di release solo se la versione è stata rilasciata.
     * 
     * @param versionsWriter File writer del csv contenente tutte le release con nome e data
    **/
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
    * Metodo per ottenere dal jsonArray dei commit, scaricato da github, le informazioni necessarie:
    * data, messaggio, sha del commit. Nella stringa url viene specificata la query che il metodo
    * readJsonArrayFromUrl esegue tramite chiamata get. Dal campo "message", con il metodo parseId,
    * viene ricavato l'id del corrispondente ticket jira di cui il commit è risolutivo.
    *
    * @param commitWriter FileWriter del csv su cui verranno scritti i dati necessari dei commit
    * @param prj Il nome del progetto analizzato è specificato in un array contenuto nella classe Variables
    *            Bisogna specificare l'indice dell'array in cui si trova il progetto di interesse
    **/
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
      * Metodo che gestisce i file prodotti dalla classe. Crea i file csv che conterranno le versioni del progetto, 
      * i dati dei commit provenienti da github ed i dati dei ticket da jira. Invoca i metodi
      * che popleranno i csv. Ciò avviene solo se le variabili booleane DOWNLOAD_VERSIONS, DOWNLOAD_COMMIT e
      * DOWNLOAD_JIRA sono poste a true. Nel caso siano false non viene effettuato il download
      * dei dati assumendo che i file siano già stati popolati in precedenza.
      *
      * @param file
      * @param incremental 
    **/
    public static void fileHandler(String file, Boolean incremental) throws IOException, InterruptedException, ParseException, JSONException, CsvException{
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
    }


    public static void main(String[] args) throws IOException, JSONException, InterruptedException, ParseException, CsvException{
        fileHandler(Variables.CSV_JIRA, true);
    }
}