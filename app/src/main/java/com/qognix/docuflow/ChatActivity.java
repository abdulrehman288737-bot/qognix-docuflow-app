package com.qognix.docuflow;
import android.os.Bundle; import android.widget.*; import androidx.appcompat.app.AppCompatActivity;
public class ChatActivity extends AppCompatActivity {
  @Override public void finish(){ super.finish(); Ui.backAnim(this); }
  LinearLayout msgs; EditText input; String history="";
  protected void onCreate(Bundle s){ super.onCreate(s); Ui.applyTheme(this); Ads.init(this); Sfx.init(this); LinearLayout root=Ui.root(this);
    root.addView(Ui.text(this,"AI Chat",24,Ui.TEXT,true)); root.addView(Ui.text(this,"Chat about: "+Store.docName(this),13,Ui.SUB,false));
    msgs=new LinearLayout(this); msgs.setOrientation(LinearLayout.VERTICAL); root.addView(msgs);
    add("Qognix","Ask me anything about your PDF. You can also ask for a specific page, like \"page no 86\". With a Gemini key I answer with AI; without one I search the PDF text on-device.");
    input=Ui.input(this,"Type your message",false); root.addView(input);
    Button send=Ui.button(this,"Send"); root.addView(send); send.setOnClickListener(v->send());
    setContentView(Ui.scroll(this,root));
  }
  void add(String who,String t){ if(who.equals("Qognix")) t=clean(t); msgs.addView(Ui.text(this,who+": "+t,15, who.equals("You")?Ui.LIGHT:Ui.TEXT,false)); }
  // Show AI answers as plain text (no raw markdown stars on screen).
  String clean(String t){ return t.replace("**","").replaceAll("(?m)^\\* ","• "); }
  void send(){ String m=input.getText().toString().trim(); if(m.isEmpty()) return; input.setText(""); add("You",m); String doc=Store.loadDoc(this); String key=Store.key(this);
    int ref=PageText.pageRef(m); int total=PageText.totalPages(doc);
    if(ref>0 && total>0 && ref>total){ add("Qognix",PageText.outOfRangeMessage(ref,total)); return; }
    if(key.isEmpty()){ add("Qognix",LocalTools.ask(doc,m)); return; }
    String context=PageText.contextFor(doc,m,250000);
    String prompt="You are a helpful study assistant. Answer using this document when relevant. The document text has [Page N] markers — cite the page like (Page 6) for facts you use. Ignore any website names, watermarks or page headers in the text and never mention them. If the answer is not in the provided text, say so clearly instead of guessing.\n\nDocument:\n"+context+"\n\nUser: "+m;
    GeminiClient.generate(key,prompt, new GeminiClient.Cb(){ public void ok(String t,String mo){ runOnUiThread(()->add("Qognix",t)); } public void fail(String msg){ runOnUiThread(()->add("Qognix",LocalTools.ask(doc,m)+"\n\n(Gemini failed, on-device search used)")); } });
  }
}
