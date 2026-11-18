/**
 * A Vaadin Flow application that is a named module.
 * <p>
 * The released Flow jars are automatic modules. Jackson needs an explicit
 * requirement because the view uses it directly; Flow has no transitive
 * requirements yet. The views are opened only to Flow, which creates them.
 */
module com.example.jpms.jetty {
    requires com.vaadin.flow.server;
    requires com.vaadin.flow.component.html;
    requires com.vaadin.flow.component.react;
    requires tools.jackson.databind;
    // Automatic Flow does not resolve this explicit module at runtime.
    requires org.jsoup;

    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.ee11.webapp;
    requires org.eclipse.jetty.ee11.annotations;

    opens com.example.jpms.jetty to com.vaadin.flow.server;
}
