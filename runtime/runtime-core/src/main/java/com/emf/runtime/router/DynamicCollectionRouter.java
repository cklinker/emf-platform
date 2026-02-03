package com.emf.runtime.router;

import com.emf.runtime.model.CollectionDefinition;
import com.emf.runtime.query.QueryEngine;
import com.emf.runtime.query.QueryRequest;
import com.emf.runtime.query.QueryResult;
import com.emf.runtime.registry.CollectionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Dynamic REST controller for collection CRUD operations.
 * 
 * <p>This controller provides a unified API for all collections registered in the
 * {@link CollectionRegistry}. It dynamically routes requests based on the collection
 * name in the URL path.
 * 
 * <p>Endpoints:
 * <ul>
 *   <li>GET /api/collections/{collectionName} - List records with pagination, sorting, filtering</li>
 *   <li>GET /api/collections/{collectionName}/{id} - Get a single record by ID</li>
 *   <li>POST /api/collections/{collectionName} - Create a new record</li>
 *   <li>PUT /api/collections/{collectionName}/{id} - Update an existing record</li>
 *   <li>DELETE /api/collections/{collectionName}/{id} - Delete a record</li>
 * </ul>
 * 
 * <p>Query Parameters for list endpoint:
 * <ul>
 *   <li>page[number] - Page number (default: 1)</li>
 *   <li>page[size] - Page size (default: 20)</li>
 *   <li>sort - Comma-separated sort fields (prefix with - for descending)</li>
 *   <li>fields - Comma-separated field names to return</li>
 *   <li>filter[field][op] - Filter conditions</li>
 * </ul>
 * 
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/collections")
public class DynamicCollectionRouter {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicCollectionRouter.class);
    
    private final CollectionRegistry registry;
    private final QueryEngine queryEngine;
    
    /**
     * Creates a new DynamicCollectionRouter.
     * 
     * @param registry the collection registry
     * @param queryEngine the query engine
     */
    public DynamicCollectionRouter(CollectionRegistry registry, QueryEngine queryEngine) {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.queryEngine = Objects.requireNonNull(queryEngine, "queryEngine cannot be null");
    }
    
    /**
     * Lists records from a collection with pagination, sorting, and filtering.
     * 
     * @param collectionName the collection name
     * @param params query parameters for pagination, sorting, filtering, and field selection
     * @return the query result or 404 if collection not found
     */
    @GetMapping("/{collectionName}")
    public ResponseEntity<QueryResult> list(
            @PathVariable("collectionName") String collectionName,
            @RequestParam(required = false) Map<String, String> params) {
        
        logger.debug("List request for collection '{}' with params: {}", collectionName, params);
        
        CollectionDefinition definition = registry.get(collectionName);
        if (definition == null) {
            logger.debug("Collection '{}' not found", collectionName);
            return ResponseEntity.notFound().build();
        }
        
        QueryRequest queryRequest = QueryRequest.fromParams(params);
        QueryResult result = queryEngine.executeQuery(definition, queryRequest);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Gets a single record by ID.
     * 
     * @param collectionName the collection name
     * @param id the record ID
     * @return the record or 404 if not found
     */
    @GetMapping("/{collectionName}/{id}")
    public ResponseEntity<Map<String, Object>> get(
            @PathVariable("collectionName") String collectionName,
            @PathVariable("id") String id) {
        
        logger.debug("Get request for collection '{}', id '{}'", collectionName, id);
        
        CollectionDefinition definition = registry.get(collectionName);
        if (definition == null) {
            logger.debug("Collection '{}' not found", collectionName);
            return ResponseEntity.notFound().build();
        }
        
        Optional<Map<String, Object>> record = queryEngine.getById(definition, id);
        return record.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new record in the collection.
     * 
     * @param collectionName the collection name
     * @param data the record data
     * @return the created record with 201 status, or 404 if collection not found
     */
    @PostMapping("/{collectionName}")
    public ResponseEntity<Map<String, Object>> create(
            @PathVariable("collectionName") String collectionName,
            @RequestBody Map<String, Object> data) {
        
        logger.debug("Create request for collection '{}' with data: {}", collectionName, data);
        
        CollectionDefinition definition = registry.get(collectionName);
        if (definition == null) {
            logger.debug("Collection '{}' not found", collectionName);
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> created = queryEngine.create(definition, data);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * Updates an existing record in the collection.
     * 
     * @param collectionName the collection name
     * @param id the record ID
     * @param data the updated record data
     * @return the updated record, or 404 if collection or record not found
     */
    @PutMapping("/{collectionName}/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable("collectionName") String collectionName,
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> data) {
        
        logger.debug("Update request for collection '{}', id '{}' with data: {}", collectionName, id, data);
        
        CollectionDefinition definition = registry.get(collectionName);
        if (definition == null) {
            logger.debug("Collection '{}' not found", collectionName);
            return ResponseEntity.notFound().build();
        }
        
        Optional<Map<String, Object>> updated = queryEngine.update(definition, id, data);
        return updated.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Deletes a record from the collection.
     * 
     * @param collectionName the collection name
     * @param id the record ID
     * @return 204 No Content if deleted, 404 if collection or record not found
     */
    @DeleteMapping("/{collectionName}/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("collectionName") String collectionName,
            @PathVariable("id") String id) {
        
        logger.debug("Delete request for collection '{}', id '{}'", collectionName, id);
        
        CollectionDefinition definition = registry.get(collectionName);
        if (definition == null) {
            logger.debug("Collection '{}' not found", collectionName);
            return ResponseEntity.notFound().build();
        }
        
        boolean deleted = queryEngine.delete(definition, id);
        return deleted ? ResponseEntity.noContent().build() 
                       : ResponseEntity.notFound().build();
    }
}
