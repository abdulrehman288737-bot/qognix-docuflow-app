package com.qognix.docuflow;
import android.content.Context; import android.content.SharedPreferences; import java.io.*;
public class Store {
  public static SharedPreferences prefs(Context c){ return c.getSharedPreferences("qognix", Context.MODE_PRIVATE); }
  public static String key(Context c){ return prefs(c).getString("gemini_key",""); }
  public static void setKey(Context c,String k){ prefs(c).edit().putString("gemini_key",k.trim()).apply(); }
  public static String docName(Context c){ return prefs(c).getString("doc_name",""); }
  public static File docFile(Context c){ return new File(c.getFilesDir(),"doc.txt"); }
  public static void saveDoc(Context c,String name,String text) throws Exception { prefs(c).edit().putString("doc_name",name).apply(); FileOutputStream o=new FileOutputStream(docFile(c)); o.write(text.getBytes("UTF-8")); o.close(); }
  public static String loadDoc(Context c){ try{ File f=docFile(c); if(!f.exists()) return ""; byte[] b=new byte[(int)f.length()]; FileInputStream i=new FileInputStream(f); int r=i.read(b); i.close(); return new String(b,"UTF-8"); }catch(Exception e){ return ""; } }
}
