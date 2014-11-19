package me.hustwsh.mvoter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.R.string;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.DefaultClientConnection;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public class MainActivity extends Activity {
	public static final String URL_VOTE = "http://gqt-xl.org/More&vote.asp";
	public static final String URL_SHOW_PAGE = "http://gqt-xl.org/Vote_List5.asp?ClassId=44&Topid=0";
	public static final String URL_SHOW_PAGE_REFER = "http://gqt-xl.org/index.asp";
	public static final String URL_AIM_PAGE = "http://gqt-xl.org/Vote_Show.asp?InfoId=48a50a51&ClassId=44&Topid=0";
	public static final String URL_AIM_PAGE_REFER = "http://gqt-xl.org/Vote_List5.asp?ClassId=44&Topid=0";
	public static final String URL_VOTE_REFER= "http://gqt-xl.org/index.asp";
	public static final String USER_AGENT=" Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36";
	public static final int TIME_OUT=3000;
	HttpClient httpClient=null;
	Button btnVote=null;
    Button btnAddHot=null;
    Button btnGetVoteShow=null;
    TextView tvVoteCount=null; 
    TextView tvHotCount=null;
    TextView tvRank=null;
    EditText etMsg=null;
    static Handler uiHandler=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnVote=(Button)findViewById(R.id.btnVote);
        btnAddHot=(Button)findViewById(R.id.btnAddHot);
        btnGetVoteShow=(Button)findViewById(R.id.btnGetVoteShow);
        tvVoteCount=(TextView)findViewById(R.id.tvVoteCount);
        tvHotCount=(TextView)findViewById(R.id.tvHotCount);
        tvRank=(TextView)findViewById(R.id.tvRank);
        etMsg=(EditText)findViewById(R.id.etMsg);
        
        HttpParams httpParams=new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, TIME_OUT);
		HttpConnectionParams.setSoTimeout(httpParams, TIME_OUT);
		httpClient=new DefaultHttpClient(httpParams);
        //界面更新
        uiHandler=new Handler()//Todo
        {
        	@Override
        	public void handleMessage(Message msg)
        	{
        		switch(msg.what)
        		{
        			case 0x01:
        				etMsg.append("\r\n"+msg.getData().getString("msg"));			
        				break;
        		}
        	}
        };
        //投票
        btnVote.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// TODO Auto-generated method stub
				Toast.makeText(MainActivity.this, "正在投票，请稍后...",Toast.LENGTH_SHORT).show();
//				btnVote.setEnabled(false);				
				new Thread()
				{
					@Override
					public void run()
					{
						VoteOnce();
					}
				}.start();
//				VoteOnce();
			}
		});
        //刷人气
        btnAddHot.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				// TODO Auto-generated method stub
				Toast.makeText(MainActivity.this, "正在刷人气，请稍后...",Toast.LENGTH_SHORT).show();
				new Thread()
				{
					@Override
					public void run()
					{
						//AddHot();
						AddHotNew();
					}
				}.start();
			}
		});
        //获取最新投票信息
        btnGetVoteShow.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				// TODO Auto-generated method stub
				new Thread()
				{
					@Override
					public void run()
					{
						GetVoteShow();
					}
				}.start();
			}
		});
    }
    //投票
    private void VoteOnce()	{
    		
		try
		{
			HttpPost post=new HttpPost(URL_VOTE);
			post.addHeader("Referer", URL_VOTE_REFER);
			post.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			post.addHeader("Connection","Keep-Alive");
			post.addHeader("Content-Type","application/x-www-form-urlencoded");
			post.addHeader("Host","gqt-xl.org");
			post.addHeader("Accept-Encoding","gzip,deflate,sdch");
			post.addHeader("Accept-Language","zh-CN");
			post.addHeader("User-Agent",USER_AGENT);	
			List<NameValuePair> params=new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("VoTeid", "320"));
			params.add(new BasicNameValuePair("Submit", "提交投票"));
			post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			HttpResponse httpResponse=httpClient.execute(post);			
			if(httpResponse.getStatusLine().getStatusCode()==200)
			{
				String msg=EntityUtils.toString(httpResponse.getEntity());
				if(msg.contains("投票成功"))
				{
					OutMsg("投票成功!");
				}
				else if(msg.contains("小时"))
				{
					OutMsg("24小时内只允许投一票!");
				}
			}
			else {
//				Toast.makeText(MainActivity.this, "刷票失败!", Toast.LENGTH_SHORT).show();
				OutMsg("刷票失败!");
			}
			
		}
		catch (Exception e)
		{
			// TODO: handle exception
			e.printStackTrace();
			OutMsg("刷票失败!"+e.getCause());
//			Toast.makeText(MainActivity.this, "刷票失败!", Toast.LENGTH_SHORT).show();
		}
	}
    //刷人气，使用httpget实现
    private void AddHotNew()
	{
		HttpGet hg=new HttpGet(URL_AIM_PAGE);
		hg.addHeader("Referer", URL_AIM_PAGE_REFER);
		hg.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		hg.addHeader("Connection","Keep-Alive");
		hg.addHeader("Host","gqt-xl.org");
		hg.addHeader("Accept-Encoding","gzip,deflate,sdch");
		hg.addHeader("Accept-Language","zh-CN,zh;q=0.8");
		hg.addHeader("User-Agent",USER_AGENT);
		
		try
		{
			HttpResponse httpResponse=httpClient.execute(hg);
			if(httpResponse.getStatusLine().getStatusCode()==200)
			{
//				Toast.makeText(MainActivity.this, "刷票成功!", Toast.LENGTH_SHORT).show();
				OutMsg("刷人气成功啦!");
			}
			else {
//				Toast.makeText(MainActivity.this, "刷票失败!", Toast.LENGTH_SHORT).show();
				OutMsg("刷人气失败!");
			}
			
		}
		catch (Exception e)
		{
			// TODO: handle exception
			e.printStackTrace();
			OutMsg("刷人气失败!"+e.getLocalizedMessage());
//			Toast.makeText(MainActivity.this, "刷票失败!", Toast.LENGTH_SHORT).show();
		}
	}
    //刷人气 obselete
    private void  AddHot()
	{
    	URL realUrl=null;
    	HttpURLConnection conn=null;
		try
		{
			realUrl = new URL(URL_AIM_PAGE);
			conn = (HttpURLConnection)realUrl.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(TIME_OUT);
			conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			conn.setRequestProperty("Connection","Keep-Alive");
			conn.setRequestProperty("Host","gqt-xl.org");
			conn.setRequestProperty("Accept-Encoding","gzip,deflate,sdch");
			conn.setRequestProperty("Accept-Language","zh-CN,zh;q=0.8");
			conn.setRequestProperty("User-Agent",USER_AGENT);
			conn.setRequestProperty("Referer",URL_AIM_PAGE_REFER);
			if(conn.getResponseCode()==200)
			{
				try
				{
					InputStream stream=conn.getInputStream();
					InputStreamReader streamReader=new InputStreamReader(stream, "gb2312");
					BufferedReader br=new BufferedReader(streamReader);
					while((br.readLine())!=null);
					br.close();
					streamReader.close();
					stream.close();
					OutMsg("刷人气成功!");
				}
				catch (Exception e)
				{
					// TODO: handle exception
					OutMsg("刷人气失败!"+e.getMessage());
				}
			}
			else {
				OutMsg("无法访问网站!");
			}
			
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
//			OutMsg("Get请求异常:"+e.getMessage());
			Toast.makeText(MainActivity.this, "Get请求异常:"+e.getMessage(),Toast.LENGTH_SHORT).show();
		}
    	finally
    	{
    		if(conn!=null)
    			conn.disconnect();
    	}   	
	}
    //获取投票信息
    private void GetVoteShow()
	{
		String htmlStr=GetVoteShowHtml();
//		OutMsg(htmlStr);
		if(htmlStr!=null)
		{
			Document doc = Jsoup.parse(htmlStr);
//			Elements el=doc.getElementsByClass("MainRight");
//			OutMsg(el.outerHtml());
			Elements elements=doc.select(":containsOwn(北大方正)");
			String keystr=elements.first().select("font").first().text();
			keystr=keystr.replace("人气：",":");
			String[] strings=keystr.split(":");
			final String voteCount=strings[0].trim();
			final String hotCount=strings[1].trim();
			Element elTbody=doc.select(".MainRight").last().parent().parent();//获取到了排名榜的tbody
			Element elTable=elTbody.select("table>tbody").last();//得到了包含排名的具体table
			Elements elLinks=elTable.select("a");
			for (int i = 0; i < elLinks.size(); i++)
			{
				if(elLinks.get(i).text().contains("北大方正"))
				{
					final int rank=i+1;
					tvRank.post(new Runnable()
					{
						@Override
						public void run()
						{
							// TODO Auto-generated method stub
							tvRank.setText(Integer.toString(rank));
						}
					});
					break;
				}
			}
			final String formstr=elTable.text();
			OutMsg(formstr);
			tvVoteCount.post(new Runnable()
			{
				
				@Override
				public void run()
				{
					// TODO Auto-generated method stub
					tvVoteCount.setText(voteCount);
				}
			});
			tvHotCount.post(new Runnable()
			{
				
				@Override
				public void run()
				{
					// TODO Auto-generated method stub
					tvHotCount.setText(hotCount);
				}
			});
		}
	}
    //获取投票页面htmlstr
    private String GetVoteShowHtml()
	{
    	URL realUrl=null;
    	HttpURLConnection conn=null;
    	InputStream stream=null;
    	InputStreamReader streamReader=null;
    	BufferedReader br=null;
    	try
		{
    		realUrl = new URL(URL_SHOW_PAGE);
    		conn = (HttpURLConnection)realUrl.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);
			conn.setRequestProperty("Referer",URL_SHOW_PAGE_REFER);
			String htmlstr="";
			if(conn.getResponseCode()==200)
			{
				stream=conn.getInputStream();
				streamReader=new InputStreamReader(stream, "gb2312");
				br=new BufferedReader(streamReader);
				String line;
				while((line=br.readLine())!=null)
				{
					htmlstr+=line;
				}
				br.close();
				streamReader.close();
				stream.close();
				return htmlstr;
//				OutMsg(htmlstr);
			}
			else {
				return null;
			}
		}
		catch (Exception e)
		{
			// TODO: handle exception			
			return null;
		}
	}
    //获取指定页面htmlstr
    private String GetVoteShowHtml(String url,String urlRefer)
	{
    	URL realUrl=null;
    	HttpURLConnection conn=null;
    	InputStream stream=null;
    	InputStreamReader streamReader=null;
    	BufferedReader br=null;
    	try
		{
    		realUrl = new URL(url);
    		conn = (HttpURLConnection)realUrl.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);
			conn.setRequestProperty("Referer",urlRefer);
			String htmlstr="";
			if(conn.getResponseCode()==200)
			{
				stream=conn.getInputStream();
				streamReader=new InputStreamReader(stream, "gb2312");
				br=new BufferedReader(streamReader);
				String line;
				while((line=br.readLine())!=null)
				{
					htmlstr+=line;
				}
				br.close();
				streamReader.close();
				stream.close();
				return htmlstr;
//				OutMsg(htmlstr);
			}
			else {
				return null;
			}
		}
		catch (Exception e)
		{
			// TODO: handle exception			
			return null;
		}
	}
    //打印消息
    private  void  OutMsg(String str)
    {
    	Bundle data=new Bundle();
    	data.putString("msg", str);
    	Message msg=new Message();
    	msg.setData(data);
    	msg.what=0x01;    	
    	uiHandler.sendMessage(msg);
    }
}
