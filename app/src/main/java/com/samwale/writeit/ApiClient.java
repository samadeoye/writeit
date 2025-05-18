package com.samwale.writeit;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "https://api.kpakpandohub.com/"; //"http://10.0.2.2:8080/";
    private static Retrofit retrofit = null;

    public static JournalApi getJournalApi() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(JournalApi.class);
    }
}
