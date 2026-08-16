package com.fiap.techchallenge.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * First-run-only settings for {@link com.fiap.techchallenge.auth.services.BootstrapManagerRunner}.
 * Left blank in every normal deployment; {@code email} being blank is what tells the runner there
 * is nothing to bootstrap. {@code documentType} is kept as a raw string (rather than
 * {@code DocumentType}) so an unset/blank value never fails property binding on a normal boot —
 * the runner only parses it once it knows it is actually bootstrapping.
 */
@ConfigurationProperties(prefix = "app.auth.bootstrap-manager")
public record BootstrapManagerProperties(
        String email,
        String firstName,
        String lastName,
        String documentType,
        String documentCode,
        String phone
) {
}
