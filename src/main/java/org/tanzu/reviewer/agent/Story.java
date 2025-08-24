package org.tanzu.reviewer.agent;

record Story(String text) {

    public String status() {
        return "Created a story:\n\n" + text;
    }
}
