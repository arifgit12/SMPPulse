package com.smppulse.config;

public class TestProfile {

    private String name = "Default Profile";
    private String description = "";
    private ConnectionConfig connection = new ConnectionConfig();
    private LoadTestConfig loadTest = new LoadTestConfig();
    private DlrConfig dlr = new DlrConfig();
    private MessageTemplate messageTemplate = new MessageTemplate();

    public TestProfile() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ConnectionConfig getConnection() { return connection; }
    public void setConnection(ConnectionConfig connection) { this.connection = connection; }

    public LoadTestConfig getLoadTest() { return loadTest; }
    public void setLoadTest(LoadTestConfig loadTest) { this.loadTest = loadTest; }

    public DlrConfig getDlr() { return dlr; }
    public void setDlr(DlrConfig dlr) { this.dlr = dlr; }

    public MessageTemplate getMessageTemplate() { return messageTemplate; }
    public void setMessageTemplate(MessageTemplate messageTemplate) { this.messageTemplate = messageTemplate; }
}
