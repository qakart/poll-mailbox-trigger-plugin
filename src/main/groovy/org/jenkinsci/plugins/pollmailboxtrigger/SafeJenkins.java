package org.jenkinsci.plugins.pollmailboxtrigger;

import hudson.model.Saveable;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * This class provides a way for us to invoke the Jenkins.getInstance() method, during testing, without it throwing NoSuchMethod errors.
 * Currently this method throws the following error: 'java.lang.NoSuchMethodError: hudson.util.XStream2.getConverterRegistry()Lcom/thoughtworks/xstream/converters/ConverterRegistry;'.
 * </p><p>
 * Unfortunately this method is invoked in many places, often nested within other static methods. This makes mocking very
 * difficult/impossible. So, I've fashioned the following class to address these issues.
 * </p>
 */
public class SafeJenkins {

    /**
     * Provides a way to override the static Jenkins instance for unit testing.
     */
    protected static DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties;
    protected static DescribableList<NodeProperty<?>, NodePropertyDescriptor> localNodeProperties;

    protected static boolean useNativeInstance = true;

    public static void useNativeInstance(boolean useNativeInstance) {
        SafeJenkins.useNativeInstance = useNativeInstance;
    }

    public static Jenkins getJenkinsInstance(){
        if (useNativeInstance){
            Jenkins instance = Jenkins.getInstance();
            if (isNull(instance)) {
                throw new RuntimeException("Could not get Jenkins instance using Jenkins.getInstance() (returns null). " +
                        "This can happen if Jenkins has not been started, or was already shut down. " +
                        "Please see http://javadoc.jenkins-ci.org/jenkins/model/Jenkins.html#getInstance() for more details. " +
                        "If you believe this is an error, please raise an 'issue' under https://wiki.jenkins-ci.org/display/JENKINS/poll-mailbox-trigger-plugin.");
            }
            return instance;
        } else {
            throw new RuntimeException("This method is not supported!");
        }
    }

    public static DescribableList<NodeProperty<?>, NodePropertyDescriptor> getGlobalNodeProperties(){
        return useNativeInstance ? getJenkinsInstance().getGlobalNodeProperties() : globalNodeProperties;
    }

    public static DescribableList<NodeProperty<?>, NodePropertyDescriptor> getNodeProperties(){
        return useNativeInstance ? getJenkinsInstance().getNodeProperties() : localNodeProperties;
    }

    /**
     * Creates a {@link EnvironmentVariablesNodeProperty} of environment properties, from the given {@link Map}.
     */
    public static void setLocalNodeProperties(Map<String, String> properties){
        if (isNull(localNodeProperties)){
            localNodeProperties = new DescribableList<NodeProperty<?>, NodePropertyDescriptor>(new NoopSaveable());
        }
        List<EnvironmentVariablesNodeProperty.Entry> envVarEntries = new ArrayList<EnvironmentVariablesNodeProperty.Entry>();
        for (Map.Entry<String, String> entry : properties.entrySet()){
            envVarEntries.add(new EnvironmentVariablesNodeProperty.Entry(entry.getKey(), entry.getValue()));
        }
        localNodeProperties.add(new EnvironmentVariablesNodeProperty(envVarEntries));
    }

    public static String encrypt(String message){
        return useNativeInstance
                ? Secret.fromString(message).getEncryptedValue()
                : new StringBuffer(message).reverse().toString();
    }

    public static String decrypt(String encryptedMessage){
        return useNativeInstance
                ? Secret.decrypt(encryptedMessage).getPlainText()
                : new StringBuffer(encryptedMessage).reverse().toString();
    }

    public static boolean isNull(Object object){
        return object == null;
    }

    public static boolean nonNull(Object object){
        return object != null;
    }

    static class NoopSaveable implements Saveable {
        @Override
        public void save() throws IOException {
            // noop
        }
    }

}
