package com.example.camerax.presenter;

import okhttp3.MultipartBody;

public interface IPlatePresenter {
    void sendImage(MultipartBody.Part image);
}
