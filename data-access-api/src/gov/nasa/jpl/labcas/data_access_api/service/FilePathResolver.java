package gov.nasa.jpl.labcas.data_access_api.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * Handles file path resolution from Solr queries.
 */
public class FilePathResolver extends SolrProxy {
    
    private static final Logger LOG = Logger.getLogger(FilePathResolver.class.getName());
    
    public FilePathResolver() throws Exception {
        super();
    }
    
    /**
     * Gets file paths for a given query.
     * 
     * @param requestContext The request context for access control
     * @param query The Solr query
     * @return List of file paths
     * @throws Exception if query fails
     */
    public List<String> getFilePathsForQuery(ContainerRequestContext requestContext, String query) throws Exception {
        List<String> files = new ArrayList<String>();
        try {
            LOG.info("🦠 Getting file paths for query " + query);

            SolrDocumentList docs = executeFileQuery(requestContext, query);
            LOG.info("℀ Number of results = " + docs.getNumFound());
            
            Iterator<SolrDocument> iter = docs.iterator();
            while (iter.hasNext()) {
                SolrDocument doc = iter.next();
                String filePath = extractFilePathFromDocument(doc);
                LOG.info("🕵️‍♀️ Adding filePath «" + filePath + "» to list of files");
                files.add(filePath);
            }
            LOG.info("🕵️‍♀️ Returning " + files.size() + " files to query " + query);
            return files;
        } catch (SolrServerException ex) {
            LOG.warning("🔥 SolrServerException: " + ex.getMessage() + "; returning files so far (if any)");
            ex.printStackTrace();
            return files;
        }
    }
    
    /**
     * Gets a single file path for a given file ID.
     * 
     * @param requestContext The request context for access control
     * @param id The file ID
     * @return The file path, or null if not found
     */
    public String getFile(ContainerRequestContext requestContext, String id) {
        try {
            LOG.info("🪪 HEY! The id is «" + id + "»");
            
            SolrDocumentList docs = executeFileQuery(requestContext, "id:\"" + id + "\"");
            LOG.info("#️⃣ Num found: " + docs.getNumFound());

            Iterator<SolrDocument> iter = docs.iterator();
            while (iter.hasNext()) {
                SolrDocument doc = iter.next();
                LOG.info(doc.toString());
                String filePath = extractFilePathFromDocument(doc);
                LOG.info("So the filePath is «" + filePath + "»");
                return filePath;
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            return null;
        }
        return null;
    }
    
    /**
     * Gets file information for a specific ID.
     * 
     * @param requestContext The request context for access control
     * @param id The file ID
     * @return FileInfo object containing file details, or null if not found
     */
    public FileInfo getFileInfo(ContainerRequestContext requestContext, String id) {
        try {
            LOG.info("🆔 HEYO! The id is «" + id + "»");
            
            SolrDocumentList docs = executeFileQuery(requestContext, "id:\"" + id + "\"");
            LOG.info("💯 Num found: " + docs.getNumFound());

            Iterator<SolrDocument> iter = docs.iterator();
            boolean iterating_through_possibilities = false;
            
            while (iter.hasNext()) {
                iterating_through_possibilities = true;
                SolrDocument doc = iter.next();
                LOG.info(doc.toString());
                FileInfo fileInfo = extractFileInfoFromDocument(doc);
                LOG.info("=== 6 filePath is «" + fileInfo.getFilePath() + "»");
                LOG.info("File path=" + fileInfo.getFilePath());
                return fileInfo;
            }
            
            if (!iterating_through_possibilities) {
                return null;
            }
            
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            return null;
        }
        return null;
    }
    
    /**
     * Executes a file query against Solr with access control.
     * 
     * @param requestContext The request context for access control
     * @param query The Solr query string
     * @return SolrDocumentList containing the results
     * @throws Exception if query fails
     */
    private SolrDocumentList executeFileQuery(ContainerRequestContext requestContext, String query) throws Exception {
        // Create the Solr query
        SolrQuery request = new SolrQuery();
        request.setQuery(query);

        // Add access control
        String acfq = getAccessControlQueryStringValue(requestContext);
        LOG.info("🧏 ACFQ = " + acfq + ".");
        if (!acfq.isEmpty()) {
            request.setFilterQueries(acfq);
        }
        
        // Set the fields we need
        request.setFields(new String[] {
            SOLR_FIELD_FILE_LOCATION, SOLR_FIELD_FILE_NAME, SOLR_FIELD_NAME
        });
        
        // Execute the query
        LOG.info("❓ Executing Solr request to 'files' core: " + request.toString());
        QueryResponse response = solrServers.get(SOLR_CORE_FILES).query(request);
        
        return response.getResults();
    }
    
    /**
     * Extracts file path information from a Solr document.
     * 
     * @param doc The Solr document
     * @return The constructed file path
     */
    private String extractFilePathFromDocument(SolrDocument doc) {
        String fileLocation = (String) doc.getFieldValue(SOLR_FIELD_FILE_LOCATION);
        String fileName = (String) doc.getFieldValue(SOLR_FIELD_FILE_NAME);
        String realFileName = (String) doc.getFieldValue(SOLR_FIELD_FILE_NAME);
        
        // Override with name field if available
        if (doc.getFieldValuesMap().containsKey(SOLR_FIELD_NAME)) {
            Object nameFieldValue = doc.getFieldValue(SOLR_FIELD_NAME);
            if (nameFieldValue != null) {
                ArrayList asList = (ArrayList) nameFieldValue;
                if (asList.size() > 0) {
                    String firstNameField = (String) asList.get(0);
                    if (firstNameField != null && firstNameField.length() > 0) {
                        LOG.info("🧑‍⚖️ Overriding realFileName «" + realFileName +
                                 "» with firstNameField value «" + firstNameField + "»");
                        realFileName = firstNameField;
                    }
                }
            }
        }
        
        return fileLocation + "/" + realFileName;
    }
    
    /**
     * Extracts complete file information from a Solr document.
     * 
     * @param doc The Solr document
     * @return FileInfo object with all file details
     */
    private FileInfo extractFileInfoFromDocument(SolrDocument doc) {
        LOG.info("=== 1 about to get fileLocation");
        String fileLocation = (String) doc.getFieldValue(SOLR_FIELD_FILE_LOCATION);
        LOG.info("=== 2 got fileLocation = «" + fileLocation + "»");
        String fileName = (String) doc.getFieldValue(SOLR_FIELD_FILE_NAME);
        String realFileName = (String) doc.getFieldValue(SOLR_FIELD_FILE_NAME);
        LOG.info("=== 3 got fileName = «" + fileName + "»");
        
        // Override with name field if available
        if (doc.getFieldValuesMap().containsKey(SOLR_FIELD_NAME)) {
            LOG.info("=== 3½ ok");
            Object nameFieldValue = doc.getFieldValue(SOLR_FIELD_NAME);
            if (nameFieldValue != null) {
                ArrayList asList = (ArrayList) nameFieldValue;
                if (asList.size() > 0) {
                    String firstNameField = (String) asList.get(0);
                    if (firstNameField != null && firstNameField.length() > 0) {
                        LOG.info("=== 4 name field value «" + firstNameField + "» overriding fileName «" + fileName + "»");
                        realFileName = firstNameField;
                    }
                }
            }
        }
        
        String filePath = fileLocation + "/" + realFileName;
        return new FileInfo(fileLocation, fileName, realFileName, filePath);
    }
    
    /**
     * Data class to hold file information.
     */
    public static class FileInfo {
        private final String fileLocation;
        private final String fileName;
        private final String realFileName;
        private final String filePath;
        
        public FileInfo(String fileLocation, String fileName, String realFileName, String filePath) {
            this.fileLocation = fileLocation;
            this.fileName = fileName;
            this.realFileName = realFileName;
            this.filePath = filePath;
        }
        
        public String getFileLocation() { return fileLocation; }
        public String getFileName() { return fileName; }
        public String getRealFileName() { return realFileName; }
        public String getFilePath() { return filePath; }
    }
} 