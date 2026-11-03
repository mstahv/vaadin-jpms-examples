/**
 * The Jetty example built with Jenesis: this descriptor is the whole build
 * description. Jenesis resolves each module it requires to a Maven artifact,
 * and the Vaadin plugin of jenesis.plugins.properties builds the frontend.
 * <p>
 * The aliases and pins are needed only because the Vaadin module names are
 * not released yet: the Jenesis Module Index knows a module name once an
 * artifact on Maven Central declares it, and the newest release is used
 * unless a version is pinned. A -SNAPSHOT version is read from the local
 * Maven repository.
 *
 * @jenesis.main com.example.jpms.jenesis.Main
 * @jenesis.alias com.vaadin.flow.server com.vaadin/flow-server
 * @jenesis.alias com.vaadin.flow.component.html com.vaadin/flow-html-components
 * @jenesis.alias com.vaadin.flow.component.react com.vaadin/flow-react
 * @jenesis.alias com.vaadin.flow.component.button com.vaadin/vaadin-button-flow
 * @jenesis.pin com.vaadin.flow.server 25.4-jpms-SNAPSHOT
 * @jenesis.pin com.vaadin.flow.component.html 25.4-jpms-SNAPSHOT
 * @jenesis.pin com.vaadin.flow.component.react 25.4-jpms-SNAPSHOT
 * @jenesis.pin com.vaadin.flow.component.button 25.4-jpms-SNAPSHOT
 * @jenesis.pin plugin.vaadin/maven/com.example.jpms/jenesis-vaadin-plugin 1.0-SNAPSHOT
 * @jenesis.pin main/maven/org.slf4j/slf4j-api 2.0.20
 * @jenesis.pin main/maven/org.slf4j/slf4j-simple 2.0.20
 */
module com.example.jpms.jenesis {
    requires com.vaadin.flow.server;
    requires com.vaadin.flow.component.html;
    requires com.vaadin.flow.component.react;
    requires com.vaadin.flow.component.button;

    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.ee11.webapp;
    requires org.eclipse.jetty.ee11.annotations;
    // A logging backend for slf4j, found as a service provider
    requires org.slf4j.simple;

    opens com.example.jpms.jenesis to com.vaadin.flow.server;
}
