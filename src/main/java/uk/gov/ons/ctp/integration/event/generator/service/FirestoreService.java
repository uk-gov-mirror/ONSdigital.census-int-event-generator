package uk.gov.ons.ctp.integration.event.generator.service;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QuerySnapshot;


public class FirestoreService {
  private static final Logger log = LoggerFactory.getLogger(FirestoreService.class);

  private Firestore firestore;
  private String gcpProject;

  
  public FirestoreService() {
    firestore = FirestoreOptions.getDefaultInstance().getService();
    
    gcpProject = firestore.getOptions().getProjectId();
    log.info("Connected to Firestore project: " + gcpProject);
  }

  
  public boolean objectExists(String collectionName, String key) throws Exception {
    // Run a query
    String schema = gcpProject + "-" + collectionName;
    FieldPath fieldPathForId = FieldPath.documentId();
    ApiFuture<QuerySnapshot> query =
        firestore.collection(schema).whereEqualTo(fieldPathForId, key).get();

    // Wait for query to complete and get results
    QuerySnapshot querySnapshot;
    try {
      querySnapshot = query.get();
    } catch (Exception e) {
      String failureMessage =
          "Failed to find object in schema '" + schema + "' for key '" + key + "'";
      log.error(e, failureMessage);
      throw new Exception(failureMessage, e);
    }
 
    int numberDocumentsFound = querySnapshot.size();
    return numberDocumentsFound > 0;
  }
}
