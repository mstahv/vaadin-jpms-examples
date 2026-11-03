package com.example.jpms.jenesis;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.react.ReactAdapterComponent;
import com.vaadin.flow.router.Route;

/**
 * Lists the modules the application and Flow are loaded in, and does a server
 * round trip and a push from a background thread.
 */
@Route("")
public class ModulesView extends Div {

    public ModulesView() {
        add(new H1("Vaadin Flow on the module path"));

        UnorderedList modules = new UnorderedList();
        modules.setId("modules");
        for (Class<?> type : new Class<?>[] { ModulesView.class, Route.class,
                Div.class, ReactAdapterComponent.class, Button.class,
                tools.jackson.databind.JsonNode.class }) {
            modules.add(new ListItem(describe(type.getModule())));
        }
        add(modules);

        Paragraph result = new Paragraph();
        result.setId("result");
        NativeButton roundTrip = new NativeButton("Server round trip",
                event -> result.setText("Clicked"));
        roundTrip.setId("round-trip");
        NativeButton push = new NativeButton("Push from a thread", event -> {
            UI ui = event.getSource().getUI().orElseThrow();
            Thread.ofVirtual()
                    .start(() -> ui.access(() -> result.setText("Pushed")));
        });
        push.setId("push");
        // A Vaadin component, whose web component the frontend build
        // installs from npm
        Button component = new Button("Vaadin button",
                event -> result.setText("Component clicked"));
        component.setId("component");
        add(roundTrip, push, component, result);
    }

    private static String describe(Module module) {
        if (!module.isNamed()) {
            return "unnamed module";
        }
        return module.getName() + " ("
                + (module.getDescriptor().isAutomatic() ? "automatic"
                        : "explicit")
                + ")";
    }
}
