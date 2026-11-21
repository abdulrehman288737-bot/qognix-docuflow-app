package com.qognix.docuflow;
import android.app.Activity; import android.content.Intent; import android.database.Cursor; import android.net.Uri; import android.os.Bundle; import android.provider.OpenableColumns; import android.view.View; import android.widget.*;
import androidx.activity.result.*; import androidx.activity.result.contract.ActivityResultContracts; import androidx.appcompat.app.AppCompatActivity;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader; import com.tom_roush.pdfbox.pdmodel.PDDocument; import com.tom_roush.pdfbox.text.PDFTextStripper; import java.io.InputStream;
public class MainActivity extends AppCompatActivity {
  TextView status; EditText keyInput;
  ActivityResultLauncher<String[]> picker;
  protected void onCreate(Bundle s){ super.onCreate(s); PDFBoxResourceLoader.init(this); picker=registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri->{ if(uri!=null) loadPdf(uri); });
    LinearLayout root=Ui.root(this);
    root.addView(Ui.text(this,"Qognix DocuFlow",26,Ui.TEXT,true));
    root.addView(Ui.text(this,"Your Documents. Smarter with AI.",15,Ui.LIGHT,false));
    root.addView(Ui.text(this,"Native app — no website loads inside. All tools run in the app itself.",13,Ui.SUB,false));
    Button pick=Ui.button(this,"Upload your PDF"); root.addView(pick);
    status=Ui.text(this, Store.docName(this).isEmpty()?"No PDF loaded yet.":("Loaded: "+Store.docName(this)),14,Ui.SUB,false); root.addView(status);
    pick.setOnClickListener(v->picker.launch(new String[]{"application/pdf"}));
    Ui.card(this,root,"Gemini AI (optional, free key)","Paste a free key from aistudio.google.com. It is stored only on this phone, in the app's private storage — never in the app code. Without a key, tools still work in on-device mode.");
    keyInput=Ui.input(this,"Paste Gemini API key",false); if(!Store.key(this).isEmpty()) keyInput.setText(Store.key(this)); root.addView(keyInput);
    Button save=Ui.button(this,"Save, Test & Use Key"); root.addView(save);
    save.setOnClickListener(v->{ String k=keyInput.getText().toString().trim(); if(k.isEmpty()){ toast("Paste a key first"); return; } Store.setKey(this,k); status.setText("Testing Gemini key..."); GeminiClient.generate(k,"Reply with exactly: OK", new GeminiClient.Cb(){ public void ok(String t,String m){ runOnUiThread(()->{ status.setText("Gemini ON — working model: "+m); toast("Gemini key works: "+m); }); } public void fail(String msg){ runOnUiThread(()->status.setText("Gemini test failed: "+msg)); } }); });
    String[][] tools={{"summary","Summary — short notes from your PDF"},{"mcq","MCQs — multiple choice questions"},{"quiz","Quiz — fill-in questions with answers"},{"ask","Ask the PDF — answer from your document"},{"translate","Translate — translate PDF text"},{"chat","AI Chat — chat about your document"}};
    for(String[] t: tools){ Button b=Ui.button(this,t[1]); final String mode=t[0]; b.setOnClickListener(v->{ if(Store.loadDoc(this).isEmpty()){ toast("Upload a PDF first"); return; } Intent i = mode.equals("chat")? new Intent(this,ChatActivity.class): new Intent(this,FeatureActivity.class).putExtra("mode",mode); startActivity(i); }); root.addView(b); }
    root.addView(Ui.text(this,"Banner ads note: the website's Adsterra banners needed a web view, so they are NOT included in this native version — no fake ads, no popunder.",12,Ui.SUB,false));
    setContentView(Ui.scroll(this,root));
  }
  void toast(String m){ Toast.makeText(this,m,Toast.LENGTH_LONG).show(); }
  String nameOf(Uri uri){ try{ Cursor c=getContentResolver().query(uri,null,null,null,null); if(c!=null&&c.moveToFirst()){ int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); String n=i>=0?c.getString(i):"document.pdf"; c.close(); return n; } }catch(Exception e){} return "document.pdf"; }
  void loadPdf(Uri uri){ final String name=nameOf(uri); status.setText("Reading PDF: "+name+" ...");
    new Thread(()->{ try{ InputStream in=getContentResolver().openInputStream(uri); PDDocument doc=PDDocument.load(in); int pages=doc.getNumberOfPages(); String text=new PDFTextStripper().getText(doc); doc.close(); if(in!=null) in.close(); if(text==null) text=""; if(text.length()>200000) text=text.substring(0,200000); final String ft=text; Store.saveDoc(this,name,ft); runOnUiThread(()->{ status.setText("Loaded: "+name+" — "+pages+" pages, "+ft.length()+" characters read."); toast("PDF loaded"); }); }catch(Exception e){ runOnUiThread(()->status.setText("Could not read this PDF: "+e.getMessage())); } }).start(); }
}
