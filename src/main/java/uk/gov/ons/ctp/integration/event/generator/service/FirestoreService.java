package uk.gov.ons.ctp.integration.event.generator.service;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
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

  
  public boolean objectExists(String collectionName, String key, Long minObjectTimestamp,
      String contentCheckPath, String expectedValue) throws Exception {
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
 
    // Bail out if nothing found
    if (querySnapshot.isEmpty()) { 
      return false;
    }

    // Optionally, only regard the object as existing if it is newer than the specified time
    QueryDocumentSnapshot targetDocuments = querySnapshot.getDocuments().get(0);
    if (minObjectTimestamp != null) {
      long objectUpdateMillis = targetDocuments.getUpdateTime().getSeconds() * 1000;
      if (objectUpdateMillis < minObjectTimestamp) {
        return false;
      }
    }

    // Optionally, determine if target field is in the expected state
    if (contentCheckPath != null) {
      String[] parts = contentCheckPath.split("\\.");
      FieldPath fieldPath = FieldPath.of(parts);
      Object actualValue = targetDocuments.get(fieldPath);

      if (!expectedValue.equals(actualValue)) {
        return false;
      }
    }
    
    return true;
  }
}
