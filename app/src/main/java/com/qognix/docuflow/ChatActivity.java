package com.qognix.docuflow;
import android.os.Bundle; import android.widget.*; import androidx.appcompat.app.AppCompatActivity;
public class ChatActivity extends AppCompatActivity {
  LinearLayout msgs; EditText input; String history="";
  protected void onCreate(Bundle s){ super.onCreate(s); LinearLayout root=Ui.root(this);
    root.addView(Ui.text(this,"AI Chat",24,Ui.TEXT,true)); root.addView(Ui.text(this,"Chat about: "+Store.docName(this),13,Ui.SUB,false));
    msgs=new LinearLayout(this); msgs.setOrientation(LinearLayout.VERTICAL); root.addView(msgs);
    add("Qognix","Ask me anything about your PDF. With a Gemini key I answer with AI; without one I search the PDF text on-device.");
    input=Ui.input(this,"Type your message",false); root.addView(input);
    Button send=Ui.button(this,"Send"); root.addView(send); send.setOnClickListener(v->send());
    setContentView(Ui.scroll(this,root));
  }
  void add(String who,String t){ msgs.addView(Ui.text(this,who+": "+t,15, who.equals("You")?Ui.LIGHT:Ui.TEXT,false)); }
  void send(){ String m=input.getText().toString().trim(); if(m.isEmpty()) return; input.setText(""); add("You",m); String doc=Store.loadDoc(this); String key=Store.key(this);
    if(key.isEmpty()){ add("Qognix",LocalTools.ask(doc,m)); return; }
    String snippet=doc.length()>10000? doc.substring(0,10000): doc;
    GeminiClient.generate(key,"You are a helpful study assistant. Answer using this document when relevant.\n\nDocument:\n"+snippet+"\n\nUser: "+m, new GeminiClient.Cb(){ public void ok(String t,String mo){ runOnUiThread(()->add("Qognix",t)); } public void fail(String msg){ runOnUiThread(()->add("Qognix",LocalTools.ask(doc,m)+"\n\n(Gemini failed, on-device search used)")); } });
  }
}
