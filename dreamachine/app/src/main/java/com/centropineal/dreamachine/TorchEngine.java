package com.centropineal.dreamachine;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.*;
import android.os.*;

public class TorchEngine {
  public static final int PULSE=0,GERLING=1; final CameraManager cm; final Handler h=new Handler(Looper.getMainLooper()); String id; int max=1,mode; float hz; boolean active=false,on=false; long start;
  public TorchEngine(Activity a){cm=(CameraManager)a.getSystemService(Context.CAMERA_SERVICE);discover();}
  void discover(){try{for(String x:cm.getCameraIdList()){CameraCharacteristics c=cm.getCameraCharacteristics(x);Boolean f=c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);Integer face=c.get(CameraCharacteristics.LENS_FACING);if(Boolean.TRUE.equals(f)&&(face==null||face==CameraCharacteristics.LENS_FACING_BACK)){id=x;if(Build.VERSION.SDK_INT>=33){Integer m=c.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);if(m!=null)max=Math.max(1,m);}break;}}}catch(Exception ignored){}}
  public boolean available(){return id!=null;} public String description(){if(id==null)return "LED traseiro não detectado.";return Build.VERSION.SDK_INT>=33&&max>1?"LED detectado • "+max+" níveis de força disponíveis.":"LED detectado • controle ON/OFF disponível.";}
  public void start(int m,float f){stop();if(id==null)return;mode=m;hz=f;active=true;start=SystemClock.elapsedRealtime();h.post(loop);} public void stop(){active=false;h.removeCallbacks(loop);off();}
  public void test(Runnable done){if(id==null){done.run();return;}set(1);h.postDelayed(()->{off();done.run();},2000);}
  final Runnable loop=new Runnable(){public void run(){if(!active)return;double t=(SystemClock.elapsedRealtime()-start)/1000.0;if(mode==PULSE){boolean want=Math.sin(2*Math.PI*hz*t)>=0;if(want!=on){if(want)set(max);else off();}h.postDelayed(this,5);}else if(Build.VERSION.SDK_INT>=33&&max>1){double target=.5*(1+.52*Math.sin(2*Math.PI*t));int level=Math.max(1,Math.min(max,(int)Math.round(target*max)));set(level);h.postDelayed(this,50);}else{boolean want=Math.sin(2*Math.PI*t)>=0;if(want!=on){if(want)set(1);else off();}h.postDelayed(this,10);}}};
  void set(int level){if(id==null)return;try{if(Build.VERSION.SDK_INT>=33&&max>1)cm.turnOnTorchWithStrengthLevel(id,Math.max(1,Math.min(max,level)));else cm.setTorchMode(id,true);on=true;}catch(Exception ignored){}}
  void off(){if(id==null)return;try{cm.setTorchMode(id,false);}catch(Exception ignored){}on=false;}
}
