package model;

import okhttp3.HttpUrl;
import util.FilestackService;
import util.Networking;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Base class for file transformations and conversions.
 */
public class Transform {
    private String apiKey;
    private String source;

    ArrayList<Task> tasks;

    private FilestackService.Process processService;

    Transform(Client client, String source) {
        this(client, source, null);
    }

    Transform(FileLink fileLink) {
        this(null, null, fileLink);
    }

    Transform(Client client, String source, FileLink fileLink) {
        if (client != null) {
            this.apiKey = client.getApiKey();
            this.source = source;
        } else {
            this.source = fileLink.getHandle();
        }

        this.tasks = new ArrayList<>();
        this.processService = Networking.getProcessService();

        Security security = client != null ? client.getSecurity() : fileLink.getSecurity();
        if (security != null) {
            Task securityTask = new Task("security");
            securityTask.addOption("policy", security.getPolicy());
            securityTask.addOption("signature", security.getSignature());
            this.tasks.add(securityTask);
        }
    }

    /**
     * Generic task object.
     * A "task" in this case is a transformation, for example resize, crop, convert, etc.
     */
    protected static class Task {
        String name;
        ArrayList<Option> options;

        Task(String name) {
            this.name = name;
            this.options = new ArrayList<>();
        }

        void addOption(String key, Object value) {
            addOption(key, value.toString());
        }

        void addOption(String key, Object value[]) {
            String valueString = Arrays.toString(value);
            // Remove spaces between array items
            valueString = valueString.replace(" ", "");
            addOption(key, valueString);
        }

        void addOption(String key, String value) {
            options.add(new Option(key, value));
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(name);
            stringBuilder.append("=");
            for (Option option : options)
                stringBuilder.append(option.key).append(":").append(option.value).append(",");
            stringBuilder.deleteCharAt(stringBuilder.length()-1);
            return stringBuilder.toString();
        }
    }

    /**
     * Each {@link Task Task} object has options.
     * These are the settings for that task.
     * For example the resize task would have options for width and height.
     */
    protected static class Option {
        String key;
        String value;

        Option(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * Build tasks into single string to insert into request.
     */
    protected String getTasksString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Task task : tasks)
            stringBuilder.append(task.toString()).append('/');
        stringBuilder.deleteCharAt(stringBuilder.length()-1);
        return stringBuilder.toString();
    }

    public String url() {
        String tasksString = getTasksString();

        if (apiKey != null) {
            // TODO Implement when we add external transforms
            return null;
        } else {
            HttpUrl httpUrl = processService.get(tasksString, source).request().url();
            String urlString = httpUrl.toString();
            // When forming the request we add a / between tasks, then add that entire string as a path variable
            // Because it's added as a single path variable, the / is URL encoded
            // That's a little confusing so we're replacing "%2F" with "/" for a more expected URL
            return urlString.replace("%2F", "/");
        }
    }
}