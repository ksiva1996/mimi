package com.leagueofshadows.enc.REST;

import android.content.Context;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

import static com.android.volley.Request.Method.POST;

public class RESTHelper {

    //TODO: add urls
    public static final String TOKEN_UPDATE_ENDPOINT = "https://loschat.000webhostapp.com/update_users.php";
    public static final String SEND_NOTIFICATION_ENDPOINT = "https://loschat.000webhostapp.com/send_message.php";
    public static final String SEND_STATUS_ENDPOINT = "https://loschat.000webhostapp.com/message_status.php";

    public static final String FIREBASE_TOKEN = "FIREBASE_TOKEN";
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String USER_ID = "USER_ID";
    private Response.Listener<String> responseListener;
    private Response.ErrorListener errorListener;

    private Context context;
    private String logKey = "logkey";

    public RESTHelper(Context context) {
        this.context = context;

         responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(logKey,response);
            }
        };

        errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(logKey,error.toString());
            }
        };

    }




    public void test(String logkey,final Map<String, String> params, final String endpoint, Response.Listener<String> listener, Response.ErrorListener error)
    {
        this.logKey = logkey;
        if(listener==null)
            listener = responseListener;
        if(error == null)
            error = errorListener;

        StringRequest stringRequest = new StringRequest(POST, endpoint,listener,error){
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };
        VolleyHelper.getInstance(context).addToRequestQueue(stringRequest);
    }


    void sendNotification(final String userId,final String accessToken)
    {

    }

    void updateToken(final String token, final String userId, final String accessToken)
    {

    }

}
