package com.example.jpms.jenesis;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Enables push with long polling, which works without websocket support in
 * the embedded server.
 */
@Push(transport = Transport.LONG_POLLING)
public class AppShell implements AppShellConfigurator {
}
