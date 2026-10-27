package com.nsp.launcher;
import android.content.*; import android.os.*; import android.util.Xml;
import org.xmlpull.v1.XmlPullParser; import java.net.*; import java.io.*; import java.util.*;
public final class RssService {
 public interface Callback { void done(ArrayList<RssItem> x); }
 public static final String[] NAMES={"Mundo","Deportes","Guatemala","Fútbol"};
 private static final String[] URLS={"https://feeds.bbci.co.uk/mundo/rss.xml","https://www.espn.com/espn/rss/news","https://news.google.com/rss/search?q=Guatemala&hl=es-419&gl=GT&ceid=GT:es-419","https://news.google.com/rss/search?q=futbol&hl=es-419&gl=GT&ceid=GT:es-419"};
 public static void load(final int cat, final Callback cb){ final Handler h=new Handler(Looper.getMainLooper()); new Thread(new Runnable(){public void run(){ArrayList<RssItem> r=new ArrayList<RssItem>(); try{HttpURLConnection c=(HttpURLConnection)new URL(URLS[cat]).openConnection();c.setConnectTimeout(9000);c.setReadTimeout(9000);c.setRequestProperty("User-Agent","NSPLauncher/1.4"); r=parse(c.getInputStream(),NAMES[cat]);c.disconnect();}catch(Exception e){} final ArrayList<RssItem>x=r;h.post(new Runnable(){public void run(){cb.done(x);}});}}).start();}
 private static ArrayList<RssItem> parse(InputStream in,String source)throws Exception{ArrayList<RssItem> out=new ArrayList<RssItem>();XmlPullParser p=Xml.newPullParser();p.setInput(in,"UTF-8");RssItem x=null;String tag=null;for(int e=p.getEventType();e!=XmlPullParser.END_DOCUMENT;e=p.next()){if(e==XmlPullParser.START_TAG){tag=p.getName();if("item".equals(tag))x=new RssItem();}else if(e==XmlPullParser.TEXT&&x!=null){String v=p.getText();if("title".equals(tag))x.title=v;else if("link".equals(tag))x.link=v;else if("description".equals(tag))x.description=v;}else if(e==XmlPullParser.END_TAG&&"item".equals(p.getName())&&x!=null){x.source=source;if(x.title!=null&&x.link!=null)out.add(x);if(out.size()==12)break;x=null;}}in.close();return out;}
}
