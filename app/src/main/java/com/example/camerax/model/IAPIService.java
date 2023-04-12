package com.example.camerax.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface IAPIService {
    Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // Thiết lập timeout kết nối
            .readTimeout(5, TimeUnit.SECONDS) // Thiết lập timeout đọc dữ liệu
            .writeTimeout(5, TimeUnit.SECONDS) // Thiết lập timeout ghi dữ liệu
            .build();
    IAPIService apiService = new Retrofit.Builder()
            .baseUrl("http:192.168.1.15:8000")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient)
            .build()
            .create(IAPIService.class);

    @Multipart
    @POST("/api/v1/plates")
    Call<ResponseBody> sendImagePlate(@Part MultipartBody.Part image);



}
