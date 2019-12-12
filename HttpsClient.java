/**
 * client to the server here the aim is to send pictures to the server and get back the prediction
 * from the server
 */
public class HttpsClient extends AsyncTask<JSONObject, String, String > {

    private String TAG = "HttpSClient";
    private OnTaskCompleted listener;
    private noConnection noCon;
    @SuppressLint("StaticFieldLeak")
    private Context context;
    private String crtificatePath; //the path to the self sign certificate in the asset folder!
    private List<String> allowedHostes = new ArrayList<>();
    private int READ_TIMEOUT;
    private int CONNECTION_TIMEOUT;


    public HttpsClient(OnTaskCompleted listener){
        //set context variables if required
        this.listener=listener;

    }

    public HttpsClient(OnTaskCompleted listener, noConnection noCon , Context mcontext){
        //set context variables if required
        this.listener=listener;
        this.context = mcontext;
        crtificatePath = Helper.getConfigValue(context, "crtificatePath");
        allowedHostes.add(Helper.getConfigValue(context, "allowedHoste1"));
        allowedHostes.add(Helper.getConfigValue(context, "allowedHoste2"));
        READ_TIMEOUT = Integer.parseInt(Objects.requireNonNull(Helper.getConfigValue(context, "READ_TIMEOUT")));
        CONNECTION_TIMEOUT = Integer.parseInt(Objects.requireNonNull(Helper.getConfigValue(context, "CONNECTION_TIMEOUT")));
    }

    public HttpsClient(OnTaskCompleted listener, noConnection noCon){
        //set context variables if required
        this.listener=listener;
        this.noCon = noCon;
    }



    /**
     * main pipeline funciton
     * @param params an array of Jsonobject, THE DATA IS ONLY THE FIRST OBJECT!
     * @return result, the server responde
     */
    @Override
    protected String doInBackground(JSONObject... params) {
        Log.i(TAG, "doInBackground started with the following input" );
        HttpsURLConnection urlConnection = null;
        String result = null;
        try {
            JSONObject mParams = params[0];
            String urlString = mParams.getString("urlString");
            String requestMethod = mParams.getString("requestMethod");
            boolean selfSignedCert = mParams.getBoolean("selfSignedCert");
            urlConnection = connectionSetting(requestMethod, urlString, selfSignedCert);
            if (urlConnection == null){
                Log.e(TAG, "doInBackground: failed in setting ");
                return null;
            }

            if (requestMethod.equals("GET")){
                result = getRequest(urlConnection);
            }
            else if (requestMethod.equals("POST")){
                String postData = mParams.getString("postData");
                result = postRequest(urlConnection, postData);
            }

        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return result;
    }

    /**
     *
     * @param requestMethod POST GET DELETE PUT ....
     * @param urlString the https string to connect to
     * @param selfSignedCert a bool responisable to mention if the url use self sign certificate or
     *                       one of the default
     * @return connection!
     */
    private HttpsURLConnection connectionSetting(String requestMethod, String urlString,
                                                  boolean selfSignedCert)
    {
        Log.d(TAG, "connectionSetting: ");
        HttpsURLConnection urlConnection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpsURLConnection) url.openConnection();
            if (selfSignedCert) {
                SSLContext sslContext = SslUtils.getSslContextForCertificateFile(crtificatePath, context);
                urlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                urlConnection.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        for (String allowHost : allowedHostes) {
                            if (allowHost.equals(hostname)) {
                                Log.d(TAG, "verify: the host " + hostname + " exist in allowed hosts");
                                return true;
                            }
                        }
                        Log.e(TAG, "verify:  " + hostname + " not in allowed hosts");
                        return false;
                    }
                });
            } else {
                SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                urlConnection.setSSLSocketFactory(sslsocketfactory);
            }

            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestMethod(requestMethod);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.e(TAG, "MalformedURLException: ", e);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "IOException: ",e );
        }
        return urlConnection;
    }

    /**
     * make a GET request
     * @param urlConnection the https connection
     * @return string or null as the server reponded
     */
    private String getRequest(HttpsURLConnection urlConnection){
        Log.d(TAG, "getRequest: ");
        String inputLine;
        String result = null;
        try{

            int code = urlConnection.getResponseCode();
            Log.d(TAG, "getRequest: responde code is" + code);
            InputStreamReader streamReader = new
                    InputStreamReader(urlConnection.getInputStream());
            //Create a new buffered reader and String Builder
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder serverRespond = new StringBuilder();
            //Check if the line we are reading is not null
            while((inputLine = reader.readLine()) != null){
                serverRespond.append(inputLine);
            }
            //Close our InputStream and Buffered reader
            reader.close();
            streamReader.close();
            //Set our result equal to our stringBuilder
            result = serverRespond.toString();
            Log.d(TAG, "getRequest: result" + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * run a POST request
     * read the postdata to byte send the request and read the return request
     * @param urlConnection the  https object conneciton
     * @param postData the data we eant to post
     * @return result, usully string as json
     */
    private String postRequest(HttpsURLConnection urlConnection, String postData){
        String result = null;
        try {
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setChunkedStreamingMode(0);

            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    out, StandardCharsets.UTF_8));
            writer.write(postData);
            Log.d(TAG, "sending to the server" + postData);
            writer.flush();

            int code = urlConnection.getResponseCode();
            if (code != 200) {
                throw new IOException("Invalid response from server: " + code);
            }

            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));
            String line;
            StringBuilder serverRespond = new StringBuilder();

            while ((line = rd.readLine()) != null) {
                Log.d("recive from the server:", line);
                serverRespond.append(line);
            }
            result = serverRespond.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * get the reutrn of doInBackground
     * @param s server return!
     */
    @Override
    public void onPostExecute(String s){
        Log.d(TAG, "onPostExecute: started");
        try {
            if (s == null){
                Log.e(TAG, "onPostExecute: No connection to server," +
                        " check the the mobile has internet connection");
                noCon.noConnection();
            } else {

                Log.i(TAG, "onPostExecute " + s);
                JSONObject prediction = new JSONObject(s);
                listener.onTaskCompleted(prediction);
            }
        }catch (JSONException err){
            Log.e("Error", err.toString());
        }
    }
}
