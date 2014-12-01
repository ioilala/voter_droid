package me.hustwsh.mvoter;
/*
 *Author:hust_wsh
 *Version:0.1.6
 *Date:2014-11-20
 *Note:
 * 实现刷人气；
 * 实现获取排名信息;
 * 实现刷票;
 * 将最近一次获取的投票排行榜信息存到本地，下次启动时读取出来
 * 排名信息室全站的票数排名，要改为只获取对应页面的排名信息;
 * 实现排行榜功能(前五名，保存本地数据)
 * 删除开始的三个数据
 * 修复上一版刷新和保存数据的bug
 * 加入网络判断
 * 改善按钮外观
 * 加入按钮震动
 * 北大方正红色显示
 * 加入友盟统计
 * 友盟自定义事件
 *Todo:
 * 投票历史曲线功能
 * 定时刷票
 * 后台service
 */
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.umeng.analytics.MobclickAgent;
import com.umeng.analytics.social.UMPlatformData;
import com.umeng.analytics.social.UMPlatformData.GENDER;
import com.umeng.analytics.social.UMPlatformData.UMedia;

public class MainActivity extends Activity {
	public static final String URL_VOTE = "http://gqt-xl.org/More&vote.asp";
	public static final String URL_SHOW_PAGE = "http://gqt-xl.org/Vote_List5.asp?ClassId=44&Topid=0";
	public static final String URL_SHOW_PAGE_REFER = "http://gqt-xl.org/index.asp";
	public static final String URL_AIM_PAGE = "http://gqt-xl.org/Vote_Show.asp?InfoId=48a50a51&ClassId=44&Topid=0";
	public static final String URL_AIM_PAGE_REFER = "http://gqt-xl.org/Vote_List5.asp?ClassId=44&Topid=0";
	public static final String URL_VOTE_REFER= "http://gqt-xl.org/index.asp";
	public static final String USER_AGENT=" Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36";
	//sharedpreference 
	public static final String PREF_VOTE_COUNT="votecount";
	public static final String PREF_HOT_COUNT="hotcount";
	public static final String PREF_RANK="rank";
	public static final String PREF_RANK_STR="rankstr";
	
	public static final int TIME_OUT=3000;
	
	HttpClient httpClient=null;
	SharedPreferences preferences=null;
	SharedPreferences.Editor editorPref=null;
//
	Button btnVote=null;
    Button btnAddHot=null;
    Button btnGetVoteShow=null;
    
    static Handler uiHandler=null;
    @Override
    //初始化
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //youmeng
        MobclickAgent.updateOnlineConfig(this);
        MobclickAgent.setDebugMode(true);

        setContentView(R.layout.activity_main);

        preferences=getSharedPreferences("mvoter", MODE_PRIVATE);
        editorPref=preferences.edit();

        btnVote=(Button)findViewById(R.id.btnVote);
        btnAddHot=(Button)findViewById(R.id.btnAddHot);
        btnGetVoteShow=(Button)findViewById(R.id.btnGetVoteShow);
        //恢复本地保存的排行榜数据
        RestoreData();
//        HttpParams httpParams=new BasicHttpParams();
//		HttpConnectionParams.setConnectionTimeout(httpParams, TIME_OUT);
//		HttpConnectionParams.setSoTimeout(httpParams, TIME_OUT);
//
//		httpClient=new DefaultHttpClient(httpParams);
//        httpClient=new DefaultHttpClient();
//        HttpConnectionParams.setConnectionTimeout(httpClient.getParams(),TIME_OUT);
//        HttpConnectionParams.setSoTimeout(httpClient.getParams(),TIME_OUT);
        //初始化httpclient，设置超时
        httpClient=new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,TIME_OUT);
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,TIME_OUT);
        //界面更新
        uiHandler=new Handler()//Todo
        {
        	@Override
        	public void handleMessage(Message msg)
        	{
        		switch(msg.what)
        		{
        			case 1:
//        				etMsg.append("\n"+msg.getData().getString("msg"));
        				break;
                    case 2:
                        Toast.makeText(getApplicationContext(),msg.getData().getString("msg"),Toast.LENGTH_SHORT).show();
                        break;
        		}
        	}
        };
        //检查网络
        if(CheckForNetWork(getApplicationContext()))
        {
            OutMsg("网络已连接:"+GetNetworkType(getApplicationContext()),2);
        }
        else
        {
            OutMsg("当前无网络连接!",2);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        //youmeng
        MobclickAgent.onResume(this);

        //投票
        btnVote.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // TODO Auto-generated method stub
                Vibrate();
                MobclickAgent.onEvent(MainActivity.this, "vote_once");
                if(!CheckForNetWork(getApplicationContext()))
                {
                    OutMsg("当前网络未连接!",2);
                    return;
                }
                Toast.makeText(MainActivity.this, "正在投票，请稍候...",Toast.LENGTH_SHORT).show();
//				btnVote.setEnabled(false);
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        VoteOnce();
                    }
                }.start();
            }
        });
        //刷人气
        btnAddHot.setOnClickListener(new OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                // TODO Auto-generated method stub
                Vibrate();
                //youmeng
                MobclickAgent.onEvent(MainActivity.this, "add_hot");
                if(!CheckForNetWork(getApplicationContext()))
                {
                    OutMsg("当前网络未连接!",2);
                    return;
                }
                Toast.makeText(MainActivity.this, "正在刷人气，请稍候...",Toast.LENGTH_SHORT).show();
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
                Vibrate();
                MobclickAgent.onEvent(MainActivity.this, "refresh_votes");
                if(!CheckForNetWork(getApplicationContext()))
                {
                    OutMsg("当前网络未连接!",2);
                    return;
                }
                OutMsg("正在刷新，请稍候...",2);
                // TODO Auto-generated method stub
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        int index=GetVoteShow();
                        switch (index)
                        {
                            case 0:
                                OutMsg("刷新成功!",2);
                                break;
                            case 1:
                                OutMsg("刷新失败,获取网页信息失败!请检查网络连接!",2);
                                break;
                            case 2:
                                OutMsg("刷新失败,字符串异常!",2);
                                break;
                        }
                    }
                }.start();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        //youmeng
        MobclickAgent.onPause(this);
    }

    //投票
    private void VoteOnce()
    {
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
					OutMsg("投票成功!",2);
				}
				else if(msg.contains("小时"))
				{
					OutMsg("24小时内只允许投一票!",2);
				}
			}
			else {
//				Toast.makeText(MainActivity.this, "刷票失败", Toast.LENGTH_SHORT).show();
				OutMsg("投票失败,访问网站失败!",2);
			}
			
		}
		catch (Exception e)
		{
			// TODO: handle exception
			e.printStackTrace();
			OutMsg("投票失败!"+e.getMessage()+e.getCause(),2);

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
				OutMsg("刷人气成功!",2);
			}
			else {
//				Toast.makeText(MainActivity.this, "刷票失败!", Toast.LENGTH_SHORT).show();
				OutMsg("刷人气失败!",2);
			}
			
		}
		catch (Exception e)
		{
			// TODO: handle exception
			e.printStackTrace();
			OutMsg("刷人气失败!"+e.getCause(),2);
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
					OutMsg("刷人气成功!",2);
				}
				catch (Exception e)
				{
					// TODO: handle exception
					OutMsg("刷人气失败!"+e.getCause(),2);
				}
			}
			else {
				OutMsg("无法访问网站!",2);
			}
			
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			OutMsg("Get请求异常"+e.getCause(),2);
//			Toast.makeText(MainActivity.this, "Get请求异常"+e.getMessage(),Toast.LENGTH_SHORT).show();
		}
    	finally
    	{
    		if(conn!=null)
    			conn.disconnect();
    	}   	
	}
    //刷新，获取投票信息
    private int GetVoteShow()
	{
        int result=-1;
    	try
		{
    		String htmlStr=GetVoteShowHtml();
//    		OutMsg(htmlStr);
    		if(htmlStr!=null&&htmlStr!="")
    		{
    			Document doc = Jsoup.parse(htmlStr);
    			Element elementForm=doc.getElementById("form1");
    			Element elementTableElements=elementForm.parents().select("table").first();
    			String srcStr=elementTableElements.text();
                srcStr=srcStr.replaceAll("\\s*","");
                SetRankTableFromStr(srcStr);//解析字符串得到排行榜
                result=0;
    		}
            else
            {
//                OutMsg("获取网页信息失败！",2);
                result=1;
            }
		}
		catch (Exception e)
		{
			// TODO: handle exception
//			OutMsg(e.getMessage()+e.getCause(),2);
            result=2;
		}
        return result;
	}
    //根据得到的字符串设置排行榜
    private void SetRankTableFromStr(String srcStr) {
        String[] srcStrArray=srcStr.split("：");
        int j=0;
        List<TextView> tvListName=new ArrayList<TextView>();
        tvListName.add((TextView) findViewById(R.id.tvRankName1));
        tvListName.add((TextView) findViewById(R.id.tvRankName2));
        tvListName.add((TextView) findViewById(R.id.tvRankName3));
        tvListName.add((TextView) findViewById(R.id.tvRankName4));
        tvListName.add((TextView) findViewById(R.id.tvRankName5));
        List<TextView> tvListVoteCount=new ArrayList<TextView>();
        tvListVoteCount.add((TextView) findViewById(R.id.tvRankVote1));
        tvListVoteCount.add((TextView) findViewById(R.id.tvRankVote2));
        tvListVoteCount.add((TextView) findViewById(R.id.tvRankVote3));
        tvListVoteCount.add((TextView) findViewById(R.id.tvRankVote4));
        tvListVoteCount.add((TextView) findViewById(R.id.tvRankVote5));
        List<TextView> tvListHotCount=new ArrayList<TextView>();
        tvListHotCount.add((TextView) findViewById(R.id.tvRankHot1));
        tvListHotCount.add((TextView) findViewById(R.id.tvRankHot2));
        tvListHotCount.add((TextView) findViewById(R.id.tvRankHot3));
        tvListHotCount.add((TextView) findViewById(R.id.tvRankHot4));
        tvListHotCount.add((TextView) findViewById(R.id.tvRankHot5));
        for(int i=0;i<srcStrArray.length;i+=2)
        {
            final String name=GetStrNotNum(srcStrArray[i]).replace("票数","");
            final int votecount=GetIntFromStr(srcStrArray[i+1]);
            final int hotcount=GetIntFromStr(srcStrArray[i+2]);
            final TextView tvName=tvListName.get(j);
            final TextView tvVoteCount=tvListVoteCount.get(j);
            final TextView tvHotCount=tvListHotCount.get(j);
            if(name.contains("北大方正"))
            {
                StoreData(votecount,hotcount,j+1,srcStr);
                tvName.post(new Runnable() {
                    @Override
                    public void run() {
                        tvName.setTextColor(Color.RED);
                        tvVoteCount.setTextColor(Color.RED);
                        tvHotCount.setTextColor(Color.RED);
                        tvName.setText(name);
                        tvVoteCount.setText(""+votecount);
                        tvHotCount.setText(""+hotcount);

                    }
                });
            }
            else
            {
                tvName.post(new Runnable() {
                    @Override
                    public void run() {
                        tvName.setTextColor(Color.BLACK);
                        tvVoteCount.setTextColor(Color.BLACK);
                        tvHotCount.setTextColor(Color.BLACK);
                        tvName.setText(name);
                        tvVoteCount.setText(""+votecount);
                        tvHotCount.setText(""+hotcount);

                    }
                });
            }
            j++;
            if(j>4)
            {
                break;
            }
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
				return "";
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
    private void OutMsg(String str,int what)
    {
        Bundle data=new Bundle();
        data.putString("msg", str);
        Message msg=new Message();
        msg.setData(data);
        msg.what=what;
        uiHandler.sendMessage(msg);
    }
    //把投票信息存到本地
    private void StoreData(int votecount,int hotcount,int rank,String rankstr)
	{
    	editorPref.putInt(PREF_VOTE_COUNT, votecount);
    	editorPref.putInt(PREF_HOT_COUNT, hotcount);
    	editorPref.putInt(PREF_RANK, rank);
    	editorPref.putString(PREF_RANK_STR, rankstr);
    	editorPref.commit();
	}
    //恢复本地信息
    private void RestoreData()
	{
		final int votecount=preferences.getInt(PREF_VOTE_COUNT,0);
		final int hotcount=preferences.getInt(PREF_HOT_COUNT, 0);
		final int rank=preferences.getInt(PREF_RANK, 0);
		final String rankstr=preferences.getString(PREF_RANK_STR, "");
		RelativeLayout rl=(RelativeLayout)findViewById(R.id.reltiveLayoutMain);
        if(rankstr!="")
        {
            SetRankTableFromStr(rankstr);
        }
	}
    //获取正确的整数
    private int GetIntFromStr(String str)
    {
    	try
		{
    		char[] chars=str.toCharArray();
        	int i=0;
        	for(i=0;i<chars.length;i++)
    		{
        		if(!Character.isDigit(chars[i]))
        		{
        			break;
        		}
    		}
        	str=str.substring(0,i);
        	return Integer.valueOf(str);
		}
		catch (Exception e)
		{
			// TODO: handle exception
			return -1;
		}	
    }
    //返回非数字的子字符串
    private String GetStrNotNum(String str)
    {
        int i=0;
        char[] tem=str.toCharArray();
        while(Character.isDigit(tem[i]))
        {
            i++;
        }
        String fiStr=str.substring(i,str.length());
        return fiStr;
    }
    //检查是否有网络
    private boolean CheckForNetWork(Context context)
    {
        try
        {
            if(context!=null)
            {
                ConnectivityManager cm=(ConnectivityManager)context.getSystemService(context.CONNECTIVITY_SERVICE);
                if(cm!=null)
                {
                    NetworkInfo ni=cm.getActiveNetworkInfo();
                    if(ni!=null&&ni.isAvailable())
                    {
                        return true;
                    }
                }
                return false;
            }
            return false;
        }
        catch(Exception ex)
        {
            OutMsg("检查网络异常:"+ex.getMessage(),2);
            return false;
        }
    }
    //获取当前网络连接类型
    private String GetNetworkType(Context contxt)
    {
        try
        {
            if(contxt!=null)
            {
                ConnectivityManager cm=(ConnectivityManager)contxt.getSystemService(contxt.CONNECTIVITY_SERVICE);
                if(cm!=null)
                {
                    NetworkInfo ni=cm.getActiveNetworkInfo();
                    if(ni!=null&&ni.isAvailable())
                    {
                        return ni.getTypeName();
                    }
                }
            }
            return null;
        }
        catch(Exception ex)
        {
            OutMsg("获取网络类型异常:"+ex.getMessage(),2);
            return null;
        }
    }
    //设置网络
    private void CheckNetWorkAndSet()
    {
        if(!CheckForNetWork(getApplicationContext()))
        {
            OutMsg("当前网络未连接!",2);
        }
    }
    //震动
    private void Vibrate()
    {
        try
        {
            Vibrator vb=(Vibrator)getSystemService(Service.VIBRATOR_SERVICE);
            vb.vibrate(40);
//            vb.cancel();
        }
        catch(Exception ex)
        {

        }
    }
}
