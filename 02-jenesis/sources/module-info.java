/**
 * The Jetty example built with Jenesis: this descriptor is the whole build
 * description. Jenesis resolves each module it requires to a Maven artifact,
 * and the Vaadin plugin of jenesis.plugins.properties builds the frontend.
 * <p>
 * The platform BOM selects component beta2 and Flow beta1 as released.
 * Keep aliases so resolution does not depend on the Module Index having
 * indexed this beta yet. Only the local build plugin uses a SNAPSHOT.
 *
 * @jenesis.bom com.vaadin/vaadin-bom 25.4.0-beta2
 * @jenesis.main com.example.jpms.jenesis.Main
 * @jenesis.alias com.vaadin.flow.server com.vaadin/flow-server
 * @jenesis.alias com.vaadin.flow.component.html com.vaadin/flow-html-components
 * @jenesis.alias com.vaadin.flow.component.react com.vaadin/flow-react
 * @jenesis.alias com.vaadin.flow.component.button com.vaadin/vaadin-button-flow
 * @jenesis.pin tools.jackson.databind 3.1.8
 * @jenesis.pin plugin.vaadin/maven/com.example.jpms/jenesis-vaadin-plugin 1.0-SNAPSHOT
 * @jenesis.pin main/maven/org.slf4j/slf4j-api 2.0.20
 * @jenesis.pin main/maven/org.slf4j/slf4j-simple 2.0.20
 */
module com.example.jpms.jenesis {
    requires com.vaadin.flow.server;
    requires com.vaadin.flow.component.html;
    requires com.vaadin.flow.component.react;
    requires com.vaadin.flow.component.button;
    // The beta has automatic modules, without Flow's transitive requirements.
    requires tools.jackson.databind;

    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.ee11.webapp;
    requires org.eclipse.jetty.ee11.annotations;
    // A logging backend for slf4j, found as a service provider
    requires org.slf4j.simple;

    opens com.example.jpms.jenesis to com.vaadin.flow.server;
}
