package com.centropineal.dreamachine;

import android.content.Context;
import android.graphics.*;
import android.view.*;

public class StimulusView extends View implements Choreographer.FrameCallback {
  final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); final Path path=new Path();
  boolean active=false,screen=true,gerling=false; long startNs; float hz=8f,brightness=.65f; String pattern="PURE",wave="PULSE";
  public StimulusView(Context c){super(c);setBackgroundColor(Color.BLACK);}
  public void configure(String pat,float f,String w,float b,boolean g,boolean s){pattern=pat;hz=f;wave=w;brightness=b;gerling=g;screen=s;}
  public void start(){active=true;startNs=System.nanoTime();Choreographer.getInstance().postFrameCallback(this);} public void stop(){active=false;Choreographer.getInstance().removeFrameCallback(this);invalidate();}
  @Override public void doFrame(long n){if(active){invalidate();Choreographer.getInstance().postFrameCallback(this);}}
  @Override protected void onDraw(Canvas c){super.onDraw(c);if(!active||!screen){c.drawColor(Color.BLACK);return;}double t=(System.nanoTime()-startNs)/1e9,ph=2*Math.PI*hz*t;float lum;if(gerling){lum=.5f*(1+.52f*(float)Math.sin(ph));lum=Math.max(.03f,Math.min(1,lum));}else if("SINE".equals(wave))lum=(float)(.5+.5*Math.sin(ph));else lum=Math.sin(ph)>=0?1f:.02f;lum*=brightness;int v=(int)(255*Math.max(0,Math.min(1,lum)));if("PURE".equals(pattern)){c.drawColor(Color.rgb(v,v,v));return;}c.drawColor(Color.BLACK);p.setColor(Color.rgb(v,v,v));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,getWidth()/250f));float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.46f;if("RADIAL".equals(pattern))radial(c,cx,cy,r);else if("SPIRAL".equals(pattern))spiral(c,cx,cy,r);else if("GRID".equals(pattern))grid(c,r);else if("TUNNEL".equals(pattern))tunnel(c,cx,cy,r);else honey(c,r);}
  void radial(Canvas c,float cx,float cy,float r){for(int i=0;i<36;i++){double a=i*Math.PI*2/36;c.drawLine(cx,cy,cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,p);}}
  void spiral(Canvas c,float cx,float cy,float r){for(int arm=0;arm<2;arm++){path.reset();for(int i=0;i<=900;i++){double th=i*.045+arm*Math.PI;float rr=r*i/900f,x=cx+(float)Math.cos(th)*rr,y=cy+(float)Math.sin(th)*rr;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}c.drawPath(path,p);}}
  void grid(Canvas c,float r){float l=getWidth()/2f-r,rr=getWidth()/2f+r,t=getHeight()/2f-r,b=getHeight()/2f+r;for(int i=0;i<=12;i++){float x=l+(rr-l)*i/12,y=t+(b-t)*i/12;c.drawLine(x,t,x,b,p);c.drawLine(l,y,rr,y,p);}}
  void tunnel(Canvas c,float cx,float cy,float r){for(int i=1;i<=16;i++)c.drawCircle(cx,cy,r*i/16f,p);}
  void honey(Canvas c,float r){float s=Math.max(16,getWidth()/18f),hh=(float)(Math.sqrt(3)*s),cx=getWidth()/2f,cy=getHeight()/2f,l=cx-r,rr=cx+r,t=cy-r,b=cy+r;for(int row=0,y=(int)(t-hh);y<b+hh;row++,y+=hh){float off=row%2==0?0:1.5f*s;for(float x=l-s+off;x<rr+s;x+=3*s)hex(c,x,y,s);}}
  void hex(Canvas c,float x,float y,float s){path.reset();for(int i=0;i<6;i++){double a=Math.PI*i/3;float px=x+(float)Math.cos(a)*s,py=y+(float)Math.sin(a)*s;if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}path.close();c.drawPath(path,p);}
}
