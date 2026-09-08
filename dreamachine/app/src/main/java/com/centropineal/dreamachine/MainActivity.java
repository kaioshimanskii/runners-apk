package com.centropineal.dreamachine;

import android.Manifest;
import android.app.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;

public class MainActivity extends Activity {
  static final int REQ_CAMERA=91;
  final Handler h=new Handler(Looper.getMainLooper());
  FrameLayout root; ScrollView controls; StimulusView stim;
  Spinner modeSp,patternSp,waveSp,durationSp; SeekBar freqBar,brightBar;
  TextView freqTxt,brightTxt,torchTxt,timerTxt; Button startBtn,testBtn,stopBtn;
  TorchEngine torch; boolean running=false; int mode=0,duration=60; float hz=8f,brightness=.65f; String pattern="PURE",wave="PULSE"; long startMs,endMs;

  @Override public void onCreate(Bundle b){ super.onCreate(b); getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); torch=new TorchEngine(this); build(); safety(); }
  int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);} 
  TextView text(String s,float size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0,1.12f);return t;}
  TextView section(String s){TextView t=text(s,13,Color.rgb(124,239,200));t.setTypeface(null,1);t.setPadding(0,dp(18),0,dp(7));return t;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setMinHeight(dp(54));return b;}
  Spinner spinner(String... a){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,a));return s;}

  void build(){
    root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(5,7,10));setContentView(root);
    controls=new ScrollView(this); LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(18),dp(18),dp(18),dp(28));controls.addView(p);root.addView(controls,new FrameLayout.LayoutParams(-1,-1));
    TextView title=text("DREAMACHINE DIY",28,Color.WHITE);title.setTypeface(null,1);p.addView(title);p.addView(text("Brion Gysin + Ian Sommerville • Android experimental",13,Color.rgb(160,177,196)));
    TextView warn=text("⚠ Luz pulsante pode provocar crises em pessoas fotossensíveis. Não use com epilepsia/fotossensibilidade. No modo lanterna use somente luz refletida/difusa; nunca aponte o LED diretamente para os olhos.",12,Color.rgb(255,165,100));warn.setBackgroundColor(Color.rgb(35,18,14));warn.setPadding(dp(12),dp(12),dp(12),dp(12));LinearLayout.LayoutParams wlp=new LinearLayout.LayoutParams(-1,-2);wlp.topMargin=dp(14);p.addView(warn,wlp);

    p.addView(section("MODO")); modeSp=spinner("Tela — Dreamachine","Lanterna difusa","Híbrido — tela + lanterna","Afterimage Lab — 1 Hz / 52%");p.addView(modeSp);
    p.addView(section("PADRÃO DA TELA")); patternSp=spinner("Flicker puro","Radial","Espiral","Grade","Túnel / anéis","Honeycomb");p.addView(patternSp);
    p.addView(section("FORMA DA MODULAÇÃO")); waveSp=spinner("Pulso 50/50 — Dreamachine","Senoidal — suave");p.addView(waveSp);
    p.addView(section("FREQUÊNCIA"));freqTxt=text("8.0 Hz",18,Color.WHITE);freqTxt.setTypeface(null,1);p.addView(freqTxt);freqBar=new SeekBar(this);freqBar.setMax(100);freqBar.setProgress(50);p.addView(freqBar);p.addView(text("Controle: 3–13 Hz. O preset histórico da Dreamachine fica aproximadamente na região alfa; o modo Afterimage fixa 1 Hz.",11,Color.rgb(150,165,185)));
    p.addView(section("BRILHO MÁXIMO DA TELA"));brightTxt=text("65%",16,Color.WHITE);p.addView(brightTxt);brightBar=new SeekBar(this);brightBar.setMax(80);brightBar.setProgress(45);p.addView(brightBar);
    p.addView(section("DURAÇÃO"));durationSp=spinner("30 s","60 s","120 s","180 s","300 s");durationSp.setSelection(1);p.addView(durationSp);
    p.addView(section("LANTERNA"));torchTxt=text(torch.description(),12,Color.rgb(178,193,211));p.addView(torchTxt);testBtn=button("Testar lanterna por 2 segundos");p.addView(testBtn);p.addView(text("O Android pode solicitar permissão de câmera apenas para acessar o LED. O app não mostra preview e não grava vídeo.",11,Color.rgb(150,165,185)));
    p.addView(section("COMO USAR"));p.addView(text("1. Ambiente escuro e confortável.\n2. Lanterna: aponte para um difusor ou parede.\n3. Escolha modo, padrão e frequência.\n4. Inicie; para a experiência clássica, feche os olhos.\n5. Toque STOP imediatamente se houver desconforto.",13,Color.rgb(215,222,233)));
    startBtn=button("▶ INICIAR SESSÃO");LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(64));slp.topMargin=dp(22);p.addView(startBtn,slp);
    p.addView(text("Afterimage 1 Hz: a tela usa modulação senoidal com profundidade de 52%. Se o aparelho Android 13+ expuser níveis de força do flash, a lanterna aproxima essa modulação por níveis discretos; caso contrário usa ON/OFF. Não é calibração fotométrica do paper.",11,Color.rgb(126,145,168)));

    stim=new StimulusView(this);stim.setVisibility(View.GONE);root.addView(stim,new FrameLayout.LayoutParams(-1,-1));
    stopBtn=button("■ STOP");stopBtn.setVisibility(View.GONE);FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(112),dp(52),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);bp.bottomMargin=dp(28);root.addView(stopBtn,bp);
    timerTxt=text("00:00",18,Color.WHITE);timerTxt.setTypeface(android.graphics.Typeface.MONOSPACE,1);timerTxt.setGravity(Gravity.CENTER);timerTxt.setBackgroundColor(Color.argb(130,0,0,0));timerTxt.setVisibility(View.GONE);FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(dp(100),dp(42),Gravity.TOP|Gravity.CENTER_HORIZONTAL);tp.topMargin=dp(24);root.addView(timerTxt,tp);

    modeSp.setOnItemSelectedListener(new Sel(){public void pick(int x){mode=x;boolean a=mode==3;freqBar.setEnabled(!a);waveSp.setEnabled(!a);if(a)freqTxt.setText("1.0 Hz • profundidade 52%");else updateHz();torchTxt.setText(torch.description());}});
    patternSp.setOnItemSelectedListener(new Sel(){public void pick(int x){pattern=new String[]{"PURE","RADIAL","SPIRAL","GRID","TUNNEL","HONEY"}[x];}});
    waveSp.setOnItemSelectedListener(new Sel(){public void pick(int x){wave=x==0?"PULSE":"SINE";}});
    durationSp.setOnItemSelectedListener(new Sel(){public void pick(int x){duration=new int[]{30,60,120,180,300}[x];}});
    freqBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int x,boolean f){updateHz();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
    brightBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int x,boolean f){brightness=(20+x)/100f;brightTxt.setText(Math.round(brightness*100)+"%");}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
    startBtn.setOnClickListener(v->prepare());stopBtn.setOnClickListener(v->stop("interrompida"));testBtn.setOnClickListener(v->testTorch());
  }

  abstract class Sel implements AdapterView.OnItemSelectedListener{public abstract void pick(int p);public void onItemSelected(AdapterView<?>a,View v,int p,long id){pick(p);}public void onNothingSelected(AdapterView<?>a){}}
  void updateHz(){hz=3f+freqBar.getProgress()/10f;freqTxt.setText(String.format(Locale.US,"%.1f Hz",hz));}
  boolean needTorch(){return mode==1||mode==2||mode==3;} boolean needScreen(){return mode==0||mode==2||mode==3;}

  void safety(){new AlertDialog.Builder(this).setTitle("Antes de usar").setMessage("Este app produz luz pulsante. Pessoas com epilepsia, convulsões, fotossensibilidade ou reação adversa a flicker não devem usar. Use sessões curtas e pare se houver desconforto. No modo lanterna, use apenas luz refletida/difusa.").setCancelable(false).setNegativeButton("Sair",(d,w)->finish()).setPositiveButton("Entendo",null).show();}
  void prepare(){
    if(needTorch()&&!torch.available()){Toast.makeText(this,"Nenhum LED traseiro compatível foi encontrado.",Toast.LENGTH_LONG).show();return;}
    if(needTorch()&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},REQ_CAMERA);return;}
    new AlertDialog.Builder(this).setTitle("Iniciar sessão?").setMessage(needTorch()?"Confirme que a lanterna está iluminando uma superfície difusora/refletora e não diretamente os olhos.":"Mantenha o aparelho a uma distância confortável. Toque STOP se sentir desconforto.").setNegativeButton("Cancelar",null).setPositiveButton("Iniciar",(d,w)->start()).show();
  }
  @Override public void onRequestPermissionsResult(int r,String[]p,int[]g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_CAMERA&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)prepare();}
  void start(){if(running)return;running=true;startMs=SystemClock.elapsedRealtime();endMs=startMs+duration*1000L;if(mode==3){hz=1f;wave="SINE";}controls.setVisibility(View.GONE);stim.setVisibility(View.VISIBLE);stopBtn.setVisibility(View.VISIBLE);timerTxt.setVisibility(View.VISIBLE);immersive(true);WindowManager.LayoutParams lp=getWindow().getAttributes();lp.screenBrightness=needScreen()?brightness:.02f;getWindow().setAttributes(lp);stim.configure(pattern,hz,wave,brightness,mode==3,needScreen());stim.start();if(needTorch())torch.start(mode==3?TorchEngine.GERLING:TorchEngine.PULSE,hz);try{new ToneGenerator(AudioManager.STREAM_MUSIC,35).startTone(ToneGenerator.TONE_PROP_BEEP,120);}catch(Exception ignored){}h.post(tick);}
  final Runnable tick=new Runnable(){public void run(){if(!running)return;long now=SystemClock.elapsedRealtime(),e=now-startMs;timerTxt.setText(String.format(Locale.US,"%02d:%02d",e/60000,(e/1000)%60));if(now>=endMs){stop("concluída");return;}h.postDelayed(this,100);}};
  void stop(String why){if(!running)return;running=false;h.removeCallbacks(tick);stim.stop();torch.stop();WindowManager.LayoutParams lp=getWindow().getAttributes();lp.screenBrightness=-1;getWindow().setAttributes(lp);stim.setVisibility(View.GONE);stopBtn.setVisibility(View.GONE);timerTxt.setVisibility(View.GONE);controls.setVisibility(View.VISIBLE);immersive(false);Toast.makeText(this,"Sessão "+why+".",Toast.LENGTH_SHORT).show();}
  void testTorch(){if(!torch.available()){Toast.makeText(this,"Lanterna não encontrada.",Toast.LENGTH_LONG).show();return;}if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},REQ_CAMERA);return;}testBtn.setEnabled(false);torch.test(()->testBtn.setEnabled(true));}
  void immersive(boolean on){getWindow().getDecorView().setSystemUiVisibility(on?(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE):View.SYSTEM_UI_FLAG_VISIBLE);}
  @Override protected void onPause(){super.onPause();if(running)stop("interrompida ao sair do app");torch.stop();}
  @Override protected void onDestroy(){torch.stop();super.onDestroy();}
}
