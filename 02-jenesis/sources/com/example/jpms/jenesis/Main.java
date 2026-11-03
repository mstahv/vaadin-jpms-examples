package com.example.jpms.jenesis;

import java.nio.file.Files;

import org.eclipse.jetty.ee11.annotations.AnnotationConfiguration;
import org.eclipse.jetty.ee11.webapp.MetaInfConfiguration;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.ResourceFactory;

/**
 * Starts the application in embedded Jetty.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Server server = new Server(Integer.getInteger("port", 8080));
        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        // Everything is served from the module path, the web application root
        // only has to exist
        context.setBaseResource(ResourceFactory.of(context).newResource(
                Files.createTempDirectory("jpms-example-jenesis").toUri()));
        // Scan all jars, including the ones on the module path, for
        // ServletContainerInitializers and their @HandlesTypes classes
        context.setAttribute(MetaInfConfiguration.CONTAINER_JAR_PATTERN,
                ".*\\.jar|.*/classes/.*");
        context.setConfigurationDiscovered(true);
        context.addConfiguration(new AnnotationConfiguration());
        context.setParentLoaderPriority(true);
        server.setHandler(context);
        server.start();
        server.join();
    }
}
