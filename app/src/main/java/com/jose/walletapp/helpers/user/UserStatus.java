package com.jose.walletapp.helpers.user;

public class UserStatus {
    public String state;
    public Object last_changed;

    public UserStatus() {}

    public UserStatus(String state, Object last_changed) {
        this.state = state;
        this.last_changed = last_changed;
    }
}

