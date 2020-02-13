package kim.jonathan.imagelabeler;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.kittinunf.fuel.Fuel;
import com.github.kittinunf.fuel.core.FuelError;
import com.github.kittinunf.fuel.core.Request;
import com.github.kittinunf.fuel.core.Response;
import com.github.kittinunf.fuel.core.ResponseHandler;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import static android.graphics.Bitmap.*;

public class MainActivity extends AppCompatActivity {

    private Button cameraBTN;
    private ImageView imageView;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraBTN = findViewById(R.id.takePictureButton);
        imageView = findViewById(R.id.previewImage);

        cameraBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 0);
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Bitmap bitmap = BitmapFactory.decodeFile((data.getAbsoultePath());
        bitmap = (Bitmap) data.getExtras().get("data");
        imageView.setImageBitmap(bitmap);


    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    bitmap.compress(CompressFormat.JPEG, 90, byteStream);

    String base64Data = Base64.encodeToString(byteStream.toByteArray(), Base64.URL_SAFE);

    String requestURL =
            "https://vision.googleapis.com/v1/images:annotate?key=" +
                    getResources().getString(R.string.mykey);

    // Create an array containing
    // the LABEL_DETECTION feature
    JSONArray features = new JSONArray();
    JSONObject feature = new JSONObject();
        try {
            feature.put("type", "LABEL_DETECTION");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        features.put(feature);

    // Create an object containing
    // the Base64-encoded image data
    JSONObject imageContent = new JSONObject();
        try {
            imageContent.put("content", base64Data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Put the array and object into a single request
    // and then put the request into an array of requests
    JSONArray requests = new JSONArray();
    JSONObject request = new JSONObject();
        try {
            request.put("image", imageContent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            request.put("features", features);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        requests.put(request);
    JSONObject postData = new JSONObject();
        try {
            postData.put("requests", requests);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Convert the JSON into a
    // string
    String body = postData.toString();

    Fuel.INSTANCE.post(requestURL, null)
            .header("content-length", body.length())
            .header("content-type", "application/json")
            .body(body.getBytes(), Charset.forName("UTF-8"))
            .responseString(new ResponseHandler<String>() {
                @Override
                public void success(@NotNull Request request,
                        @NotNull Response response,
                        String data) {
                    // Access the labelAnnotations arrays
                    try {
                        JSONArray labels = new JSONObject(data)
                                .getJSONArray("responses")
                                .getJSONObject(0)
                                .getJSONArray("labelAnnotations");

                        // Loop through the array and extract the
                        // description key for each item

                        String results = "";

                        for(int i=0;i<labels.length();i++) {
                            try {
                                results = results +
                                        labels.getJSONObject(i).getString("description") +
                                        "\n";
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        // Display the annotations inside the TextView
                        ((TextView)findViewById(R.id.resultsText)).setText(results);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
                @Override
                public void failure(@NotNull Request request,
                                    @NotNull Response response,
                                    @NotNull FuelError fuelError) {}
            });
    }

}
