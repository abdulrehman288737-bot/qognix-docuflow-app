package com.qognix.docuflow;
import java.io.*; import java.net.*; import org.json.*;
public class GeminiClient {
  public interface Cb { void ok(String text,String model); void fail(String msg); }
  static final String[] MODELS={"gemini-3.5-flash-lite","gemini-2.5-flash","gemini-2.5-flash-lite"};
  public static void generate(final String key, final String prompt, final Cb cb){
    new Thread(new Runnable(){ public void run(){
      String last="Gemini failed";
      for(String m: MODELS){ try{ String out=call(key,m,prompt); cb.ok(out,m); return; }catch(Exception e){ last=m+": "+e.getMessage(); } }
      cb.fail(last);
    }}).start();
  }
  static String call(String key,String model,String prompt) throws Exception {
    URL u=new URL("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent");
    HttpURLConnection c=(HttpURLConnection)u.openConnection(); c.setRequestMethod("POST"); c.setConnectTimeout(30000); c.setReadTimeout(60000); c.setDoOutput(true);
    c.setRequestProperty("Content-Type","application/json"); c.setRequestProperty("x-goog-api-key",key);
    JSONObject part=new JSONObject(); part.put("text",prompt);
    JSONObject parts=new JSONObject(); parts.put("parts",new JSONArray().put(part));
    JSONObject body=new JSONObject(); body.put("contents",new JSONArray().put(parts));
    OutputStream o=c.getOutputStream(); o.write(body.toString().getBytes("UTF-8")); o.close();
    int code=c.getResponseCode(); InputStream in= code>=200&&code<300? c.getInputStream(): c.getErrorStream();
    StringBuilder sb=new StringBuilder(); BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8")); String line; while((line=r.readLine())!=null) sb.append(line); r.close();
    if(code<200||code>=300) throw new Exception("HTTP "+code+" "+sb.toString());
    JSONObject j=new JSONObject(sb.toString());
    return j.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
  }
}
