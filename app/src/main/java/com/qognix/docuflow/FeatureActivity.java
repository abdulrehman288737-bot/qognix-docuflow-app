package com.qognix.docuflow;
import android.content.Intent; import android.net.Uri; import android.os.Bundle; import android.widget.*; import androidx.appcompat.app.AppCompatActivity; import java.io.OutputStream;
public class FeatureActivity extends AppCompatActivity {
  String mode, result=""; TextView out; EditText extra; Spinner lang; String[] langs={"Urdu","English","Arabic","Hindi","Spanish","French"};
  protected void onCreate(Bundle s){ super.onCreate(s); mode=getIntent().getStringExtra("mode"); if(mode==null) mode="summary";
    LinearLayout root=Ui.root(this); String title = mode.equals("mcq")?"MCQs": mode.equals("quiz")?"Quiz": mode.equals("ask")?"Ask the PDF": mode.equals("translate")?"Translate":"Summary";
    root.addView(Ui.text(this,title,24,Ui.TEXT,true)); root.addView(Ui.text(this,"Document: "+Store.docName(this),13,Ui.SUB,false));
    if(mode.equals("ask")){ extra=Ui.input(this,"Type your question about the PDF",true); root.addView(extra); }
    if(mode.equals("translate")){ lang=new Spinner(this); lang.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,langs)); root.addView(lang); }
    Button go=Ui.button(this,"Generate"); root.addView(go);
    out=Ui.text(this,"Press Generate.",15,Ui.TEXT,false); out.setTextIsSelectable(true); root.addView(out);
    Button dl=Ui.button(this,"Download result (.txt)"); root.addView(dl);
    go.setOnClickListener(v->generate()); dl.setOnClickListener(v->{ if(result.isEmpty()){ Toast.makeText(this,"Generate first",Toast.LENGTH_SHORT).show(); return; } Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TITLE,"qognix-"+mode+".txt"); startActivityForResult(i,7); });
    setContentView(Ui.scroll(this,root));
  }
  protected void onActivityResult(int req,int res,Intent data){ super.onActivityResult(req,res,data); if(req==7 && res==RESULT_OK && data!=null && data.getData()!=null){ try{ OutputStream o=getContentResolver().openOutputStream(data.getData()); o.write(result.getBytes("UTF-8")); o.close(); Toast.makeText(this,"Saved",Toast.LENGTH_LONG).show(); }catch(Exception e){ Toast.makeText(this,"Save failed: "+e.getMessage(),Toast.LENGTH_LONG).show(); } } }
  void generate(){ String doc=Store.loadDoc(this); if(doc.isEmpty()){ out.setText("No PDF loaded. Go back and upload a PDF first."); return; } out.setText("Working...");
    String key=Store.key(this); String q = extra!=null? extra.getText().toString():""; String target = lang!=null? langs[lang.getSelectedItemPosition()]:"";
    if(key.isEmpty()){ result=local(doc,q); runOnUiThread(()->out.setText(result)); return; }
    String snippet=doc.length()>12000? doc.substring(0,12000): doc;
    String prompt;
    if(mode.equals("summary")) prompt="Summarize this document clearly for a student, with headings and bullet points:\n\n"+snippet;
    else if(mode.equals("mcq")) prompt="Make 10 MCQs with 4 options and the correct answer from this document:\n\n"+snippet;
    else if(mode.equals("quiz")) prompt="Make a 10-question quiz with answers from this document:\n\n"+snippet;
    else if(mode.equals("ask")) prompt="Answer this question using only this document. Question: "+q+"\n\nDocument:\n"+snippet;
    else if(mode.equals("translate")) prompt="Translate this document text into "+target+", keep the meaning and formatting:\n\n"+snippet;
    else prompt="Summarize:\n\n"+snippet;
    GeminiClient.generate(key,prompt,new GeminiClient.Cb(){ public void ok(String t,String m){ result=t+"\n\n— Generated with Gemini ("+m+")"; runOnUiThread(()->out.setText(result)); } public void fail(String msg){ result=local(doc,q)+"\n\n(Gemini failed, on-device mode used: "+msg+")"; runOnUiThread(()->out.setText(result)); } });
  }
  String local(String doc,String q){ if(mode.equals("summary")) return LocalTools.summary(doc); if(mode.equals("mcq")) return LocalTools.mcqs(doc,false); if(mode.equals("quiz")) return LocalTools.mcqs(doc,true); if(mode.equals("ask")) return LocalTools.ask(doc,q); if(mode.equals("translate")) return "Translation needs a Gemini key in this native app. Paste your free key on the home screen, then try again.\n\nOriginal text preview:\n\n"+(doc.length()>2000?doc.substring(0,2000):doc); return LocalTools.summary(doc); }
}
