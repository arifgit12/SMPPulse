package com.smppulse.app;

import com.smppulse.config.ProfileManager;
import com.smppulse.core.session.SessionManager;
import com.smppulse.dlr.DlrGenerator;
import com.smppulse.dlr.DlrTracker;
import com.smppulse.generator.LoadEngine;
import com.smppulse.metrics.MetricsCollector;
import com.smppulse.metrics.ResultExporter;
import com.smppulse.param.DataSourceManager;
import com.smppulse.param.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppContext {

    private static final Logger log = LoggerFactory.getLogger(AppContext.class);
    private static AppContext instance;

    private final ProfileManager profileManager;
    private final MetricsCollector metricsCollector;
    private final SessionManager sessionManager;
    private final ParameterResolver parameterResolver;
    private final DataSourceManager dataSourceManager;
    private final DlrTracker dlrTracker;
    private final DlrGenerator dlrGenerator;
    private final LoadEngine loadEngine;
    private final ResultExporter resultExporter;

    private AppContext() {
        log.info("Initializing AppContext services");
        this.profileManager = new ProfileManager();
        this.metricsCollector = new MetricsCollector();
        this.dataSourceManager = new DataSourceManager();
        this.parameterResolver = new ParameterResolver(dataSourceManager);
        this.dlrTracker = new DlrTracker(metricsCollector);
        this.sessionManager = new SessionManager(metricsCollector, dlrTracker);
        this.dlrGenerator = new DlrGenerator(metricsCollector);
        this.loadEngine = new LoadEngine(sessionManager, parameterResolver, metricsCollector, dlrTracker);
        this.resultExporter = new ResultExporter(metricsCollector);
    }

    public static synchronized void initialize() {
        if (instance == null) {
            instance = new AppContext();
        }
    }

    public static AppContext getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppContext not initialized. Call initialize() first.");
        }
        return instance;
    }

    public void shutdown() {
        log.info("Shutting down all services");
        try {
            loadEngine.stop();
        } catch (Exception e) {
            log.warn("Error stopping load engine", e);
        }
        try {
            sessionManager.disconnectAll();
        } catch (Exception e) {
            log.warn("Error disconnecting sessions", e);
        }
        try {
            dlrTracker.shutdown();
        } catch (Exception e) {
            log.warn("Error shutting down DLR tracker", e);
        }
        try {
            dataSourceManager.closeAll();
        } catch (Exception e) {
            log.warn("Error closing data sources", e);
        }
        log.info("All services shut down");
    }

    public ProfileManager getProfileManager() { return profileManager; }
    public MetricsCollector getMetricsCollector() { return metricsCollector; }
    public SessionManager getSessionManager() { return sessionManager; }
    public ParameterResolver getParameterResolver() { return parameterResolver; }
    public DataSourceManager getDataSourceManager() { return dataSourceManager; }
    public DlrTracker getDlrTracker() { return dlrTracker; }
    public DlrGenerator getDlrGenerator() { return dlrGenerator; }
    public LoadEngine getLoadEngine() { return loadEngine; }
    public ResultExporter getResultExporter() { return resultExporter; }
}
