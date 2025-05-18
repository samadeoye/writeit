package com.samwale.writeit;

import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Body;
import retrofit2.http.Path;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface JournalApi {

    @GET("journals")
    Call<JournalResponse> getJournals(@Header("deviceId") String deviceId, @Query("page") int page, @Query("limit") int limit);

    @POST("journals")
    Call<JournalModel> createJournal(@Header("deviceId") String deviceId, @Body JournalModel journal);

    @PUT("journals/{id}")
    Call<JournalModel> updateJournal(@Header("deviceId") String deviceId, @Path("id") int id, @Body JournalModel journal);

    @DELETE("journals/{id}")
    Call<Void> deleteJournal(@Header("deviceId") String deviceId, @Path("id") int id);
}