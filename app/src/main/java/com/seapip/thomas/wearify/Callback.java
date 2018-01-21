package com.seapip.thomas.wearify;

public interface Callback<I> {
    void onSuccess(I i);

    void onError();
}