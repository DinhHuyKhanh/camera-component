package com.example.camerax.presenter;
import com.example.camerax.model.IAPIService;
import com.example.camerax.view.IPlateView;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlatePresenter implements IPlatePresenter{
    private IPlateView plateView;
    public PlatePresenter(IPlateView plateView){
        this.plateView = plateView;
    }
    @Override
    public void sendImage(MultipartBody.Part image) {
        IAPIService.apiService.sendImagePlate(image).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()){
                    plateView.onComplete(response.isSuccessful());
                }else{
                    plateView.onError("error");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                plateView.onError(t.getMessage());
            }
        });
    }
}
