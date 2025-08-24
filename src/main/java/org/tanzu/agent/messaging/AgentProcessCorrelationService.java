package org.tanzu.agent.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage correlations between AgentProcess IDs and correlation IDs
 */
@Service
public class AgentProcessCorrelationService {

    private static final Logger logger = LoggerFactory.getLogger(AgentProcessCorrelationService.class);

    // Maps agent process ID to correlation ID
    private final ConcurrentHashMap<String, String> processToCorrelationMap = new ConcurrentHashMap<>();

    // Maps correlation ID to agent process ID for reverse lookup
    private final ConcurrentHashMap<String, String> correlationToProcessMap = new ConcurrentHashMap<>();

    public void associateProcessWithCorrelation(String processId, String correlationId) {
        if (processId == null || correlationId == null) {
            logger.warn("Cannot associate null processId or correlationId: processId={}, correlationId={}",
                    processId, correlationId);
            return;
        }

        logger.debug("Associating process {} with correlation {}", processId, correlationId);

        processToCorrelationMap.put(processId, correlationId);
        correlationToProcessMap.put(correlationId, processId);
    }

    public String getCorrelationId(String processId) {
        String correlationId = processToCorrelationMap.get(processId);
        logger.debug("Retrieved correlation {} for process {}", correlationId, processId);
        return correlationId;
    }

    public String getProcessId(String correlationId) {
        String processId = correlationToProcessMap.get(correlationId);
        logger.debug("Retrieved process {} for correlation {}", processId, correlationId);
        return processId;
    }

    public void removeProcessAssociation(String processId) {
        String correlationId = processToCorrelationMap.remove(processId);
        if (correlationId != null) {
            correlationToProcessMap.remove(correlationId);
            logger.debug("Removed association for process {} and correlation {}", processId, correlationId);
        }
    }

    public void removeCorrelationAssociation(String correlationId) {
        String processId = correlationToProcessMap.remove(correlationId);
        if (processId != null) {
            processToCorrelationMap.remove(processId);
            logger.debug("Removed association for correlation {} and process {}", correlationId, processId);
        }
    }

    public boolean hasCorrelation(String processId) {
        return processToCorrelationMap.containsKey(processId);
    }

    public int getActiveCorrelationCount() {
        return processToCorrelationMap.size();
    }

    public void clearAll() {
        logger.info("Clearing all process-correlation associations");
        processToCorrelationMap.clear();
        correlationToProcessMap.clear();
    }
}