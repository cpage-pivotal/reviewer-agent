package org.tanzu.agent.literary;

record Story(String text) {

    public String status() {
        return "Created a story:\n\n" + text;
    }
}
