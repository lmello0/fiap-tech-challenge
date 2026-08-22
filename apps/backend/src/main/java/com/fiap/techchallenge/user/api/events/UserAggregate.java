package com.fiap.techchallenge.user.api.events;

/** The Timeline (CONTEXT.md) every User event belongs to — a Customer or Worker facet is not
 * independently addressable, so its events belong to the owning User's Timeline. */
public final class UserAggregate {

    public static final String TYPE = "USER";

    private UserAggregate() {
    }
}
