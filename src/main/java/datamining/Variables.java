package datamining;

import java.util.logging.Logger;

public class Variables {
    private static final Logger LOGGER = Logger.getLogger(Variables.class.getName());
    public static final String[] PRJ_NAME = {"BOOKKEEPER", "SYNCOPE", "TAJO"};
    public static final String CSV_COMMIT = "01-commitdata.csv";
    public static final String CSV_JIRA = "02-ticketdata.csv";
    public static final String CSV_VERSIONS = "03-versionsdata.csv";
    public static final String CSV_METHRICS = "04-data.csv";
    public static final boolean DOWNLOAD_DATA = true;
    public static final boolean DOWNLOAD_FILES = true;
    public static final boolean DOWNLOAD_COMMIT = true;
    public static final boolean DOWNLOAD_JIRA = true;
    public static final boolean DOWNLOAD_VERSIONS = true;
    public static final boolean LABELING = true;
    public static final String USERNAME = "VerzaElisa";
    public static final String AUTH_CODE = "auth_code.txt";
    public static final String DATE_FORMAT = "yyyy-MM-dd";


    public static void main(String[] args){
        LOGGER.info("Project name: "+PRJ_NAME);
        LOGGER.info("Commit file: "+CSV_COMMIT);
        LOGGER.info("Jira file: "+CSV_JIRA);
        LOGGER.info("Version file: "+CSV_VERSIONS);
        LOGGER.info("Data file: "+CSV_METHRICS);
        LOGGER.info("Data download status: "+DOWNLOAD_DATA);
        LOGGER.info("Files download status: "+DOWNLOAD_FILES);
        LOGGER.info("Username: "+USERNAME);
        LOGGER.info("Auth code file: "+AUTH_CODE);
        LOGGER.info("Date format: "+DATE_FORMAT);
        LOGGER.info("Commit download status: "+DOWNLOAD_COMMIT);
        LOGGER.info("Jira download status: "+DOWNLOAD_JIRA);
        LOGGER.info("Version download status: "+DOWNLOAD_VERSIONS);
    }

    
}