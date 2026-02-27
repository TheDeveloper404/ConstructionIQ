package com.constructiq.backendjava.security;

public final class AuthContextHolder {
    private static final ThreadLocal<AuthContext> HOLDER = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    public static void set(AuthContext context) {
        HOLDER.set(context);
    }

    public static AuthContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
