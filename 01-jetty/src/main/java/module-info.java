/**
 * A Vaadin Flow application that is a named module.
 * <p>
 * Requiring Flow is enough: Jackson, jsoup, slf4j and the servlet API come
 * with com.vaadin.flow.server. The views are opened only to Flow, which
 * creates them.
 */
module com.example.jpms.jetty {
    requires com.vaadin.flow.server;
    requires com.vaadin.flow.component.html;
    requires com.vaadin.flow.component.react;

    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.ee11.webapp;
    requires org.eclipse.jetty.ee11.annotations;

    opens com.example.jpms.jetty to com.vaadin.flow.server;
}
